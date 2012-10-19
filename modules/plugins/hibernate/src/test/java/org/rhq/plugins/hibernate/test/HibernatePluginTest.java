/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc. All rights reserved.
 */

package org.rhq.plugins.hibernate.test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.FileSystemPluginFinder;
import org.rhq.core.pc.plugin.PluginEnvironment;
import org.rhq.core.pc.plugin.PluginManager;

/**
 * @author Greg Hinkle
 */
@Test(groups = "hibernate-plugin")
public class HibernatePluginTest
{
   private Log log = LogFactory.getLog(HibernatePluginTest.class);
   private static final String PLUGIN_NAME = "Hibernate";

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
         log.info("PC started.");
         for (String plugin : PluginContainer.getInstance().getPluginManager().getMetadataManager().getPluginNames()) {
             log.info("...Loaded plugin: " + plugin);
         }
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
      PluginEnvironment pluginEnvironment = pluginManager.getPlugin(PLUGIN_NAME);
      assert (pluginEnvironment != null) : "Null environment, plugin not loaded";
      assert (pluginEnvironment.getPluginName().equals(PLUGIN_NAME));
   }

   @Test(dependsOnMethods = "testPluginLoad")
   public void testDiscovery() throws Exception
   {
      InventoryReport report = PluginContainer.getInstance().getInventoryManager().executeServerScanImmediately();
      assert report != null;
      System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

      report = PluginContainer.getInstance().getInventoryManager().executeServiceScanImmediately();
      assert report != null;
      System.out.println("Discovery took: " + (report.getEndTime() - report.getStartTime()) + "ms");

      Set<Resource> resources = findResource(PluginContainer.getInstance().getInventoryManager().getPlatform(), "Hibernate Statistics");
      System.out.println("Found " + resources.size() + " hibernate services");
   }

   @Test(dependsOnMethods = "testDiscovery")
   public void testMeasurement() throws Exception
   {
      for (Resource server : findResource(PluginContainer.getInstance().getInventoryManager().getPlatform(), "Hibernate Statistics"))
      {
         testResourceMeasurement(server);
      }
   }

   private Set<Resource> findResource(Resource parent, String typeName)
   {
      Set<Resource> found = new HashSet<Resource>();
      @SuppressWarnings({"UnnecessaryLocalVariable"})
      Resource platform = parent;
      for (Resource resource : platform.getChildResources())
      {
         if (resource.getResourceType().getName().equals(typeName))
         {
            found.add(resource);
         }
         if (resource.getChildResources() != null)
         {
            for (Resource child : found)
            {
               found.addAll(findResource(child,typeName));
            }
         }
      }
      return found;
   }

   private void testResourceMeasurement(Resource resource)throws Exception
   {
      ResourceComponent resourceComponent = PluginContainer.getInstance().getInventoryManager().getResourceComponent(resource);
      if (resourceComponent instanceof MeasurementFacet)
      {
         for (MeasurementDefinition def : resource.getResourceType().getMetricDefinitions())
         {
            Set<MeasurementScheduleRequest> metricList = new HashSet<MeasurementScheduleRequest>();
            metricList.add(new MeasurementScheduleRequest(1, def.getName(), 1000, true, def.getDataType(), null));
            MeasurementReport report = new MeasurementReport();
            ((MeasurementFacet) resourceComponent).getValues(report, metricList);

            assert report.getNumericData().size() > 0 : "Measurement " + def.getName() + " not collected from " + resource;
            MeasurementData data = report.getNumericData().iterator().next();
            assert data != null : "Unable to collect metric [" + def.getName() + "] on " + resource;
            System.out.println("Measurement: " + def.getName() + "=" + data.getValue());
         }
      }
   }
}
