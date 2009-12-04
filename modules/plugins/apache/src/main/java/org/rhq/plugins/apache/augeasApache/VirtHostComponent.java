package org.rhq.plugins.apache.augeasApache;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.rhqtransform.AugeasRHQComponent;

public class VirtHostComponent implements AugeasRHQComponent,ConfigurationFacet{

	public AugeasProxy getAugeasComponent() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public AugeasTree getAugeasTree() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public void start(ResourceContext context)
			throws InvalidPluginConfigurationException, Exception {
		// TODO Auto-generated method stub
		
	}

	public void stop() {
		// TODO Auto-generated method stub
		
	}

	public AvailabilityType getAvailability() {
		// TODO Auto-generated method stub
		return null;
	}

	public Configuration loadResourceConfiguration() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateResourceConfiguration(ConfigurationUpdateReport report) {
		// TODO Auto-generated method stub
		
	}

}
