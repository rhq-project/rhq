package org.rhq.plugins.apache.augeas.mappingImpl;

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
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.mapping.ApacheDirectiveRegExpression;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.impl.AugeasToConfigurationSimple;

/**
 * The most complicated mapping strategy.
 * It is used when we have a list of maps in the configuration and
 * each map corresponds only to a parameter (or more) of a directive (i.e. single directive
 * can be mapped as multiple maps).
 * 
 * This tries to map each (set of) parameters of a directive as a standalone map.
 * 
 * The map definition's name is taken as the name of the directive to look for.
 * Then for all such directives, take all their parameters, chunk them up by the number of 
 * sub-properties of the map and assign params from each chunk to a new map.
 * 
 * Each map is supposed to contain the "_index" property ({@link ApacheServerComponent#AUXILIARY_INDEX_PROP})
 * which is set to the index of the corresponding directive in the config file.
 * 
 * @author Lukas Krejci
 */
public class MappingParamPerMap extends AugeasToConfigurationSimple {

    @Override
    public Property createPropertyList(PropertyDefinitionList propDefList, AugeasNode node) throws AugeasRhqException {
        PropertyList propList = new PropertyList(propDefList.getName());

        PropertyDefinition listMemberPropDef = propDefList.getMemberDefinition();

        if (!(listMemberPropDef instanceof PropertyDefinitionMap)) {
            return loadProperty(listMemberPropDef, node);
        }

        PropertyDefinitionMap mapDef = (PropertyDefinitionMap) listMemberPropDef;

        //now count how many properties there are in the member map
        int propCnt = mapDef.getPropertyDefinitions().size();
        //don't count the auxiliary "_index" property
        if (mapDef.getPropertyDefinitions().containsKey(ApacheServerComponent.AUXILIARY_INDEX_PROP))
            propCnt -= 1;

        //get all the directives
        List<AugeasNode> nodes = tree.matchRelative(node, mapDef.getName());

        for (AugeasNode directiveNode : nodes) {
            List<String> params = ApacheDirectiveRegExpression.getParams(directiveNode);

            for (int i = 0; i < params.size(); i += propCnt) {
                int idx = i;
                PropertyMap map = new PropertyMap(mapDef.getName());
                propList.add(map);
                for (PropertyDefinition def : mapDef.getPropertyDefinitions().values()) {
                    if (ApacheServerComponent.AUXILIARY_INDEX_PROP.equals(def.getName())) {
                        map.put(new PropertySimple(ApacheServerComponent.AUXILIARY_INDEX_PROP, directiveNode.getSeq()));
                    } else {
                        map.put(Util.createPropertySimple((PropertyDefinitionSimple) def, params.get(idx)));
                        idx++;
                    }
                }
            }
        }
        return propList;
    }
}
