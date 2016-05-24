package org.apache.commons.javaflow.instrumentation.owb;

import org.apache.commons.javaflow.core.StackRecorder;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

abstract class AroundOwbProxyInvokeAdvice extends AdviceAdapter {
    final protected String className;
    final protected String methodName;
    
    private Label startFinally;
    private int stackRecorderVar;
    
    protected AroundOwbProxyInvokeAdvice(int api, MethodVisitor mv, int acc, String className, String methodName, String desc) {
        super(api, mv, acc, methodName, desc);
        this.className = className;
        this.methodName = methodName;
    }
    
    abstract protected void loadProxiedInstance();

    @Override
    protected void onMethodEnter() {
        // verify if restoring
        stackRecorderVar = newLocal(STACK_RECORDER_TYPE);
        Label startDelegated = new Label();

        // PC: StackRecorder stackRecorder = StackRecorder.get();
        invokeStatic(STACK_RECORDER_TYPE, STACK_RECORDER_GET);
        dup();
        storeLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        // PC: if (stackRecorder != null && stackRecorder.isRestoring) {
        ifNull(startDelegated);
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        getField(STACK_RECORDER_TYPE, "isRestoring", Type.BOOLEAN_TYPE);
        visitJumpInsn(IFEQ, startDelegated);
        
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        invokeVirtual(STACK_RECORDER_TYPE, STACK_RECORDER_POP_REF);
        pop();
        
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        loadProxiedInstance();
        invokeVirtual(STACK_RECORDER_TYPE, STACK_RECORDER_PUSH_REF);

        visitLabel(startDelegated);
        
        super.onMethodEnter();
    }
    
    @Override
    public void visitCode() {
        super.visitCode();
        mv.visitLabel(startFinally = new Label());
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        Label endFinally = new Label();
        mv.visitTryCatchBlock(startFinally, endFinally, endFinally, null);
        mv.visitLabel(endFinally);
        onFinally(ATHROW);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(maxStack, maxLocals);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != ATHROW) {
            onFinally(opcode);
        }
    }

    private void onFinally(int opcode) {
        Label done = new Label();
        loadLocal(stackRecorderVar);
        // PC: if (stackRecorder != null && stackRecorder.isCapturing) {
        ifNull(done);
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        getField(STACK_RECORDER_TYPE, "isCapturing", Type.BOOLEAN_TYPE);
        visitJumpInsn(IFEQ, done);
        
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        invokeVirtual(STACK_RECORDER_TYPE, STACK_RECORDER_POP_REF);
        pop();
        loadLocal(stackRecorderVar, STACK_RECORDER_TYPE);
        loadThis();
        invokeVirtual(STACK_RECORDER_TYPE, STACK_RECORDER_PUSH_REF);
        visitLabel(done);
      
    }
    
    private static final Type STACK_RECORDER_TYPE = Type.getType(StackRecorder.class);
    private static final Method STACK_RECORDER_GET;
    private static final Method STACK_RECORDER_POP_REF;
    private static final Method STACK_RECORDER_PUSH_REF;
    
    static {
        try {
            STACK_RECORDER_GET = Method.getMethod(StackRecorder.class.getMethod("get"));
            STACK_RECORDER_POP_REF = Method.getMethod(StackRecorder.class.getMethod("popReference"));
            STACK_RECORDER_PUSH_REF = Method.getMethod(StackRecorder.class.getMethod("pushReference", Object.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
}
