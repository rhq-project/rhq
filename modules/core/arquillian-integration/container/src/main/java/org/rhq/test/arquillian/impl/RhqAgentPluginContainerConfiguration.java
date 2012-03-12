/**
 * 
 */
package org.rhq.test.arquillian.impl;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

import org.rhq.core.pc.PluginContainerConfiguration;

/**
 * @author Lukas Krejci
 */
public class RhqAgentPluginContainerConfiguration extends PluginContainerConfiguration implements
		ContainerConfiguration  {

    private String serverServicesImplementationClassName;
    
    private static final long HUNDRED_YEARS = 100L * 365 * 24 * 60 * 60;
    
    public RhqAgentPluginContainerConfiguration() {
        //hardcode some defaults
        setAvailabilityScanInitialDelay(HUNDRED_YEARS);
        setAvailabilityScanPeriod(HUNDRED_YEARS);
        setConfigurationDiscoveryInitialDelay(HUNDRED_YEARS);
        setConfigurationDiscoveryPeriod(HUNDRED_YEARS);
        setContentDiscoveryInitialDelay(HUNDRED_YEARS);
        setContentDiscoveryPeriod(HUNDRED_YEARS);
        setDriftDetectionInitialDelay(HUNDRED_YEARS);
        setDriftDetectionPeriod(HUNDRED_YEARS);
        setEventSenderInitialDelay(HUNDRED_YEARS);
        setEventSenderPeriod(HUNDRED_YEARS);
        setMeasurementCollectionInitialDelay(HUNDRED_YEARS);
        setServerDiscoveryInitialDelay(HUNDRED_YEARS);
        setServerDiscoveryPeriod(HUNDRED_YEARS);
        setServiceDiscoveryInitialDelay(HUNDRED_YEARS);
        setServiceDiscoveryPeriod(HUNDRED_YEARS);
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
