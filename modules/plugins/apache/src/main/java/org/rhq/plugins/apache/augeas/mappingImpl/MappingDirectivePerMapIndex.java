package org.rhq.plugins.apache.augeas.mappingImpl;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.augeas.ApacheDirectiveRegExpression;
import org.rhq.rhqtransform.AugeasRhqException;

public class MappingDirectivePerMapIndex extends MappingDirectivePerMap {

    @Override
    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, AugeasNode node) throws AugeasRhqException {
        String directiveName = propDefMap.getName();

        List<String> params = ApacheDirectiveRegExpression.getParams(node);

        PropertyMap map = new PropertyMap(directiveName);

        int idx = 0;
        for (PropertyDefinition propDef : propDefMap.getPropertyDefinitions().values()) {
            if (propDef instanceof PropertyDefinitionSimple) {
                if (ApacheServerComponent.AUXILIARY_INDEX_PROP.equals(propDef.getName())) {
                    map.put(new PropertySimple(ApacheServerComponent.AUXILIARY_INDEX_PROP, node.getSeq()));
                    continue;
                }
                String value = params.get(idx);
                map.put(Util.createPropertySimple((PropertyDefinitionSimple) propDef, value));
            }
            idx++;
        }
        return map;
    }

    
}
