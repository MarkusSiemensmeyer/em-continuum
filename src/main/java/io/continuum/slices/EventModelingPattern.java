package io.continuum.slices;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares, on a slice's {@code package-info.java}, which Event Modeling pattern the slice
 * implements - so the mapping from board pattern to code is explicit, and checkable by tests
 * ({@code BlueprintPatternMapTest} for the {@code blueprint} reference context).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface EventModelingPattern {

    Type value();

    /** What this example shows beyond the pattern itself, e.g. "REST trigger" or "inbound". */
    String variant() default "";

    enum Type {
        /** Command → event(s): a decision against the facts in its consistency boundary. */
        STATE_CHANGE,
        /** Event(s) → read model: a projection answering queries. */
        STATE_VIEW,
        /** Event → command: a processor reacting on its own, optionally with a todo list. */
        AUTOMATION,
        /** External system ⇄ this context: their messages in, or our events out. */
        TRANSLATION
    }
}
