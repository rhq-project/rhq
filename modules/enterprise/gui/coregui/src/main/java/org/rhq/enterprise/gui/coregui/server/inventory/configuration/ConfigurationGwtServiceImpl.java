package org.rhq.enterprise.gui.coregui.server.inventory.configuration;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.inventory.configuration.ConfigurationGwtService;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.HibernateDetachUtility;
import org.rhq.enterprise.server.util.LookupUtil;

public class ConfigurationGwtServiceImpl extends RemoteServiceServlet implements ConfigurationGwtService {


    public Configuration getResourceConfiguration(int resourceId) {

        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        Configuration configuration = configurationManager.getResourceConfiguration(subjectManager.getOverlord(), resourceId);

        try {
            HibernateDetachUtility.nullOutUninitializedFields(configuration, HibernateDetachUtility.SerializationType.SERIALIZATION);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configuration;
    }

    public ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId) {
        ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        ConfigurationDefinition definition = configurationManager.getResourceConfigurationDefinitionWithTemplatesForResourceType(subjectManager.getOverlord(), resourceTypeId);
        try {
            HibernateDetachUtility.nullOutUninitializedFields(definition, HibernateDetachUtility.SerializationType.SERIALIZATION);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return definition;
    }
}