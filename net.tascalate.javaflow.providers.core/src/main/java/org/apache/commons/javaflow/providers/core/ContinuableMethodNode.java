/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2022 Valery Silaev (http://vsilaev.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.javaflow.providers.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tascalate.asmx.Handle;
import net.tascalate.asmx.Label;
import net.tascalate.asmx.MethodVisitor;
import net.tascalate.asmx.Opcodes;
import net.tascalate.asmx.Type;
import net.tascalate.asmx.plus.ClassHierarchy;
import net.tascalate.asmx.tree.AbstractInsnNode;
import net.tascalate.asmx.tree.AnnotationNode;
import net.tascalate.asmx.tree.InsnList;
import net.tascalate.asmx.tree.InsnNode;
import net.tascalate.asmx.tree.InvokeDynamicInsnNode;
import net.tascalate.asmx.tree.LabelNode;
import net.tascalate.asmx.tree.LocalVariableAnnotationNode;
import net.tascalate.asmx.tree.MethodInsnNode;
import net.tascalate.asmx.tree.MethodNode;
import net.tascalate.asmx.tree.VarInsnNode;
import net.tascalate.asmx.tree.analysis.Analyzer;
import net.tascalate.asmx.tree.analysis.AnalyzerException;
import net.tascalate.asmx.tree.analysis.BasicValue;
import net.tascalate.asmx.tree.analysis.Frame;
import net.tascalate.asmx.tree.analysis.SourceInterpreter;
import net.tascalate.asmx.tree.analysis.SourceValue;

class ContinuableMethodNode extends MethodNode implements Opcodes {
    private final ClassHierarchy classHierarchy;
    private final ContinuableClassInfoResolver cciResolver;
    private final String className;

    final MethodVisitor mv;

    final List<Label>            labels  = new ArrayList<Label>();
    final List<AbstractInsnNode> nodes   = new ArrayList<AbstractInsnNode>();
    final List<MethodInsnNode>   methods = new ArrayList<MethodInsnNode>();

    private Analyzer<BasicValue> analyzer;
    int stackRecorderVar;

    ContinuableMethodNode(int api, int access, String name, String desc, String signature, String[] exceptions, 
                          String className, 
                          ClassHierarchy classHierarchy, 
                          ContinuableClassInfoResolver cciResolver, 
                          MethodVisitor mv) {
        
        super(api, access, name, desc, signature, exceptions);
        this.className = className;
        this.classHierarchy = classHierarchy;
        this.cciResolver = cciResolver;
        this.mv = mv;
    }

    // Bug in rev 1632 (Refactoring to remove redundant code (and for easier subclassing).
    @Override
    protected LabelNode getLabelNode(Label l) {
        if (!(l.info instanceof LabelNode)) {
            l.info = new LabelNode(l); // error was here -- new LabelNode(/* nothing */);
        }
        return (LabelNode)l.info;
    }

    Frame<BasicValue> getFrameByNode(AbstractInsnNode node) {
        int insIndex = instructions.indexOf(node);
        Frame<BasicValue>[] frames = analyzer.getFrames();
        return null == frames || insIndex >= frames.length ? null : frames[insIndex];
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean intf) {
        MethodInsnNode mnode = new MethodInsnNode(opcode, owner, name, desc, intf);
        if (opcode == INVOKESPECIAL || "<init>".equals(name)) {
            methods.add(mnode);
        }
        if (needsFrameGuard(opcode, owner, name, desc)) {
            Label label = new Label();
            super.visitLabel(label);
            labels.add(label);
            nodes.add(mnode);
        }
        instructions.add(mnode);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        visitMethodInsn(opcode, owner, name, desc, opcode == Opcodes.INVOKEINTERFACE);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        InvokeDynamicInsnNode mnode = new InvokeDynamicInsnNode(name, desc, bsm, bsmArgs);
        if (needsFrameGuard(INVOKEDYNAMIC, bsm.getOwner(), name, desc)) {
            Label label = new Label();
            super.visitLabel(label);
            labels.add(label);
            nodes.add(mnode);
        }
        instructions.add(mnode);
    }

    @Override
    public void visitEnd() {

        checkCallSites();

        if (instructions.size() == 0 || labels.size() == 0) {
            accept(mv);
            return;
        }

        this.stackRecorderVar = maxLocals;
        try {
            moveNew();

            analyzer = new Analyzer<BasicValue>(new FastClassVerifier(this.api, classHierarchy)) {
                @Override
                protected Frame<BasicValue> newFrame(int nLocals, int nStack) {
                    return new MonitoringFrame<BasicValue>(nLocals, nStack);
                }

                @Override
                protected Frame<BasicValue> newFrame(Frame<? extends BasicValue> src) {
                    return new MonitoringFrame<BasicValue>(src);
                }
            };

            analyzer.analyze(className, this);
            accept(new ContinuableMethodVisitor(this.api, this));

        } catch (AnalyzerException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void checkCallSites() {
        List<LocalVariableAnnotationNode> varAnnotations = new ArrayList<LocalVariableAnnotationNode>();
        Map<Integer, List<AnnotationNode>> paramAnnotations = new HashMap<Integer, List<AnnotationNode>>();

        varAnnotations.addAll(CallSiteFinder.annotationsList(visibleLocalVariableAnnotations));
        varAnnotations.addAll(CallSiteFinder.annotationsList(invisibleLocalVariableAnnotations));
        paramAnnotations.putAll(CallSiteFinder.annotationsList(visibleParameterAnnotations));
        paramAnnotations.putAll(CallSiteFinder.annotationsList(invisibleParameterAnnotations));

        List<CallSiteFinder.Result> results = new CallSiteFinder().findMatchingCallSites(instructions, varAnnotations, paramAnnotations);
        for (CallSiteFinder.Result result : results) {
            if (!nodes.contains(result.methodCall) && checkContinuableAnnotation(result.annotations)) {
                Label label = new Label();
                instructions.insertBefore(result.methodCall, getLabelNode(label));
                int insertionIndex = findCallSiteInvocationInsertionIndex(result.methodCall);

                if (insertionIndex < 0) {
                    labels.add(label);
                    nodes.add(result.methodCall);
                } else {
                    labels.add(insertionIndex, label);
                    nodes.add(insertionIndex, result.methodCall);
                }
            }
        }
    }

    private boolean checkContinuableAnnotation(Collection<String> annotationDescriptors) {
        for (String annotationDescriptor : annotationDescriptors) {
            if (cciResolver.isContinuableAnnotation(annotationDescriptor)) {
                return true;
            }
        }
        return false;
    }

    private int findCallSiteInvocationInsertionIndex(MethodInsnNode mnode) {
        int inSelected = -1;
        for (AbstractInsnNode otherIns = mnode; otherIns != null && inSelected < 0; ) {
            otherIns = otherIns.getNext();
            if (otherIns instanceof MethodInsnNode || otherIns instanceof InvokeDynamicInsnNode) {
                inSelected = nodes.indexOf(otherIns);
            }
        }
        return inSelected;
    }

    private void moveNew() throws AnalyzerException {
        SourceInterpreter i = new SourceInterpreter();
        Analyzer<SourceValue> a = new Analyzer<SourceValue>(i);
        a.analyze(className, this);

        HashMap<AbstractInsnNode, MethodInsnNode> movable = new HashMap<AbstractInsnNode, MethodInsnNode>();

        Frame<SourceValue>[] frames = a.getFrames();
        for (int j = 0; j < methods.size(); j++) {
            MethodInsnNode mnode = (MethodInsnNode) methods.get(j);
            // require to move NEW instruction
            int n = instructions.indexOf(mnode);
            Frame<SourceValue> f = frames[n];
            Type[] args = Type.getArgumentTypes(mnode.desc);

            SourceValue v = (SourceValue) f.getStack(f.getStackSize() - args.length - 1);
            Set<AbstractInsnNode> insns = v.insns;
            for (AbstractInsnNode ins : insns) {
                if (ins.getOpcode() == NEW) {
                    movable.put(ins, mnode);
                } else {
                    // other known patterns
                    int n1 = instructions.indexOf(ins);
                    if (ins.getOpcode() == DUP) { // <init> with params
                        AbstractInsnNode ins1 = instructions.get(n1 - 1);
                        if (ins1.getOpcode() == NEW) {
                            movable.put(ins1, mnode);
                        }
                    } else if (ins.getOpcode() == SWAP) {  // in exception handler
                        AbstractInsnNode ins1 = instructions.get(n1 - 1);
                        AbstractInsnNode ins2 = instructions.get(n1 - 2);
                        if (ins1.getOpcode() == DUP_X1 && ins2.getOpcode() == NEW) {
                            movable.put(ins2, mnode);
                        }
                    }
                }
            }
        }

        int updateMaxStack = 0;
        for (Map.Entry<AbstractInsnNode, MethodInsnNode> e : movable.entrySet()) {
            AbstractInsnNode node1 = e.getKey();
            int n1 = instructions.indexOf(node1);
            AbstractInsnNode node2 = instructions.get(n1 + 1);
            AbstractInsnNode node3 = instructions.get(n1 + 2);
            int producer = node2.getOpcode();

            instructions.remove(node1);  // NEW
            boolean requireDup = false;
            if (producer == DUP) {
                instructions.remove(node2);  // DUP
                requireDup = true;
            } else if (producer == DUP_X1) {
                instructions.remove(node2);  // DUP_X1
                instructions.remove(node3);  // SWAP
                requireDup = true;
            }

            MethodInsnNode mnode = (MethodInsnNode) e.getValue();
            AbstractInsnNode nm = mnode;

            int varOffset = stackRecorderVar + 1;
            Type[] args = Type.getArgumentTypes(mnode.desc);


            // optimizations for some common cases
            if (args.length == 0) {
                InsnList doNew = new InsnList();
                doNew.add(node1); // NEW
                if (requireDup)
                    doNew.add(new InsnNode(DUP));
                instructions.insertBefore(nm, doNew);
                nm = doNew.getLast();  
                continue;
            }

            if (args.length == 1 && args[0].getSize() == 1) {
                InsnList doNew = new InsnList();
                doNew.add(node1); // NEW
                if (requireDup) {
                    doNew.add(new InsnNode(DUP));
                    doNew.add(new InsnNode(DUP2_X1));
                    doNew.add(new InsnNode(POP2));
                    updateMaxStack = updateMaxStack < 2 ? 2 : updateMaxStack; // a two extra slots for temp values
                } else 
                    doNew.add(new InsnNode(SWAP));
                instructions.insertBefore(nm, doNew);
                nm = doNew.getLast();    
                continue;
            }


            // TODO this one untested!
            if ((args.length == 1 && args[0].getSize() == 2) ||
                    (args.length == 2 && args[0].getSize() == 1 && args[1].getSize() == 1)) {
                InsnList doNew = new InsnList();
                doNew.add(node1); // NEW
                if (requireDup) {
                    doNew.add(new InsnNode(DUP));
                    doNew.add(new InsnNode(DUP2_X2));
                    doNew.add(new InsnNode(POP2));
                    updateMaxStack = updateMaxStack < 2 ? 2 : updateMaxStack; // a two extra slots for temp values
                } else {
                    doNew.add(new InsnNode(DUP_X2));
                    doNew.add(new InsnNode(POP));
                    updateMaxStack = updateMaxStack < 1 ? 1 : updateMaxStack; // an extra slot for temp value
                }
                instructions.insertBefore(nm, doNew);
                nm = doNew.getLast();    
                continue;
            }

            InsnList doNew = new InsnList();
            // generic code using temporary locals
            // save stack
            for (int j = args.length - 1; j >= 0; j--) {
                Type type = args[j];

                doNew.add(new VarInsnNode(type.getOpcode(ISTORE), varOffset));
                varOffset += type.getSize();
            }
            if (varOffset > maxLocals) {
                maxLocals = varOffset;
            }

            doNew.add(node1); // NEW

            if (requireDup)
                doNew.add(new InsnNode(DUP));

            // restore stack
            for (int j = 0; j < args.length; j++) {
                Type type = args[j];
                varOffset -= type.getSize();

                doNew.add(new VarInsnNode(type.getOpcode(ILOAD), varOffset));

                // clean up store to avoid memory leak?
                if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                    updateMaxStack = updateMaxStack < 1 ? 1 : updateMaxStack; // an extra slot for ACONST_NULL

                    doNew.add(new InsnNode(ACONST_NULL));

                    doNew.add(new VarInsnNode(type.getOpcode(ISTORE), varOffset));
                }
            }
            instructions.insertBefore(nm, doNew);
            nm = doNew.getLast(); 
        }

        maxStack += updateMaxStack;
    }

    private boolean needsFrameGuard(int opcode, String owner, String name, String desc) {
        if (owner.startsWith("java/") || owner.startsWith("javax/")) {
            //System.out.println("SKIP:: " + owner + "." + name + desc);
            return false;
        }

        // Always create save-point before Continuation methods (like suspend)
        if (CONTINUATION_CLASS_INTERNAL_NAME.equals(owner)) {
            return CONTINUATION_CLASS_CONTINUABLE_METHODS.contains(name);
        }

        // No need to create save-point before constructors -- it's forbidden to suspend in constructors anyway
        if (opcode == Opcodes.INVOKESPECIAL && "<init>".equals(name)) {
            return false;
        }

        if (opcode == Opcodes.INVOKEDYNAMIC) {
            // TODO verify CallSite to be continuable?
            return true;
        }

        if (opcode == Opcodes.INVOKEINTERFACE ||
            opcode == Opcodes.INVOKESPECIAL   ||
            opcode == Opcodes.INVOKESTATIC    ||
            opcode == Opcodes.INVOKEVIRTUAL) {
            ContinuableClassInfo classInfo;
            try {
                classInfo = cciResolver.resolve(owner);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            return null != classInfo && classInfo.isContinuableMethod(opcode, name, desc, desc);
        }
        return false;
    }

    private static final String CONTINUATION_CLASS_INTERNAL_NAME = "org/apache/commons/javaflow/api/Continuation";
    private static final Set<String> CONTINUATION_CLASS_CONTINUABLE_METHODS = new HashSet<String>(Arrays.asList(
        "suspend", "again", "cancel" 
        // we are suspending here with potential resume later
        // "startWith", "continueWith", "exit" are unnecessary
    ));
}