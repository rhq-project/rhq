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
import org.rhq.plugins.test.measurement.BZ834019ResourceComponent.CollectedMetric;
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

        // do things to test BZ 834019. Here's what is happening:
        // - server has metric1 and metric2 with intervals of 30s and 35s respectively
        //   (MUST make sure metric2 is no less than 35s, else the collections won't be late!)
        // - the resource component will sleep in its getValues for 91s - no metrics will be reported the first 91s
        // - the PC will call getValues() in this sequence:
        // ** metric1 - starting at time 0  (due to the sleep in our getValues, this won't complete until time 91)
        // ** metric2 - starting at time 0  (completes at time 91)
        // ** metric1 - starting at time 30 (completes at time 91)
        // ** metric2 - starting at time 60 (completes at time 91)
        // ** metric2 - starting at time 90 (completes at time 91)
        // ** metric2 - starting at time 105 (completes at time 105)
        // ** metric1 - starting at time [121..150] (completes at time 90 + 30 + [1..30])
        // 
        // Metric 1 is late because it was supposed to start at t60 but instead came up for eval at t=90. It is then
        // rescheduled by our fix for (currentTime=t90 + delay=30s + randomInterval=[1..30] based on the 30s Interval).
        // And you can see above, that is when the next request to collect metric1 is done.
        // Without the fix, the next collection of metric1 would have been at around time 60 + (interval) == 60 + (30) == 90

        // rather than just blindly sleeping, make this test complete as fast as possible by
        // stop waiting as soon as we see metric1 having been collected at least 3 times.
        CollectedMetric[] collectedMetrics = null;
        boolean keepWaiting = true;
        int waitCycles = 0;
        while (keepWaiting) {
            Thread.sleep(1000);
            int numberOfTimesMetric1WasCollectedSoFar = 0;
            collectedMetrics = server.collectedMetrics.toArray(new CollectedMetric[0]); // copy it fast, this is synchronized so our component's thread won't break us
            for (CollectedMetric collectedMetric : collectedMetrics) {
                if (collectedMetric.metricName.equals("metric1")) {
                    numberOfTimesMetric1WasCollectedSoFar++;
                }
            }
            if (numberOfTimesMetric1WasCollectedSoFar >= 3) {
                keepWaiting = false;
            }
            if (waitCycles++ > 180) {
                keepWaiting = false; // stop waiting but the test will probably fail because we should have had three metric1 collections by now
            }
        }

        // round all the time values to the nearest second
        for (CollectedMetric metric : collectedMetrics) {
            metric.collectedTime = getRoundedSeconds(metric.collectedTime);
            metric.finishedTime = getRoundedSeconds(metric.finishedTime);
            System.out.println("BZ 834019 test metric=" + metric.metricName + "; start=" + metric.collectedTime
                + "; stop=" + metric.finishedTime);
        }

        // now look at the timings and make sure we see metric1 pushed out into the future like our bug fix wants it to be
        int collectionNumber = 0;
        for (CollectedMetric metric : collectedMetrics) {
            if ("metric1".equals(metric.metricName)) {
                collectionNumber++; // this tracks that we are on the Nth time the metric was collected, we expect 3 total
                if (collectionNumber == 1) {
                    assert metric.collectedTime <= 3; // should be 0, but give it some leeway in case our test box is slow
                } else if (collectionNumber == 2) {
                    assert metric.collectedTime >= 29 && metric.collectedTime <= 32; // should be 30, but give it some leeway
                } else if (collectionNumber == 3) {
                    assert metric.collectedTime >= 120 && metric.collectedTime <= 151; // should between 90 + 30 + [1..30] = [121..150], but give it some leeway
                }
            }
        }
        assert collectionNumber >= 3 : "test should have collected metric1 at least 3 times: " + collectionNumber;
    }

    private long getRoundedSeconds(long millis) {
        return (long) ((millis / 1000.0) + 0.5);
    }
}
