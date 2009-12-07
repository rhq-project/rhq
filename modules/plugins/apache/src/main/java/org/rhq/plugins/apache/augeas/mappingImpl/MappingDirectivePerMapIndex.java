package org.rhq.plugins.apache.augeas.mappingImpl;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.plugins.apache.ApacheServerComponent;
import org.rhq.rhqtransform.AugeasRhqException;

public class MappingDirectivePerMapIndex extends MappingDirectivePerMap {

    @Override
    public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap, AugeasNode node) throws AugeasRhqException {
        PropertyMap map = super.createPropertyMap(propDefMap, node);
        map.put(new PropertySimple(ApacheServerComponent.AUXILIARY_INDEX_PROP, node.getSeq()));
        
        return map;
    }

    
}
