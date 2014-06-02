package org.rhq.enterprise.server.resource.metadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory.Context;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinition.BaseDirectory;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.event.EventDefinition;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

@Test(groups = { "plugin.extension.metadata", "plugin.metadata" })
public class PluginExtensionMetadataTest extends MetadataBeanTest {

    private static final String PLUGIN_NAME_PARENT = "PluginExtensionMetadataParentTestPlugin";
    private static final String PLUGIN_NAME_CHILD = "PluginExtensionMetadataChildTestPlugin";
    private static final String TYPE_NAME_PARENT = "ParentServerA";
    private static final String TYPE_NAME_CHILD = "ChildServerA";

    // names of things from the first version of the plugin metadata
    private static final String SUBCAT = "A-subcat";
    private static final String SUBCAT_DISPLAYNAME = "a subcat";
    private static final String PC_GROUP = "A-pc-group";
    private static final boolean PC_GROUP_HIDDEN = true;
    private static final String PC_PROP = "A-pc-prop";
    private static final boolean PC_PROP_REQUIRED = false;
    private static final String PROCESS_SCAN_NAME = "A-process-scan";
    private static final String PROCESS_SCAN_QUERY = "process|basename|match=A.exe";
    private static final String OP_NAME = "A-op";
    private static final int OP_TIMEOUT = 123456;
    private static final String OP_DESC = "a op";
    private static final String METRIC_PROP = "A-metric";
    private static final long METRIC_DEFAULT_INTERVAL = 123456L;
    private static final String EVENT_NAME = "A-event";
    private static final String EVENT_DESC = "a event";
    private static final String RC_PROP = "A-rc-prop";
    private static final boolean RC_PROP_REQUIRED = false;
    private static final String DRIFT_DEF_NAME = "A-drift-def";
    private static final BaseDirValueContext DRIFT_DEF_BASEDIR_CONTEXT = BaseDirValueContext.pluginConfiguration;
    private static final String DRIFT_DEF_BASEDIR_VALUE = PC_PROP;
    private static final String BUNDLE_TARGET_NAME = "A-bundle-basedir";
    private static final Context BUNDLE_BASEDIR_CONTEXT = Context.resourceConfiguration;
    private static final String BUNDLE_BASEDIR_VALUE = RC_PROP;

    // names of things from the second, updated version of the plugin metadata
    // updated plugin config
    private static final String CHANGED_PC_GROUP = PC_GROUP; // we only change its members, not the name itself
    private static final boolean CHANGED_PC_GROUP_HIDDEN = false;
    private static final String CHANGED_PC_PROP = "A-pc-prop-CHANGED";
    private static final boolean CHANGED_PC_PROP_REQUIRED = true;
    private static final String NEW_PC_GROUP = "A-pc-group-NEW";
    private static final boolean NEW_PC_GROUP_HIDDEN = true;
    private static final String NEW_PC_PROP = "A-pc-prop-NEW";
    private static final boolean NEW_PC_PROP_REQUIRED = false;
    // updated process scans
    private static final String CHANGED_PROCESS_SCAN_NAME = "A-process-scan";
    private static final String CHANGED_PROCESS_SCAN_QUERY = "process|basename|match=CHANGED.exe";
    private static final String NEW_PROCESS_SCAN_NAME = "A-process-scan-NEW";
    private static final String NEW_PROCESS_SCAN_QUERY = "process|basename|match=NEW.exe";
    // updated operations
    private static final String CHANGED_OP_NAME = OP_NAME; // we don't actually change its name in our test
    private static final int CHANGED_OP_TIMEOUT = 987654;
    private static final String CHANGED_OP_DESC = "a changed op";
    private static final String NEW_OP_NAME = "A-op-NEW";
    private static final int NEW_OP_TIMEOUT = 111111;
    private static final String NEW_OP_DESC = "a new op";
    // updated metrics
    private static final String CHANGED_METRIC_PROP = METRIC_PROP; // we don't actually change its name in our test
    private static final String NEW_METRIC_PROP = "A-metric-NEW";
    private static final long NEW_METRIC_DEFAULT_INTERVAL = 98765L;
    // updated events
    private static final String CHANGED_EVENT_NAME = "A-event-CHANGED";
    private static final String CHANGED_EVENT_DESC = "a changed event";
    private static final String NEW_EVENT_NAME = "A-event-NEW";
    private static final String NEW_EVENT_DESC = "a new event";
    // updated resource config
    private static final String CHANGED_RC_PROP = "A-rc-prop-CHANGED";
    private static final boolean CHANGED_RC_PROP_REQUIRED = true;
    private static final String NEW_RC_PROP = "A-rc-prop-NEW";
    private static final boolean NEW_RC_PROP_REQUIRED = false;
    // updated drift
    private static final String CHANGED_DRIFT_DEF_NAME = "A-drift-def-CHANGED";
    private static final BaseDirValueContext CHANGED_DRIFT_DEF_BASEDIR_CONTEXT = BaseDirValueContext.pluginConfiguration;
    private static final String CHANGED_DRIFT_DEF_BASEDIR_VALUE = PC_PROP;
    private static final String NEW_DRIFT_DEF_NAME = "A-drift-def-NEW";
    private static final BaseDirValueContext NEW_DRIFT_DEF_BASEDIR_CONTEXT = BaseDirValueContext.resourceConfiguration;
    private static final String NEW_DRIFT_DEF_BASEDIR_VALUE = RC_PROP;
    // updated bundle
    private static final String CHANGED_BUNDLE_TARGET_NAME = "A-bundle-basedir-CHANGED";
    private static final Context CHANGED_BUNDLE_BASEDIR_CONTEXT = Context.resourceConfiguration;
    private static final String CHANGED_BUNDLE_BASEDIR_VALUE = RC_PROP;
    private static final String NEW_BUNDLE_TARGET_NAME = "A-bundle-basedir-NEW";
    private static final Context NEW_BUNDLE_BASEDIR_CONTEXT = Context.pluginConfiguration;
    private static final String NEW_BUNDLE_BASEDIR_VALUE = PC_PROP;

    private SubjectManagerLocal subjectMgr;
    private ResourceTypeManagerLocal resourceTypeMgr;

    public void testRegisterPlugins() throws Exception {
        subjectMgr = LookupUtil.getSubjectManager();
        resourceTypeMgr = LookupUtil.getResourceTypeManager();

        registerParentPluginV1(); // create an initial type (called the parent)
        registerChildPluginV1(); // using plugin extension mechanism, create a child type that extends that parent type
        registerParentPluginV2(); // update the parent type
        checkChildPlugin(); // check that the changes to the parent type propogated to the child
    }

    // this needs to be the last test executed in the class, it does cleanup
    @Test(priority = 10, alwaysRun = true, dependsOnMethods = { "testRegisterPlugins" })
    public void afterClassWorkTest() throws Exception {
        afterClassWork();
    }

    private void registerParentPluginV1() throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createPlugin("parent-plugin.jar", "1.0", "parent_plugin_v1.xml");
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_PARENT, PLUGIN_NAME_PARENT);
        assert resourceType.getName().equals(TYPE_NAME_PARENT);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_PARENT);
        assertVersion1(resourceType);
    }

    private void registerChildPluginV1() throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createPlugin("child-plugin.jar", "1.0", "child_plugin_v1.xml");
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD, PLUGIN_NAME_CHILD);
        assert resourceType.getName().equals(TYPE_NAME_CHILD);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD);
        assertVersion1(resourceType);
    }

    private void registerParentPluginV2() throws Exception {
        // register the plugin, load the new type and test to make sure its what we expect
        createPlugin("parent-plugin.jar", "2.0", "parent_plugin_v2.xml");
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_PARENT, PLUGIN_NAME_PARENT);
        assert resourceType.getName().equals(TYPE_NAME_PARENT);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_PARENT);
        assertVersion2(resourceType);
    }

    private void checkChildPlugin() throws Exception {
        // load the child type and test to make sure it has been updated to what we expect
        ResourceType resourceType = loadResourceTypeFully(TYPE_NAME_CHILD, PLUGIN_NAME_CHILD);
        assert resourceType.getName().equals(TYPE_NAME_CHILD);
        assert resourceType.getPlugin().equals(PLUGIN_NAME_CHILD);
        assertVersion2(resourceType);
    }

    private void assertVersion1(ResourceType resourceType) {
        PropertyGroupDefinition group;
        PropertyDefinition prop;
        ProcessScan processScan;
        OperationDefinition op;
        MeasurementDefinition metric;
        EventDefinition event;
        DriftDefinitionTemplate drift;
        BaseDirectory driftBasedir;
        ResourceTypeBundleConfiguration bundle;
        BundleDestinationBaseDirectory bundleBasedir;

        assert resourceType.getChildSubCategories() == null;

        assert resourceType.getPluginConfigurationDefinition().getGroupDefinitions().size() == 1;
        group = resourceType.getPluginConfigurationDefinition().getGroupDefinitions().get(0);
        assert group.getName().equals(PC_GROUP);
        assert group.isDefaultHidden() == PC_GROUP_HIDDEN;
        prop = resourceType.getPluginConfigurationDefinition().get(PC_PROP);
        assert prop != null;
        assert prop.getName().equals(PC_PROP);
        assert prop.isRequired() == PC_PROP_REQUIRED;
        assert prop.getPropertyGroupDefinition().getName().equals(PC_GROUP);

        assert resourceType.getProcessScans().size() == 1;
        processScan = resourceType.getProcessScans().iterator().next();
        assert processScan.getName().equals(PROCESS_SCAN_NAME);
        assert processScan.getQuery().equals(PROCESS_SCAN_QUERY);

        assert resourceType.getOperationDefinitions().size() == 1;
        op = resourceType.getOperationDefinitions().iterator().next();
        assert op.getName().equals(OP_NAME);
        assert op.getTimeout().intValue() == OP_TIMEOUT;
        assert op.getDescription().equals(OP_DESC);

        assert resourceType.getMetricDefinitions().size() == 2; // include built-in Availability metric
        metric = resourceType.getMetricDefinitions().iterator().next();
        assert metric.getName().equals(METRIC_PROP);
        assert metric.getDefaultInterval() == METRIC_DEFAULT_INTERVAL;

        assert resourceType.getEventDefinitions().size() == 1;
        event = resourceType.getEventDefinitions().iterator().next();
        assert event.getName().equals(EVENT_NAME);
        assert event.getDescription().equals(EVENT_DESC);

        assert resourceType.getResourceConfigurationDefinition().getGroupDefinitions().size() == 0;
        prop = resourceType.getResourceConfigurationDefinition().get(RC_PROP);
        assert prop != null;
        assert prop.getName().equals(RC_PROP);
        assert prop.isRequired() == RC_PROP_REQUIRED;

        assert resourceType.getDriftDefinitionTemplates().size() == 1;
        drift = resourceType.getDriftDefinitionTemplates().iterator().next();
        assert drift.getTemplateDefinition().getName().equals(DRIFT_DEF_NAME);
        driftBasedir = drift.getTemplateDefinition().getBasedir();
        assert driftBasedir.getValueContext().equals(DRIFT_DEF_BASEDIR_CONTEXT);
        assert driftBasedir.getValueName().equals(DRIFT_DEF_BASEDIR_VALUE);

        bundle = resourceType.getResourceTypeBundleConfiguration();
        assert bundle.getBundleDestinationBaseDirectories().size() == 1;
        bundleBasedir = bundle.getBundleDestinationBaseDirectories().iterator().next();
        assert bundleBasedir.getName().equals(BUNDLE_TARGET_NAME);
        assert bundleBasedir.getValueContext().equals(BUNDLE_BASEDIR_CONTEXT);
        assert bundleBasedir.getValueName().equals(BUNDLE_BASEDIR_VALUE);
    }

    private void assertVersion2(ResourceType resourceType) {
        PropertyDefinition prop;
        Set<String> seen = new HashSet<String>(2); // we use to this remember names of the things that we've seen

        assert resourceType.getChildSubCategories() == null;

        seen.clear();
        ConfigurationDefinition pcDef = resourceType.getPluginConfigurationDefinition();
        assert pcDef.getGroupDefinitions().size() == 2;
        for (PropertyGroupDefinition group : pcDef.getGroupDefinitions()) {
            seen.add(group.getName());
            if (group.getName().equals(CHANGED_PC_GROUP)) {
                assert group.isDefaultHidden() == CHANGED_PC_GROUP_HIDDEN;
            } else if (group.getName().equals(NEW_PC_GROUP)) {
                assert group.isDefaultHidden() == NEW_PC_GROUP_HIDDEN;
            } else {
                assert false : "Unexpected group [" + group.getName() + "]:" + group;
            }
        }
        if (seen.size() != 2) {
            assert false : "did not see what we expected to see: " + seen;
        }

        prop = pcDef.get(CHANGED_PC_PROP);
        assert prop != null;
        assert prop.getName().equals(CHANGED_PC_PROP);
        assert prop.isRequired() == CHANGED_PC_PROP_REQUIRED;
        assert prop.getPropertyGroupDefinition().getName().equals(CHANGED_PC_GROUP);
        prop = pcDef.get(NEW_PC_PROP);
        assert prop != null;
        assert prop.getName().equals(NEW_PC_PROP);
        assert prop.isRequired() == NEW_PC_PROP_REQUIRED;
        assert prop.getPropertyGroupDefinition().getName().equals(NEW_PC_GROUP);

        seen.clear();
        assert resourceType.getProcessScans().size() == 2;
        for (ProcessScan processScan : resourceType.getProcessScans()) {
            seen.add(processScan.getName());
            if (processScan.getName().equals(CHANGED_PROCESS_SCAN_NAME)) {
                assert processScan.getQuery().equals(CHANGED_PROCESS_SCAN_QUERY);
            } else if (processScan.getName().equals(NEW_PROCESS_SCAN_NAME)) {
                assert processScan.getQuery().equals(NEW_PROCESS_SCAN_QUERY);
            } else {
                assert false : "Unexpected process scan[" + processScan.getName() + "]:" + processScan;
            }
        }
        if (seen.size() != 2) {
            assert false : "did not see what we expected to see: " + seen;
        }

        seen.clear();
        assert resourceType.getOperationDefinitions().size() == 2;
        for (OperationDefinition op : resourceType.getOperationDefinitions()) {
            seen.add(op.getName());
            if (op.getName().equals(CHANGED_OP_NAME)) {
                assert op.getTimeout().intValue() == CHANGED_OP_TIMEOUT;
                assert op.getDescription().equals(CHANGED_OP_DESC);
            } else if (op.getName().equals(NEW_OP_NAME)) {
                assert op.getTimeout().intValue() == NEW_OP_TIMEOUT;
                assert op.getDescription().equals(NEW_OP_DESC);
            } else {
                assert false : "Unexpected operation [" + op.getName() + "]:" + op;
            }
        }
        if (seen.size() != 2) {
            assert false : "did not see what we expected to see: " + seen;
        }

        seen.clear();
        assert resourceType.getMetricDefinitions().size() == 3; // include built-in Availability metric
        for (MeasurementDefinition metric : resourceType.getMetricDefinitions()) {
            if (metric.getName().equals(MeasurementDefinition.AVAILABILITY_NAME)) {
                // expected, ignore
                continue;
            }

            seen.add(metric.getName());
            if (metric.getName().equals(CHANGED_METRIC_PROP)) {
                // even though our _v2 plugin set this to something different, our upgrade doesn't change it because
                // we don't want to overwrite changes a user possibly made to the defaut interval (aka metric template)
                assert metric.getDefaultInterval() == METRIC_DEFAULT_INTERVAL;
            } else if (metric.getName().equals(NEW_METRIC_PROP)) {
                assert metric.getDefaultInterval() == NEW_METRIC_DEFAULT_INTERVAL;
            } else {
                assert false : "Unexpected metric [" + metric.getName() + "]:" + metric;
            }
        }
        if (seen.size() != 2) {
            assert false : "did not see what we expected to see: " + seen;
        }

        seen.clear();
        assert resourceType.getEventDefinitions().size() == 2;
        for (EventDefinition event : resourceType.getEventDefinitions()) {
            seen.add(event.getName());
            if (event.getName().equals(CHANGED_EVENT_NAME)) {
                assert event.getDescription().equals(CHANGED_EVENT_DESC);
            } else if (event.getName().equals(NEW_EVENT_NAME)) {
                assert event.getDescription().equals(NEW_EVENT_DESC);
            } else {
                assert false : "Unexpected event [" + event.getName() + "]:" + event;
            }
        }
        if (seen.size() != 2) {
            assert false : "did not see what we expected to see: " + seen;
        }

        assert resourceType.getResourceConfigurationDefinition().getGroupDefinitions().size() == 0;
        prop = resourceType.getResourceConfigurationDefinition().get(CHANGED_RC_PROP);
        assert prop != null;
        assert prop.getName().equals(CHANGED_RC_PROP);
        assert prop.isRequired() == CHANGED_RC_PROP_REQUIRED;
        prop = resourceType.getResourceConfigurationDefinition().get(NEW_RC_PROP);
        assert prop != null;
        assert prop.getName().equals(NEW_RC_PROP);
        assert prop.isRequired() == NEW_RC_PROP_REQUIRED;

        seen.clear();
        assert resourceType.getDriftDefinitionTemplates().size() == 2;
        for (DriftDefinitionTemplate drift : resourceType.getDriftDefinitionTemplates()) {
            DriftDefinition def = drift.getTemplateDefinition();
            seen.add(def.getName());
            if (def.getName().equals(CHANGED_DRIFT_DEF_NAME)) {
                BaseDirectory driftBasedir = def.getBasedir();
                assert driftBasedir.getValueContext().equals(CHANGED_DRIFT_DEF_BASEDIR_CONTEXT);
                assert driftBasedir.getValueName().equals(CHANGED_DRIFT_DEF_BASEDIR_VALUE);
            } else if (def.getName().equals(NEW_DRIFT_DEF_NAME)) {
                BaseDirectory driftBasedir = def.getBasedir();
                assert driftBasedir.getValueContext().equals(NEW_DRIFT_DEF_BASEDIR_CONTEXT);
                assert driftBasedir.getValueName().equals(NEW_DRIFT_DEF_BASEDIR_VALUE);
            } else {
                assert false : "Unexpected drift def [" + def.getName() + "]:" + def;
            }
        }
        if (seen.size() != 2) {
            assert false : "did not see what we expected to see: " + seen;
        }

        seen.clear();
        ResourceTypeBundleConfiguration bundle = resourceType.getResourceTypeBundleConfiguration();
        assert bundle.getBundleDestinationBaseDirectories().size() == 2;
        for (BundleDestinationBaseDirectory bundleBasedir : bundle.getBundleDestinationBaseDirectories()) {
            seen.add(bundleBasedir.getName());
            if (bundleBasedir.getName().equals(CHANGED_BUNDLE_TARGET_NAME)) {
                assert bundleBasedir.getValueContext().equals(CHANGED_BUNDLE_BASEDIR_CONTEXT);
                assert bundleBasedir.getValueName().equals(CHANGED_BUNDLE_BASEDIR_VALUE);
            } else if (bundleBasedir.getName().equals(NEW_BUNDLE_TARGET_NAME)) {
                assert bundleBasedir.getValueContext().equals(NEW_BUNDLE_BASEDIR_CONTEXT);
                assert bundleBasedir.getValueName().equals(NEW_BUNDLE_BASEDIR_VALUE);
            } else {
                assert false : "Unexpected bundle basedir [" + bundleBasedir.getName() + "]:" + bundleBasedir;
            }
        }
        if (seen.size() != 2) {
            assert false : "did not see what we expected to see: " + seen;
        }
    }

    private ResourceType loadResourceTypeFully(String typeName, String typePlugin) {
        ResourceTypeCriteria c = new ResourceTypeCriteria();
        c.addFilterName(typeName);
        c.addFilterPluginName(typePlugin);
        c.setStrict(true);
        c.fetchPluginConfigurationDefinition(true);
        c.fetchProcessScans(true);
        c.fetchOperationDefinitions(true);
        c.fetchMetricDefinitions(true);
        c.fetchEventDefinitions(true);
        c.fetchResourceConfigurationDefinition(true);
        c.fetchDriftDefinitionTemplates(true);
        c.fetchBundleConfiguration(true);
        List<ResourceType> t = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(), c);
        ResourceType resourceType = t.get(0);
        return resourceType;
    }
}
