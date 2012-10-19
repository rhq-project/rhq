package org.rhq.plugins.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freedesktop.dbus.DBusConnection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.services.Service;
import org.rhq.services.SysVService;
import org.rhq.services.XinetdService;

public class ServiceComponent<T extends ResourceComponent> implements ResourceComponent<T>, ConfigurationFacet {
    private final Log log = LogFactory.getLog(this.getClass());
    private ResourceContext<T> resourceContext;
    private String resourceDescription;
    private Service service;
    DBusConnection conn;

    @Override
    public void start(ResourceContext<T> context) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = context;
        this.resourceDescription = this.resourceContext.getResourceType() + " Resource with key ["
            + this.resourceContext.getResourceKey() + "]";
        Configuration pluginConfig = resourceContext.getPluginConfiguration();
        String type = pluginConfig.getSimple("type").getStringValue();
        String name = pluginConfig.getSimple("name").getStringValue();
        conn = DBusConnection.getConnection(DBusConnection.SYSTEM);
        if (ServiceDiscoveryComponent.SYS_V_SERVICE.equals(type)) {
            service = SysVService.load(name, conn);
        } else if (ServiceDiscoveryComponent.XINETD_SERVICE.equals(type)) {
            service = XinetdService.load(name, conn);
        }

    }

    @Override
    public void stop() {
        if (conn != null) {
            conn.disconnect();
        }
    }

    @Override
    public AvailabilityType getAvailability() {
        if (service.isSysVService()) {
            SysVService s = (SysVService) service;
            if (s.isRunning()) {
                return AvailabilityType.UP;
            } else {
                return AvailabilityType.DOWN;
            }
        }
        return AvailabilityType.UP;
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = new Configuration();
        String name = resourceContext.getPluginConfiguration().getSimple("name").getStringValue();
        config.put(new PropertySimple("name", name));
        config.put(new PropertySimple("enabled", service.isEnabled()));
        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // TODO Auto-generated method stub

    }

}
