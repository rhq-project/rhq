package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.composite.ResourceConfigurationComposite;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.group.GroupPluginConfigurationUpdate;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupPluginConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * API for resource and plugin configurations for resources and groups.
 */
@RemoteServiceRelativePath("ConfigurationGWTService")
public interface ConfigurationGWTService extends RemoteService {

    void purgePluginConfigurationUpdates(int[] configUpdateIds, boolean purgeInProgress) throws RuntimeException;

    void purgeResourceConfigurationUpdates(int[] configUpdateIds, boolean purgeInProgress) throws RuntimeException;

    void rollbackPluginConfiguration(int resourceId, int configHistoryId) throws RuntimeException;

    void rollbackResourceConfiguration(int resourceId, int configHistoryId) throws RuntimeException;

    ResourceConfigurationUpdate getLatestResourceConfigurationUpdate(int resourceId) throws RuntimeException;

    PluginConfigurationUpdate getLatestPluginConfigurationUpdate(int resourceId) throws RuntimeException;

    Configuration getPluginConfiguration(int resourceId) throws RuntimeException;

    ConfigurationDefinition getPluginConfigurationDefinition(int resourceTypeId) throws RuntimeException;

    Configuration getResourceConfiguration(int resourceId) throws RuntimeException;

    ConfigurationDefinition getResourceConfigurationDefinition(int resourceTypeId) throws RuntimeException;

    PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdates(Integer resourceId, Long beginDate,
        Long endDate, boolean suppressOldest, PageControl pc) throws RuntimeException;

    ResourceConfigurationUpdate updateResourceConfiguration(int resourceId, Configuration configuration)
        throws RuntimeException;

    PluginConfigurationUpdate updatePluginConfiguration(int resourceId, Configuration configuration)
        throws RuntimeException;

    PageList<ResourceConfigurationUpdate> findResourceConfigurationUpdatesByCriteria(
        ResourceConfigurationUpdateCriteria criteria) throws RuntimeException;

    PageList<PluginConfigurationUpdate> findPluginConfigurationUpdatesByCriteria(
        PluginConfigurationUpdateCriteria criteria) throws RuntimeException;

    PageList<GroupResourceConfigurationUpdate> findGroupResourceConfigurationUpdatesByCriteria(
        GroupResourceConfigurationUpdateCriteria criteria) throws RuntimeException;

    PageList<GroupPluginConfigurationUpdate> findGroupPluginConfigurationUpdatesByCriteria(
        GroupPluginConfigurationUpdateCriteria criteria) throws RuntimeException;

    List<DisambiguationReport<ResourceConfigurationComposite>> findResourceConfigurationsForGroup(int groupId)
        throws RuntimeException;

    List<DisambiguationReport<ResourceConfigurationComposite>> findPluginConfigurationsForGroup(int groupId)
        throws RuntimeException;

    List<DisambiguationReport<ResourceConfigurationComposite>> findPluginConfigurationsForGroupUpdate(int groupUpdateId)
        throws RuntimeException;

    void updateResourceConfigurationsForGroup(int groupId, List<ResourceConfigurationComposite> resourceConfigurations)
        throws RuntimeException;

    void updatePluginConfigurationsForGroup(int groupId, List<ResourceConfigurationComposite> pluginConfigurations)
        throws RuntimeException;

    void deleteGroupPluginConfigurationUpdate(Integer groupId, Integer[] groupPluginConfigUpdateIds)
        throws RuntimeException;

    void deleteGroupResourceConfigurationUpdate(Integer groupId, Integer[] groupResourceConfigUpdateIds)
        throws RuntimeException;

    //RawConfiguration dummy(RawConfiguration config) throws RuntimeException;
}
