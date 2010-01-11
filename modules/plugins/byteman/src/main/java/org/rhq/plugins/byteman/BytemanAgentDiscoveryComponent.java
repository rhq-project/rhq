package org.rhq.plugins.byteman;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discovers a Byteman agent.
 *
 * @author John Mazzitelli
 */
public class BytemanAgentDiscoveryComponent implements ResourceDiscoveryComponent<BytemanAgentComponent>,
    ManualAddFacet<BytemanAgentComponent>, ClassLoaderFacet<BytemanAgentComponent> {
    private final Log log = LogFactory.getLog(BytemanAgentDiscoveryComponent.class);

    public static final String DEFAULT_BYTEMAN_ADDRESS = "127.0.0.1";
    public static final String DEFAULT_BYTEMAN_PORT = "9091";

    public static final String PLUGIN_CONFIG_PROP_ADDRESS = "listenerAddress";
    public static final String PLUGIN_CONFIG_PROP_PORT = "listenerPort";
    public static final String PLUGIN_CONFIG_PROP_CLIENT_JAR = "bytemanClientJar";

    private static final String DEFAULT_DESCRIPTION = "Byteman agent that is able to perform byte-code manipulation within its JVM";
    private static final String DEFAULT_NAME = "Byteman";

    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<BytemanAgentComponent> context) {
        log.info("Discovering byteman agents");

        HashSet<DiscoveredResourceDetails> set = new HashSet<DiscoveredResourceDetails>();

        // auto-discovery is performed by the container for us, via process scans
        List<ProcessScanResult> autoDiscoveryResults = context.getAutoDiscoveredProcesses();
        for (ProcessScanResult autoDiscoveryResult : autoDiscoveryResults) {
            String[] cmdline = autoDiscoveryResult.getProcessInfo().getCommandLine();
            if (cmdline != null) {
                for (String arg : cmdline) {
                    if (arg.startsWith("-javaagent:") && arg.contains("byteman") && arg.contains(".jar")) {
                        // we know the main byteman jar has the submit client in it, too - so use it for the client jar
                        String libPath = arg.substring("-javaagent:".length(), arg.indexOf(".jar") + ".jar".length());

                        // try to normalize libPath to the byteman jar by getting the absoluate path to it
                        File libFile = new File(libPath);
                        if (libFile.exists()) {
                            libPath = libFile.getAbsolutePath();
                        } else {
                            // hmm... the byteman jar doesn't exist at the path we found; its probably a relative path to the VM's current working dir 
                            String cwd = autoDiscoveryResult.getProcessInfo().getCurrentWorkingDirectory();
                            libFile = new File(cwd, libPath);
                            if (libFile.exists()) {
                                libPath = libFile.getAbsolutePath();
                            }
                        }

                        // get the listener address
                        String address = DEFAULT_BYTEMAN_ADDRESS;
                        Pattern pattern = Pattern.compile(".*address:([^,]+).*");
                        Matcher matcher = pattern.matcher(arg);
                        if (matcher.matches()) {
                            address = matcher.group(1);

                            // sanity check
                            if (address == null) {
                                log.warn("Address could not be parsed from byteman cmdline: " + arg);
                                address = DEFAULT_BYTEMAN_ADDRESS;
                            }
                        }

                        // get the listener port
                        String port = DEFAULT_BYTEMAN_PORT;
                        pattern = Pattern.compile(".*port:(\\d+).*");
                        matcher = pattern.matcher(arg);
                        if (matcher.matches()) {
                            port = matcher.group(1);

                            // sanity check
                            try {
                                Integer.parseInt(port);
                            } catch (NumberFormatException e) {
                                log.warn("Port # could not be parsed from byteman cmdline: " + arg);
                                port = DEFAULT_BYTEMAN_PORT;
                            }
                        }

                        // create a default plugin config, with the data we just discovered
                        Configuration pluginConfig = context.getDefaultPluginConfiguration();
                        pluginConfig.put(new PropertySimple(PLUGIN_CONFIG_PROP_ADDRESS, address));
                        pluginConfig.put(new PropertySimple(PLUGIN_CONFIG_PROP_PORT, port));
                        pluginConfig.put(new PropertySimple(PLUGIN_CONFIG_PROP_CLIENT_JAR, libPath));

                        // build the general information for the new resource
                        String key = address + ':' + port;
                        String name = DEFAULT_NAME;
                        String description = DEFAULT_DESCRIPTION;
                        String version = getVersion(address, port, libPath);

                        // add the new details for the new resource in our set to be returned
                        DiscoveredResourceDetails details = new DiscoveredResourceDetails(context.getResourceType(),
                            key, name, version, description, pluginConfig, null);
                        set.add(details);

                        // done, no need to keep looking at the command line args, we found what we need
                        break;
                    }
                } // END for each command line argument
            } // END if command line arguments exist
        } // END for each process found in the process scan

        return set;
    }

    public DiscoveredResourceDetails discoverResource(Configuration pluginConfiguration,
        ResourceDiscoveryContext<BytemanAgentComponent> context) throws InvalidPluginConfigurationException {

        // verify the plugin config for correctness
        String address = pluginConfiguration.getSimpleValue(PLUGIN_CONFIG_PROP_ADDRESS, null);
        String port = pluginConfiguration.getSimpleValue(PLUGIN_CONFIG_PROP_PORT, null);
        String clientJar = pluginConfiguration.getSimpleValue(PLUGIN_CONFIG_PROP_CLIENT_JAR, null);

        if (address == null) {
            throw new InvalidPluginConfigurationException("Byteman address was not specified");
        }

        if (port == null) {
            throw new InvalidPluginConfigurationException("Byteman port was not specified");
        }

        if (clientJar == null) {
            throw new InvalidPluginConfigurationException("Byteman client jar was not specified");
        }

        try {
            Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new InvalidPluginConfigurationException("Port number was invalid: " + port, e);
        }

        File clientJarFile = new File(clientJar);
        if (!clientJarFile.isFile() || !clientJarFile.canRead()) {
            throw new InvalidPluginConfigurationException("Byteman client jar [" + clientJar + "] cannot be read");
        }

        // build the general information for the new resource
        String key = address + ':' + port;
        String name = DEFAULT_NAME;
        String description = DEFAULT_DESCRIPTION;
        String version = getVersion(address, port, clientJar);

        DiscoveredResourceDetails details = new DiscoveredResourceDetails(context.getResourceType(), key, name,
            version, description, pluginConfiguration, null);

        return details;
    }

    public List<URL> getAdditionalClasspathUrls(ResourceDiscoveryContext<BytemanAgentComponent> context,
        DiscoveredResourceDetails details) throws Exception {

        PropertySimple clientJarProperty = details.getPluginConfiguration().getSimple(PLUGIN_CONFIG_PROP_CLIENT_JAR);
        if (clientJarProperty == null || clientJarProperty.getStringValue() == null) {
            throw new InvalidPluginConfigurationException("Byteman client jar not specified in plugin configuration");
        }

        String clientJarString = clientJarProperty.getStringValue();
        File clientJarFile = new File(clientJarString);
        if (!clientJarFile.exists()) {
            throw new InvalidPluginConfigurationException("Byteman client jar [" + clientJarString + "] does not exist");
        }
        if (!clientJarFile.canRead()) {
            throw new InvalidPluginConfigurationException("Byteman client jar [" + clientJarString + "] is unreadable");
        }

        List<URL> list = new ArrayList<URL>(1);
        list.add(clientJarFile.toURI().toURL());
        return list;
    }

    /**
     * Tries to determine the version of the remote byteman agent. If, for some reason, the remote agent
     * cannot be contacted, the manifest within the given jar will be used as the version as a "best guess".
     * If all else fails, "0" will be returned as the version.
     * 
     * @param address the address of the remote byteman agent listener
     * @param port the port that the remote byteman agent is listening to
     * @param jarPath the path to the byteman jar (which may be the client jar, or the core byteman jar)
     * 
     * @return the version of the managed byteman agent; "0" if unknown
     */
    protected String getVersion(String address, String port, String jarPath) {
        String version = null;

        Socket commSocket = null;
        try {
            // make a simple request to the remote agent for the version string
            // we can't use the byteman Submit client here because its not in our classloader
            int portInt = Integer.parseInt(port);
            commSocket = new Socket(address, portInt);
            BufferedReader commInput = new BufferedReader(new InputStreamReader(commSocket.getInputStream()));
            PrintWriter commOutput = new PrintWriter(new OutputStreamWriter(commSocket.getOutputStream()));
            commOutput.println("VERSION");
            commOutput.flush();
            String line = commInput.readLine();
            while (line != null && !line.trim().equals("OK")) {
                version = line.trim();
                line = commInput.readLine();
            }
        } catch (Throwable t) {
            // for some reason, failed to communicate with the remote agent, read version from jar manifest
            version = getJarAttribute(jarPath, "Implementation-Version", null);
        } finally {
            if (commSocket != null) {
                try {
                    commSocket.close();
                } catch (Exception ignore) {
                }
            }
        }

        if (version == null) {
            version = "0";
        }

        return version;
    }

    /**
     * Static utility method that can extract a main attribute value from a given jar file's manifest.
     * 
     * @param jarPath location of the jar file
     * @param attributeName name of the main attribute to retrieve
     * @param defaultValue if the manifest doesn't have the attribute or the manifest itself doesn't exist, this is returned
     * @return the value of the attribute
     */
    public static String getJarAttribute(String jarPath, String attributeName, String defaultValue) {
        String attributeValue = null;
        try {
            JarFile jarFile = new JarFile(jarPath);
            try {
                Manifest manifest = jarFile.getManifest();
                attributeValue = manifest.getMainAttributes().getValue(attributeName);
            } finally {
                jarFile.close();
            }
        } catch (Throwable t1) {
            // jar file doesn't have a manifest?
        }

        if (attributeValue == null) {
            attributeValue = defaultValue;
        }
        return attributeValue;
    }
}