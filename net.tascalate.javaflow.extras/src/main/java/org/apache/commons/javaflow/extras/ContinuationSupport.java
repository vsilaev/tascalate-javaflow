package org.apache.commons.javaflow.extras;

import java.io.Serializable;

import org.apache.commons.javaflow.api.continuable;

final class ContinuationSupport {
    private ContinuationSupport() {
    }

    static Runnable toRunnable(ContinuableRunnable code) {
        @SuppressWarnings("serial")
        abstract class ContinuableRunnableAdapter implements Runnable, Serializable {
        }

        return new ContinuableRunnableAdapter() {
            private static final long serialVersionUID = 0L;

            @Override
            public @continuable void run() {
                code.run();
            }
        };
    }
}
