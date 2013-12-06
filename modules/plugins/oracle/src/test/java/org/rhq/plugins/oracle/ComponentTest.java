package org.rhq.plugins.oracle;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.availability.AvailabilityContextImpl;
import org.rhq.core.pc.content.ContentContextImpl;
import org.rhq.core.pc.event.EventContextImpl;
import org.rhq.core.pc.event.EventManager;
import org.rhq.core.pc.inventory.InventoryContextImpl;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pc.upgrade.plugins.multi.base.NothingDiscoveringDiscoveryComponent;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InventoryContext;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;

/**
 * Base class for RHQ Component Testing.
 * Initializes a plugin configuration.
 */
public abstract class ComponentTest {

    static {
        ResourceComponent.class.toString();
    }

    protected final Log log = LogFactory.getLog(getClass());
    private static File temp = new File(System.getProperty("java.io.tmpdir"));

    /**
     * Associates a resource component with a resource.
     */
    protected Map<ResourceComponent, Resource> components = new LinkedHashMap<ResourceComponent, Resource>();

    protected EventManager eventManager;
    protected ResourceDiscoveryContext resourceDiscoveryContext;
    protected PluginContainer pluginContainer = PluginContainer.getInstance();
    protected SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

    // TODO

    PluginMetadataManager pmm = new PluginMetadataManager();
    ResourceDiscoveryComponent resourceDiscoveryComponent = new NothingDiscoveringDiscoveryComponent();
    File temporaryDirectory = temp;
    File dataDirectory = temp;
    String pluginContainerName = "rhq";
    OperationContext operationContext = new OperationContextImpl(0);
    ContentContext contentContext = new ContentContextImpl(0);
    PluginContainerDeployment pluginContainerDeployment = null;

    /**
     * Constructs a new component test.
     */
    protected ComponentTest() {
        // Speed up propagation of events by adjusting delay/period to 1 second
        PluginContainerConfiguration pcc = new PluginContainerConfiguration();
        pcc.setEventSenderInitialDelay(1);
        pcc.setEventSenderPeriod(1);

        pluginContainer.setConfiguration(pcc);
        pluginContainer.initialize();
        eventManager = pluginContainer.getEventManager();
    }

    /**
     * Initializes all components in the system; using auto-discovery where possible.
     */
    @BeforeTest
    protected void before() throws Exception {

        InputStream is = getClass().getResourceAsStream("/META-INF/rhq-plugin.xml");
        PluginDescriptor pd = AgentPluginDescriptorUtil.parsePluginDescriptor(is);
        ResourceType platformType = pmm.addTestPlatformType();
        Set<ResourceType> rts = pmm.loadPlugin(pd);
        for (ResourceType resourceType : pmm.loadPlugin(pd)) {
            String componentType = pmm.getComponentClass(resourceType);
            ResourceComponent component = (ResourceComponent) Class.forName(componentType).newInstance();
            Configuration configuration = resourceType.getPluginConfigurationDefinition().getDefaultTemplate()
                .createConfiguration();

            setConfiguration(configuration, resourceType);

            Resource resource = new Resource();
            resource.setResourceKey(this.toString());
            resource.setResourceType(resourceType);
            resource.setPluginConfiguration(configuration);
            resource.setName(resourceType.getName());

            ResourceComponent parentResourceComponent = null;
            ResourceContext<?> parentResourceContext = null;

            components.put(component, resource);
            resource.setParentResource(pluginContainer.getInventoryManager().getPlatform());

            ResourceDiscoveryComponent resourceDiscoveryComponent = new NothingDiscoveringDiscoveryComponent();
            EventContext eventContext = new EventContextImpl(resource);
            AvailabilityContext availContext = new AvailabilityContextImpl(resource);
            InventoryContext inventoryContext = new InventoryContextImpl(resource);
            ResourceContext context = new ResourceContext(resource, parentResourceComponent, parentResourceContext,
                resourceDiscoveryComponent, systemInfo, temporaryDirectory, dataDirectory, pluginContainerName,
                eventContext, operationContext, contentContext, availContext, inventoryContext,
                pluginContainerDeployment);
            component.start(context);

            resourceDiscoveryContext = new ResourceDiscoveryContext(resourceType, parentResourceComponent, context,
                systemInfo, Collections.emptyList(), Collections.emptyList(), pluginContainerName,
                pluginContainerDeployment);

            for (ResourceType rt : resourceType.getChildResourceTypes()) {
                processChild(rt, component, context, resource);
            }
        }

        log.info("ResourceComponent map: " + components);
    }

    /**
     * Process a child
     * @param component parent component
     * @param resource parent resource
     * @param resourceType child resource type
     */
    private void processChild(ResourceType resourceType, ResourceComponent component, ResourceContext<?> parentContext,
        Resource resource) throws Exception {
        Configuration configuration = resourceType.getPluginConfigurationDefinition().getDefaultTemplate()
            .createConfiguration();
        setConfiguration(configuration, resourceType);
        log.info("childResource " + resourceType + " properties " + configuration.getProperties());

        String s = pmm.getDiscoveryClass(resourceType);
        ResourceDiscoveryComponent rdc = (ResourceDiscoveryComponent) Class.forName(s).newInstance();
        log.debug("rdc=" + rdc);

        EventContext eventContext = new EventContextImpl(resource);
        AvailabilityContext availContext = new AvailabilityContextImpl(resource);
        InventoryContext inventoryContext = new InventoryContextImpl(resource);
        ResourceContext context = new ResourceContext(resource, component, parentContext, resourceDiscoveryComponent,
            systemInfo, temporaryDirectory, dataDirectory, pluginContainerName, eventContext, operationContext,
            contentContext, availContext, inventoryContext, pluginContainerDeployment);
        ResourceDiscoveryContext resourceDiscoveryContext = new ResourceDiscoveryContext(resourceType, component,
            context, systemInfo, Collections.emptyList(), Collections.emptyList(), pluginContainerName,
            pluginContainerDeployment);
        Assert.assertNotNull(context.getEventContext());
        Set<DiscoveredResourceDetails> d = rdc.discoverResources(resourceDiscoveryContext);
        for (DiscoveredResourceDetails drd : d) {
            log.debug("discovered " + drd);
            createChild(drd, resource, configuration, component, context);
        }

    }

    private void createChild(DiscoveredResourceDetails drd, Resource resource, Configuration configuration,
        ResourceComponent parentComponent, ResourceContext<?> parentContext) throws Exception {
        ResourceType type = pmm.getType(drd.getResourceType());

        Resource cresource = new Resource();
        cresource.setResourceType(type);
        cresource.setPluginConfiguration(configuration);
        cresource.setResourceKey(drd.getResourceKey());
        cresource.setParentResource(resource);
        cresource.setName(drd.getResourceName());

        String rclassname = pmm.getComponentClass(type);
        ResourceComponent component = (ResourceComponent) Class.forName(rclassname).newInstance();

        EventContext eventContext = new EventContextImpl(resource);
        AvailabilityContext availContext = new AvailabilityContextImpl(resource);
        InventoryContext inventoryContext = new InventoryContextImpl(resource);
        ResourceContext context = new ResourceContext(cresource, parentComponent, parentContext,
            resourceDiscoveryComponent, systemInfo, temporaryDirectory, dataDirectory, pluginContainerName,
            eventContext, operationContext, contentContext, availContext, inventoryContext, pluginContainerDeployment);

        component.start(context);
        components.put(component, cresource);
    }

    /**
     * Called before the configuration is processed; override to set specific plugin parameters.
     * @see #configuration
     */
    protected void setConfiguration(Configuration configuration, ResourceType resourceType) {
    }

    /**
     * Stops all components, stops the plugin container.
     */
    @AfterTest
    protected void after() throws Exception {
        for (ResourceComponent c : components.keySet())
            c.stop();
        PluginContainer.getInstance().shutdown();
    }

    /**
     * Returns a measurement report.
     */
    protected MeasurementReport getMeasurementReport(ResourceComponent component) throws Exception {
        Resource resource = this.components.get(component);
        ResourceType type = resource.getResourceType();
        MeasurementReport report = new MeasurementReport();
        Set<MeasurementScheduleRequest> s = new HashSet<MeasurementScheduleRequest>();
        for (MeasurementDefinition md : type.getMetricDefinitions())
            s.add(new MeasurementScheduleRequest(new MeasurementSchedule(md, resource)));
        ((MeasurementFacet) component).getValues(report, s);
        return report;
    }

    /**
     * Finds the first resource component by resource name, or asserts failure if not found.
     * By default this is the resource type, or for a discovered resource the discovered
     * resource name.
     */
    protected ResourceComponent byName(String name) {
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet())
            if (c.getValue().getName().equals(name))
                return c.getKey();
        fail("component not found " + name + " in " + components.entrySet());
        return null;
    }

    /**
     * From a measurement report, returns a measurement value, or asserts failure if no such value exists.
     */
    protected Double getValue(MeasurementReport report, String name) {
        for (MeasurementDataNumeric m : report.getNumericData()) {
            if (m.getName().equals(name)) {
                log.debug("measurement name " + name + " " + m.getValue());
                return m.getValue();
            }
        }
        fail("report does not incude " + name + " report " + report.getNumericData());
        return null;
    }

    /**
     * Asserts the resource component is available.
     */
    protected void assertUp(ResourceComponent component) {
        assertEquals("up " + component, AvailabilityType.UP, component.getAvailability());
    }

    /**
     * Asserts the resource component is unavailable.
     */
    protected void assertDown(ResourceComponent component) {
        assertEquals("down " + component, AvailabilityType.DOWN, component.getAvailability());
    }

}
