package org.rhq.plugins.apache.augeas.mappingImpl;

import java.util.Collection;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.mapping.ConfigurationToAugeasApacheBase;
import org.rhq.rhqtransform.AugeasRhqException;

public class MappingToAugeasParamPerMap extends ConfigurationToAugeasApacheBase{

	public void updateList(PropertyDefinitionList propDef, Property prop,
			AugeasNode listNode, int seq) throws AugeasRhqException {
		
       String propertyName = propDef.getName();
       PropertyDefinition memberPropDef = propDef.getMemberDefinition();
       List<AugeasNode> nodes = tree.matchRelative(listNode, propertyName);
       
       for (AugeasNode node : nodes){
    	   updateProperty(memberPropDef, prop, node, seq);
       }
	}

	public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
			AugeasNode mapNode, int seq) throws AugeasRhqException {
		
		PropertyMap propMap = (PropertyMap) prop;
		List<AugeasNode> paramNodes = tree.matchRelative(mapNode, "param");
		Collection<PropertyDefinition> definitions = propDefMap.getPropertyDefinitions().values();
		
		if (definitions.size()>paramNodes.size()){
			for (int i=0;i<definitions.size()-paramNodes.size();i++)
				tree.createNode(mapNode, "param", null, definitions.size()+i);
		}
		
		if (definitions.size()<paramNodes.size()){
			for (int i=0;i<paramNodes.size()-paramNodes.size();i++)
				{
				tree.removeNode(paramNodes.get(paramNodes.size()-i), false);
				}
		}
		
		paramNodes = tree.matchRelative(mapNode, "param");
		
		int i=0;
        
		for (PropertyDefinition propDef : propDefMap.getPropertyDefinitions().values()){
            PropertySimple valProp = (PropertySimple) propMap.get(propDef.getName());
            String value = valProp.getStringValue();
            paramNodes.get(i).setValue(value);
            i++;
        }
		
	}

	public void updateSimple(AugeasNode parentNode,
			PropertyDefinitionSimple propDef, Property prop, int seq)
			throws AugeasRhqException {
		
	}
}
