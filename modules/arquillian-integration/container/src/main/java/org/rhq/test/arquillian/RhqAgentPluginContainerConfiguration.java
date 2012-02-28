/**
 * 
 */
package org.rhq.test.arquillian;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

import org.rhq.core.pc.PluginContainerConfiguration;

/**
 * @author Lukas Krejci
 */
public class RhqAgentPluginContainerConfiguration extends PluginContainerConfiguration implements
		ContainerConfiguration  {

    private String deploymentName; 
    
    /**
     * @return the deploymentName
     */
    public String getDeploymentName() {
        return deploymentName;
    }
    
    /**
     * @param deploymentName the deploymentName to set
     */
    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }
    
    @Override
    public void validate() throws ConfigurationException {
    }
}
