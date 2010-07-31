package org.rhq.enterprise.gui.coregui.client.gwt;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.configuration.ConfigurationUpdateStillInProgressException;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("ConfigurationGWTService")
public interface ConfigurationGWTService extends RemoteService {

    Configuration getPluginConfiguration(int resourceId);

    ConfigurationDefinition getPluginConfigurationDefinition(int resourceTypeId);

    Configuration getResourceConfiguration(int resourceId);

    ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId);

    PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdates(
            Integer resourceId, Long beginDate, Long endDate, boolean suppressOldest, PageControl pc);


    ResourceConfigurationUpdate updateResourceConfiguration(int resourceId, Configuration configuration);

    PluginConfigurationUpdate updatePluginConfiguration(int resourceId, Configuration configuration);

    PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdatesByCriteria(ResourceConfigurationUpdateCriteria criteria);

    RawConfiguration dummy(RawConfiguration config);

}
