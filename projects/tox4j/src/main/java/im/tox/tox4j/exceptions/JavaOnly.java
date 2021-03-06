package im.tox.tox4j.exceptions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JavaOnly {
  /**
   * This is just here so the annotation is retained at runtime.
   */
  String value() default "";
}
