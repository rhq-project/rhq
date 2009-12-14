package org.rhq.plugins.apache.augeas.mappingImpl;

import java.io.File;
import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.mapping.ConfigurationToAugeasApacheBase;
import org.rhq.rhqtransform.AugeasRhqException;

public class MappingToAugeasDirectiveToSimple extends ConfigurationToAugeasApacheBase{

	
	public void updateList(PropertyDefinitionList propDef, Property prop,
			AugeasNode listNode, int seq) throws AugeasRhqException {
		throw new AugeasRhqException("Error in configuration. There can not be any ListProperty in simple directive.");
		
	}

	public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
			AugeasNode mapNode, int seq) throws AugeasRhqException {
		throw new AugeasRhqException("Error in configuration.  There can not be any MapProperty in simple directive.");
	}

	public void updateSimple(AugeasNode parentNode,
			PropertyDefinitionSimple propDef, Property prop, int seq)
			throws AugeasRhqException {
		
		String propName = propDef.getName();
		String propertyValue = ((PropertySimple) prop).getStringValue();
		
		List<AugeasNode> nodes = tree.matchRelative(parentNode, propName);
		try {
		//NODE WAS DELETED IN CONFIGURATION
	    if (propertyValue == null)
	       {
	    	for (AugeasNode nod : nodes)
	    		nod.remove(false);
	        return;
	       }
	    
	    //NODE IS NOT PRESENT IN AUGEAS
	    if (nodes.isEmpty())
	    	{
	    	tree.createNode(parentNode, propName+File.separator+"param", propertyValue, 0);
	    	return;
	    	}
	    
	    //NODE IS PRESENT IN AUGEAS AND IN CONFIGURATION AND WILL BE UPDATED
	    if (nodes.size()>1)
	    	throw new AugeasRhqException("Error in configuration. Directive"+propName+" is declared multiple times.");
	    
	    List<AugeasNode> params = nodes.get(0).getChildByLabel("param");
	    
	    if (params.isEmpty())
	    	throw new AugeasRhqException("Error in configuration. Directive"+propName+" has no value.");
	    
	    if (params.size() > 1)
	    	throw new AugeasRhqException("Too many parameters in directive"+propName+" .");
	    
	    AugeasNode node = params.get(0);
	    node.setValue(propertyValue);
	    
		}catch(Exception e){
			throw new AugeasRhqException("Mapping configuration to Augeas failed. "+e.getMessage());
		}
	}

}
