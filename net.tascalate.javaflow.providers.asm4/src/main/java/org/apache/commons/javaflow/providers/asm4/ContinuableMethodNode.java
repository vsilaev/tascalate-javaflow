/**
 * ﻿Original work: copyright 1999-2004 The Apache Software Foundation
 * (http://www.apache.org/)
 *
 * This project is based on the work licensed to the Apache Software
 * Foundation (ASF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Modified work: copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.providers.asm4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.javaflow.api.Continuation;
import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

public class ContinuableMethodNode extends MethodNode implements Opcodes {
    private final ContinuableClassInfoResolver cciResolver;
    private final String className;

    protected final MethodVisitor mv;

    protected final List<Label>            labels  = new ArrayList<Label>();
    protected final List<AbstractInsnNode> nodes   = new ArrayList<AbstractInsnNode>();
    protected final List<MethodInsnNode>   methods = new ArrayList<MethodInsnNode>();

    protected Analyzer analyzer;
    public int stackRecorderVar;

    public ContinuableMethodNode(int access, String name, String desc, String signature, String[] exceptions, String className, ContinuableClassInfoResolver cciResolver, MethodVisitor mv) {
        super(Opcodes.ASM4, access, name, desc, signature, exceptions);
        this.className = className;
        this.cciResolver = cciResolver;
        this.mv = mv;
    }

    // Bug in rev 1632 (Refactoring to remove redundant code (and for easier subclassing).
    protected LabelNode getLabelNode(final Label l) {
        if (!(l.info instanceof LabelNode)) {
            l.info = new LabelNode(l); // error was here -- new LabelNode(/* nothing */);
        }
        return (LabelNode)l.info;
    }

    Frame getFrameByNode(AbstractInsnNode node) {
        final int insIndex = instructions.indexOf(node);
        final Frame[] frames = analyzer.getFrames();
        return null == frames || insIndex >= frames.length ? null : frames[insIndex];
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        MethodInsnNode mnode = new MethodInsnNode(opcode, owner, name, desc);
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

    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        final InvokeDynamicInsnNode mnode = new InvokeDynamicInsnNode(name, desc, bsm, bsmArgs);
        if (needsFrameGuard(INVOKEDYNAMIC, bsm.getOwner(), name, desc)) {
            Label label = new Label();
            super.visitLabel(label);
            labels.add(label);
            nodes.add(mnode);
        }
        instructions.add(mnode);
    }

    public void visitEnd() {
        if (instructions.size() == 0 || labels.size() == 0) {
            accept(mv);
            return;
        }

        this.stackRecorderVar = maxLocals;
        try {
            moveNew();

            analyzer = new Analyzer(new FastClassVerifier()) {
                @Override
                protected Frame newFrame(final int nLocals, final int nStack) {
                    return new MonitoringFrame(nLocals, nStack);
                }

                @Override
                protected Frame newFrame(final Frame src) {
                    return new MonitoringFrame(src);
                }
            };

            analyzer.analyze(className, this);
            accept(new ContinuableMethodVisitor(this));

        } catch (final AnalyzerException ex) {
            throw new RuntimeException(ex);
        }
    }

    void moveNew() throws AnalyzerException {
        final SourceInterpreter i = new SourceInterpreter();
        final Analyzer a = new Analyzer(i);
        a.analyze(className, this);

        final HashMap<AbstractInsnNode, MethodInsnNode> movable = new HashMap<AbstractInsnNode, MethodInsnNode>();

        final Frame[] frames = a.getFrames();
        for (int j = 0; j < methods.size(); j++) {
            final MethodInsnNode mnode = (MethodInsnNode) methods.get(j);
            // require to move NEW instruction
            int n = instructions.indexOf(mnode);
            Frame f = frames[n];
            Type[] args = Type.getArgumentTypes(mnode.desc);

            SourceValue v = (SourceValue) f.getStack(f.getStackSize() - args.length - 1);
            @SuppressWarnings("unchecked")
            Set<AbstractInsnNode> insns = v.insns;
            for (final AbstractInsnNode ins : insns) {
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
        for (final Map.Entry<AbstractInsnNode, MethodInsnNode> e : movable.entrySet()) {
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
                final InsnList doNew = new InsnList();
                doNew.add(node1); // NEW
                if (requireDup)
                    doNew.add(new InsnNode(DUP));
                instructions.insertBefore(nm, doNew);
                nm = doNew.getLast();  
                continue;
            }

            if (args.length == 1 && args[0].getSize() == 1) {
                final InsnList doNew = new InsnList();
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
                final InsnList doNew = new InsnList();
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

            final InsnList doNew = new InsnList();
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

    boolean needsFrameGuard(int opcode, String owner, String name, String desc) {
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
            final ContinuableClassInfo classInfo;
            try {
                classInfo = cciResolver.resolve(owner);
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
            return null != classInfo && classInfo.isContinuableMethod(opcode, name, desc, desc);
        }
        return false;
    }

    final private static String CONTINUATION_CLASS_INTERNAL_NAME = Type.getInternalName(Continuation.class);
    final private static Set<String> CONTINUATION_CLASS_CONTINUABLE_METHODS = new HashSet<String>(Arrays.asList(
            "suspend", "again", "cancel" 
            // we are suspending here with potential resume later
            // "startWith", "continueWith", "exit" are unnecessary
            ));
}