package org.rhq.plugins.tomcat.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.rhq.plugins.tomcat.TomcatDiscoveryComponent;

/**
 * @author Jason Dobies
 */
public class TomcatDiscoveryComponentTest {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    // Test Cases  --------------------------------------------

    @Test
    public void discovery() {
        // Setup
        ResourceType resourceType = new ResourceType("Tomcat Server", "tomcat", ResourceCategory.SERVER, null);

        // Tell the native system where to find the SIGAR libraries. This is done here in addition to the maven script
        // to allow these tests to be run from outside of maven
        System.setProperty("org.hyperic.sigar.path", "." + File.separator + "target" + File.separator + "jboss-sigar");
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        // The query is defined in the plugin's rhq-plugin.xml. It's easier to just hardcode it here than try to read it
        // from the descriptor. If this starts to act weird, compare this query string to the process-scan element
        // in the plugin descriptor to make sure it hasn't changed in the plugin.
        ProcessScan tomcatProcessScan1 = new ProcessScan(
            "process|basename|match=^java.*,arg|org.apache.catalina.startup.Bootstrap|match=.*", "Tomcat");
        ProcessScan tomcatProcessScan2 = new ProcessScan(
            "process|basename|match=^java.*,arg|org.apache.tomcat.startup.Main|match=.*", "Tomcat3");
        resourceType.addProcessScan(tomcatProcessScan1);
        resourceType.addProcessScan(tomcatProcessScan2);

        // The process scan is run automatically by the plugin container and passed into the discovery component
        List<ProcessScanResult> processScanResults = null;
        try {
            processScanResults = performProcessScan(resourceType, systemInfo);
        } catch (Exception e) {
            log.error("Could not perform process scan for Tomcat servers", e);
            return;
        }

        // Package up for the discovery call
        ResourceDiscoveryContext context = new ResourceDiscoveryContext(resourceType, null, null, systemInfo,
            processScanResults, null, "test");

        // Test
        TomcatDiscoveryComponent discoveryComponent = new TomcatDiscoveryComponent();
        Set<DiscoveredResourceDetails> resources = discoveryComponent.discoverResources(context);

        // Log them first so that we can see the results even if there is a failure
        log.info("===================================");
        log.info("Discovered Tomcat Servers");
        log.info("");

        int counter = 0;
        for (DiscoveredResourceDetails resource : resources) {
            log.info("");
            log.info("Resource Number: " + counter++);
            log.info("Name: " + resource.getResourceName());
            log.info("Key: " + resource.getResourceKey());
            log.info("Version: " + resource.getResourceVersion());
            log.info("Type: " + resource.getResourceType());
            log.info("Process Info: " + resource.getProcessInfo());

            Configuration pluginConfiguration = resource.getPluginConfiguration();
            if (pluginConfiguration != null) {
                log.info("Plugin Configuration:");

                Collection<String> names = pluginConfiguration.getNames();
                for (String name : names) {
                    Property property = pluginConfiguration.get(name);

                    if (property instanceof PropertySimple) {
                        PropertySimple simple = (PropertySimple) property;
                        log.info("  Property: " + name + " -> " + simple.getStringValue());
                    }
                }
            }

            log.info("");
            log.info("===================================");
        }

        counter = 0;
        for (DiscoveredResourceDetails resource : resources) {
            assert resource.getResourceName() != null : "Resource name was null for resource number " + counter;
            assert resource.getResourceKey() != null : "Resource key was null for resource number " + counter;
            assert resource.getResourceVersion() != null : "Resource version was null for resource number " + counter;
            assert !resource.getResourceVersion().equals(TomcatDiscoveryComponent.UNKNOWN_VERSION) : "Resource version was unknown for resource number "
                + counter;
            assert resource.getResourceType() != null : "Resource type was null for resource number " + counter;

            counter++;
        }
    }

    // Private  --------------------------------------------

    /**
     * Simplified version of the process scan found in the plugin container's <code>AutoDiscoveryExecutor</code> that is
     * used to execute the automatic process scan to find running Tomcat instances.
     *
     * @param resourceType describes the Tomcat server resource type
     * @param systemInfo   native system info class used to perform the process scan
     *
     * @return list of found processes matching the Tomcat server resource type's description of how to find them;
     *         empty list if none are matched
     */
    private List<ProcessScanResult> performProcessScan(ResourceType resourceType, SystemInfo systemInfo) {
        List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();

        for (ProcessScan processScan : resourceType.getProcessScans()) {
            ProcessInfoQuery piq = new ProcessInfoQuery(systemInfo.getAllProcesses());
            List<ProcessInfo> queryResults = piq.query(processScan.getQuery());

            if ((queryResults != null) && (queryResults.size() > 0)) {
                for (ProcessInfo autoDiscoveredProcess : queryResults) {
                    scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                }
            }
        }

        return scanResults;
    }
}
