package org.rhq.modules.plugins.wildfly10;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.modules.plugins.wildfly10.json.ResultFailedException;

public class BroadcastGroupComponent extends BaseComponent {

    static final String JGROUP_CLUSTERS = "jgroups-cluster";

    private boolean jGroupClusterExists() throws Exception {
        try {
            this.readAttribute(JGROUP_CLUSTERS);
            return true;
        } catch (ResultFailedException ex) {
            return false;
        }
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        // If no jgroup cluster found, remove it from the update call, as this property was introduced in EAP 7.2
        try {
            if (!this.jGroupClusterExists()) {
                final ConfigurationDefinition configDef = context.getResourceType().getResourceConfigurationDefinition().copy();
                configDef.getPropertyDefinitions().remove(JGROUP_CLUSTERS);
                report.getConfiguration().remove(JGROUP_CLUSTERS);
                ConfigurationWriteDelegate delegate = new ConfigurationWriteDelegate(configDef, getASConnection(), address);
                delegate.updateResourceConfiguration(report);
            } else {
                super.updateResourceConfiguration(report);
            }
        } catch (Exception ex) {
            getLog().error("Error while updating a BroadcastGroup", ex);
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            report.setErrorMessage(ex.getMessage());
        }
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        // If no jgroup cluster found, remove it from the create call, as this property was introduced in EAP 7.2
        try {
            if (!this.jGroupClusterExists()) {
                ConfigurationDefinition configDef = report.getResourceType().getResourceConfigurationDefinition().copy();
                configDef.getPropertyDefinitions().remove(JGROUP_CLUSTERS);
                CreateResourceDelegate delegate = new CreateResourceDelegate(configDef, getASConnection(), address);
                return delegate.createResource(report);
            } else {
                return super.createResource(report);
            }
        } catch (Exception ex) {
            getLog().error("Error while creating a BroadcastGroup", ex);
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage(ex.getMessage());
            return report;
        }
    }
}
