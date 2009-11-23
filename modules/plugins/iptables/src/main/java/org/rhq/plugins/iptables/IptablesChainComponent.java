package org.rhq.plugins.iptables;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.AugeasComponent;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;

public class IptablesChainComponent implements AugeasRHQComponent<IptablesTableComponent>, ConfigurationFacet{

	private String tableName;
	private String chainName;
	private AugeasTree augeasTree;
	private ResourceContext<IptablesTableComponent> context;
	private IptablesTableComponent parentComponent;
	private final Log log = LogFactory.getLog(this.getClass());
	
	public void start(ResourceContext<IptablesTableComponent> context)
			throws InvalidPluginConfigurationException, Exception {
		this.context = context;
		this.parentComponent = context.getParentResourceComponent();
		this.chainName = context.getResourceKey();
		this.tableName= context.getParentResourceComponent().getTableName();
	}

	public void stop() {
		
	}

	public AvailabilityType getAvailability() {
		return AvailabilityType.UP;
	}

	public Configuration loadResourceConfiguration() throws Exception {
		augeasTree = getAugeasTree();
		
		 ConfigurationDefinition resourceConfigDef = this.context.getResourceType()
          .getResourceConfigurationDefinition();
        
		IptablesConfigTransform trans = new IptablesConfigTransform(augeasTree);
		List<AugeasNode> nodes = getIptablesChainNode(chainName);
		Configuration config= null;
		if (!nodes.isEmpty())
		  config = trans.transform(nodes, resourceConfigDef);

		return config;
	}

	public void updateResourceConfiguration(ConfigurationUpdateReport config) {
		IptablesConfigTransform trans = new IptablesConfigTransform(augeasTree);
		trans.updateAugeas(config.getConfiguration(), new ArrayList<AugeasNode>());
	}
	
	public AugeasComponent getAugeasComponent() throws Exception {
		return parentComponent.getAugeasComponent();
	}

	public AugeasTree getAugeasTree() throws Exception {
		return parentComponent.getAugeasTree();
	}
	
	private List<AugeasNode> getIptablesChainNode(String chainName) throws Exception{
		if (augeasTree == null)
			return null;
		String expr = File.separatorChar+tableName+File.separatorChar+chainName;
		
		List<AugeasNode> nodes = augeasTree.matchRelative(augeasTree.getRootNode(), expr);
		
	   return nodes;
	}

}
