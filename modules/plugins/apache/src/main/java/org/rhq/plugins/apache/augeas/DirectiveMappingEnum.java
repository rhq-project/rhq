package org.rhq.plugins.apache.augeas;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingParamPerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMapIndex;
import org.rhq.rhqtransform.AugeasToConfiguration;

public enum DirectiveMappingEnum {
	
	DirectivePerMap{ 
		public Configuration execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef){
			AugeasToConfiguration config = new MappingDirectivePerMap();
			config.setTree(tree);
			return config.loadResourceConfiguration(startNode, resourceConfigDef);
			
		};
	},
	ParamPerMap{
		public Configuration execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef){
			AugeasToConfiguration config = new MappingParamPerMap();
			config.setTree(tree);
			return config.loadResourceConfiguration(startNode, resourceConfigDef);
		};
	},
	DirectivePerMapIndex{
		public Configuration execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef){
			AugeasToConfiguration config = new MappingDirectivePerMapIndex();
			config.setTree(tree);
			return config.loadResourceConfiguration(startNode, resourceConfigDef);
		};
	};

	public abstract Configuration execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef);

}
