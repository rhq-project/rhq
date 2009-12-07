package org.rhq.plugins.apache.augeas;

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
	
	public Configuration resourceConfigDef(AugeasNode node,ConfigurationDefinition configDef){

        Configuration resourceConfig = new Configuration();

        Collection<PropertyDefinition> propDefs = configDef.getPropertyDefinitions().values();

        for (PropertyDefinition propDef : propDefs) {
        	DirectiveMappingEnum mapping = ApacheDirectiveRegExpression.getMappingType(propDef.getName());
        	ConfigurationDefinition def = new ConfigurationDefinition("temp","");
        	def.put(propDef);
        	Configuration conf = mapping.execute(tree, node, def);
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

}
