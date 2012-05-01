/**
 * 
 */
package org.rhq.test.arquillian.impl;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.system.SystemInfoFactory;

/**
 * @author Lukas Krejci
 */
public class RhqAgentPluginContainerConfiguration extends PluginContainerConfiguration implements
        ContainerConfiguration  {

    private String serverServicesImplementationClassName;
    private boolean nativeSystemInfoEnabled;
    
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

        setWaitForShutdownServiceTermination(true);
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

    public boolean isNativeSystemInfoEnabled() {
        return nativeSystemInfoEnabled;
    }

    public void setNativeSystemInfoEnabled(boolean nativeSystemInfoEnabled) {
        this.nativeSystemInfoEnabled = nativeSystemInfoEnabled;
    }

    @Override
    public void validate() throws ConfigurationException {
        RhqAgentPluginContainer.init();
        
        if (isNativeSystemInfoEnabled() && !SystemInfoFactory.isNativeSystemInfoAvailable()) {
            throw new ConfigurationException("SIGAR is not available - cannot use native SystemInfo.");
        }
        
        try {
            String serverServices = getServerServicesImplementationClassName();
            if (serverServices != null) {
                Class.forName(serverServices);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to look up the ServerServices implementation class ["
                    + getServerServicesImplementationClassName() + "].", e);
        }
    }

}
