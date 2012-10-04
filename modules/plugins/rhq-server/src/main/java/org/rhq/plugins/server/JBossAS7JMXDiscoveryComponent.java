package org.rhq.plugins.server;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Just returns the singleton jmx mbeans parent resource.
 * 
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class JBossAS7JMXDiscoveryComponent<T extends ResourceComponent<JBossAS7JMXComponent<?>>> implements
    ResourceDiscoveryComponent<T>, ClassLoaderFacet<ResourceComponent<JBossAS7JMXComponent<?>>> {

    private Log log = LogFactory.getLog(JBossAS7JMXDiscoveryComponent.class);

    @Override
    public List<URL> getAdditionalClasspathUrls(
        ResourceDiscoveryContext<ResourceComponent<JBossAS7JMXComponent<?>>> context,
        DiscoveredResourceDetails details) throws Exception {

        Configuration pluginConfig = details.getPluginConfiguration();
        String clientJarLocation = pluginConfig.getSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_CLIENT_JAR_LOCATION);
        if (clientJarLocation == null) {
            log.warn("Missing the client jar location - cannot connect to the JBossAS instance: "
                + details.getResourceKey());
            return null;
        }

        File clientJarDir = new File(clientJarLocation);
        if (!clientJarDir.isDirectory()) {
            log.warn("The client jar location [" + clientJarDir.getAbsolutePath()
                + "] does not exist - cannot connect to the JBossAS instance: " + details.getResourceKey());
            return null;
        }

        ArrayList<URL> clientJars = new ArrayList<URL>();
        for (File clientJarFile : clientJarDir.listFiles()) {
            if (clientJarFile.getName().endsWith(".jar")) {
                clientJars.add(clientJarFile.toURI().toURL());
            }
        }

        if (clientJars.size() == 0) {
            log.warn("The client jar location [" + clientJarDir.getAbsolutePath()
                + "] is missing client jars - cannot connect to the JBossAS instance: " + details.getResourceKey());
            return null;
        }

        // XXX: hack to get the JMX plugin class in our classloader
        File tmpDir = new File("/home/mazz/source/rhq/modules/enterprise/agent/target/rhq-agent/data/tmp");
        File pluginsDir = new File(tmpDir, "../../plugins");
        for (File jarFile : pluginsDir.listFiles()) {
            if (jarFile.getName().startsWith("rhq-jmx-plugin")) {
                clientJars.add(jarFile.toURI().toURL());
                break;
            }
        }
        clientJars
            .add(new File("/home/mazz/.m2/repository/mc4j/org-mc4j-ems/1.3/org-mc4j-ems-1.3.jar").toURI().toURL());

        return clientJars;
    }

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<T> context) {

        // TODO: use additional methods to look around for other places where this can be find
        // for example, we might be able to look in the /modules directory for some jars to use if the bin/client dir is gone
        File clientJarDir = null;
        String homeDirStr = context.getParentResourceContext().getPluginConfiguration().getSimpleValue("homeDir");
        if (homeDirStr != null) {
            File homeDirFile = new File(homeDirStr);
            if (homeDirFile.exists()) {
                clientJarDir = new File(homeDirFile, "bin/client");
                if (!clientJarDir.exists()) {
                    log.warn("The client jar location [" + clientJarDir.getAbsolutePath()
                        + "] does not exist - will not be able to connect to the AS7 instance");
                }
            }
        }

        String clientJarLocation = (clientJarDir != null) ? clientJarDir.getAbsolutePath() : null;

        HashSet<DiscoveredResourceDetails> result = new HashSet<DiscoveredResourceDetails>();

        String key = "JBossAS7JMX";
        String name = "jmx mbeans";
        String version = "7"; // this should probably be the actual version of the remote AS7 server being monitored
        String description = "Container for JMX MBeans deployed to AS7";

        Configuration pluginConfig = context.getDefaultPluginConfiguration();
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_HOSTNAME, "127.0.0.1"); // TODO how can we get this from the parent?
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PORT,
            JBossAS7JMXComponent.DEFAULT_PLUGIN_CONFIG_PORT); // TODO how can we get this from the parent?
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_USERNAME, "rhqadmin");
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_PASSWORD, "rhqadmin");
        pluginConfig.setSimpleValue(JBossAS7JMXComponent.PLUGIN_CONFIG_CLIENT_JAR_LOCATION, clientJarLocation);

        DiscoveredResourceDetails resource = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, pluginConfig, null);

        result.add(resource);

        return result;
    }
}