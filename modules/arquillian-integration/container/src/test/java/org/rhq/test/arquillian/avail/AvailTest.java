package org.rhq.test.arquillian.avail;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.AvailabilityReport.Datum;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.AvailabilityExecutor;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.test.arquillian.BeforeDiscovery;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.ResourceComponentInstances;
import org.rhq.test.arquillian.ResourceContainers;
import org.rhq.test.arquillian.RunDiscovery;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

public class AvailTest extends Arquillian {

    @Deployment(name = "availPlugin")
    @TargetsContainer("connected-pc")
    public static RhqAgentPluginArchive getTestPlugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "avail-plugin-1.0.jar").setPluginDescriptor(
            "avail-rhq-plugin.xml");
    }

    @ArquillianResource
    private ContainerController pcController;

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

    @BeforeDiscovery
    public void resetServerServices() throws Exception {
        serverServices.resetMocks();
        fakeServerInventory = new FakeServerInventory();

        //autoimport everything
        when(serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));
    }

    @Test
    @RunDiscovery
    public void testConfirmInitialInventory() throws Exception {
        Assert.assertNotNull(pluginContainer);
        Assert.assertTrue(pluginContainer.isStarted());

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

    @Test(dependsOnMethods = "testConfirmInitialInventory")
    public void testAvailReport() throws Exception {
        AvailabilityExecutor executor = new AvailabilityExecutor(this.pluginContainer.getInventoryManager());
        AvailabilityReport report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), false, "First report should have been a full report");
        List<Datum> availData = report.getResourceAvailability();
        for (Datum datum : availData) {
            assert datum.getResourceId() > 0 : "resource IDs should be > zero since it should be committed";
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.UP, "should be UP at the start");
        }

        // do an avail check again - nothing changed, so we should have an empty report
        report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "Second report should have been changes-only");
        Assert.assertEquals(report.getResourceAvailability().isEmpty(), true, "Nothing changed, should be empty");

        // make one of the top parents down and see all other children are down
        AvailResourceComponent downParent = this.parentComponents1.iterator().next();
        downParent.setNextAvailability(AvailabilityType.DOWN);
        report = executor.call();
        Assert.assertNotNull(report);
        Assert.assertEquals(report.isChangesOnlyReport(), true, "report should have been changes-only");
        availData = report.getResourceAvailability();
        Assert.assertEquals(availData.size(), 7, "Should have 1 parent, its 2 children and 4 grandchildren");
        for (Datum datum : availData) {
            Assert.assertEquals(datum.getAvailabilityType(), AvailabilityType.DOWN);
        }
    }
}
