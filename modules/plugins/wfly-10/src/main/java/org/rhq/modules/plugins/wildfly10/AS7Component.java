package org.rhq.modules.plugins.wildfly10;

import org.rhq.core.pluginapi.inventory.ResourceComponent;

/**
 * An AS7 Resource component.
 */
public interface AS7Component<T extends ResourceComponent<?>> extends ResourceComponent<T> {

    /**
     * Returns the connection that can be used to send management requests to the managed AS& instance.
     *
     * @return the connection that can be used to send management requests to the managed AS& instance
     */
    ASConnection getASConnection();

    /**
     * Returns a path in the form (key=value)?(,key=value)* that can be used to construct an address to this component's
     * underlying managed service.
     *
     * @return a path in the form (key=value)?(,key=value)* that can be used to construct an address to this component's
     *         underlying managed service
     */
    String getPath();

}
