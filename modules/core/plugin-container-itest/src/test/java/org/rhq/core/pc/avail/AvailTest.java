package org.rhq.core.pc.avail;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.AvailabilityReport.Datum;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.inventory.AvailabilityExecutor;
import org.rhq.core.pc.inventory.AvailabilityExecutor.Scan;
import org.rhq.core.pc.inventory.ForceAvailabilityExecutor;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.plugins.test.avail.AvailDiscoveryComponent;
import org.rhq.plugins.test.avail.AvailResourceComponent;
import org.rhq.test.arquillian.AfterDiscovery;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.ResourceComponentInstances;
import org.rhq.test.arquillian.ResourceContainers;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

@RunDiscovery
public class AvailTest extends Arquillian {

    @Deployment(name = "availPlugin")
    @TargetsContainer("connected-pc")
    public static RhqAgentPluginArchive getTestPlugin() {
        RhqAgentPluginArchive pluginJar = ShrinkWrap.create(RhqAgentPluginArchive.class, "avail-plugin-1.0.jar");
        return pluginJar
                .setPluginDescriptor("avail-rhq-plugin.xml")
                .addClasses(AvailDiscoveryComponent.class, AvailResourceComponent.class);
    }

    @ArquillianResource
    private PluginContainer pluginContainer;

    @ArquillianResource
    public MockingServerServices serverServices;

    private FakeServerInventory fakeServerInventory;

    @ResourceContainers(plugin = "availPlugin", resourceType = "AvailParentServer1")
    private Set<ResourceContainer> parentContainers1;

    @ResourceContainers(plugin = "availPlugin", resourceType = "AvailParentServer2")
    private Set<ResourceContainer> parentContainers2;

    @ResourceContainers(plugin = "availPlugin", resourceType = "AvailChildService1")
    private Set<ResourceContainer> childContainers1;

    @ResourceContainers(plugin = "availPlugin", resourceType = "AvailChildService2")
    private Set<ResourceContainer> childContainers2;

    @ResourceContainers(plugin = "availPlugin", resourceType = "AvailGrandchildService1")
    private Set<ResourceContainer> grandchildContainers1;

    @ResourceContainers(plugin = "availPlugin", resourceType = "AvailGrandchildService2")
    private Set<ResourceContainer> grandchildContainers2;

    @ResourceComponentInstances(plugin = "availPlugin", resourceType = "AvailParentServer1")
    private Set<AvailResourceComponent> parentComponents1;

    @ResourceComponentInstances(plugin = "availPlugin", resourceType = "AvailParentServer2")
    private Set<AvailResourceComponent> parentComponents2;

    @ResourceComponentInstances(plugin = "availPlugin", resourceType = "AvailChildService1")
    private Set<AvailResourceComponent> childComponents1;

    @ResourceComponentInstances(plugin = "availPlugin", resourceType = "AvailChildService2")
    private Set<AvailResourceComponent> childComponents2;

    @ResourceComponentInstances(plugin = "availPlugin", resourceType = "AvailGrandchildService1")
    private Set<AvailResourceComponent> grandchildComponents1;

    @ResourceComponentInstances(plugin = "availPlugin", resourceType = "AvailGrandchildService2")
    private Set<AvailResourceComponent> grandchildComponents2;

    private FakeServerInventory.CompleteDiscoveryChecker discoveryCompleteChecker;

    @BeforeDiscovery(testMethods = "testDiscovery")
    public void resetServerServices() throws Exception {
        serverServices.resetMocks();
        fakeServerInventory = new FakeServerInventory();
        discoveryCompleteChecker = fakeServerInventory.createAsyncDiscoveryCompletionChecker(4);

        // autoimport everything
        when(serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
    }

    @AfterDiscovery
    public void waitForAsyncDiscoveries() throws Exception {
        if (discoveryCompleteChecker != null) {
            discoveryCompleteChecker.waitForDiscoveryComplete(10000);
        }
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        System.out.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!! BEFORE METHOD (" + Thread.currentThread().getName() + ")");
        scrub();
    }

    private void scrub() throws Exception {
        if (null == parentContainers1)
            return;

        List<Set<ResourceContainer>> containerSets = new ArrayList<Set<ResourceContainer>>();
        containerSets.add(parentContainers1);
        containerSets.add(parentContainers2);
        containerSets.add(childContainers1);
        containerSets.add(childContainers2);
        containerSets.add(grandchildContainers1);
        containerSets.add(grandchildContainers2);

        // scrub res containers of avail state and ensure schedules are blanked
        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                c.setAvailabilityScheduleTime(null);
                c.updateAvailability(null);
                c.setAvailabilitySchedule(null);
                // reset state of res component
                ResourceComponent resourceComponent = c.getResourceComponent();
                resourceComponent.stop();
                resourceComponent.start(c.getResourceContext());
            }
        }

        // this is a hack to get this test to pass. If you run this test class on its own, it passes fine.
        // but if you run it in conjunction with other tests (e.g. mvn -Dtest=AvailTest,DiscoveryCallbackAbortTest)
        // then it fails. It appears our arquillian test infrastructure is shutting down the PC / ResourceContainer
        // but the ResourceContainer is not getting reinitialized by the time this test class needs it. What then
        // happens is the avail thread pool in ResourceContainer is shutdown and will not accept any more tasks, causing failures.
        // We should try to figure out why the Arquillian infrastructure is doing this, but for now, to get tests
        // passing again, this one-line fix needs to be here.
        ResourceContainer.initialize(new PluginContainerConfiguration());
    }

    @Test(groups = "pc.itest.avail", priority = 20)
    public void testDiscovery() throws Exception {
        Assert.assertNotNull(pluginContainer);
        Assert.assertTrue(pluginContainer.isStarted());
        Assert.assertTrue(pluginContainer.isRunning());

        // make sure we have all the resource containers for all the resources
        Assert.assertEquals(parentContainers1.size(), 2, "missing parent1");
        Assert.assertEquals(parentContainers2.size(), 2, "missing parent2");
        Assert.assertEquals(childContainers1.size(), 4, "missing child1");
        Assert.assertEquals(childContainers2.size(), 4, "missing child2");
        Assert.assertEquals(grandchildContainers1.size(), 8, "missing grandchild1");
        Assert.assertEquals(grandchildContainers2.size(), 8, "missing grandchild2");

        // make sure we have all the resource components for all the resources
        Assert.assertEquals(parentComponents1.size(), 2, "missing parent1");
        Assert.assertEquals(parentComponents2.size(), 2, "missing parent2");
        Assert.assertEquals(childComponents1.size(), 4, "missing child1");
        Assert.assertEquals(childComponents2.size(), 4, "missing child2");
        Assert.assertEquals(grandchildComponents1.size(), 8, "missing grandchild1");
        Assert.assertEquals(grandchildComponents2.size(), 8, "missing grandchild2");
    }

    @Test(groups = "pc.itest.avail", priority = 21)
    public void testAvailReport() throws Exception {
        Assert.assertTrue(pluginContainer.isStarted());
        Assert.assertTrue(pluginContainer.isRunning());
        AvailabilityExecutor executor = new ForceAvailabilityExecutor(this.pluginContainer.getInventoryManager());
        dumpContainers("testAvailReport() Start");
        AvailabilityReport report = executor.call();
        dumpContainers("testAvailReport() After First Avail Check");
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), false, "First report should have been a full report");
        List<Datum> availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.UP, "should be UP at the start");
        }
        AvailabilityExecutor.Scan scan = executor.getMostRecentScanHistory();
        assertScan(scan, true, true, 29, 29, 29, 28, 0, 0);

        // do a forced avail check again - nothing changed, so we should have an empty report
        report = executor.call();
        dumpContainers("testAvailReport() After Second Avail Check");
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "Second report should have been changes-only");
        Assert.assertEquals(report.getResourceAvailability().isEmpty(), true, "Nothing changed, should be empty");
        scan = executor.getMostRecentScanHistory();
        assertScan(scan, true, false, 29, 0, 29, 28, 0, 0);

        // make one of the top parents down and see all other children are down, force a scan of all to make sure we pick
        // up the changed resource.
        AvailResourceComponent downParent = this.parentComponents1.iterator().next();
        downParent.setNextAvailability(AvailabilityType.DOWN);
        report = executor.call();
        dumpContainers("testAvailReport() After Third Avail Check");
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "report should have been changes-only");
        availData = report.getResourceAvailability();
        Assert.assertEquals(availData.size(), 7, "Should have 1 parent, its 2 children and 4 grandchildren");
        for (Datum datum : availData) {
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.DOWN);
        }
        scan = executor.getMostRecentScanHistory();
        // Children shoud defer to newly down parent.
        assertScan(scan, true, false, 29, 7, 23, 28, 0, 6);
    }

    @Test(groups = "pc.itest.avail", priority = 21)
    public void testScheduling() throws Exception {
        Assert.assertTrue(pluginContainer.isStarted());
        Assert.assertTrue(pluginContainer.isRunning());
        AvailabilityExecutor executor = new ForceAvailabilityExecutor(this.pluginContainer.getInventoryManager());
        AvailabilityReport report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), false, "First report should have been a full report");
        List<Datum> availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.UP, "should be UP at the start");
        }
        AvailabilityExecutor.Scan scan = executor.getMostRecentScanHistory();
        assertScan(scan, true, true, 29, 28, 29, 28, 0, 0);

        // Servers should have been scheduled within 1 minute
        long scanTime = scan.getStartTime();
        long maxServerSched = scanTime + 60000L + 1;
        List<Set<ResourceContainer>> containerSets = new ArrayList<Set<ResourceContainer>>();
        containerSets.add(parentContainers1);
        containerSets.add(parentContainers2);

        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                Long schedTime = c.getAvailabilityScheduleTime();
                Assert.assertTrue(schedTime > scanTime && schedTime < maxServerSched);
            }
        }

        long maxServiceSched = scanTime + (10 * 60000L) + 1;
        boolean[] buckets = new boolean[10];
        int numBuckets = 0;

        containerSets.clear();
        containerSets.add(childContainers1);
        containerSets.add(childContainers2);
        containerSets.add(grandchildContainers1);
        containerSets.add(grandchildContainers2);

        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                Long schedTime = c.getAvailabilityScheduleTime();
                Assert.assertTrue(schedTime > scanTime && schedTime < maxServiceSched);
                long slice = (maxServiceSched - scanTime) / 10L;
                int i = (int) ((schedTime - scanTime) / slice);
                if (!buckets[i]) {
                    buckets[i] = true;
                    ++numBuckets;
                }
            }
        }
        Assert.assertTrue(numBuckets >= 3, "Random distribution seems wrong, buckets hit= " + numBuckets);
    }

    @Test(groups = "pc.itest.avail", priority = 21)
    // If a parent changes to UP, its children must all be checked as they could legitimately be something
    // other than UP.
    public void testForceChildrenOfParentUp() throws Exception {
        Assert.assertTrue(pluginContainer.isStarted());
        Assert.assertTrue(pluginContainer.isRunning());
        // don't use a ForceAvailabilityExecutor for this test, we want to manipulate what gets checked
        AvailabilityExecutor executor = new AvailabilityExecutor(this.pluginContainer.getInventoryManager());
        AvailabilityReport report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), false, "First report should have been a full report");
        List<Datum> availData = report.getResourceAvailability();
        int numUp = 0;
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            if (datum.getAvailabilityType() == AvailabilityType.UP) {
                ++numUp;
            }
        }
        Assert.assertEquals(numUp, 1);
        // only the platform should have been checked, all others should only have been scheduled for a check
        AvailabilityExecutor.Scan scan = executor.getMostRecentScanHistory();
        assertScan(scan, false, true, 29, 0, 1, 28, 0, 0);

        // At this point all of the non-platform resources are scheduled but still at NULL avail

        // Manipulate the scheduled time of the "1" servers so they are checked
        List<Set<ResourceContainer>> containerSets = new ArrayList<Set<ResourceContainer>>();
        containerSets.add(parentContainers1);
        long now = System.currentTimeMillis();
        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                c.setAvailabilityScheduleTime(now);
            }
        }

        // make sure nothing else is scheduled to be checked
        containerSets.clear();
        containerSets.add(childContainers1);
        containerSets.add(grandchildContainers1);
        containerSets.add(parentContainers2);
        containerSets.add(childContainers2);
        containerSets.add(grandchildContainers2);
        long later = now + 10000000L;
        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                c.setAvailabilityScheduleTime(later);
            }
        }

        // a changes-only report, even though only 2 checks are scheduled, we should see checks for half
        // the resources, as the children will be forced. (they should change from null to UP).  The scheduled
        // checks should see their schedules pushed out, but the forced checks should be rescheduled randomly
        report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "Second report should have been changes-only");
        Assert.assertEquals(report.getResourceAvailability().size(), 14, "should report half the resources");
        availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.UP, "should be UP at the start");
        }
        scan = executor.getMostRecentScanHistory();
        assertScan(scan, false, false, 29, 14, 15, 12, 2, 0);
    }

    @Test(groups = "pc.itest.avail", priority = 21)
    // If a parent changes to DOWN, its children should all defer to being DOWN as well.
    public void testDeferToParentDown() throws Exception {
        Assert.assertTrue(pluginContainer.isStarted());
        Assert.assertTrue(pluginContainer.isRunning());
        // don't use a ForceAvailabilityExecutor for this test, we want to manipulate what gets checked
        AvailabilityExecutor executor = new AvailabilityExecutor(this.pluginContainer.getInventoryManager());
        AvailabilityReport report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), false, "First report should have been a full report");
        List<Datum> availData = report.getResourceAvailability();
        int numUp = 0;
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            if (datum.getAvailabilityType() == AvailabilityType.UP) {
                ++numUp;
            }
        }
        Assert.assertEquals(numUp, 1);
        // only the platform should have been checked, all others should only have been scheduled for a check
        AvailabilityExecutor.Scan scan = executor.getMostRecentScanHistory();
        assertScan(scan, false, true, 29, 0, 1, 28, 0, 0);

        // At this point all of the non-platform resources are scheduled but still at NULL avail

        // Manipulate the scheduled time of the "1" servers so they are checked
        List<Set<ResourceContainer>> containerSets = new ArrayList<Set<ResourceContainer>>();
        containerSets.add(parentContainers1);
        long now = System.currentTimeMillis();
        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                c.setAvailabilityScheduleTime(now);
            }
        }

        // make sure nothing else is scheduled to be checked
        containerSets.clear();
        containerSets.add(childContainers1);
        containerSets.add(grandchildContainers1);
        containerSets.add(parentContainers2);
        containerSets.add(childContainers2);
        containerSets.add(grandchildContainers2);
        long later = now + 10000000L;
        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                c.setAvailabilityScheduleTime(later);
            }
        }

        // make the "1" servers return DOWN and ensure all other children are down, even though their schedules
        // are not yet met.
        for (AvailResourceComponent downParent : this.parentComponents1) {
            downParent.setNextAvailability(AvailabilityType.DOWN);
        }

        // a changes-only report, even though only 2 checks are scheduled, we should see checks for half
        // the resources, as the children should defer to the DOWN parent. (they should change from null to DOWN).
        // The scheduled checks should see their schedules pushed out but the deferred checks should not, their
        // schedules remain unchanged in this scenario.
        report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "Second report should have been changes-only");
        Assert.assertEquals(report.getResourceAvailability().size(), 14, "should report half the resources");
        availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.DOWN, "should be DOWN");
        }
        scan = executor.getMostRecentScanHistory();
        // Children should defer to newly down parent.
        assertScan(scan, false, false, 29, 14, 3, 0, 2, 12);
    }

    @Test(groups = "pc.itest.avail", priority = 21)
    public void testCheckOnlyEligible() throws Exception {
        Assert.assertTrue(pluginContainer.isStarted());
        Assert.assertTrue(pluginContainer.isRunning());
        // Force all the avails to UP to start so we can avoid the scenario in  testForceChildrenOfParentUp()
        AvailabilityExecutor executor = new ForceAvailabilityExecutor(this.pluginContainer.getInventoryManager());
        AvailabilityReport report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), false, "First report should have been a full report");
        List<Datum> availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.UP, "should be UP at the start");
        }
        AvailabilityExecutor.Scan scan = executor.getMostRecentScanHistory();
        assertScan(scan, true, true, 29, 28, 29, 28, 0, 0);

        // don't use a ForceAvailabilityExecutor for this scan, we want to manipulate what gets checked.
        // by default new executors always do a full scan to start, we don't want that
        executor = new AvailabilityExecutor(this.pluginContainer.getInventoryManager());
        executor.sendChangesOnlyReportNextTime();

        // Manipulate the scheduled times such that the "1" resources should be checked and the "2"s should not
        List<Set<ResourceContainer>> containerSets = new ArrayList<Set<ResourceContainer>>();
        containerSets.add(parentContainers1);
        containerSets.add(childContainers1);
        containerSets.add(grandchildContainers1);
        long now = System.currentTimeMillis();
        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                c.setAvailabilityScheduleTime(now);
            }
        }

        containerSets.clear();
        containerSets.add(parentContainers2);
        containerSets.add(childContainers2);
        containerSets.add(grandchildContainers2);
        long later = now + 10000000L;
        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                c.setAvailabilityScheduleTime(later);
            }
        }

        // a changes-only report, check half the resources, no changes - should all be UP already. push out scheds for each
        report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "Second report should have been changes-only");
        Assert.assertEquals(report.getResourceAvailability().size(), 0, "no changes, everything was already up");
        availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.UP, "should be UP at the start");
        }
        scan = executor.getMostRecentScanHistory();
        assertScan(scan, false, false, 29, 0, 15, 0, 14, 0);

        // another quick scan should see no calls, check times should be pushed out at least a minute
        report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "Third report should have been changes-only");
        Assert.assertEquals(report.getResourceAvailability().isEmpty(), true, "Nothing changed, should be empty");
        scan = executor.getMostRecentScanHistory();
        assertScan(scan, false, false, 29, 0, 1, 0, 0, 0);
    }

    @Test(groups = "pc.itest.avail", priority = 21)
    public void testDeferToParent() throws Exception {
        Assert.assertTrue(pluginContainer.isStarted());
        Assert.assertTrue(pluginContainer.isRunning());
        AvailabilityExecutor executor = new ForceAvailabilityExecutor(this.pluginContainer.getInventoryManager());
        AvailabilityReport report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), false, "First report should have been a full report");
        List<Datum> availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.UP, "should be UP at the start");
        }
        AvailabilityExecutor.Scan scan = executor.getMostRecentScanHistory();
        assertScan(scan, true, true, 29, 28, 29, 28, 0, 0);

        // disable the schedules for all "1" children
        List<Set<ResourceContainer>> containerSets = new ArrayList<Set<ResourceContainer>>();
        containerSets.add(childContainers1);
        containerSets.add(grandchildContainers1);
        long now = System.currentTimeMillis();
        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                MeasurementScheduleRequest sched = c.getAvailabilitySchedule();
                sched.setEnabled(false);
                c.setAvailabilitySchedule(sched);
            }
        }

        // a changes-only report, force checks, no changes, ensure 1/2 children defer to parent
        report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "Second report should have been changes-only");
        Assert.assertEquals(report.getResourceAvailability().size(), 0, "no changes, everything was already up");
        availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.UP, "should be UP at the start");
        }
        scan = executor.getMostRecentScanHistory();
        assertScan(scan, true, false, 29, 0, 17, 16, 0, 12);
    }

    private void assertScan(Scan scan, boolean isForced, boolean isFull, int numResources, int numChanges,
        int numCalls, int numSched, int numPushed, int numDeferred) {
        Assert.assertEquals(scan.isForced(), isForced, "Unexpected isForced");
        Assert.assertEquals(scan.isFull(), isFull, "Unexpected isFull");
        Assert.assertEquals(scan.getNumResources(), numResources,
            "Unexpected numResources, remember to include the implied platform?");
        Assert.assertEquals(scan.getNumAvailabilityChanges(), numChanges,
            "Unexpected numChanges, remember to include the implied platform");
        Assert.assertEquals(scan.getNumGetAvailabilityCalls(), numCalls,
            "Unexpected numGetAvailCalls, remember to include the implied platform");
        Assert.assertEquals(scan.getNumScheduledRandomly(), numSched,
            "Unexpected numSched, remember to omit the implied platform");
        Assert.assertEquals(scan.getNumPushedByInterval(), numPushed, "Unexpected numPushed");
        Assert.assertEquals(scan.getNumDeferToParent(), numDeferred,
            "Unexpected numDeferred, remember to include disabled and implied (when parent goes DOWN)");
    }

    private void dumpContainers(String title) {
        List<Set<ResourceContainer>> containerSets = new ArrayList<Set<ResourceContainer>>();
        containerSets.add(parentContainers1);
        containerSets.add(parentContainers2);
        containerSets.add(childContainers1);
        containerSets.add(childContainers2);
        containerSets.add(grandchildContainers1);
        containerSets.add(grandchildContainers2);

        System.out.println("---------> " + title);

        for (Set<ResourceContainer> cs : containerSets) {
            for (ResourceContainer c : cs) {
                String name = c.getResource().getName();
                AvailabilityType availType = c.getAvailability().getAvailabilityType();
                String avail = (null == availType) ? null : availType.name();
                Long time = c.getAvailabilityScheduleTime();
                System.out.println("----------> " + name + " " + avail + " " + time);
            }
        }

        System.out.println("---------------------------------> ");
    }

}
