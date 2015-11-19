package org.apache.commons.javaflow.providers.asm3;

import org.objectweb.asm.Type;
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
    public Value newValue(final Type type) {
        if (type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }

        boolean isArray = type.getSort() == Type.ARRAY;
        if (isArray) {
            switch (type.getElementType().getSort()) {
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                    return new BasicValue(type);
            }
        }

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
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                return type.equals(expectedType);
            case Type.ARRAY:
            case Type.OBJECT:
                if ("Lnull;".equals(type.getDescriptor())) {
                    return true;
                } else if (type.getSort() == Type.OBJECT
                        || type.getSort() == Type.ARRAY)
                {
                    // We are transforming valid bytecode to (hopefully) valid bytecode
                	// hence pairs of "value" and "expected" must be compatible
                	return true;//isAssignableFrom(expectedType, type);
                } else {
                    return false;
                }
            default:
                throw new Error("Internal error");
        }
    }
}