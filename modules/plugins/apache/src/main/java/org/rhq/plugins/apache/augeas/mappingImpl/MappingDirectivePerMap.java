package org.rhq.plugins.apache.augeas.mappingImpl;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.mapping.ApacheDirectiveRegExpression;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.impl.AugeasToConfigurationSimple;

/**
 * A mapping strategy that creates a map for each directive it finds
 * in the augeas tree. The map is supposed to be enclosed in a list.  
 * The name of the map definition is supposed to represent the name of the directives
 * to create the maps for.
 * 
 * @author Lukas Krejci
 */
public class MappingDirectivePerMap extends AugeasToConfigurationSimple {

    @Override
    public Property createPropertyList(PropertyDefinitionList propDefList, AugeasNode node) throws AugeasRhqException {
        PropertyList propList = new PropertyList(propDefList.getName());

        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();

        List<AugeasNode> nodes = tree.matchRelative(node, listMemberPropDef.getName());

        for (AugeasNode nd : nodes) {
            propList.add(loadProperty(listMemberPropDef, nd));
        }

        return propList;
    }

    @Override
    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, AugeasNode node) throws AugeasRhqException {
        String directiveName = propDefMap.getName();

        List<String> params = ApacheDirectiveRegExpression.getParams(node);

        PropertyMap map = new PropertyMap(directiveName);

        int idx = 0;
        for (PropertyDefinition propDef : propDefMap.getPropertyDefinitions().values()) {
            if (propDef instanceof PropertyDefinitionSimple) {
                String value = params.get(idx);
                map.put(Util.createPropertySimple((PropertyDefinitionSimple) propDef, value));
            }
            idx++;
        }
        return map;
    }

}
