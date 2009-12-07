package org.rhq.plugins.apache.augeas;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.impl.AugeasToConfigurationSimple;

public class AugeasToApacheConfiguration extends AugeasToConfigurationSimple {

    public AugeasToApacheConfiguration() {
        super();
    }

    public Property createPropertySimple(PropertyDefinitionSimple propDefSimple, AugeasNode node) throws AugeasRhqException {
        String propertyName = propDefSimple.getName();

        List<AugeasNode> simpleNode = node.getChildByLabel(propertyName);
        if (simpleNode.size() > 1) {
            throw new AugeasRhqException("Found multiple values for a simple property " + propertyName);
        }
        
        StringBuilder valueBld = new StringBuilder();
        List<AugeasNode> params = simpleNode.get(0).getChildByLabel("param");
        for(AugeasNode param : params) {
            valueBld.append(param.getValue()).append(" ");
        }
        valueBld.deleteCharAt(valueBld.length() - 1);
        
        return new PropertySimple(propertyName, valueBld.toString());
    }

    public Property createPropertyList(PropertyDefinitionList propDefList, AugeasNode node) throws AugeasRhqException {

        PropertyList propList = new PropertyList(propDefList.getName());

        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();

        List<AugeasNode> nodes = tree.matchRelative(node, listMemberPropDef.getName());

        for (AugeasNode nd : nodes) {
            propList.add(loadProperty(listMemberPropDef, nd));
        }

        return propList;
    }

    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, AugeasNode node) throws AugeasRhqException {
        PropertyMap propMap = new PropertyMap(propDefMap.getName());
        for (PropertyDefinition mapEntryPropDef : propDefMap.getPropertyDefinitions().values()) {
            propMap.put(loadProperty(mapEntryPropDef, node));
        }
        return propMap;
    }

}
