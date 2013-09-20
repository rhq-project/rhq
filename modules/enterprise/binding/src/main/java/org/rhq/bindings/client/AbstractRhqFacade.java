package org.rhq.bindings.client;

import java.util.HashMap;
import java.util.Map;

import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.bundle.BundleManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;
import org.rhq.enterprise.server.discovery.DiscoveryBossRemote;
import org.rhq.enterprise.server.drift.DriftManagerRemote;
import org.rhq.enterprise.server.event.EventManagerRemote;
import org.rhq.enterprise.server.install.remote.RemoteInstallManagerRemote;
import org.rhq.enterprise.server.measurement.AvailabilityManagerRemote;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.search.SavedSearchManagerRemote;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.sync.SynchronizationManagerRemote;
import org.rhq.enterprise.server.system.SystemManagerRemote;
import org.rhq.enterprise.server.tagging.TagManagerRemote;

/**
 * This is a support base class for the implementations of the RhqFacade interface that
 * implements the deprecated methods by the means of the new version of the {@link RhqFacade} methods.
 *
 * @author Lukas Krejci
 * @since 4.10
 */
public abstract class AbstractRhqFacade implements RhqFacade {

    @Override
    public AlertDefinitionManagerRemote getAlertDefinitionManager() {
        return getProxy(AlertDefinitionManagerRemote.class);
    }

    @Override
    public AlertManagerRemote getAlertManager() {
        return getProxy(AlertManagerRemote.class);
    }

    @Override
    public AvailabilityManagerRemote getAvailabilityManager() {
        return getProxy(AvailabilityManagerRemote.class);
    }

    @Override
    public BundleManagerRemote getBundleManager() {
        return getProxy(BundleManagerRemote.class);
    }

    @Override
    public CallTimeDataManagerRemote getCallTimeDataManager() {
        return getProxy(CallTimeDataManagerRemote.class);
    }

    @Override
    public ConfigurationManagerRemote getConfigurationManager() {
        return getProxy(ConfigurationManagerRemote.class);
    }

    @Override
    public ContentManagerRemote getContentManager() {
        return getProxy(ContentManagerRemote.class);
    }

    @Override
    public DataAccessManagerRemote getDataAccessManager() {
        return getProxy(DataAccessManagerRemote.class);
    }

    @Override
    public DiscoveryBossRemote getDiscoveryBoss() {
        return getProxy(DiscoveryBossRemote.class);
    }

    @Override
    public DriftManagerRemote getDriftManager() {
        return getProxy(DriftManagerRemote.class);
    }

    @Override
    public EventManagerRemote getEventManager() {
        return getProxy(EventManagerRemote.class);
    }

    @Override
    public Map<RhqManagers, Object> getManagers() {
        HashMap<RhqManagers, Object> ret = new HashMap<RhqManagers, Object>();

        for(RhqManagers m  : RhqManagers.values()) {
            ret.put(m, getProxy(m.remote()));
        }

        return ret;
    }

    @Override
    public MeasurementBaselineManagerRemote getMeasurementBaselineManager() {
        return getProxy(MeasurementBaselineManagerRemote.class);
    }

    @Override
    public MeasurementDataManagerRemote getMeasurementDataManager() {
        return getProxy(MeasurementDataManagerRemote.class);
    }

    @Override
    public MeasurementDefinitionManagerRemote getMeasurementDefinitionManager() {
        return getProxy(MeasurementDefinitionManagerRemote.class);
    }

    @Override
    public MeasurementScheduleManagerRemote getMeasurementScheduleManager() {
        return getProxy(MeasurementScheduleManagerRemote.class);
    }

    @Override
    public OperationManagerRemote getOperationManager() {
        return getProxy(OperationManagerRemote.class);
    }

    @Override
    public RemoteInstallManagerRemote getRemoteInstallManager() {
        return getProxy(RemoteInstallManagerRemote.class);
    }

    @Override
    public RepoManagerRemote getRepoManager() {
        return getProxy(RepoManagerRemote.class);
    }

    @Override
    public ResourceFactoryManagerRemote getResourceFactoryManager() {
        return getProxy(ResourceFactoryManagerRemote.class);
    }

    @Override
    public ResourceGroupManagerRemote getResourceGroupManager() {
        return getProxy(ResourceGroupManagerRemote.class);
    }

    @Override
    public ResourceManagerRemote getResourceManager() {
        return getProxy(ResourceManagerRemote.class);
    }

    @Override
    public ResourceTypeManagerRemote getResourceTypeManager() {
        return getProxy(ResourceTypeManagerRemote.class);
    }

    @Override
    public RoleManagerRemote getRoleManager() {
        return getProxy(RoleManagerRemote.class);
    }

    @Override
    public SavedSearchManagerRemote getSavedSearchManager() {
        return getProxy(SavedSearchManagerRemote.class);
    }

    @Override
    public SubjectManagerRemote getSubjectManager() {
        return getProxy(SubjectManagerRemote.class);
    }

    @Override
    public SupportManagerRemote getSupportManager() {
        return getProxy(SupportManagerRemote.class);
    }

    @Override
    public SynchronizationManagerRemote getSynchronizationManager() {
        return getProxy(SynchronizationManagerRemote.class);
    }

    @Override
    public SystemManagerRemote getSystemManager() {
        return getProxy(SystemManagerRemote.class);
    }

    @Override
    public TagManagerRemote getTagManager() {
        return getProxy(TagManagerRemote.class);
    }
}
