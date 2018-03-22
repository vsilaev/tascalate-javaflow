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
package org.apache.commons.javaflow.core;

public class ResumeParameter {
    private final Object value;

    private ResumeParameter(Object value) {
        this.value = value;
    }

    public static ResumeParameter value(Object value) {
        return null == value ? NULL_VALUE : new ResumeParameter(value);
    }

    public static ResumeParameter exit() {
        return EXIT;
    }

    final Object value() {
        return value;
    }

    void checkExit() {

    }

    private static final ResumeParameter NULL_VALUE = new ResumeParameter(null);

    private static final ResumeParameter EXIT = new ResumeParameter(null) {
        void checkExit() {
            throw ContinuationDeath.INSTANCE;
        }
    };
}
