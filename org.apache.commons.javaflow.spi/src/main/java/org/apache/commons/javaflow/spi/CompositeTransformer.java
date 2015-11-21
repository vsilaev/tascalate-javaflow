package org.apache.commons.javaflow.spi;


/**
 * {@link ResourceTransformer} whose transformation
 * is defined in terms of multiple {@link ResourceTransformer}s.
 *
 * @author Kohsuke Kawaguchi
 */
public class CompositeTransformer implements ResourceTransformer {
    final private ResourceTransformer[] transformers;

    public CompositeTransformer(final ResourceTransformer[] transformers) {
        this.transformers = transformers;
    }

    public byte[] transform(byte[] image) {
        for (int i = 0; i < transformers.length; i++) {
            final byte[] result = transformers[i].transform(image);
            if (null != result) {
            	image = result;
            }
        }
        return image;
    }
}
