/**
 * 
 */
package org.rhq.test.arquillian;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When placed on type, this annotation merely configures the behavior of the plugin container
 * instance before each test.
 * <p>
 * When used on a field, the field is in addition assigned the plugin container instance so that
 * it can be easily used in the tests.
 * 
 * @author Lukas Krejci
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface PluginContainerInstance {

    boolean discoverServers() default false;
    
    boolean discoverServices() default false;    
}
