/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

/**
 * Thus test script works with a real env including at least one platform resource and a running agent on
 * said platform.
 */

var TestsEnabled = true;

var bundleName = 'testScriptBundle';

// note, super-user, will not test any security constraints
var subject = rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

rhq.logout();

function testDeployment() {
   if ( !TestsEnabled ) {
      return;
   }
   
   // delete the test bundle if it exists
   cleanupTestBundle();

   // create the test bundle
   var testBundle = BundleManager.createBundle( bundleName, bundleName, getBundleType() );
   
   // define the recipe for bundleVersion 1.0 
   var recipe = "file -s testBundle.war -d <%bundleTest.deployHome%>/testBundle.war"
      
   // create bundleVersion 1.0
   var testBundleVersion = BundleManager.createBundleVersion( testBundle.getId(), bundleName, bundleName, null, recipe);

   // add the single bundleFile, the test war file
   var fileBytes = scriptUtil.getFileBytes("./src/test/resources/testBundle.war"); 
   var bundleFile = BundleManager.addBundleFileViaByteArray(testBundleVersion.getId(), "testBundle.war",
         "1.0", null, fileBytes, false);

   // create the config, setting the required properties from the recipe
   var config = new Configuration();   
   var property = new PropertySimple("bundleTest.deployHome", "/tmp/bundle-test");
   config.put( property );

   // create a deploy def using the above config
   var testDeployDef = BundleManager.createBundleDeployDefinition(testBundleVersion.getId(), "Deployment Test", "Deployment Test of testBundle WAR", config, false, -1, false);

   // Find a target platform
   var rc = new ResourceCriteria();
   rc.addFilterResourceTypeName("in"); // wINdows, lINux
   var winPlatforms = ResourceManager.findResourcesByCriteria(rc);
   var platformId = winPlatforms.get(0).getId();
   
   var bd = BundleManager.scheduleBundleDeployment(testDeployDef.getId(), platformId);
   Assert.assertNotNull( bd );   
   
   
   // delete the test bundle if it exists (after allowing agent audit messages to complete)
   sleep( 5000 );
   cleanupTestBundle();
}

function testGroupDeployment() {
   if ( !TestsEnabled ) {
      return;
   }
   
   // delete the test bundle if it exists
   cleanupTestBundle();

   // create the test bundle
   var testBundle = BundleManager.createBundle( bundleName, bundleName, getBundleType() );
   
   // define the recipe for bundleVersion 1.0 
   var recipe = "file -s testBundle.war -d <%bundleTest.deployHome%>/group/testBundle.war"
      
   // create bundleVersion 1.0
   var testBundleVersion = BundleManager.createBundleVersion( testBundle.getId(), bundleName, bundleName, null, recipe);

   // add the single bundleFile, the test war file
   var fileBytes = scriptUtil.getFileBytes("./src/test/resources/testBundle.war"); 
   var bundleFile = BundleManager.addBundleFileViaByteArray(testBundleVersion.getId(), "testBundle.war",
         "1.0", null, fileBytes, false);

   // create the config, setting the required properties from the recipe
   var config = new Configuration();   
   var property = new PropertySimple("bundleTest.deployHome", "/tmp/bundle-test");
   config.put( property );

   // create a deploy def using the above config
   var testDeployDef = BundleManager.createBundleDeployDefinition(testBundleVersion.getId(), "Deployment Test", "Deployment Test of testBundle WAR", config, false, -1, false);

   // Find a target platform group
   var rgc = new ResourceGroupCriteria();
   rgc.addFilterName("platforms"); // wINdows, lINux
   var groups = ResourceGroupManager.findResourceGroupsByCriteria(rgc);
   Assert.assertTrue( groups.size() > 0 );
   var groupId = groups.get(0).getId();
   
   var bgd = BundleManager.scheduleBundleGroupDeployment(testDeployDef.getId(), groupId);
   Assert.assertNotNull( bgd );      
   
   // delete the test bundle if it exists (after allowing agent audit messages to complete)
   sleep( 5000 );
   cleanupTestBundle();
}

function getBundleType() {
 
   var types = BundleManager.getAllBundleTypes();
   for (i=0; ( i < types.size()); ++i ) {
      if ( types.get(i).getName().equals( "File Template Bundle" )) {
         return types.get(i).getId();
      }
   }
   
   print( "\n Could not find template bundle type, is the plugin loaded?");
}

function cleanupTestBundle() {
   // delete the test bundle if it exists
   var bc = new BundleCriteria();
   bc.addFilterName( bundleName );
   var bundles = BundleManager.findBundlesByCriteria( bc );
   if ( null != bundles && bundles.size() > 0 ) {
      print( "\n Deleting existing testScriptBundle in order to test a fresh deploy...")
      BundleManager.deleteBundle( bundles.get(0).getId() );      
   }
}

