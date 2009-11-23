package org.rhq.rhqtransform;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.AbstractPropertyMap;
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
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

public class ConfigurationToAugeas {

	 private AugeasTree tree;
	 
	 public void updateResourceConfiguration(ConfigurationDefinition resourceConfigDef,Configuration resourceConfig) throws Exception{
	       
	        Collection<PropertyDefinition> propDefs = resourceConfigDef.getPropertyDefinitions().values();
	        for (PropertyDefinition propDef : propDefs) {
	            setNode(propDef, resourceConfig, tree.getRootNode());
	        }
	    }

	    protected void setNode(PropertyDefinition propDef, AbstractPropertyMap parentPropMap,
	        AugeasNode parentNode) throws Exception{
	    	
	        String propName = propDef.getName();
	        
	        AugeasNode node;
	        
	        if (propName.equals("."))
	        	node = parentNode;
	        else
	        	{
	        	List<AugeasNode> nodes = tree.matchRelative(parentNode, propName);
	        	if (nodes.isEmpty())
	        		throw new Exception("Error in mapping");
	        	node = nodes.get(0);
	        	}

	        if (isPropertyDefined(propDef, parentPropMap)) {
	            // The property *is* defined, which means we either need to add or update the corresponding node in the
	            // Augeas tree.
	            if (propDef instanceof PropertyDefinitionSimple) {
	                PropertyDefinitionSimple propDefSimple = (PropertyDefinitionSimple) propDef;
	                PropertySimple propSimple = parentPropMap.getSimple(propDefSimple.getName());
	                setNodeFromPropertySimple( node, propDefSimple, propSimple);
	            } else if (propDef instanceof PropertyDefinitionMap) {
	                PropertyDefinitionMap propDefMap = (PropertyDefinitionMap) propDef;
	                PropertyMap propMap = parentPropMap.getMap(propDefMap.getName());
	                setNodeFromPropertyMap(propDefMap, propMap, node);
	            } else if (propDef instanceof PropertyDefinitionList) {
	                PropertyDefinitionList propDefList = (PropertyDefinitionList) propDef;
	                PropertyList propList = parentPropMap.getList(propDefList.getName());
	                setNodeFromPropertyList(propDefList, propList, node);
	            } else {
	                throw new IllegalStateException("Unsupported PropertyDefinition subclass: "
	                    + propDef.getClass().getName());
	            }
	        } else {
	            // The property *is not* defined - remove the corresponding node from the Augeas tree if it exists.
	            node.remove();
	        }
	    }

	    protected void setNodeFromPropertySimple(AugeasNode node, PropertyDefinitionSimple propDefSimple,
	        PropertySimple propSimple) {
	        String value = propSimple.getStringValue();
	        
	            // Update the value of the existing node.
	            node.setValue(value);
	    }

	    protected void setNodeFromPropertyMap(PropertyDefinitionMap propDefMap, PropertyMap propMap,
	        AugeasNode mapNode) throws Exception{
	        for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
	            setNode(mapEntryPropDef, propMap, mapNode);
	        }
	    }

	    protected void setNodeFromPropertyList(PropertyDefinitionList propDefList, PropertyList propList,
	        AugeasNode listNode) {
	        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();

	        PropertyDefinitionMap listMemberPropDefMap = (PropertyDefinitionMap) listMemberPropDef;

	        int listIndex = 0;
	        String listMemberPropDefMapPath = listMemberPropDefMap.getName();
	        List<AugeasNode> existingListMemberNodes = listNode.getChildByLabel(listMemberPropDefMapPath);
	        
	        Set<AugeasNode> updatedListMemberNodes = new HashSet<AugeasNode>();
	        for (Property listMemberProp : propList.getList()) {
	            PropertyMap listMemberPropMap = (PropertyMap) listMemberProp;
	            AugeasNode memberNodeToUpdate = getExistingChildNodeForListMemberPropertyMap(listNode, propDefList,
	                listMemberPropMap);
	            if (memberNodeToUpdate != null) {
	                // Keep track of the existing nodes that we'll be updating, so that we can remove all other existing
	                // nodes.
	                updatedListMemberNodes.add(memberNodeToUpdate);
	            } else {
	                // The maps in the list are non-keyed, or there is no map in the list with the same key as the map
	                // being added, so create a new node for the map to add to the list.
	                AugeasNode basePathNode = getNewListMemberNode(listNode, listMemberPropDefMap, listIndex);
	                String var = "prop" + listIndex;
	                augeas.defineNode(var, basePathNode.getPath(), null);
	                memberNodeToUpdate = new AugeasNode("$" + var);
	                listIndex++;
	            }

	            // Update the node's children.
	            setNodeFromPropertyMap(listMemberPropDefMap, listMemberPropMap, augeas, memberNodeToUpdate);
	        }

	        // Now remove any existing nodes that we did not update in the previous loop.
	        for (AugeasNode existingListMemberNode : existingListMemberNodes) {
	            if (!updatedListMemberNodes.contains(existingListMemberNode)) {
	                augeas.remove(existingListMemberNode.getPath());
	            }
	        }
	    }

	    protected AugeasNode getNewListMemberNode(AugeasNode listNode, PropertyDefinitionMap listMemberPropDefMap,
	        int listIndex) {
	        return new AugeasNode(listNode, getAugeasPathRelativeToParent(listMemberPropDefMap, listNode, getAugeas())
	            + "[" + listIndex + "]");
	    }

	    private boolean isPropertyDefined(PropertyDefinition propDef, AbstractPropertyMap parentPropMap) {
	        Property prop = parentPropMap.get(propDef.getName());
	        if (prop == null) {
	            return false;
	        } else {
	            return (!(prop instanceof PropertySimple) || ((PropertySimple) prop).getStringValue() != null);
	        }
	    }
}
