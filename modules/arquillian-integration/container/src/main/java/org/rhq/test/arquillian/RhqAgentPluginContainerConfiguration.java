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

    private String serverServicesImplementationClassName;
    
    public RhqAgentPluginContainerConfiguration() {
        //hardcode some defaults
        setAvailabilityScanInitialDelay(Long.MAX_VALUE);
        setAvailabilityScanPeriod(Long.MAX_VALUE);
        setConfigurationDiscoveryInitialDelay(Long.MAX_VALUE);
        setConfigurationDiscoveryPeriod(Long.MAX_VALUE);
        setContentDiscoveryInitialDelay(Long.MAX_VALUE);
        setContentDiscoveryPeriod(Long.MAX_VALUE);
        setDriftDetectionInitialDelay(Long.MAX_VALUE);
        setDriftDetectionPeriod(Long.MAX_VALUE);
        setEventSenderInitialDelay(Long.MAX_VALUE);
        setEventSenderPeriod(Long.MAX_VALUE);
        setMeasurementCollectionInitialDelay(Long.MAX_VALUE);
        setServerDiscoveryInitialDelay(Long.MAX_VALUE);
        setServerDiscoveryPeriod(Long.MAX_VALUE);
        setServiceDiscoveryInitialDelay(Long.MAX_VALUE);
        setServiceDiscoveryPeriod(Long.MAX_VALUE);  
    }
    
    /**
     * @return the serverServicesImplementationClassName
     */
    public String getServerServicesImplementationClassName() {
        return serverServicesImplementationClassName;
    }
    
    /**
     * @param value the serverServicesImplementationClassName to set
     */
    public void setServerServicesImplementationClassName(String value) {
        this.serverServicesImplementationClassName = value;
    }
    
    @Override
    public void validate() throws ConfigurationException {
    }
}
