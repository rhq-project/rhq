/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc. All rights reserved.
 */

package org.rhq.plugins.platform.test;

import java.io.File;
import java.io.PrintWriter;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.InventoryPrinter;

public class PlatformPluginTest
{
    private Log log = LogFactory.getLog(PlatformPluginTest.class);

   @BeforeSuite
   public void start()
   {
      try
      {
         File pluginDir = new File("target/itest/plugins");
         PluginContainerConfiguration pcConfig = new PluginContainerConfiguration();
         pcConfig.setPluginFinder(new FileSystemPluginFinder(pluginDir));
         pcConfig.setPluginDirectory(pluginDir);
         pcConfig.setInsideAgent(false);

         PluginContainer.getInstance().setConfiguration(pcConfig);
         PluginContainer.getInstance().initialize();
         System.out.println("PC Started");
         for (String plugin : PluginContainer.getInstance().getPluginManager().getMetadataManager().getPluginNames())
            System.out.println("PLUGIN: " + plugin);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   @AfterSuite
   public void stop()
   {
      PluginContainer.getInstance().shutdown();
   }

   @Test
   public void testPluginLoad()
   {
      PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();

      PluginEnvironment pluginEnvironment = pluginManager.getPlugin("Platforms");
      assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
      System.out.println("Plugin package: " + pluginEnvironment.getDescriptor().getPackage());

   }

   @Test(dependsOnMethods = "testPluginLoad")
   public void testServerDiscovery() throws Exception
   {
      InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
      assert report != null;
      System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

      Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();
      Set<Resource> servers = platform.getChildResources();
      // assert servers.size() != 0; Don't require a running app server for testing at this point
      System.out.println("Found " + servers.size() + " servers");

   }

   @Test(dependsOnMethods = "testServerDiscovery")
   public void testServiceDiscovery() throws Exception
   {
      try
      {
         InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
         Resource platform = PluginContainer.getInstance().getInventoryManager().getPlatform();

         /*System.out.println("RUNTIME SERVERS: " + platform.getChildResources().size());
         for (Resource server : platform.getChildResources())
         {
            System.out.println("Server: " + server.toString());
            System.out.println("Found with " + server.getChildResources().size() + " child services");
         }*/
      }
      catch (Exception e)
      {
         log.error("Failure to run discovery", e);
         throw e;
      }
      InventoryPrinter.outputInventory(new PrintWriter(System.out), false);
   }

}
