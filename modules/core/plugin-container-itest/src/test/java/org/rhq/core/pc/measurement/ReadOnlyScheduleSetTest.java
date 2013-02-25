package org.rhq.core.pc.measurement;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import org.rhq.core.pc.inventory.ForceAvailabilityExecutor;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.plugins.test.SingleResourceDiscoveryComponent;
import org.rhq.plugins.test.measurement.BZ821058ResourceComponent;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.ResourceComponentInstances;
import org.rhq.test.arquillian.ResourceContainers;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * Test for BZ 821058
 */
@RunDiscovery
public class ReadOnlyScheduleSetTest extends Arquillian {

    @Deployment(name = "TwoMetricPlugin")
    @TargetsContainer("connected-pc-with-metric-collection")
    public static RhqAgentPluginArchive getTestPlugin() {
        RhqAgentPluginArchive pluginJar = ShrinkWrap.create(RhqAgentPluginArchive.class, "bz821058-plugin-1.0.jar");
        HashMap<String, String> replacements = new HashMap<String, String>();
        replacements.put("@@@discovery@@@", SingleResourceDiscoveryComponent.class.getName());
        replacements.put("@@@class@@@", BZ821058ResourceComponent.class.getName());
        replacements.put("@@@metric1.interval@@@", "30000");
        replacements.put("@@@metric2.interval@@@", "30000");
        return pluginJar.setPluginDescriptorFromTemplate("two-metric-rhq-plugin.xml", replacements).addClasses(
            SingleResourceDiscoveryComponent.class, BZ821058ResourceComponent.class);
    }

    @ArquillianResource
    private PluginContainer pluginContainer;

    @ArquillianResource
    public MockingServerServices serverServices;

    private FakeServerInventory fakeServerInventory;
    private FakeServerInventory.CompleteDiscoveryChecker discoveryCompleteChecker;

    @ResourceContainers(plugin = "TwoMetricPlugin", resourceType = "TwoMetricServer")
    private Set<ResourceContainer> containers;

    @ResourceComponentInstances(plugin = "TwoMetricPlugin", resourceType = "TwoMetricServer")
    private Set<BZ821058ResourceComponent> components;

    @BeforeDiscovery(testMethods = "testBZ821058")
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

            // Since the avail job is not running, make sure our discovered resources get their initial UP avail
            new ForceAvailabilityExecutor(pluginContainer.getInventoryManager()).call();
        }
    }

    @Test(groups = "pc.itest.bz821058", priority = 20)
    public void testBZ821058() throws Exception {
        Assert.assertNotNull(pluginContainer);
        Assert.assertTrue(pluginContainer.isStarted());

        // make sure we have the resource container
        Assert.assertEquals(containers.size(), 1, "missing container");

        // make sure we have the resource component
        Assert.assertEquals(components.size(), 1, "missing component");

        assert containers.iterator().next().getResource().getInventoryStatus() == InventoryStatus.COMMITTED;

        BZ821058ResourceComponent server = this.components.iterator().next();
        // collection interval is set to 30s, and our container "connected-pc-with-metric-collection"
        // is configured to start collecting metrics after an initial delay of 10s (see arquillian.xml).
        // So let's give the test some time so the measurement facet can be called.
        server.getValuesLatch.await(45, TimeUnit.SECONDS);

        assert !server.errors.isEmpty() : "there should have been exceptions that occurred in the getValues method";
    }
}
