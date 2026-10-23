package tech.petrepopescu.common.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Attach this annotation to the controller method so that the request and the response code is logged
 */
@Target({METHOD, TYPE})
@Retention(RUNTIME)
@Documented
public @interface LogRequestData {
    boolean logRequest() default true;

    boolean logResponse() default true;
}
