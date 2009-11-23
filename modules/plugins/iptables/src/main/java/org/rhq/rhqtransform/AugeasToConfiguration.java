package org.rhq.rhqtransform;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

public class AugeasToConfiguration {
	
	private final Log log = LogFactory.getLog(this.getClass());
    private AugeasTree tree;
    private NameMap nameMap;
    
	public AugeasToConfiguration(){
	  	
	}
	
	public void setTree(AugeasTree tree)
	{
		this.tree = tree;
	}
	
	public void setNameMap(NameMap nameMap){
		this.nameMap = nameMap;
	}
	
	  public Configuration loadResourceConfiguration(AugeasNode startNode,ConfigurationDefinition resourceConfigDef) throws Exception {

	        Configuration resourceConfig = new Configuration();
	       
	        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();

	        for (PropertyDefinition propDef : propDefs) {
	            resourceConfig.put(loadProperty(propDef,startNode));
	        }

	        return resourceConfig;
	    }

	    public Property loadProperty(PropertyDefinition propDef, AugeasNode parentNode) throws Exception{
	    	   
	        Property prop;
	        if (propDef instanceof PropertyDefinitionSimple) {
	            prop = createPropertySimple((PropertyDefinitionSimple) propDef, parentNode);
	        } else if (propDef instanceof PropertyDefinitionMap) {
	            prop = createPropertyMap((PropertyDefinitionMap) propDef,  parentNode);
	        } else if (propDef instanceof PropertyDefinitionList) {
	            prop = createPropertyList((PropertyDefinitionList) propDef,  parentNode);
	        } else {
	            throw new IllegalStateException("Unsupported PropertyDefinition subclass: " + propDef.getClass().getName());
	        }
	       return prop;
	    }

	    public Property createPropertySimple(PropertyDefinitionSimple propDefSimple, AugeasNode node) throws Exception{
	        Object value;
	        value = node.getValue();
	        return new PropertySimple(propDefSimple.getName(), value);
	    }

	    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, AugeasNode node) throws Exception {
	        PropertyMap propMap = new PropertyMap(propDefMap.getName());
	        for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
	            propMap.put(loadProperty(mapEntryPropDef, node));
	        }
	        return propMap;
	    }

	    public Property createPropertyList(PropertyDefinitionList propDefList, AugeasNode node) throws Exception{
	        	        
	    	PropertyList propList = new PropertyList(propDefList.getName());
	    	 
	        List<AugeasNode> nodes = tree.matchRelative(node, propDefList.getName());
	        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();
	       
	        for (AugeasNode listMemberNode : nodes){
	        	propList.add(loadProperty(listMemberPropDef,listMemberNode));
	        }

	        return propList;
	    }
}
