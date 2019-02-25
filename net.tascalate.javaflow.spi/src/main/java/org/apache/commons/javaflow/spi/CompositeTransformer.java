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
package org.apache.commons.javaflow.spi;

import java.util.Collection;

/**
 * {@link ResourceTransformer} whose transformation is defined in terms of
 * multiple {@link ResourceTransformer}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class CompositeTransformer extends AbstractResourceTransformer {
    private final ResourceTransformer[] transformers;

    public CompositeTransformer(ResourceTransformer[] transformers) {
        this.transformers = transformers;
    }

    public byte[] transform(byte[] image, Collection<String> retransformClasses) {
        for (ResourceTransformer transformer : transformers) {
            byte[] result = transformer.transform(image, retransformClasses);
            if (null != result) {
                image = result;
            }
        }
        return image;
    }
    
    public void release() {
        for (ResourceTransformer transformer : transformers) {
            transformer.release();
        }
    }
}
