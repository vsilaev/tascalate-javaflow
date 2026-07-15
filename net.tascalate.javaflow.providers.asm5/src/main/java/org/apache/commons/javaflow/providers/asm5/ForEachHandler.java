/**
 * ﻿Copyright 2013-2022 Valery Silaev (http://vsilaev.com)
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
package org.apache.commons.javaflow.providers.asm5;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.objectweb.asm.Type;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

class ForEachHandler {
    
    private static final int LOCAL_VARIABLE_TYPE_REF = TypeReference.newTypeReference(TypeReference.LOCAL_VARIABLE).getValue();
    
    private static final Type ITERATOR_TYPE   = Type.getObjectType("java/util/Iterator");
    private static final Type ITERABLE_TYPE   = Type.getObjectType("java/lang/Iterable");
    
    private final int api;
    private final MethodNode owner;
    private final ClassHierarchy classHierarchy;
    private final ContinuableClassInfoResolver cciResolver;
    
    ForEachHandler(int api, MethodNode owner, ClassHierarchy classHierarchy, ContinuableClassInfoResolver cciResolver) {
        this.api = api;
        this.owner = owner;
        this.classHierarchy = classHierarchy;
        this.cciResolver = cciResolver;
    }
    
    void liftForEachVars() {
        InsnList instructions = owner.instructions;
        for (Iterator<AbstractInsnNode> i = instructions.iterator(); i.hasNext(); ) {
            AbstractInsnNode ins = i.next();
            if (!(ins instanceof MethodInsnNode)) {
                continue;
            }
            
            MethodInsnNode methodInsn = (MethodInsnNode)ins;

            // methodInsn.owner may be of concrete subtype, 
            // however methodInsn.desc MIGHT report java/util/Iterator 
            // rather then concrete subtype
            if (isIteratorProducingMethod(methodInsn)) {
                
                try {
                    ContinuableClassInfo cci = cciResolver.resolve(methodInsn.owner); 
                    boolean isContinuableIteratorProducer = null == cci ? false : cci.isContinuableMethod(methodInsn.getOpcode(), methodInsn.name, methodInsn.desc, methodInsn.desc);
                    if (!isContinuableIteratorProducer) {
                        continue;
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                
                /*
                AbstractInsnNode r = findMatchingCallSite(mins);
                if (null != r) {
                */    
                VarInsnNode insStoreIterator = findIteratorVarIdx(methodInsn); 
                if (insStoreIterator != null) {
                    int iteratorVarIdx = insStoreIterator.var;
                          
                    LabelNode[] bounds = findIteratorBoundaries(insStoreIterator);
                    LabelNode startLabel = bounds[0];
                    LabelNode endLabel = bounds[1];

                    if (null != startLabel && null != endLabel) {
                        // Initialize target tracking nodes
                        LocalVariableAnnotationNode annotationNode = new LocalVariableAnnotationNode(
                            api,
                            LOCAL_VARIABLE_TYPE_REF,
                            null, // TypePath
                            new LabelNode[]{ startLabel },
                            new LabelNode[]{ endLabel },
                            new int[]{ iteratorVarIdx },
                            CallSiteFinder.CCS_ANNOTATION_DESCRIPTOR
                        );
                        
                        if (owner.visibleLocalVariableAnnotations == null) {
                            owner.visibleLocalVariableAnnotations = new ArrayList<LocalVariableAnnotationNode>();
                        }
                        owner.visibleLocalVariableAnnotations.add(annotationNode);
                    }
                }
                /*
                }
                */                    
            }
        }
    }
    
    private boolean isIteratorProducingMethod(MethodInsnNode methodInsn) {
        if (methodInsn.getOpcode() != INVOKEINTERFACE && methodInsn.getOpcode() != INVOKEVIRTUAL) {
            return false;
        }
        
        if (!"iterator".equals(methodInsn.name)) {
            return false;
        }
        
        if (!classHierarchy.isSubClass(methodInsn.owner, ITERABLE_TYPE.getInternalName())) {
            return false;
        }
        
        Type[] argumentTypes = Type.getArgumentTypes(methodInsn.desc);
        Type returnType = Type.getReturnType(methodInsn.desc);
        return (null == argumentTypes || argumentTypes.length == 0) && classHierarchy.isSubClass(returnType.getInternalName(), ITERATOR_TYPE.getInternalName());
    }
    
    private AbstractInsnNode findMatchingCallSite(MethodInsnNode m) {
        int size = Type.getArgumentsAndReturnSizes(m.desc) >> 2;
        assert size >= 1;

        for (AbstractInsnNode n = m.getPrevious(); n != null; n = n.getPrevious()) {
            size -= CallSiteFinder.getStackSizeChange(n);
            if (size == 0) {
                if (n instanceof VarInsnNode ||
                    n instanceof FieldInsnNode ||
                    n instanceof MethodInsnNode) {
                    return n;
                } else {
                    // What the hell are you? (c) Predator
                    return n;
                }
            } else if (size <= 0) {
                throw new RuntimeException();
            }
        }
        return null;
    }
    
    private static VarInsnNode findIteratorVarIdx(AbstractInsnNode methodCall) {
        for (AbstractInsnNode n = methodCall.getNext(); n != null; n = n.getNext()) {
            if (n.getOpcode() == ASTORE) {
                return (VarInsnNode)n;
            } else {
                if (CallSiteFinder.getStackSizeChange(n)  != 0) {
                    break;
                }
            }
        }
        
        return null;
    }
    
    private LabelNode[] findIteratorBoundaries(VarInsnNode astoreNode) {
        int targetIteratorVar = astoreNode.var;
        LabelNode startLabel = null;
        LabelNode endLabel = null;

        // Identify the Start Label (The visual tracking point where loop activity begins)
        for (AbstractInsnNode nextNode = astoreNode.getNext(); nextNode != null; nextNode = nextNode.getNext()) {
            if (nextNode instanceof LabelNode) {
                startLabel = (LabelNode) nextNode;
                break;
            }
        }

        // Locate the verification block (Handles standard and GOTO optimizations)
        AbstractInsnNode scanStart = astoreNode.getNext();
        
        // If a GOTO immediately follows the ASTORE (skipping over lines or labels), jump with it!
        if (scanStart instanceof JumpInsnNode && scanStart.getOpcode() == GOTO) {
            scanStart = ((JumpInsnNode) scanStart).label;
        }

        // Scan forward from our verified starting node location
        for (AbstractInsnNode current = scanStart; current != null; current = current.getNext()) {
            if (!(current instanceof MethodInsnNode)) {
                continue;
            }
            
            MethodInsnNode methodInsn = (MethodInsnNode) current;
            
            // Confirm the target call is Iterator.hasNext()
            if (methodInsn.getOpcode() == INVOKEINTERFACE &&
                "hasNext".equals(methodInsn.name) &&
                "()Z".equals(methodInsn.desc) &&
                classHierarchy.isSubClass(methodInsn.owner, ITERATOR_TYPE.getInternalName())) {

                // Verify the target variable register matches
                AbstractInsnNode loadInsn = findMatchingCallSite(methodInsn);
                if (loadInsn instanceof VarInsnNode && loadInsn.getOpcode() == ALOAD) {

                    if (((VarInsnNode) loadInsn).var == targetIteratorVar) {
                        // Look forward to extract the associated conditional boundary jump
                        AbstractInsnNode branchInsn = methodInsn.getNext();
                        if (branchInsn instanceof JumpInsnNode) {
                            JumpInsnNode jump = (JumpInsnNode) branchInsn;
                            if (jump.getOpcode() == IFEQ) {
                                // Strategy A: Top-check structure (IFEQ points to end)
                                endLabel = jump.label;
                            } else if (jump.getOpcode() == IFNE) {
                                // Strategy B: Bottom-check structure (IFNE jumps back up, end is right after)
                                AbstractInsnNode postJump = jump.getNext();
                                while (postJump != null && !(postJump instanceof LabelNode)) {
                                    postJump = postJump.getNext();
                                }
                                endLabel = (LabelNode) postJump;
                            }
                            break; // Target found
                        }
                    }
                }
            }
            // First method call without match by iterator variable
            break;
        }

        return new LabelNode[] {startLabel, endLabel};
    }
}