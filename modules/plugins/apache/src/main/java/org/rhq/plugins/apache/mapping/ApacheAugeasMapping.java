package org.rhq.plugins.apache.mapping;

import java.util.Collection;

import org.rhq.augeas.AugeasProxy;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.rhqtransform.RhqAugeasMapping;

public class ApacheAugeasMapping implements RhqAugeasMapping {

	private AugeasTree tree;
	
	public ApacheAugeasMapping(AugeasTree tree) {
	    this.tree = tree;
	}
	
	public Configuration updateConfiguration(AugeasNode node,ConfigurationDefinition configDef){

        Configuration resourceConfig = new Configuration();

        Collection<PropertyDefinition> propDefs = configDef.getPropertyDefinitions().values();

        for (PropertyDefinition propDef : propDefs) {
        	DirectiveMappingEnum mapping = ApacheDirectiveRegExpression.getMappingType(propDef.getName());
        	ConfigurationDefinition def = new ConfigurationDefinition("temp","");
        	 def.put(propDef);
        	Configuration conf = mapping.mapToConfiguration(tree, node, def);
        	for (Property prop : conf.getProperties())
        	{
        		resourceConfig.put(prop);
        	}
        }
        return resourceConfig;
	}
	
    public void updateAugeas(AugeasProxy augeasComponent, Configuration config, ConfigurationDefinition configDef) {
     
    }

    public Configuration updateConfiguration(AugeasProxy augeasComponent, ConfigurationDefinition configDef) {
        return null;
    }
    
    public void updateAugeas(AugeasNode node,Configuration config,ConfigurationDefinition configDef){
    	  
          Collection<PropertyDefinition> propDefs = configDef.getPropertyDefinitions().values();

          for (PropertyDefinition propDef : propDefs) {
          	DirectiveMappingEnum mapping = ApacheDirectiveRegExpression.getMappingType(propDef.getName());
          	ConfigurationDefinition def = new ConfigurationDefinition("temp","");
          	  def.put(propDef);
          	Configuration configuration = new Configuration();
          	  configuration.put(config.get(propDef.getName()));
          	mapping.mapToAugeas(tree, node, config, def);
          }
    }

}
