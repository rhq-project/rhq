package org.rhq.plugins.iptables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.AugeasComponent;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.rhqtransform.RhqConfig;

public class IptablesComponent implements AugeasRHQComponent, ConfigurationFacet{

	private ResourceContext context;
	private final Log log = LogFactory.getLog(this.getClass());
	private AugeasTree augeasTree;
	private AugeasComponent augeasComponent;
	
	public void start(ResourceContext context)
			throws InvalidPluginConfigurationException, Exception {
		this.context = context;
	}

	public void stop() {
		
	}

	public AvailabilityType getAvailability() {
		return AvailabilityType.UP;
		}

	public Configuration loadResourceConfiguration() throws Exception {
		Configuration pluginConfiguration = new Configuration();
		
		AugeasTree tree=null;
		AugeasComponent augeas =null;
		try {	
			 RhqConfig config = new RhqConfig(context.getPluginConfiguration());
			 augeas = new AugeasComponent(config);
			 augeas.load();
			 tree = augeas.getAugeasTree("Iptables", true);
			 tree.load();
			 
		}catch(Exception e)
		{
			log.error(e.getMessage());
			
		}
	return pluginConfiguration;
	}
	

	public void updateResourceConfiguration(ConfigurationUpdateReport report) {
		Configuration pluginConfiguration = new Configuration();
		AugeasTree tree=null;
		AugeasComponent augeas =null;
		try {	
			 RhqConfig config = new RhqConfig(context.getPluginConfiguration());
			 augeas = new AugeasComponent(config);
			 
			 tree = augeas.getAugeasTree("Iptables", false);
			 
			 
		}catch(Exception e)
		{
			log.error(e);
		}
		
	}

	public void loadAugeas() throws Exception{		
		RhqConfig config = new RhqConfig(context.getPluginConfiguration());
		augeasComponent = new AugeasComponent(config);
		augeasComponent.load();
		augeasTree = augeasComponent.getAugeasTree("Iptables", true);
		augeasTree.load();
			 
	}
	public AugeasComponent getAugeasComponent() throws Exception{
       if (augeasComponent == null)
    	   loadAugeas();
       
       return augeasComponent;
	}

	public AugeasTree getAugeasTree() throws Exception {
		 if (augeasTree == null)
	    	   loadAugeas();
	       
	       return augeasTree;
	}

}
