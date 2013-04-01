package org.rhq.cassandra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to provide a mapping from properties defined in cassandra.properties to properties
 * in {@link DeploymentOptions}.
 *
 * @author John Sanda
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD })
public @interface DeploymentProperty {

    String name();

}
