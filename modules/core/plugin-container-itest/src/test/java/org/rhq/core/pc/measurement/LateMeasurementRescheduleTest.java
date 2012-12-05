package org.rhq.core.pc.measurement;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.plugins.test.SingleResourceDiscoveryComponent;
import org.rhq.plugins.test.measurement.BZ834019ResourceComponent;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.ResourceComponentInstances;
import org.rhq.test.arquillian.ResourceContainers;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * Test for BZ 834019.
 */
@RunDiscovery
public class LateMeasurementRescheduleTest extends Arquillian {

    @Deployment(name = "SingleMetricPlugin")
    @TargetsContainer("connected-pc-with-metric-collection")
    public static RhqAgentPluginArchive getTestPlugin() {
        RhqAgentPluginArchive pluginJar = ShrinkWrap
            .create(RhqAgentPluginArchive.class, "single-metric-plugin-1.0.jar");
        HashMap<String, String> replacements = new HashMap<String, String>();
        replacements.put("@@@discovery@@@", SingleResourceDiscoveryComponent.class.getName());
        replacements.put("@@@class@@@", BZ834019ResourceComponent.class.getName());
        return pluginJar.setPluginDescriptorFromTemplate("single-metric-rhq-plugin.xml", replacements).addClasses(
            SingleResourceDiscoveryComponent.class, BZ834019ResourceComponent.class);
    }

    @ArquillianResource
    private PluginContainer pluginContainer;

    @ArquillianResource
    public MockingServerServices serverServices;

    private FakeServerInventory fakeServerInventory;
    private FakeServerInventory.CompleteDiscoveryChecker discoveryCompleteChecker;

    @ResourceContainers(plugin = "SingleMetricPlugin", resourceType = "SingleMetricServer")
    private Set<ResourceContainer> containers;

    @ResourceComponentInstances(plugin = "SingleMetricPlugin", resourceType = "SingleMetricServer")
    private Set<BZ834019ResourceComponent> components;

    @BeforeDiscovery(testMethods = "testBZ834019")
    public void resetServerServices() throws Exception {
        serverServices.resetMocks();
        fakeServerInventory = new FakeServerInventory();
        discoveryCompleteChecker = fakeServerInventory.createAsyncDiscoveryCompletionChecker(1);

        // autoimport everything
        when(serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));

        // set up the metric schedules using the metric metadata to determine default intervals and enablement
        when(serverServices.getDiscoveryServerService().postProcessNewlyCommittedResources(any(Set.class))).then(
            fakeServerInventory.postProcessNewlyCommittedResources());
    }

    @AfterDiscovery
    public void waitForAsyncDiscoveries() throws Exception {
        if (discoveryCompleteChecker != null) {
            discoveryCompleteChecker.waitForDiscoveryComplete(10000);
        }
    }

    @Test(groups = "pc.itest.bz834019", priority = 20)
    public void testBZ834019() throws Exception {
        Assert.assertNotNull(pluginContainer);
        Assert.assertTrue(pluginContainer.isStarted());

        // make sure we have the resource container
        Assert.assertEquals(containers.size(), 1, "missing container");

        // make sure we have the resource component
        Assert.assertEquals(components.size(), 1, "missing component");

        assert containers.iterator().next().getResource().getInventoryStatus() == InventoryStatus.COMMITTED;

        BZ834019ResourceComponent server = this.components.iterator().next();

        // TODO do things to test BZ 834019
    }
}
