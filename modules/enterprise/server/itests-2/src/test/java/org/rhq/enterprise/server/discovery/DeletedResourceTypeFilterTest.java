package org.rhq.enterprise.server.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jmock.Expectations;

import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.test.JMockTest;

import static org.testng.Assert.*;

public class DeletedResourceTypeFilterTest extends JMockTest {

    SubjectManagerLocal subjectMgr;

    ResourceTypeManagerLocal resourceTypeMgr;

    PluginManagerLocal pluginMgr;

    DeletedResourceTypeFilter filter;

    @BeforeMethod
    public void init() {
        subjectMgr = context.mock(SubjectManagerLocal.class);
        resourceTypeMgr = context.mock(ResourceTypeManagerLocal.class);
        pluginMgr = context.mock(PluginManagerLocal.class);
    }

    @Test
    public void acceptReportWithNoDeletedTypes() {
        InventoryReport report = createReport();
        report.addAddedRoot(new ResourceBuilder()
            .createRandomServer()
            .with(2).randomChildServices()
            .build());
        report.addAddedRoot(new ResourceBuilder()
            .createRandomService()
            .with(2).randomChildServices()
            .build());

        final List<Plugin> plugins = new ArrayList<Plugin>(getPluginsInReport(report.getAddedRoots()));

        context.checking(new Expectations() {{
            allowing(subjectMgr).getOverlord();
            will(returnValue(new Subject("overlord", true, true)));

            allowing(resourceTypeMgr).findResourceTypesByCriteria(with(aNonNull(Subject.class)),
                with(aNonNull(ResourceTypeCriteria.class)));
            will(returnValue(new PageList<ResourceType>()));

            allowing(pluginMgr).getInstalledPlugins();
            will(returnValue(plugins));
        }});

        filter = new DeletedResourceTypeFilter(subjectMgr, resourceTypeMgr, pluginMgr);

        assertTrue(filter.accept(report), "Expected report to be accepted when it does not contain any deleted " +
            "resource types");
    }

    @Test
    public void rejectReportWithDeletedTypes() {
        ResourceType deletedServiceType = new ResourceTypeBuilder()
            .createServiceResourceType()
            .withName("TestService")
            .withPlugin("TestPlugin")
            .thatIsDeleted()
            .build();

        final PageList<ResourceType> deletedTypes = new PageList<ResourceType>();
        deletedTypes.add(deletedServiceType);

        InventoryReport report = createReport();
        report.addAddedRoot(new ResourceBuilder()
            .createRandomServer()
            .withChildService()
                .withName("ChildService")
                .withUuid("c1")
                .withResourceType(new ResourceTypeBuilder()
                    .createServiceResourceType()
                    .withName("TestService")
                    .withPlugin("TestPlugin")
                    .build())
                .included()
            .build());

        final List<Plugin> plugins = new ArrayList<Plugin>(getPluginsInReport(report.getAddedRoots()));

        context.checking(new Expectations() {{
            allowing(subjectMgr).getOverlord();
            will(returnValue(new Subject("overlord", true, true)));

            allowing(resourceTypeMgr).findResourceTypesByCriteria(with(aNonNull(Subject.class)),
                with(aNonNull(ResourceTypeCriteria.class)));
            will(returnValue(deletedTypes));

            allowing(pluginMgr).getInstalledPlugins();
            will(returnValue(plugins));
        }});

        filter = new DeletedResourceTypeFilter(subjectMgr, resourceTypeMgr, pluginMgr);

        assertFalse(filter.accept(report), "Expected report to be rejected since it contains deleted resource types");
    }

    @Test
    public void rejectReportWithPluginThatIsNotInstalled() {
        InventoryReport report = createReport();
        report.addAddedRoot(new ResourceBuilder()
            .createRandomServer()
            .with(2).randomChildServices()
            .build());
        report.addAddedRoot(new ResourceBuilder()
            .createRandomService()
            .withResourceType(new ResourceTypeBuilder()
                .createServerResourceType()
                .withName("StaleServer")
                .withPlugin("DeletedPlugin")
                .build())
            .with(2).randomChildServices()
            .build());

        List<Plugin> plugins = new ArrayList<Plugin>(getPluginsInReport(report.getAddedRoots()));
        final List<Plugin> installedPlugins = removePlugins(plugins, "DeletedPlugin");

        context.checking(new Expectations() {{
            allowing(subjectMgr).getOverlord();
            will(returnValue(new Subject("overlord", true, true)));

            allowing(resourceTypeMgr).findResourceTypesByCriteria(with(aNonNull(Subject.class)),
                with(aNonNull(ResourceTypeCriteria.class)));
            will(returnValue(new PageList<ResourceType>()));

            allowing(pluginMgr).getInstalledPlugins();
            will(returnValue(installedPlugins));
        }});

        filter = new DeletedResourceTypeFilter(subjectMgr, resourceTypeMgr, pluginMgr);

        assertFalse(filter.accept(report), "Expected report to be rejected since it contains a deleted plugin");
    }

    InventoryReport createReport() {
        Agent agent = new Agent("localhost", "localhost", 1234, "1234", "test-token");
        return new InventoryReport(agent);
    }

    private Set<Plugin> getPluginsInReport(Set<Resource> resources) {
        Set<Plugin> plugins = new HashSet<Plugin>();
        for (Resource resource : resources) {
            String name = resource.getResourceType().getPlugin();
            plugins.add(new Plugin(name, "/plugins/" + name));
            plugins.addAll(getPluginsInReport(resource.getChildResources()));
        }
        return plugins;
    }

    private List<Plugin> removePlugins(List<Plugin> plugins, String... pluginsToRemove) {
        Set<String> removedPlugins = new HashSet<String>(Arrays.asList(pluginsToRemove));
        List<Plugin> filteredPlugins = new ArrayList<Plugin>();

        for (Plugin plugin : plugins) {
            if (!removedPlugins.contains(plugin.getName())) {
                filteredPlugins.add(plugin);
            }
        }
        return filteredPlugins;
    }

}
