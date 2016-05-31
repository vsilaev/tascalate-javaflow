package org.apache.commons.javaflow.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Meta-annotation that is used to annotate other continuation-related annotations.
 * It provides an option to declare and use own annotations instead of supplied 
 * {@link continuable} and {@link ccs} annotations, for ex:
 * 
 * <pre><code>
 * import java.lang.annotation.Documented;
 * import java.lang.annotation.ElementType;
 * import java.lang.annotation.Retention;
 * import java.lang.annotation.RetentionPolicy;
 * import java.lang.annotation.Target;
 * 
 * {@literal @}Documented
 * {@literal @}Retention(RetentionPolicy.CLASS)
 * {@literal @}Target({ElementType.METHOD})
 * <b>{@literal @}ContinuableAnnotation</b>
 * public {@literal @}interface ContinuableMethod {
 *   // The annotation to mark continuable methods
 * }
 * 
 * </code></pre>
 * 
 * @author Valery Silaev
 *
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ContinuableAnnotation {

}
