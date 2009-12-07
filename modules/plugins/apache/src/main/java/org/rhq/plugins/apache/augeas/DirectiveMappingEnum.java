package org.rhq.plugins.apache.augeas;

import java.util.Collection;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMap;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingDirectivePerMapIndex;
import org.rhq.plugins.apache.augeas.mappingImpl.MappingParamPerMap;
import org.rhq.rhqtransform.AugeasRhqException;
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
	},
	
	SimpleProp{
		public Configuration execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef){
			
			   Configuration resourceConfig = new Configuration();

		        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();

		        for (PropertyDefinition propDef : propDefs) {
		          
		       	   String propertyName = propDef.getName();
			
			       List<AugeasNode> simpleNode = startNode.getChildByLabel(propertyName);
		           
			       if (simpleNode.size() > 1) {
		            throw new AugeasRhqException("Found multiple values for a simple property " + propertyName);
		             }
		        
		            StringBuilder valueBld = new StringBuilder();
		        
		            List<AugeasNode> params = simpleNode.get(0).getChildByLabel("param");
		        
		           for(AugeasNode param : params) {
		               valueBld.append(param.getValue()).append(" ");
		            }
		        
		            valueBld.deleteCharAt(valueBld.length() - 1);
		        }
		        
		        return resourceConfig;
			
		};
	};

	public abstract Configuration execute(AugeasTree tree,AugeasNode startNode, ConfigurationDefinition resourceConfigDef);

}
