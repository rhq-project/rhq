package org.rhq.core.pc.inventory.upgrade;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;

import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.pc.PluginContainer;
import org.rhq.plugins.test.upgrade.v1.DiscoveryComponentV1;
import org.rhq.plugins.test.upgrade.v1.ResourceComponentV1;
import org.rhq.plugins.test.upgrade.v2.DiscoveryComponentV2;
import org.rhq.plugins.test.upgrade.v2.ResourceComponentV2;
import org.rhq.plugins.test.upgrade.v2.ResourceContextStress;
import org.rhq.test.arquillian.FakeServerInventory;
import org.rhq.test.arquillian.MockingServerServices;
import org.rhq.test.arquillian.ServerServicesSetup;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

public class ResourceUpgradeTest extends Arquillian {

    private static final String CONTAINER = "resource-upgrade";

    @Deployment(name = "v1", managed = false)
    @TargetsContainer(CONTAINER)
    public static RhqAgentPluginArchive getV1Plugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "v1.jar")
            .addClasses(DiscoveryComponentV1.class, ResourceComponentV1.class)
            .setPluginDescriptor("upgrade-v1-plugin.xml");
    }

    @Deployment(name = "v2", managed = false)
    @TargetsContainer(CONTAINER)
    public static RhqAgentPluginArchive getV2Plugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "v2.jar")
            .addClasses(DiscoveryComponentV2.class, ResourceComponentV2.class, ResourceContextStress.class)
            .setPluginDescriptor("upgrade-v2-plugin.xml");
    }

    @ArquillianResource
    private Deployer pluginDeployer;

    @ArquillianResource
    private PluginContainer pluginContainer;

    @ArquillianResource
    private MockingServerServices serverServices;

    private FakeServerInventory fakeServerInventory;

    private FakeServerInventory.CompleteDiscoveryChecker discoveryCompleteChecker;

    private static final String FAKE_PACKAGE_CONTENTS = "packageContents";

    @ServerServicesSetup
    public void setupServer() throws Exception {
        fakeServerInventory = new FakeServerInventory();
        discoveryCompleteChecker = fakeServerInventory.createAsyncDiscoveryCompletionChecker(2);
        when(serverServices.getDiscoveryServerService().mergeInventoryReport(any(InventoryReport.class))).then(
            fakeServerInventory.mergeInventoryReport(InventoryStatus.COMMITTED));

        ContentServerService contentServerService = serverServices.getContentServerService();

        when(
            contentServerService.downloadPackageBitsGivenResource(anyInt(), any(PackageDetailsKey.class),
                any(OutputStream.class))).thenReturn((long) FAKE_PACKAGE_CONTENTS.length());

        when(
            contentServerService.downloadPackageBitsForChildResource(anyInt(), anyString(),
                any(PackageDetailsKey.class), any(OutputStream.class))).thenReturn(
            (long) FAKE_PACKAGE_CONTENTS.length());

        when(
            contentServerService.downloadPackageBitsRangeGivenResource(anyInt(), any(PackageDetailsKey.class),
                any(OutputStream.class), anyLong(), anyLong())).thenReturn(0L);

        when(contentServerService.getPackageBitsLength(anyInt(), any(PackageDetailsKey.class))).thenReturn(
            (long) FAKE_PACKAGE_CONTENTS.length());
        
        when(contentServerService.getPackageVersionMetadata(anyInt(), any(PageControl.class))).thenReturn(
            new PageList<PackageVersionMetadataComposite>());

        when(contentServerService.getResourceSubscriptionMD5(anyInt())).thenReturn(null);
    }

    @Test
    public void testResourceContextUsableDuringUpgrade() throws Exception {
        pluginDeployer.deploy("v1");

        pluginContainer.getInventoryManager().executeServerScanImmediately();

        discoveryCompleteChecker.waitForDiscoveryComplete();

        ResourceContextStress.resetReports();

        pluginDeployer.deploy("v2");
        pluginDeployer.undeploy("v1");

        failIfContextStressFailed();
    }

    private void failIfContextStressFailed() {
        Map<String, List<ResourceContextStress.Report>> reports = ResourceContextStress.getAllReports();
        for (List<ResourceContextStress.Report> l : reports.values()) {
            if (!l.isEmpty()) {
                fail("ResourceContext stress test failed in the following stages: " + reports);
                break;
            }
        }
    }
}
