package org.rhq.plugins.apache.augeas;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingParamPerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingParamPerMapIndex;
import org.rhq.rhqtransform.AugeasToConfiguration;

public enum DirectiveMappingEnum {
	
	DirectivePerMap{ 
		public void execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef){
			AugeasToConfiguration config = new MappingDirectivePerMap();
			config.setTree(tree);
			config.loadResourceConfiguration(startNode, resourceConfigDef);
			
		};
	},
	ParamPerMap{
		public void execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef){
			AugeasToConfiguration config = new MappingParamPerMap();
			config.setTree(tree);
			config.loadResourceConfiguration(startNode, resourceConfigDef);
		};
	},
	ParamPerMapIndex{
		public void execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef){
			AugeasToConfiguration config = new MappingParamPerMapIndex();
			config.setTree(tree);
			config.loadResourceConfiguration(startNode, resourceConfigDef);
		};
	};

	public abstract void execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef);

}
