/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.javaflow.providers.asm4;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

import java.util.LinkedList;
import java.util.List;

class MonitoringFrame extends Frame {

    // keeps track of monitored locals
    private List<Integer> monitored;

    MonitoringFrame(Frame frame) {
        super(frame);
    }

    MonitoringFrame(int nLocals, int nStack) {
        super(nLocals, nStack);
        monitored = new LinkedList<Integer>();
    }

    @Override
    public void execute(AbstractInsnNode insn, Interpreter interpreter) throws AnalyzerException {

        boolean never = false;
        if (never) {
            super.execute(insn, interpreter);
            return;
        }

        int insnOpcode = insn.getOpcode();

        if (insnOpcode == Opcodes.MONITORENTER || insnOpcode == Opcodes.MONITOREXIT) {
            Value pop = pop();
            interpreter.unaryOperation(insn, pop);

            int local = -1;
            for (int i = 0; i < getLocals(); i++) {
                if (getLocal(i) == pop) local = i;
            }

            if (local > -1) {
                if (insnOpcode == Opcodes.MONITORENTER) {
                    monitorEnter(local);
                } else {
                    monitorExit(local);
                }
            }

        } else {
            super.execute(insn, interpreter);
        }
    }

    @Override
    public Frame init(Frame frame) {
        super.init(frame);
        if (frame instanceof MonitoringFrame) {
            monitored = new LinkedList<Integer>(MonitoringFrame.class.cast(frame).monitored);
        } else {
            monitored = new LinkedList<Integer>();
        }
        return this;
    }

    int[] getMonitored() {
        int[] res = new int[monitored.size()];
        for (int i = 0; i < monitored.size(); i++) {
            res[i] = monitored.get(i);
        }
        return res;
    }

    private void monitorEnter(int local) {
        monitored.add(Integer.valueOf(local));
    }

    private void monitorExit(int local) {
        int index = monitored.lastIndexOf(local);
        if (index == -1) {
            // throw new IllegalStateException("Monitor Exit never entered");
        } else {
            monitored.remove(index);
        }
    }

}