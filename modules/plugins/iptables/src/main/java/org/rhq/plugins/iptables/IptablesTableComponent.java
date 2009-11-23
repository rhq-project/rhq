package org.rhq.plugins.iptables;

import org.rhq.augeas.AugeasComponent;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;

public class IptablesTableComponent implements AugeasRHQComponent<AugeasRHQComponent>, ConfigurationFacet{

	private String tableName;
	private AugeasRHQComponent parentComponent;
	
	public void start(ResourceContext<AugeasRHQComponent> context)
			throws InvalidPluginConfigurationException, Exception {

		tableName =	context.getResourceKey();
	    parentComponent = context.getParentResourceComponent();
	}

	public void stop() {
				
	}

	public AvailabilityType getAvailability() {		
		return AvailabilityType.UP;
	}

	public Configuration loadResourceConfiguration() throws Exception {
		
		return null;
	}

	public void updateResourceConfiguration(ConfigurationUpdateReport arg0) {
			
	}
	
	public String getTableName(){
		return tableName;
	}

	public AugeasComponent getAugeasComponent() throws Exception {
		return parentComponent.getAugeasComponent(); 
	}

	public AugeasTree getAugeasTree() throws Exception {
		return parentComponent.getAugeasTree();
	}

}
