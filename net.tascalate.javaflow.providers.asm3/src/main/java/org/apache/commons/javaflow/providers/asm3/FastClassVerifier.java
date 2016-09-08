package org.apache.commons.javaflow.providers.asm3;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Value;

public class FastClassVerifier extends BasicVerifier {

    /**
     * Constructs a new {@link FastClassVerifier} to verify a specific class. This
     * class will not be loaded into the JVM since it may be incorrect.
     *
     */
    public FastClassVerifier()
    {
        super();
    }

    @Override
    public Value copyOperation(final AbstractInsnNode insn, Value value) throws AnalyzerException {
        // Fix error with analyzer for try-with-resources (it sees uninitialized values)
        if (insn.getOpcode() == Opcodes.ALOAD && (value instanceof BasicValue && !((BasicValue)value).isReference())) {
            value = newValue(Type.getType("Lnull;"));
        }
        return super.copyOperation(insn, value);
    }

    @Override
    public Value unaryOperation(final AbstractInsnNode insn, Value value) throws AnalyzerException {
        // Fix error with analyzer for try-with-resources (it sees uninitialized values)
        if (insn.getOpcode() == Opcodes.ARETURN && (value instanceof BasicValue && !((BasicValue)value).isReference())) {
            value = newValue(Type.getType("Lnull;"));
        }
        return super.unaryOperation(insn, value);    
    }    

    @Override
    public Value newValue(final Type type) {
        if (type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }

        final boolean isArray = type.getSort() == Type.ARRAY;
        Value v = super.newValue(type);
        if (BasicValue.REFERENCE_VALUE.equals(v)) {
            if (isArray) {
                v = newValue(type.getElementType());
                String desc = ((BasicValue)v).getType().getDescriptor();
                for (int i = 0; i < type.getDimensions(); ++i) {
                    desc = '[' + desc;
                }
                v = new BasicValue(Type.getType(desc));
            } else {
                v = new BasicValue(type);
            }
        }
        return v;
    }

    @Override
    protected boolean isSubTypeOf(final Value value, final Value expected) {
        if (!(value instanceof BasicValue))
        {
            return value.equals(expected);
        }
        Type expectedType = ((BasicValue)expected).getType();
        Type type = ((BasicValue)value).getType();
        switch (expectedType.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                return type.equals(expectedType);
            case Type.ARRAY:
            case Type.OBJECT:
                // We are transforming valid bytecode to (hopefully) valid bytecode
                // hence pairs of "value" and "expected" must be compatible
                return true;//isAssignableFrom(expectedType, type);
            default:
                throw new Error("Internal error");
        }
    }
}