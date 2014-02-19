package org.rhq.cassandra.schema;

import java.util.Properties;

import com.datastax.driver.core.Session;

/**
 * This corresponds to a step element in a schema update file. A &lt;step&gt; element can declare a <code>class</code>
 * attribute which specifies the runtime type to use. The value must be the FQCN and must implement this interface. The
 * default implementation is {@link CQLStep} which will be used when the <code>class</code> attribute is not declared.
 *
 * @author John Sanda
 */
public interface Step {

    /**
     * Invoked prior to {@link #execute()}. Note that this is the same session used by {@link SchemaManager} and it
     * should not be closed.
     */
    void setSession(Session session);

    /**
     * <p>
     * Bind the set of provided properties to the input step. The text should have
     * all the variable to be bound in %variable_name% form.
     * </p>
     * <p>
     * This method should be called even if no properties are provided because it will
     * throw a runtime exception if the text contains properties that are expected to be
     * bound but the list of variable is either empty or does not contain
     * them.
     * </p>
     * <p>
     * This method is invoked prior to {@link #execute()}
     * </p>
     *
     * @param properties properties to bind
     */
    void bind(Properties properties);

    /**
     * Perform the actual work of the step.
     */
    void execute();

}
