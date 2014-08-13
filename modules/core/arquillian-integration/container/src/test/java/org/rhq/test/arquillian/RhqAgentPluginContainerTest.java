package org.rhq.test.arquillian;

import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.test.shrinkwrap.RhqAgentPluginArchive;

/**
 * @author Lukas Krejci
 */
@RunDiscovery
public class RhqAgentPluginContainerTest extends Arquillian {

    @Deployment(name = "simplePlugin")
    public static RhqAgentPluginArchive getTestPlugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "test-plugin-1.0.0.jar")
            .addClasses(TestDiscoveryComponent.class, TestResourceComponent.class)
            .setPluginDescriptor("test-rhq-plugin.xml");
    }

    @Deployment(name = "dependentPlugin")
    public static RhqAgentPluginArchive getDependentTestPlugin() {
        return ShrinkWrap
            .create(RhqAgentPluginArchive.class, "test-dependent-plugin-1.0.0.jar")
            .addClasses(TestDiscoveryComponent.class, TestResourceComponent.class)
            .setPluginDescriptor("test-dependent-rhq-plugin.xml")
            .withRequiredPluginsFrom(
                Maven.resolver().loadPomFromFile("pom.xml").importDependencies(ScopeType.TEST, ScopeType.RUNTIME)
                    .resolve().withTransitivity().as(JavaArchive.class));
    }

    @Deployment(name = "manuallyDeployed", managed = false)
    public static RhqAgentPluginArchive getManualPlugin() {
        return ShrinkWrap.create(RhqAgentPluginArchive.class, "test-manual-plugin-1.0.0.jar")
            .addClasses(TestDiscoveryComponent.class, TestResourceComponent.class)
            .setPluginDescriptor("test-manual-rhq-plugin.xml");
    }

    @ArquillianResource
    private PluginContainer pluginContainer;

    @DiscoveredResources(plugin = "testPlugin", resourceType = "TestServer")
    private Set<Resource> testResources;

    @DiscoveredResources(plugin = "testDependentPlugin", resourceType = "TestServer")
    private Set<Resource> testDependentResources;

    @ResourceComponentInstances(plugin = "testDependentPlugin", resourceType = "TestServer")
    private Set<TestResourceComponent> dependentComponents;

    @ResourceContainers(plugin = "testDependentPlugin", resourceType = "TestServer")
    private Set<ResourceContainer> dependentResourceContainers;

    @ArquillianResource
    private ContainerController pcController;

    @ArquillianResource
    private Deployer pluginDeployer;

    @Test
    public void testEnricherSetsPluginContainerInstance() {
        Assert.assertNotNull(pluginContainer);
    }

    @Test(dependsOnMethods = "testEnricherSetsPluginContainerInstance")
    public void testPluginContainerStartedInTest() {
        Assert.assertTrue(pluginContainer.isStarted());
    }

    @Test(dependsOnMethods = "testEnricherSetsPluginContainerInstance")
    public void testPluginDeployed() {
        Assert.assertEquals(testResources.size(), 1, "There should be one resource with the test type");
    }

    @Test
    public void testRequiredPluginsFoundAndDeployed() {
        Assert.assertEquals(testDependentResources.size(), 1,
            "The dependent plugin should have been loaded and its single resource discovered.");
    }

    @Test
    public void testResourceComponentInstancesAssigned() {
        Assert.assertEquals(dependentComponents.size(), 1, "There should be 1 resource component available");
    }

    @Test
    public void testResourceContainersAssigned() {
        Assert.assertEquals(dependentResourceContainers.size(), 1, "There should be one resource container available");
    }

    @Test
    public void manualDeployment() {
        pluginDeployer.deploy("manuallyDeployed");

        pluginContainer.getInventoryManager().executeServerScanImmediately();

        ResourceType expectedResourceType = new ResourceType("TestServer", "testManualPlugin", ResourceCategory.SERVER,
            null);

        Set<Resource> resources = pluginContainer.getInventoryManager().getResourcesWithType(expectedResourceType);

        //make the deployment look as original again so that other tests still work
        pluginDeployer.undeploy("manuallyDeployed");

        Assert.assertEquals(resources.size(), 1,
            "There should be a newly discovered resource of a manually deployed plugin");

        //now try again, we should no longer see the resource
        pluginContainer.getInventoryManager().executeServerScanImmediately();

        resources = pluginContainer.getInventoryManager().getResourcesWithType(expectedResourceType);

        Assert.assertEquals(resources.size(), 0,
            "There should no longer be any resource from the manually deployed plugin after undeployment.");
    }
}
