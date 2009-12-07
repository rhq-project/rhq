package org.rhq.plugins.apache.augeas.mappingImpl;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.rhqtransform.AugeasRhqException;
import org.rhq.rhqtransform.AugeasToConfiguration;
import org.rhq.rhqtransform.NameMap;

public class MappingDirectivePerMap implements AugeasToConfiguration{

	public Property createPropertyList(PropertyDefinitionList propDefList,
			AugeasNode node) throws AugeasRhqException {
		// TODO Auto-generated method stub
		return null;
	}

	public PropertyMap createPropertyMap(PropertyDefinitionMap propDefMap,
			AugeasNode node) throws AugeasRhqException {
		// TODO Auto-generated method stub
		return null;
	}

	public Property createPropertySimple(
			PropertyDefinitionSimple propDefSimple, AugeasNode node)
			throws AugeasRhqException {
		// TODO Auto-generated method stub
		return null;
	}

	public Property loadProperty(PropertyDefinition propDef,
			AugeasNode parentNode) throws AugeasRhqException {
		// TODO Auto-generated method stub
		return null;
	}

	public Configuration loadResourceConfiguration(AugeasNode startNode,
			ConfigurationDefinition resourceConfigDef)
			throws AugeasRhqException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setNameMap(NameMap nameMap) {
		// TODO Auto-generated method stub
		
	}

	public void setTree(AugeasTree tree) {
		// TODO Auto-generated method stub
		
	}

}
