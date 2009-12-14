package org.rhq.plugins.apache.augeas.mappingImpl;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.apache.mapping.ConfigurationToAugeasApacheBase;
import org.rhq.rhqtransform.AugeasRhqException;
/**
 * 
 * @author Filip Drabek
 *
 */
public class MappingToAugeasDirectivePerMapIndex extends ConfigurationToAugeasApacheBase{

	public void updateList(PropertyDefinitionList propDef, Property prop,
			AugeasNode listNode, int seq) throws AugeasRhqException {
		// TODO Auto-generated method stub
		
	}

	public void updateMap(PropertyDefinitionMap propDefMap, Property prop,
			AugeasNode mapNode, int seq) throws AugeasRhqException {
		// TODO Auto-generated method stub
		
	}

	public void updateSimple(AugeasNode parentNode,
			PropertyDefinitionSimple propDef, Property prop, int seq)
			throws AugeasRhqException {
		// TODO Auto-generated method stub
		
	}

}
