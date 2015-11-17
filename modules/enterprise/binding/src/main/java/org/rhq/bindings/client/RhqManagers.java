package org.rhq.bindings.client;

import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.bundle.BundleManagerRemote;
import org.rhq.enterprise.server.cloud.StorageNodeManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;
import org.rhq.enterprise.server.discovery.DiscoveryBossRemote;
import org.rhq.enterprise.server.drift.DriftManagerRemote;
import org.rhq.enterprise.server.drift.DriftTemplateManagerRemote;
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
import org.rhq.enterprise.server.resource.group.definition.GroupDefinitionManagerRemote;
import org.rhq.enterprise.server.search.SavedSearchManagerRemote;
import org.rhq.enterprise.server.storage.StorageClusterSettingsManagerRemote;
import org.rhq.enterprise.server.support.SupportManagerRemote;
import org.rhq.enterprise.server.sync.SynchronizationManagerRemote;
import org.rhq.enterprise.server.system.SystemManagerRemote;
import org.rhq.enterprise.server.tagging.TagManagerRemote;

/**
 * @author Lukas Krejci
 *
 * @deprecated since 4.10 do not use this. Use {@link RhqManager} instead.
 */
@Deprecated
public enum RhqManagers {
    AlertManager(AlertManagerRemote.class, "${AlertManager}"), //
    AlertDefinitionManager(AlertDefinitionManagerRemote.class, "${AlertDefinitionManager}"), //
    AvailabilityManager(AvailabilityManagerRemote.class, "${AvailabilityManager}"), //
    BundleManager(BundleManagerRemote.class, "${BundleManager}"), //
    CallTimeDataManager(CallTimeDataManagerRemote.class, "${CallTimeDataManager}"), //
    RepoManager(RepoManagerRemote.class, "${RepoManager}"), //
    ConfigurationManager(ConfigurationManagerRemote.class, "${ConfigurationManager}"), //
    ContentManager(ContentManagerRemote.class, "${ContentManager}"), //
    DataAccessManager(DataAccessManagerRemote.class, "${DataAccessManager}"), //
    DriftManager(DriftManagerRemote.class, "${DriftManager}"), //
    DriftTemplateManager(DriftTemplateManagerRemote.class, "${DriftTemplateManager}"), //
    DiscoveryBoss(DiscoveryBossRemote.class, "${DiscoveryBoss}"), //
    EventManager(EventManagerRemote.class, "${EventManager}"), //
    GroupDefinitionManager(GroupDefinitionManagerRemote.class, "${GroupDefinitionManager}"), //
    MeasurementBaselineManager(MeasurementBaselineManagerRemote.class, "${MeasurementBaselineManager}"), //
    MeasurementDataManager(MeasurementDataManagerRemote.class, "${MeasurementDataManager}"), //
    MeasurementDefinitionManager(MeasurementDefinitionManagerRemote.class, "${MeasurementDefinitionManager}"), //
    MeasurementScheduleManager(MeasurementScheduleManagerRemote.class, "${MeasurementScheduleManager}"), //
    OperationManager(OperationManagerRemote.class, "${OperationManager}"), //
    ResourceManager(ResourceManagerRemote.class, "${ResourceManager}"), //
    ResourceFactoryManager(ResourceFactoryManagerRemote.class, "${ResourceFactoryManager}"), //
    ResourceGroupManager(ResourceGroupManagerRemote.class, "${ResourceGroupManager}"), //
    ResourceTypeManager(ResourceTypeManagerRemote.class, "${ResourceTypeManager}"), //
    RoleManager(RoleManagerRemote.class, "${RoleManager}"), //
    SavedSearchManager(SavedSearchManagerRemote.class, "${SavedSearchManager}"), //
    StorageClusterSettingsManager(StorageClusterSettingsManagerRemote.class, "${StorageClusterSettingsManager}"), //
    StorageNodeManager(StorageNodeManagerRemote.class, "${StorageNodeManager}"), //
    SubjectManager(SubjectManagerRemote.class, "${SubjectManager}"), //
    SupportManager(SupportManagerRemote.class, "${SupportManager}"), //
    SystemManager(SystemManagerRemote.class, "${SystemManager}"), //
    RemoteInstallManager(RemoteInstallManagerRemote.class, "${RemoteInstallManager}"), //
    TagManager(TagManagerRemote.class, "${TagManager}"), //
    SynchronizationManager(SynchronizationManagerRemote.class, "${SynchronizationManager}");

    private Class<?> remote;
    private String localInterfaceClassName;
    private String beanName;
    private boolean enabled;


    private RhqManagers(Class<?> remote, String enable) {
        this.remote = remote;
        this.beanName = this.name() + "Bean";
        localInterfaceClassName = getLocalInterfaceClassName(remote);

        //defaults and evaluates to TRUE unless the string contains "false". Done to defend against
        //possible errors in string replacement during rhq build.
        this.enabled = true;
        if ((enable != null) && (enable.trim().length() > 0)) {
            this.enabled = (enable.trim().equalsIgnoreCase("false")) ? Boolean.FALSE : Boolean.TRUE;
        }
    }

    public static RhqManagers forInterface(Class<?> iface) {
        for (RhqManagers m : values()) {
            if (m.remote().equals(iface)) {
                return m;
            }
        }

        return null;
    }

    public Class<?> remote() {
        return this.remote;
    }

    /**
     * @deprecated since 4.6.0, use the {@link #remote()} method instead
     * @return the class name of the remote interface
     */
    @Deprecated
    public String remoteName() {
        return this.remote.getName();
    }

    public String localInterfaceClassName() {
        return localInterfaceClassName;
    }

    public String beanName() {
        return this.beanName;
    }

    public boolean enabled() {
        return this.enabled;
    }

    private static String getLocalInterfaceClassName(Class<?> remoteIface) {
        String ifaceName = remoteIface.getName();
        if (!ifaceName.endsWith("Remote")) {
            throw new AssertionError("Inconsistent SLSB naming in RHQ! Remote interface '" + remoteIface.getName()
                + "' does not follow the established naming convention. This is a bug, please report it.");
        }

        return (ifaceName.substring(0, ifaceName.lastIndexOf("Remote")) + "Local");
    }
}
