package act.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate a binding from a {@link act.app.CliSession#attribute(String) CLI session variable} to
 * a commander field or parameter;
 *
 * Or binding from a {@link org.osgl.http.H.Session#get(String)} to a controller field or action handler
 * parameter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface SessionVariable {

    /**
     * The cli session attribute name or a http session variable name
     *
     * Default value: "", meaning it shall
     * use the field name or parameter name to
     * get the session attribute value
     */
    String value() default "";
}
