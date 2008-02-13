/*
 * JBoss, a division of Red Hat.
 * Copyright 2005-2007, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.plugins.apache;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.plugins.apache.util.ApacheBinaryInfo;
import org.rhq.plugins.www.snmp.SNMPSession;
import org.rhq.plugins.www.snmp.SNMPValue;
import org.rhq.plugins.www.snmp.SNMPException;
import org.rhq.plugins.www.snmp.SNMPClient;
import org.rhq.core.system.ProcessInfo;

/**
 * The discovery component for Apache 1.3/2.x servers. 
 *
 * @author Ian Springer
 */
public class ApacheServerDiscoveryComponent
      implements ResourceDiscoveryComponent
{

   private static final String PRODUCT_DESCRIPTION = "Apache Web Server";

   private final Log log = LogFactory.getLog(this.getClass());

   public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext)
         throws Exception
   {
      Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();

      // Process any PC-discovered OS processes...
      List<ProcessScanResult> processes = discoveryContext.getAutoDiscoveredProcesses();
      for (ProcessScanResult process : processes)
      {
         String executablePath = process.getProcessInfo().getName();
         ApacheBinaryInfo binaryInfo;
         try
         {
            binaryInfo = ApacheBinaryInfo.getInfo(executablePath, discoveryContext.getSystemInformation());
         }
         catch (Exception e)
         {
            log.error("'" + executablePath + "' is not a valid Apache executable (" + e + ").");
            continue;
         }
         if (isSupportedVersion(binaryInfo.getVersion()))
         {
            String serverRoot = getServerRoot(binaryInfo, process.getProcessInfo());
            if (serverRoot == null)
            {
               log.error("Unable to determine server root for Apache process: " + process.getProcessInfo());
               continue;
            }
            Configuration pluginConfig = discoveryContext.getDefaultPluginConfiguration();

            PropertySimple executablePathProp = new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, executablePath);
            pluginConfig.put(executablePathProp);

            PropertySimple serverRootProp = new PropertySimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT, serverRoot);
            pluginConfig.put(serverRootProp);

            String url = getUrl(pluginConfig);
            Property urlProp = new PropertySimple(ApacheVirtualHostServiceComponent.URL_CONFIG_PROP, url);
            pluginConfig.put(urlProp);

            discoveredResources.add(createResourceDetails(discoveryContext, pluginConfig, process.getProcessInfo(), binaryInfo));
         }
      }

      // Process any manually added resources (NOTE: the PC will never actually pass in more than one)...
      List<Configuration> pluginConfigs = discoveryContext.getPluginConfigurations();
      for (Configuration pluginConfig : pluginConfigs)
      {
         String serverRoot = ApacheServerComponent.getRequiredPropertyValue(pluginConfig, ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT);
         if (!new File(serverRoot).isDirectory())
         {
            throw new InvalidPluginConfigurationException("'" + serverRoot + "' is not a directory. Please make sure the '" + ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT + "' connection property is set correctly.");
         }
         String executablePath = pluginConfig.getSimpleValue(ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH, ApacheServerComponent.DEFAULT_EXECUTABLE_PATH);
         String absoluteExecutablePath = ApacheServerComponent.resolvePathRelativeToServerRoot(pluginConfig, executablePath).getPath();
         ApacheBinaryInfo binaryInfo;
         try
         {
            binaryInfo = ApacheBinaryInfo.getInfo(absoluteExecutablePath, discoveryContext.getSystemInformation());
         }
         catch (Exception e)
         {
            throw new InvalidPluginConfigurationException("'" + absoluteExecutablePath + "' is not a valid Apache executable (" + e + "). Please make sure the '" + ApacheServerComponent.PLUGIN_CONFIG_PROP_EXECUTABLE_PATH + "' connection property is set correctly.");
         }
         if (!isSupportedVersion(binaryInfo.getVersion()))
         {
            throw new InvalidPluginConfigurationException("Version of Apache executable (" + binaryInfo.getVersion() + ") is not a supported version; supported versions are 1.3.x and 2.x.");
         }
         ProcessInfo processInfo = null;
         DiscoveredResourceDetails resourceDetails = createResourceDetails(discoveryContext, pluginConfig, processInfo, binaryInfo);
         discoveredResources.add(resourceDetails);
      }

      return discoveredResources;
   }

   private boolean isSupportedVersion(String version)
   {
      // TODO: Compare against a version range defined in the plugin descriptor.
      return (version != null) && (version.startsWith("1.3") || version.startsWith("2."));
   }

   private DiscoveredResourceDetails createResourceDetails(ResourceDiscoveryContext discoveryContext, Configuration pluginConfig, ProcessInfo processInfo, ApacheBinaryInfo binaryInfo)
                  throws Exception
   {
      String serverRoot = pluginConfig.getSimple(ApacheServerComponent.PLUGIN_CONFIG_PROP_SERVER_ROOT).getStringValue();
      String key = FileUtils.getCanonicalPath(serverRoot);
      String version = binaryInfo.getVersion();
      String hostname = discoveryContext.getSystemInformation().getHostname();
      String name = hostname + " Apache " + version + " (" + serverRoot + File.separator + ")";

      DiscoveredResourceDetails resourceDetails = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
            key,
            name,
            version,
            PRODUCT_DESCRIPTION,
            pluginConfig,
            processInfo);

      log.debug("Apache Server resource details created: " + resourceDetails);

      return resourceDetails;
   }

   /**
    * Return the root URL of the first virtual host (i.e. the "main" Apache server). The URL's host and port is
    * determined by querying the Apache SNMP agent. The URL's protocol is assumed to be "http" and its path is
    * assumed to be "/". If the SNMP agent cannot be reached, null will be returned.
    *
    * @param pluginConfig
    * @return
    * @throws Exception
    */
   @Nullable
   private static String getUrl(Configuration pluginConfig)
         throws Exception
   {
      SNMPSession snmpSession = ApacheServerComponent.getSNMPSession(new SNMPClient(), pluginConfig);
      if (!snmpSession.ping())
      {
         return null;
      }

      SNMPValue nameValue, portValue;
      try
      {
         nameValue = snmpSession.getNextValue(SNMPConstants.COLUMN_VHOST_NAME);
      }
      catch (SNMPException e)
      {
         throw new Exception("Error getting SNMP value: "
               + SNMPConstants.COLUMN_VHOST_NAME + ": " + e.getMessage(), e);
      }

      try
      {
         portValue = snmpSession.getNextValue(SNMPConstants.COLUMN_VHOST_PORT);
      }
      catch (SNMPException e)
      {
         throw new Exception("Error getting SNMP column: "
               + SNMPConstants.COLUMN_VHOST_PORT + ": " + e.getMessage(), e);
      }

      String host = nameValue.toString();
      String fullPort = portValue.toString();
      // The port value will be in the form "1.3.6.1.2.1.6.XXXXX",
      // where "1.3.6.1.2.1.6" represents the TCP protocol ID,
      // and XXXXX is the actual port number
      String port = fullPort.substring(fullPort.lastIndexOf(".") + 1);

      String url = "http://" + host + ":" + port + "/";
      return url;
   }

   @Nullable
   private String getServerRoot(@NotNull ApacheBinaryInfo binaryInfo, @NotNull ProcessInfo processInfo)
   {
      String root = null;
      String[] cmdLine = processInfo.getCommandLine();
      for (int i = 1; i < cmdLine.length; i++)
      {
         String arg = cmdLine[i];
         if (arg.startsWith("-d"))
         {
            root = arg.substring(2, arg.length());
            if (root.length() == 0)
            {
               root = cmdLine[i + 1];
            }
            break;
         }
      }
      if (root == null)
      {
         root = binaryInfo.getRoot();
      }
      if (root != null)
      {
         root = FileUtils.getCanonicalPath(root);
      }
      return root;
   }

}