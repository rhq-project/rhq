package org.rhq.plugins.apache.augeas.mappingImpl;

import java.util.List;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.plugins.apache.mapping.ApacheDirectiveRegExpression;
import org.rhq.rhqtransform.AugeasRhqException;

/**
 * A mapping strategy similar to {@link MappingDirectivePerMap}.
 * In addition to base class, the map definition is checked for
 * a property called {@link ApacheServerComponent#AUXILIARY_INDEX_PROP}
 * that is supposed to contain the index of the directive inside the
 * configuration file and if found the property is set the appropriate
 * value.
 * 
 * @author Lukas Krejci
 */
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
