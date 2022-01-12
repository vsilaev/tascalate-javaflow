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
package org.apache.commons.javaflow.core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stack to store the frame information along the invocation trace.
 *
 * @author <a href="mailto:tcurdt@apache.org">Torsten Curdt</a>
 * @author <a href="mailto:stephan@apache.org">Stephan Michels</a>
 * @version CVS $Id: Stack.java 733503 2009-01-11 19:51:40Z tcurdt $
 */
public class Stack implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Stack.class);
    private static final long serialVersionUID = 3L;

    private long[] pstack;
    private Object[] ostack;
    private Object[] rstack;
    private int dTop, fTop, lTop, iTop, oTop, rTop;
    protected Runnable runnable;

    Stack(Runnable pRunnable) {
        pstack = new long[16];
        ostack = new Object[8];
        rstack = new Object[4];
        runnable = pRunnable;
    }

    Stack(final Stack pParent) {
        pstack = new long[pParent.pstack.length];
        ostack = new Object[pParent.ostack.length];
        rstack = new Object[pParent.rstack.length];
        iTop = pParent.iTop;
        fTop = pParent.fTop;
        dTop = pParent.dTop;
        lTop = pParent.lTop;
        oTop = pParent.oTop;
        rTop = pParent.rTop;
        System.arraycopy(pParent.pstack, 0, pstack, 0, pTop());
        System.arraycopy(pParent.ostack, 0, ostack, 0, oTop);
        System.arraycopy(pParent.rstack, 0, rstack, 0, rTop);
        runnable = pParent.runnable;
    }

    public final boolean hasDouble() {
        return dTop > 0;
    }

    public final double popDouble() {
        if (dTop == 0) {
            throw new EmptyStackException("pop double");
        }

        --dTop;
        final double d = Double.longBitsToDouble(popPrimitive());
        if (log.isDebugEnabled()) {
            log.debug("pop double " + d + " " + getStats());
        }
        return d;
    }
    
    public final boolean hasFloat() {
        return fTop > 0;
    }

    public final float popFloat() {
        if (fTop == 0) {
            throw new EmptyStackException("pop float");
        }

        --fTop;
        final float f = Float.intBitsToFloat((int)popPrimitive());
        if (log.isDebugEnabled()) {
            log.debug("pop float " + f + " " + getStats());
        }
        return f;
    }
    
    public final boolean hasLong() {
        return lTop > 0;
    }

    public final long popLong() {
        if (lTop == 0) {
            throw new EmptyStackException("pop long");
        }

        --lTop;
        final long l = popPrimitive();
        if (log.isDebugEnabled()) {
            log.debug("pop long " + l + " " + getStats());
        }
        return l;
    }

    public final boolean hasInt() {
        return iTop > 0;
    }

    public final int popInt() {
        if (iTop == 0) {
            throw new EmptyStackException("pop int");
        }

        --iTop;
        final int i = (int)popPrimitive();
        if (log.isDebugEnabled()) {
            log.debug("pop int " + i + " " + getStats());
        }
        return i;
    }

    public final boolean hasObject() {
        return oTop > 0;
    }

    public final Object popObject() {
        if (oTop == 0) {
            throw new EmptyStackException("pop object");
        }

        final Object o = ostack[--oTop];
        ostack[oTop] = null; // avoid unnecessary reference to object

        if (log.isDebugEnabled()) {
            log.debug("pop object " + ReflectionUtils.descriptionOfObject(o) + " " + getStats());
        }

        return o;
    }

    public final boolean hasReference() {
        return rTop > 0;
    }

    public final Object popReference() {
        if (rTop == 0) {
            throw new EmptyStackException("pop reference");
        }

        final Object o = rstack[--rTop];
        rstack[rTop] = null; // avoid unnecessary reference to object

        if (log.isDebugEnabled()) {
            log.debug("pop reference " + ReflectionUtils.descriptionOfObject(o) + " " + getStats());
        }

        return o;
    }

    public final void pushDouble(double d) {
        if (log.isDebugEnabled()) {
            log.debug("push double " + d + " " + getStats());
        }

        ensurePrimitivesStackSize();
        pushPrimitive(Double.doubleToLongBits(d));
        dTop++;
    }

    public final void pushFloat(float f) {
        if (log.isDebugEnabled()) {
            log.debug("push float " + f + " " + getStats());
        }
        
        ensurePrimitivesStackSize();
        pushPrimitive(Float.floatToIntBits(f));
        fTop++;
    }
    
    public final void pushLong(long l) {
        if (log.isDebugEnabled()) {
            log.debug("push long " + l + " " + getStats());
        }

        ensurePrimitivesStackSize();
        pushPrimitive(l);
        lTop++;
    }

    public final void pushInt(int i) {
        if (log.isDebugEnabled()) {
            log.debug("push int " + i + " " + getStats());
        }
        ensurePrimitivesStackSize();
        pushPrimitive(i);
        iTop++;
    }
    
    public final void pushObject(Object o) {
        if (log.isDebugEnabled()) {
            log.debug("push object " + ReflectionUtils.descriptionOfObject(o) + " " + getStats());
        }

        if (oTop == ostack.length) {
            Object[] hlp = new Object[Math.max(8, ostack.length * 2)];
            System.arraycopy(ostack, 0, hlp, 0, ostack.length);
            ostack = hlp;
        }
        ostack[oTop++] = o;
    }

    public final void pushReference(Object o) {
        if (log.isDebugEnabled()) {
            log.debug("push reference " + ReflectionUtils.descriptionOfObject(o) + " " + getStats());
        }

        if (rTop == rstack.length) {
            Object[] hlp = new Object[Math.max(8, rstack.length * 2)];
            System.arraycopy(rstack, 0, hlp, 0, rstack.length);
            rstack = hlp;
        }
        rstack[rTop++] = o;
    }

    public boolean isSerializable() {
        for (int i = 0; i < rTop; i++) {
            final Object r = rstack[i];
            if (!(r instanceof Serializable)) {
                return false;
            }
        }
        for (int i = 0; i < oTop; i++) {
            final Object o = ostack[i];
            if (!(o instanceof Serializable)) {
                return false;
            }
        }
        return true;
    }

    public final boolean isEmpty() {
        return iTop == 0 && lTop == 0 && dTop == 0 && fTop == 0 && oTop == 0 && rTop == 0;
    }

    public final Runnable getRunnable() {
        return runnable;
    }

    private String getStats() {
        final StringBuilder sb = new StringBuilder();
        sb.append("i[").append(iTop).append("],");
        sb.append("l[").append(lTop).append("],");
        sb.append("d[").append(dTop).append("],");
        sb.append("f[").append(fTop).append("],");
        sb.append("o[").append(oTop).append("],");
        sb.append("r[").append(rTop).append("]");
        return sb.toString();
    }

    private String getContent() {
        final StringBuilder sb = new StringBuilder();
        sb.append("i[").append(iTop).append("]\n");
        sb.append("l[").append(lTop).append("]\n");
        sb.append("d[").append(dTop).append("]\n");
        sb.append("f[").append(fTop).append("]\n");
        sb.append("o[").append(oTop).append("]\n");
        for (int i = 0; i < oTop; i++) {
            sb.append(' ').append(i).append(": ")
              .append(ReflectionUtils.descriptionOfObject(ostack[i]))
              .append('\n')
            ;
        }
        sb.append("r[").append(rTop).append("]\n");
        for (int i = 0; i < rTop; i++) {
            sb.append(' ').append(i).append(": ")
              .append(ReflectionUtils.descriptionOfObject(rstack[i]))
              .append('\n')
            ;
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getContent();
    }
    
    private final int pTop() {
        return dTop + fTop + lTop + iTop;
    }
    
    private final void pushPrimitive(long value) {
        pstack[pTop()] = value;
    }
    
    private final long popPrimitive() {
        return pstack[pTop()];
    }
    
    private final void ensurePrimitivesStackSize() {
        if (pTop() == pstack.length) {
            long[] hlp = new long[Math.max(8, pstack.length * 2)];
            System.arraycopy(pstack, 0, hlp, 0, pstack.length);
            pstack = hlp;
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeInt(dTop);
        s.writeInt(fTop);
        s.writeInt(lTop);
        s.writeInt(iTop);
        int pTop = pTop();
        for (int i = 0; i < pTop; i++) {
            s.writeLong(pstack[i]);
        }

        s.writeInt(oTop);
        for (int i = 0; i < oTop; i++) {
            s.writeObject(ostack[i]);
        }

        s.writeInt(rTop);
        for (int i = 0; i < rTop; i++) {
            s.writeObject(rstack[i]);
        }

        s.writeObject(runnable);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        dTop = s.readInt();
        fTop = s.readInt();
        lTop = s.readInt();
        iTop = s.readInt();
        int pTop = pTop();
        pstack = new long[pTop];
        for (int i = 0; i < pTop; i++) {
            pstack[i] = s.readLong();
        }
        
        oTop = s.readInt();
        ostack = new Object[oTop];
        for (int i = 0; i < oTop; i++) {
            ostack[i] = s.readObject();
        }

        rTop = s.readInt();
        rstack = new Object[rTop];
        for (int i = 0; i < rTop; i++) {
            rstack[i] = s.readObject();
        }

        runnable = (Runnable) s.readObject();
    }
}
