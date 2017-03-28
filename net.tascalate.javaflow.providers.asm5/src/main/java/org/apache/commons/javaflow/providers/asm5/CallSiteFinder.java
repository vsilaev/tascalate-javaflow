/**
 * ﻿Copyright 2013-2017 Valery Silaev (http://vsilaev.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableAnnotationNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

class CallSiteFinder {

    public static class Result {
        final public AbstractInsnNode callSite;
        final public MethodInsnNode methodCall;
        final public Set<String> annotations;

        Result(final AbstractInsnNode callSite, final MethodInsnNode methodCall, final Set<String> annotations) {
            this.callSite = callSite;
            this.methodCall = methodCall;
            this.annotations = annotations;
        }

        @Override
        public String toString() {
            final String caller;
            if (callSite instanceof VarInsnNode) {
                caller = "Var #" + VarInsnNode.class.cast(callSite).var;
            } else {
                caller = "Field";
            }
            return '{' + caller + '.' + methodCall.name + methodCall.desc + annotations + '}';
        }
    }

    List<Result> findMatchingCallSites(final InsnList instructions, final List<LocalVariableAnnotationNode> varAnnotations, final Map<Integer, List<AnnotationNode>> paramAnnotations) {
        final List<Result> result = new ArrayList<>();
        for (@SuppressWarnings("unchecked") Iterator<AbstractInsnNode> i = instructions.iterator(); i.hasNext(); ) {
            final AbstractInsnNode ins = i.next();
            if (ins instanceof MethodInsnNode) {
                final MethodInsnNode mins = (MethodInsnNode)ins;
                final Result entry = findMatchingCallSite(mins, varAnnotations, paramAnnotations);
                if (entry != null) {
                    result.add(entry);
                }
            }
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    static List<LocalVariableAnnotationNode> annotationsList(final List<?> v) {
        return null == v ? Collections.emptyList() : (List<LocalVariableAnnotationNode>)v;
    }

    static Map<Integer, List<AnnotationNode>> annotationsList(final List<?>[] v) {
        if (v == null) {
            return Collections.emptyMap();
        }
        final Map<Integer, List<AnnotationNode>> result = new HashMap<>();
        int i = 0;
        for (final List<?> list : v) {
            @SuppressWarnings("unchecked")
            final List<AnnotationNode> typedList = (List<AnnotationNode>)list;
            if (null != typedList)
                result.put(i++, typedList);
        }
        return result;
    }

    protected Result findMatchingCallSite(final MethodInsnNode m, final List<LocalVariableAnnotationNode> varAnnotations, final Map<Integer, List<AnnotationNode>> paramAnnotations) {
        final int opcode = m.getOpcode(); 

        if (INVOKEVIRTUAL != opcode && INVOKEINTERFACE != opcode ) {
            return null;
        }

        int size = Type.getArgumentsAndReturnSizes(m.desc) >> 2;
        int argCount = Type.getArgumentTypes(m.desc).length;
        assert size >= 1;

        for (AbstractInsnNode n = m.getPrevious(); n != null; n = n.getPrevious()) {
            size -= getStackSizeChange(n);
            if (size == 0) {
                if (n instanceof VarInsnNode) {
                    // Local variable (incl. this) or parameter
                    final VarInsnNode v = (VarInsnNode)n;
                    final Set<String> annotations;
                    if (v.var > argCount) { // 0 -- this, 1..argCount-1 -- params
                        annotations = getVarAnnotations(varAnnotations, v);
                    } else {
                        annotations = getParamAnnotations(paramAnnotations, v.var - 1); // -1 while instance method
                    }
                    return new Result(v, m, annotations);
                } else if (n instanceof FieldInsnNode) {
                    // static/instance field
                    return new Result(n, m, Collections.emptySet());
                } else {
                    // Not interested in other types like directly reused return value from chained calls
                    break;
                }
            } else if (size <= 0) {
                throw new RuntimeException();
            }
        }
        return null;
    }

    protected Set<String> getVarAnnotations(final List<LocalVariableAnnotationNode> varAnnotations, final VarInsnNode v) {
        final Set<String> result = new TreeSet<>();
        for (final LocalVariableAnnotationNode n : varAnnotations) {
            final int idx = n.index.indexOf(v.var);
            if (idx < 0) {
                continue;
            }
            final LabelNode start = (LabelNode) n.start.get(idx);
            final LabelNode end = (LabelNode) n.end.get(idx);
            if (isVarBetweenBounds(v, start, end)) {
                result.add(n.desc);
            }
        }
        return result;
    }

    protected boolean isVarBetweenBounds(final AbstractInsnNode var, final LabelNode lo, final LabelNode hi) {
        AbstractInsnNode x;
        boolean loFound = false;
        for (x = var; !(x == null || loFound); x = x.getPrevious()) {
            loFound = x == lo;
        }
        if (!loFound)
            return false;

        boolean hiFound = false;
        for (x = var; !(x == null || hiFound); x = x.getNext()) {
            hiFound = x == hi;
        }

        return hiFound;

    }

    protected Set<String> getParamAnnotations(Map<Integer, List<AnnotationNode>> paramAnnotations, final int varIdx) {
        final Set<String> result = new TreeSet<>();
        final List<AnnotationNode> annos = paramAnnotations.get(varIdx);
        if (null != annos) {
            for (final AnnotationNode n : annos) {
                result.add(n.desc);
            }
        }
        return result;

    }

    protected static int getStackSizeChange(final AbstractInsnNode ins) {
        /**
         * See http://cs.au.dk/~mis/dOvs/jvmspec/ref-Java.html
         */

        final int s;
        final int o = ins.getOpcode();

        if (o < 0) {
            return 0;
        }

        switch (o) {
            case NOP: return 0;
    
            case ACONST_NULL: return 1;
    
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5: return 1;
    
            case LCONST_0:
            case LCONST_1: return 2;
    
            case FCONST_0:
            case FCONST_1:
            case FCONST_2: return 1;
    
            case DCONST_0:
            case DCONST_1: return 2;
    
            case BIPUSH:
            case SIPUSH: return 1;
    
            case LDC: 
                final LdcInsnNode l = (LdcInsnNode)ins;
                if (l.cst instanceof Long || l.cst instanceof Double)
                    return 2;
                else
                    return 1;
    
            case ILOAD: return 1;
            case LLOAD: return 2;
            case FLOAD: return 1;
            case DLOAD: return 2;
            case ALOAD: return 1;
    
            // POP index + array, PUSH element (1-2 bytes depending on type)
            case IALOAD: return -1; 
            case LALOAD: return 0;
            case FALOAD: return -1;
            case DALOAD: return 0;
            case AALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD: return -1;
    
            case ISTORE: return -1;
            case LSTORE: return -2;
            case FSTORE: return -1;
            case DSTORE: return -2;
            case ASTORE: return -1;
    
            // POP index + array + element (1 + 1 + 1or2 bytes depending on type)
            case IASTORE: return -3;
            case LASTORE: return -4;
            case FASTORE: return -3;
            case DASTORE: return -4;
            case AASTORE:
            case BASTORE:
            case CASTORE:
            case SASTORE: return -3;
    
            case POP: return -1;
            case POP2: return -2;
            case DUP:
            case DUP_X1:;
            case DUP_X2: return 1;
            case DUP2:
            case DUP2_X1: return 2;
            case DUP2_X2: return 2;
            case SWAP: return 0;
    
            case IADD: return -1;
            case LADD: return -2;
            case FADD: return -1;
            case DADD: return -2;
            case ISUB: return -1;
    
            case LSUB: return -2;
            case FSUB: return -1;
            case DSUB: return -2;
            case IMUL: return -1;
            case LMUL: return -2;
            case FMUL: return -1;
            case DMUL: return -2;
            case IDIV: return -1;
            case LDIV: return -2;
            case FDIV: return -1;
            case DDIV: return -2;
            case IREM: return -1;
            case LREM: return -2;
            case FREM: return -1;
            case DREM: return -2;
            case INEG:
            case LNEG:
            case FNEG:
            case DNEG: return 0;
            case ISHL: return -1;
            case LSHL: return -2;
            case ISHR: return -1;
            case LSHR: return -2;
            case IUSHR: return -1;
            case LUSHR: return -2;
            case IAND: return -1;
            case LAND: return -2;
            case IOR: return -1;
            case LOR: return -2;
            case IXOR: return -1;
            case LXOR: return -2;
            case IINC: return 0;
    
            case I2L: return 1;
            case I2F: return 0;
            case I2D: return 1;
            case L2I: return -1;
            case L2F: return -1;
            case L2D: return 0;
            case F2I: return 0;
            case F2L: return 1;
            case F2D: return 1;
            case D2I: return -1;
            case D2L: return 0;
            case D2F: return -1;
            case I2B: return 0;
            case I2C: return 0;
            case I2S: return 0;
    
            // POP 2 * size from frame for compared elements, PUSH int result
            case LCMP: return -2 * 2 +1;
            case FCMPL: 
            case FCMPG: return -2 * 1 + 1;
            case DCMPL:
            case DCMPG: return -2 * 2 + 1;
    
            // POP int flag from stack
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE: return -1;
    
            // POP 2 comparing args from stack (each is signle word)
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE: return -2;
    
            case GOTO: return 0;
            case JSR: return 1; // PUSH return address on stack
            case RET: return 0;
            case TABLESWITCH: 
            case LOOKUPSWITCH: return -1; // POP switch var from stack
    
            // Removes result from stack, actually stack is destroyed after this
            case IRETURN: return -1;
            case LRETURN: return -2;
            case FRETURN: return -1;
            case DRETURN: return -2;
            case ARETURN: return -1;
            case RETURN: return 0;
    
            case GETSTATIC:
            case PUTSTATIC:
                final FieldInsnNode fs = (FieldInsnNode)ins;
                return Type.getType(fs.desc).getSize() * (o == PUTSTATIC ? -1 : +1); 
    
            case GETFIELD:
                final FieldInsnNode fg = (FieldInsnNode)ins;
                return -1 + Type.getType(fg.desc).getSize(); // POP this PUSH current field value 
    
            case PUTFIELD:
                final FieldInsnNode fp = (FieldInsnNode)ins;
                return -1 - Type.getType(fp.desc).getSize(); // POP this and POP field value to assign
    
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEINTERFACE:
                final MethodInsnNode m = (MethodInsnNode)ins;
                s = Type.getArgumentsAndReturnSizes(m.desc);
                return - (s / 2) + (s % 2); 
            case INVOKEDYNAMIC:
                final InvokeDynamicInsnNode d = (InvokeDynamicInsnNode)ins;
                s = Type.getArgumentsAndReturnSizes(d.desc);
                return - (s / 2) + (s % 2); 
    
            case NEW:
                return 1;
            case NEWARRAY:
            case ANEWARRAY:
                return 0; // POP length and PUSH Array
            case ARRAYLENGTH:
                return 0; // POP array and PUSH length
    
            case ATHROW:
                return -1; // Technically, no stack available after this, but we POP exception object
            case CHECKCAST:
                return 0;
            case INSTANCEOF:
                return 0; // POP object ref PUSH int
            case MONITORENTER:
            case MONITOREXIT:
                return -1; // POP synchronized object from stack
    
            case MULTIANEWARRAY:
                final MultiANewArrayInsnNode ma = (MultiANewArrayInsnNode)ins;
                return -ma.dims + 1;
            case IFNULL:
            case IFNONNULL:
                return -1; // POP-s object to compare
    
            default:
                throw new RuntimeException(ins.toString() + " = " + ins.getOpcode());
        }
    }
}
