package io.quarkus.deployment.annotations;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The build item name selector meta-annotation.  Annotated types should have a single property called {@code value}
 * whose type is a subclass of {@link Enum} with no specified default value.  They should have a retention of
 * {@link RetentionPolicy#RUNTIME} and targets of {@link ElementType#PARAMETER} and {@link ElementType#FIELD}.
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface NameSelector {
}
