/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.plugins.mysql;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationProperty;
import org.rhq.core.clientapi.descriptor.plugin.MetricDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ResourceDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServerDescriptor;
import org.rhq.core.clientapi.descriptor.plugin.ServiceDescriptor;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.availability.AvailabilityContextImpl;
import org.rhq.core.pc.content.ContentContextImpl;
import org.rhq.core.pc.event.EventContextImpl;
import org.rhq.core.pc.event.EventManager;
import org.rhq.core.pc.inventory.InventoryContextImpl;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InventoryContext;
import org.rhq.core.pluginapi.inventory.PluginContainerDeployment;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;

/**
 * Base class for RHQ Component Testing.
 * Initializes a plugin configuration.
 *
 * Methods to override:
 *
 * {@link #setConfiguration(Configuration, ResourceType)}
 */
public abstract class ComponentTest {

    /**
     * Logging component.
     */
    protected final Log log = LogFactory.getLog(getClass());

    private static File temp = new File(System.getProperty("java.io.tmpdir"));

    /**
     * Associates a resource component with a resource.
     */
    protected Map<ResourceComponent, Resource> components = new LinkedHashMap<ResourceComponent, Resource>();

    /**
     * Associates a name of a resource with a resource descriptor.
     */
    protected Map<String, ResourceDescriptor> descriptors = new LinkedHashMap<String, ResourceDescriptor>();

    /**
     * Associates a name of a resource with a type.
     * This is useful for manually adding resources.
     *
     * @see #manuallyAdd(ResourceType, Configuration)
     */
    protected Map<String, ResourceType> resourceTypes = new LinkedHashMap<String, ResourceType>();

    /**
     * Event manager; used to obtain events.
     */
    private EventManager eventManager;

    private PluginContainer pluginContainer = PluginContainer.getInstance();

    private SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

    /**
     * Scan all processes before starting; false will disable this feature.
     * Disabling is suggested for running tests against a remote instance.
     */
    private boolean processScan = true;

    // TODO

    private final PluginMetadataManager pmm = new PluginMetadataManager();
    private final File temporaryDirectory = temp;
    private final File dataDirectory = temp;
    private final String pluginContainerName = "rhq";
    private final OperationContext operationContext = new OperationContextImpl(0);
    private final ContentContext contentContext = new ContentContextImpl(0);
    private PluginContainerDeployment pluginContainerDeployment = null;
    private Resource platform;
    private ResourceContainer platformContainer;
    private List<ProcessInfo> processInfo = Collections.emptyList();
    private PluginDescriptor pluginDescriptor;

    /**
     * Constructs a new component test.
     */
    protected ComponentTest() {
    }

    /**
     * Initializes the plugin container.
     * This is run before {@link #before()}.
     */
    @BeforeSuite
    protected void beforeSuite() {
        // Speed up propagation of events by adjusting delay/period to 1 second
        PluginContainerConfiguration pcc = new PluginContainerConfiguration();
        pcc.setEventSenderInitialDelay(1);
        pcc.setEventSenderPeriod(1);
        pluginContainer.setConfiguration(pcc);
        pluginContainer.initialize();
        eventManager = pluginContainer.getEventManager();
        platform = pluginContainer.getInventoryManager().getPlatform();
        platformContainer = pluginContainer.getInventoryManager().getResourceContainer(platform);
        if (platformContainer == null) {
            platformContainer = new ResourceContainer(platform, getClass().getClassLoader());
        }
    }

    /**
     * Initializes all plugins defined in the system; using auto-discovery where possible.
     * This is run once per test class.
     */
    @BeforeClass
    protected void before() throws Exception {
        if (processScan) {
            processInfo = getProcessInfos();
            if (processInfo == null)
                processInfo = Collections.emptyList();
            log.debug("Process Info " + processInfo);
            for (ProcessInfo i : processInfo) {
                log.debug(i.getBaseName() + " " + Arrays.toString(i.getCommandLine()));
            }
        }
        Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF/rhq-plugin.xml");
        List<URL> l = Collections.list(e);
        Collections.sort(l, new Comparator<URL>() {
            @Override
            public int compare(URL u1, URL u2) {
                return u2.toString().compareTo(u1.toString());
            }
        });
        for (URL url : l) {
            log.debug("parse " + url);
            InputStream is = url.openStream();
            PluginDescriptor pd = AgentPluginDescriptorUtil.parsePluginDescriptor(is);
            processPluginDescriptor(pd);
            log.debug("pmm names " + pmm.getPluginNames());
            buildDesc(pd.getServers());
            buildDesc(pd.getServices());
            is.close();
        }
    }

    @AfterSuite
    protected void afterSuite() {
        pluginContainer.shutdown();
    }

    /**
     * Process a plugin descriptor.
     */
    private void processPluginDescriptor(PluginDescriptor pd) throws Exception {
        this.pluginDescriptor = pd;
        Set<ResourceType> types = pmm.loadPlugin(pd);
        mapResourceTypeNames(types);
        log.info("Resource types: " + resourceTypes);
        resources(types, platform, platformContainer.getResourceComponent(), platformContainer.getResourceContext());
        log.info("ResourceComponent map: " + components);
    }

    private void mapResourceTypeNames(Set<ResourceType> types) {
        for (ResourceType type : types) {
            this.resourceTypes.put(type.getName(), type);
            mapResourceTypeNames(type.getChildResourceTypes());
        }
    }

    /**
     * Manually create a component by name.
     */
    public ResourceComponent manuallyAdd(String name) throws Exception {
        ResourceType resourceType = resourceTypes.get(name);
        if (resourceType == null)
            throw new IllegalStateException("no type " + name);
        Configuration configuration = resourceType.getPluginConfigurationDefinition().getDefaultTemplate().createConfiguration();
        setConfiguration(configuration, resourceType);
        return manuallyAdd(resourceType, configuration);
    }

    /**
     * Manually create a component by resource type.
     */
    public ResourceComponent manuallyAdd(ResourceType type, Configuration configuration) throws Exception {
        return manuallyAdd(type, configuration, platformContainer.getResourceComponent());
    }

    /**
     * Manually create a component by resource type, configuration, parent.
     */
    public ResourceComponent manuallyAdd(ResourceType type, Configuration configuration, ResourceComponent parent) throws Exception {
        DiscoveredResourceDetails drd = new DiscoveredResourceDetails(type,
                "key", "name", "ver", "desc", configuration, (ProcessInfo) null);
        ResourceDiscoveryComponent c = null;
        return createChild(drd, platform, configuration, parent, c);
    }

    private ResourceComponent createChild(DiscoveredResourceDetails drd,
            Resource resource,
            Configuration configuration,
            ResourceComponent parentComponent,
            ResourceDiscoveryComponent rdc) throws Exception
    {
        ResourceType type = pmm.getType(drd.getResourceType());

        Resource cresource = new Resource();
        cresource.setResourceType(type);
        cresource.setPluginConfiguration(configuration);
        cresource.setResourceKey(drd.getResourceKey());
        cresource.setParentResource(resource);
        cresource.setName(drd.getResourceName());
        cresource.setVersion(drd.getResourceVersion());

        String rclassname = pmm.getComponentClass(type);
        ResourceComponent component = (ResourceComponent) Class.forName(rclassname).newInstance();

        AvailabilityContext availContext = new AvailabilityContextImpl(cresource);
        InventoryContext inventoryContext = new InventoryContextImpl(cresource);

        EventContext eventContext = new EventContextImpl(resource);
        ResourceContext context = new ResourceContext(cresource, parentComponent,
                null, rdc, systemInfo, temporaryDirectory, dataDirectory,
                pluginContainerName, eventContext, operationContext, contentContext,
                availContext, inventoryContext,pluginContainerDeployment);

        component.start(context);
        components.put(component, cresource);
        resources(type.getChildResourceTypes(), cresource, component, context);
        return component;
    }

    private void resources(Set<ResourceType> types, Resource parent, ResourceComponent component, ResourceContext context) throws Exception {
        for (ResourceType type : types) {
            String s = pmm.getDiscoveryClass(type);
            if (s == null) {
                throw new NullPointerException("no discovery " + type);
            }
            ResourceDiscoveryComponent rdc = (ResourceDiscoveryComponent) Class.forName(s).newInstance();
            log.debug("rdc=" + rdc);
            List<Configuration> configList = new ArrayList<Configuration>();
            ResourceDiscoveryContext resourceDiscoveryContext = new ResourceDiscoveryContext(type, component,
                    context, systemInfo,
                    performProcessScans(type), configList,
                    pluginContainerName, pluginContainerDeployment);
            Set<DiscoveredResourceDetails> drds = rdc.discoverResources(resourceDiscoveryContext);
            for (DiscoveredResourceDetails drd : drds) {
                log.debug("discovered " + drd);
                ResourceType resourceType = drd.getResourceType();
                setConfiguration(drd.getPluginConfiguration(), resourceType);
                createChild(drd, parent, drd.getPluginConfiguration(), component, rdc);
            }
            if (drds.isEmpty()) {
                log.warn("not discovered " + type);
                context.getPluginConfiguration();
            }
        }

    }

    /**
     * Called before the configuration is processed; override to set specific plugin parameters.
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
    }

    /**
     * Returns a measurement report.
     */
    public MeasurementReport getMeasurementReport(ResourceComponent component) throws Exception {
        Resource resource = this.components.get(component);
        ResourceType type = resource.getResourceType();
        MeasurementReport report = new MeasurementReport();
        Set<MeasurementScheduleRequest> s = new HashSet<MeasurementScheduleRequest>();
        for (MeasurementDefinition md : type.getMetricDefinitions())
            s.add(new MeasurementScheduleRequest(new MeasurementSchedule(md, resource)));
        ((MeasurementFacet)component).getValues(report, s);
        return report;
    }

    /**
     * Returns the first resource component by resource name, then looks by matching resource type name,
     * then asserts failure if not found.
     */
    public ResourceComponent getComponent(String name) {
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet())
            if (c.getValue().getName().equals(name))
                return c.getKey();
        for (Map.Entry<ResourceComponent, Resource> c : components.entrySet())
            if (c.getValue().getResourceType().getName().equals(name))
                return c.getKey();
        fail("component not found " + name + " in " + components.entrySet());
        return null;
    }

    /**
     * Returns a resource matching this component.
     */
    public Resource getResource(ResourceComponent rc) {
        Resource r = components.get(rc);
        if (r == null)
            throw new IllegalStateException();
        return r;
    }

    /**
     * Returns a resource matching this name.
     */
    public Resource getResource(String name) {
        return getResource(getComponent(name));
    }

    /**
     * Builds a new configuration for a resource type.
     */
    public Configuration getConfiguration(ResourceType resourceType) {
        Configuration configuration = resourceType.getPluginConfigurationDefinition().getDefaultTemplate().createConfiguration();
        setConfiguration(configuration, resourceType);
        return configuration;
    }

    // ASSERT METHOD

    /**
     * From a measurement report, returns a measurement value, or asserts failure if no such value exists.
     */
    public static Double getValue(MeasurementReport report, String name) {
        for (MeasurementDataNumeric m: report.getNumericData()) {
            if (m.getName().equals(name)) {
                return m.getValue();
            }
        }
        fail("report does not incude " + name + " report " + report.getNumericData());
        return null;
    }

    /**
     * Asserts the resource component is available.
     */
    public static void assertUp(ResourceComponent component) {
        assertEquals("up " + component, AvailabilityType.UP, component.getAvailability());
    }

    /**
     * Asserts the resource component is unavailable.
     */
    public static void assertDown(ResourceComponent component) {
        assertEquals("down " + component, AvailabilityType.DOWN, component.getAvailability());
    }

    /**
     * Sets a configuration option.
     */
    public static void set(Configuration config, String name, String value) {
        PropertySimple s = config.getSimple(name);
        if (s == null) {
            s = new PropertySimple(name, value);
            config.put(s);
        } else {
            s.setStringValue(value);
        }
    }

    private List<ProcessScanResult> performProcessScans(ResourceType serverType) {
        List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
        Set<ProcessScan> processScans = serverType.getProcessScans();
        log.debug("Executing process scans for server type " + serverType + "...");
        ProcessInfoQuery piq = new ProcessInfoQuery(processInfo);
        for (ProcessScan processScan : processScans) {
            List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
            for (ProcessInfo autoDiscoveredProcess : queryResults) {
                scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                log.info("Process scan auto-detected new server resource: scan=[" + processScan
                        + "], discovered-process=[" + autoDiscoveredProcess + "]");
            }
        }
        return scanResults;
    }

    /**
     * AutoDiscoveryExecutor method.
     */
    private List<ProcessInfo> getProcessInfos() {
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        log.debug("Retrieving process table...");
        long startTime = System.currentTimeMillis();
        List<ProcessInfo> processInfos = null;
        try {
            processInfos = systemInfo.getAllProcesses();
        } catch (UnsupportedOperationException uoe) {
            log.debug("Cannot perform process scan - not supported on this platform. (" + systemInfo.getClass() + ")");
        }
        long elapsedTime = System.currentTimeMillis() - startTime;
        log.debug("Retrieval of process table took " + elapsedTime + " ms.");
        return processInfos;
    }

    /**
     * Returns the plugin descriptor.
     */
    public PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    /**
     * Returns the plugin descriptor.
     */
    public ResourceDescriptor getResourceDescriptor(String name) {
        ResourceDescriptor rd = descriptors.get(name);
        if (rd == null)
            throw new IllegalStateException("no descriptor " + name + " in " + descriptors.keySet());
        return rd;
    }

    private void buildDesc(List<? extends ResourceDescriptor> l) {
        for (ResourceDescriptor rd : l) {
            descriptors.put(rd.getName(), rd);
            if (rd instanceof ServerDescriptor) {
                buildDesc(((ServerDescriptor)rd).getServers());
                buildDesc(((ServerDescriptor)rd).getServices());
            }
            if (rd instanceof ServiceDescriptor) {
                buildDesc(((ServiceDescriptor)rd).getServices());
                buildDesc(((ServiceDescriptor)rd).getServices());
            }
        }
    }

    /**
     * Asserts that all measurements in the report are present
     * according to the resource descriptor.
     *
     * @see #getResourceDescriptor(String) for obtaining this.
     * @param report
     */
    public static void assertAll(MeasurementReport report, ResourceDescriptor l) {
        HashMap<String, MetricDescriptor> map = new HashMap<String, MetricDescriptor>();
        for (MetricDescriptor md : l.getMetric()) {
            map.put(md.getProperty(), md);
        }
        for (MeasurementDataNumeric n : report.getNumericData()) {
            map.remove(n.getName());
        }
        for (MeasurementDataTrait n : report.getTraitData()) {
            map.remove(n.getName());
        }
        assertTrue("Measurements not found " + map.keySet(), map.isEmpty());
    }

    /**
     * Returns the event manager.
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Set to false to avoid scanning local machine processes to speed up testing.
     */
    public void setProcessScan(boolean processScan) {
        this.processScan = processScan;
    }

    public void assertAll(ConfigurationFacet cf, ResourceDescriptor rd) throws Exception {
        Configuration config = cf.loadResourceConfiguration();
        List<JAXBElement<? extends ConfigurationProperty>> templates = rd.getResourceConfiguration().getConfigurationProperty();
        for (JAXBElement<? extends ConfigurationProperty> template : templates) {
            String name = template.getValue().getName();
            // Property property = config.get(name);
            assertNotNull("config contains " + name, config.get(name));
            Object value = config.getSimpleValue(name, null);
            assertNotNull("value for " + name, value);
            log.debug("config found " + name + " value " + value );
        }
    }

}
