package org.rhq.plugins.apache;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.apache.parser.ApacheConfigReader;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;
import org.rhq.plugins.apache.parser.ApacheParser;
import org.rhq.plugins.apache.parser.ApacheParserImpl;
import org.rhq.plugins.apache.util.HttpdAddressUtility;
import org.rhq.plugins.platform.PlatformComponent;

public class ApacheServerConfiguration {

    private final Log log = LogFactory.getLog(this.getClass());
    public static final String PLUGIN_CONFIG_PROP_SERVER_ROOT = "serverRoot";
    public static final String PLUGIN_CONFIG_PROP_EXECUTABLE_PATH = "executablePath";
    public static final String PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH = "controlScriptPath";
    public static final String PLUGIN_CONFIG_PROP_URL = "url";
    public static final String PLUGIN_CONFIG_PROP_HTTPD_CONF = "configFile";
    public static final String AUXILIARY_INDEX_PROP = "_index";
    public static final String DEFAULT_EXECUTABLE_PATH = "bin" + File.separator
        + ((File.separatorChar == '/') ? "httpd" : "Apache.exe");
    public static final String DEFAULT_ERROR_LOG_PATH = "logs" + File.separator
        + ((File.separatorChar == '/') ? "error_log" : "error.log");
    private static final String[] CONTROL_SCRIPT_PATHS = { "bin/apachectl", "sbin/apachectl", "bin/apachectl2",
        "sbin/apachectl2" };

    private ResourceContext<PlatformComponent> resourceContext;
    
    public ApacheServerConfiguration(ResourceContext<PlatformComponent> context){
        this.resourceContext = context;
    }
    /**
     * Return the absolute path of this Apache server's executable (e.g. "C:\Program Files\Apache
     * Group\Apache2\bin\Apache.exe").
     *
     * @return the absolute path of this Apache server's executable (e.g. "C:\Program Files\Apache
     *         Group\Apache2\bin\Apache.exe")
     */
    @NotNull
    public File getExecutablePath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String executablePath = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, null);
        File executableFile;
        if (executablePath != null) {
            executableFile = resolvePathRelativeToServerRoot(pluginConfig,executablePath);
        } else {
            List<ApacheDirective> serverRootDirectives = loadParser().getRootNode().getChildByName("ServerRoot");
            String serverRoot = serverRootDirectives.get(0).getValues().get(0);
            
            SystemInfo systemInfo = this.resourceContext.getSystemInformation();
            if (systemInfo.getOperatingSystemType() != OperatingSystemType.WINDOWS) // UNIX
            {
                // Try some combinations in turn
                executableFile = new File(serverRoot, "bin/httpd");
                if (!executableFile.exists()) {
                    executableFile = new File(serverRoot, "bin/apache2");
                }
                if (!executableFile.exists()) {
                    executableFile = new File(serverRoot, "bin/apache");
                }
            } else // Windows
            {
                executableFile = new File(serverRoot, "bin/Apache.exe");
            }
        }

        return executableFile;
    }
    
    /**
     * Return the absolute path of this Apache server's control script (e.g. "C:\Program Files\Apache
     * Group\Apache2\bin\Apache.exe").
     *
     * On Unix we need to try various locations, as some unixes have bin/ conf/ .. all within one root
     * and on others those are separated.
     *
     * @return the absolute path of this Apache server's control script (e.g. "C:\Program Files\Apache
     *         Group\Apache2\bin\Apache.exe")
     */
    @NotNull
    public File getControlScriptPath() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String controlScriptPath = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_CONTROL_SCRIPT_PATH, null);
        File controlScriptFile = null;
        if (controlScriptPath != null) {
            controlScriptFile = resolvePathRelativeToServerRoot(pluginConfig,controlScriptPath);
        } else {
            SystemInfo systemInfo = this.resourceContext.getSystemInformation();
            if (systemInfo.getOperatingSystemType() != OperatingSystemType.WINDOWS) // UNIX
            {
                boolean found = false;
                // First try server root as base
                List<ApacheDirective> serverRootDirectives = loadParser().getRootNode().getChildByName("ServerRoot");
                String serverRoot = serverRootDirectives.get(0).getValues().get(0);
                
                for (String path : CONTROL_SCRIPT_PATHS) {
                    controlScriptFile = new File(serverRoot, path);
                    if (controlScriptFile.exists()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    String executablePath = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, null);
                    if (executablePath != null) {
                        // this is now somethig like /usr/sbin/httpd .. trim off the last 2 parts
                        int i = executablePath.lastIndexOf('/');
                        executablePath = executablePath.substring(0, i);
                        i = executablePath.lastIndexOf('/');
                        executablePath = executablePath.substring(0, i);
                        for (String path : CONTROL_SCRIPT_PATHS) {
                            controlScriptFile = new File(executablePath, path);
                            if (controlScriptFile.exists()) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (!found) {
                    controlScriptFile = getExecutablePath(); // fall back to the httpd binary
                }
            } else // Windows
            {
                controlScriptFile = getExecutablePath();
            }
        }

        return controlScriptFile;
    }

    //TODO this needs to go...
    @NotNull
    static File resolvePathRelativeToServerRoot(Configuration pluginConfig, @NotNull String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            String serverRoot = getRequiredPropertyValue(pluginConfig, PLUGIN_CONFIG_PROP_SERVER_ROOT);
            file = new File(serverRoot, path);
        }

        return file;
    }
    
    @NotNull
    static String getRequiredPropertyValue(@NotNull Configuration config, @NotNull String propName) {
        String propValue = config.getSimpleValue(propName, null);
        if (propValue == null) {
            // Something's not right - neither autodiscovery, nor the config edit GUI, should ever allow this.
            throw new IllegalStateException("Required property '" + propName + "' is not set.");
        }

        return propValue;
    }
    
    public HttpdAddressUtility getAddressUtility() {
        String version = resourceContext.getVersion();
        return HttpdAddressUtility.get(version);
    }
    
    public File getFileRelativeToServerRoot(String path) {
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        } else {
            return new File(getServerRoot(), path);
        }   
    }
    
    public ApacheDirectiveTree loadParser() {
        ApacheDirectiveTree tree = new ApacheDirectiveTree();
        ApacheParser parser = new ApacheParserImpl(tree,getServerRoot().getAbsolutePath());
        ApacheConfigReader.buildTree(getHttpdConfFile().getAbsolutePath(), parser);
        return tree;       
    }

    /**
     * Returns the httpd.conf file
     * @return A File object that represents the httpd.conf file or null in case of error
     */
    public File getHttpdConfFile() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        PropertySimple prop = pluginConfig.getSimple(PLUGIN_CONFIG_PROP_HTTPD_CONF);
        if (prop == null || prop.getStringValue() == null)
            return null;
        return resolvePathRelativeToServerRoot(pluginConfig, prop.getStringValue());
    }
    
    /**
     * Return the absolute path of this Apache server's server root (e.g. "C:\Program Files\Apache Group\Apache2").
     *
     * @return the absolute path of this Apache server's server root (e.g. "C:\Program Files\Apache Group\Apache2")
     */
    @NotNull
    public File getServerRoot() {
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        String serverRoot = getRequiredPropertyValue(pluginConfig, PLUGIN_CONFIG_PROP_SERVER_ROOT);
        return new File(serverRoot);
    }

    public URL getUrl(){
      Configuration pluginConfig = resourceContext.getPluginConfiguration();
      String urlStr = pluginConfig.getSimpleValue(PLUGIN_CONFIG_PROP_URL, null);
      if (urlStr != null) {
        try {
            URL url = new URL(urlStr);
            if (url.getPort() == 0) {
                log
                    .error("The 'url' connection property is invalid - 0 is not a valid port; please change the value to the "
                        + "port the \"main\" Apache server is listening on. NOTE: If the 'url' property was set this way "
                        + "after autodiscovery, you most likely did not include the port in the ServerName directive for "
                        + "the \"main\" Apache server in httpd.conf.");
            } else {
                return url;
            }
        } catch (MalformedURLException e) {
            throw new InvalidPluginConfigurationException("Value of '" + PLUGIN_CONFIG_PROP_URL
                + "' connection property ('" + urlStr + "') is not a valid URL.");
        }
      }
      return null;
    }
}
