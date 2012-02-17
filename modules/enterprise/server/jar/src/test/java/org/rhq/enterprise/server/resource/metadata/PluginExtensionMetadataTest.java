package org.rhq.enterprise.server.resource.metadata;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory;
import org.rhq.core.domain.bundle.ResourceTypeBundleConfiguration.BundleDestinationBaseDirectory.Context;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
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

    private static final String SUBCAT = "A-subcat";
    private static final String PC_GROUP = "A-pc-group";
    private static final String PC_PROP = "A-pc-prop";
    private static final String PROCESS_SCAN_NAME = "A-process-scan";
    private static final String PROCESS_SCAN_QUERY = "process|basename|match=A.exe";
    private static final String OP_NAME = "A-op";
    private static final String METRIC_PROP = "A-metric";
    private static final long METRIC_DEFAULT_INTERVAL = 123456L;
    private static final String EVENT_NAME = "A-event";
    private static final String RC_PROP = "A-rc-prop";
    private static final String DRIFT_DEF_NAME = "A-drift-def";
    private static final BaseDirValueContext DRIFT_DEF_BASEDIR_CONTEXT = BaseDirValueContext.pluginConfiguration;
    private static final String DRIFT_DEF_BASEDIR_VALUE = PC_PROP;
    private static final String BUNDLE_TARGET_NAME = "A-bundle-basedir";
    private static final Context BUNDLE_BASEDIR_CONTEXT = Context.resourceConfiguration;
    private static final String BUNDLE_BASEDIR_VALUE = RC_PROP;

    private SubjectManagerLocal subjectMgr;
    private ResourceTypeManagerLocal resourceTypeMgr;

    @BeforeMethod
    public void beforeMethod() {
        subjectMgr = LookupUtil.getSubjectManager();
        resourceTypeMgr = LookupUtil.getResourceTypeManager();
    }

    public void testRegisterPlugins() throws Exception {
        registerParentPluginV1();
        registerChildPluginV1();
        registerParentPluginV2();
        checkChildPlugin();
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
        assert resourceType.getChildSubCategories().size() == 1;
        assert resourceType.getChildSubCategories().get(0).getName().equals(SUBCAT);

        assert resourceType.getPluginConfigurationDefinition().getGroupDefinitions().size() == 1;
        group = resourceType.getPluginConfigurationDefinition().getGroupDefinitions().get(0);
        assert group.getName().equals(PC_GROUP);
        prop = resourceType.getPluginConfigurationDefinition().get(PC_PROP);
        assert prop.getName().equals(PC_PROP);

        assert resourceType.getProcessScans().size() == 1;
        processScan = resourceType.getProcessScans().iterator().next();
        assert processScan.getName().equals(PROCESS_SCAN_NAME);
        assert processScan.getQuery().equals(PROCESS_SCAN_QUERY);

        assert resourceType.getOperationDefinitions().size() == 1;
        op = resourceType.getOperationDefinitions().iterator().next();
        assert op.getName().equals(OP_NAME);

        assert resourceType.getMetricDefinitions().size() == 1;
        metric = resourceType.getMetricDefinitions().iterator().next();
        assert metric.getName().equals(METRIC_PROP);

        assert resourceType.getEventDefinitions().size() == 1;
        event = resourceType.getEventDefinitions().iterator().next();
        assert event.getName().equals(EVENT_NAME);

        assert resourceType.getResourceConfigurationDefinition().getGroupDefinitions().size() == 0;
        prop = resourceType.getResourceConfigurationDefinition().get(RC_PROP);
        assert prop.getName().equals(RC_PROP);

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
        assert resourceType.getChildSubCategories().size() == 1;
        assert resourceType.getChildSubCategories().get(0).getName().equals(SUBCAT);

        assert resourceType.getPluginConfigurationDefinition().getGroupDefinitions().size() == 1;
        group = resourceType.getPluginConfigurationDefinition().getGroupDefinitions().get(0);
        assert group.getName().equals(PC_GROUP);
        prop = resourceType.getPluginConfigurationDefinition().get(PC_PROP);
        assert prop.getName().equals(PC_PROP);

        assert resourceType.getProcessScans().size() == 1;
        processScan = resourceType.getProcessScans().iterator().next();
        assert processScan.getName().equals(PROCESS_SCAN_NAME);
        assert processScan.getQuery().equals(PROCESS_SCAN_QUERY);

        assert resourceType.getOperationDefinitions().size() == 1;
        op = resourceType.getOperationDefinitions().iterator().next();
        assert op.getName().equals(OP_NAME);

        assert resourceType.getMetricDefinitions().size() == 1;
        metric = resourceType.getMetricDefinitions().iterator().next();
        assert metric.getName().equals(METRIC_PROP);
        // even though our _v2 plugin set this to something different, our upgrade doesn't change it because
        // we don't want to overwrite changes a user possibly made to the defaut interval (aka metric template)
        assert metric.getDefaultInterval() == METRIC_DEFAULT_INTERVAL;

        assert resourceType.getEventDefinitions().size() == 1;
        event = resourceType.getEventDefinitions().iterator().next();
        assert event.getName().equals(EVENT_NAME);

        assert resourceType.getResourceConfigurationDefinition().getGroupDefinitions().size() == 0;
        prop = resourceType.getResourceConfigurationDefinition().get(RC_PROP);
        assert prop.getName().equals(RC_PROP);

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

    private ResourceType loadResourceTypeFully(String typeName, String typePlugin) {
        ResourceTypeCriteria c = new ResourceTypeCriteria();
        c.addFilterName(typeName);
        c.addFilterPluginName(typePlugin);
        c.setStrict(true);
        c.fetchSubCategories(true);
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
