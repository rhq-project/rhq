/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

var bundleName = 'test-cli-bundle-zip';

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
   
   // create bundleVersion 1.0
   var distributionFile = new java.io.File("./src/test/resources/cli-test-bundle-zip.zip");
   distributionFile = new java.io.File(distributionFile.getAbsolutePath());
   var testBundleVersion = BundleManager.createBundleVersionViaFile( distributionFile );

   // Find a target platform group
   var rgc = new ResourceGroupCriteria();
   rgc.addFilterName("platforms"); // wINdows, lINux
   var groups = ResourceGroupManager.findResourceGroupsByCriteria(rgc);
   var groupId;
   // create if needed (and possible)
   if ( groups.isEmpty() ) {
      var c = new ResourceCriteria();
      c.addFilterResourceCategory(ResourceCategory.PLATFORM);
      var platforms = ResourceManager.findResourcesByCriteria(c);
      Assert.assertTrue( platforms.size() > 0 );
      
      var rg = new ResourceGroup("platforms");
      var platformSet = new java.util.HashSet();
      platformSet.addAll( platforms );
      rg.setExplicitResources(platformSet);
      rg = ResourceGroupManager.createResourceGroup(rg);
      groupId = rg.getId();
   } else { 
      groupId = groups.get(0).getId();
   }

   // create a destination to deploy to
   var testDest = BundleManager.createBundleDestination( testBundleVersion.getBundle().getId(), "test-cli-bundle-zip destination", "test-cli-bundle-zip destination", "/tmp/bundle-zip-test", groupId);

   // create the config, setting the required properties from the recipe
   var config = new Configuration();   
   var property = new PropertySimple("dummy.name", "NAME REPLACED HERE!!!");
   config.put( property );
   var property = new PropertySimple("dummy.description", "FLOPPY!!!");
   config.put( property );

   // create a deployment using the above config
   var testDeployment = BundleManager.createBundleDeployment(testBundleVersion.getId(), testDest.getId(), "Deployment Test of dummy ZIP", config);
   
   var bd = BundleManager.scheduleBundleDeployment(testDeployment.getId(), false);
   Assert.assertNotNull( bd );      

   // Now performa redeploy, the same thing but a change to the config
   // create the config, setting the required properties from the recipe
   var config2 = new Configuration();   
   var property = new PropertySimple("dummy.name", "NAME REPLACED HERE!!!");
   config2.put( property );
   var property = new PropertySimple("dummy.description", "FLOPPY V2.0 !!!");
   config2.put( property );
   
   // create a deployment using the above config   
   var testRedeploy = BundleManager.createBundleDeployment(testBundleVersion.getId(), testDest.getId(), "Redeploy Test of dummy ZIP", config2);
      
   var bd2 = BundleManager.scheduleBundleDeployment(testRedeploy.getId(), false);
   Assert.assertNotNull( bd2 );

   // delete the test bundle if it exists (after allowing agent audit messages to complete)
   //sleep( 5000 );
   //cleanupTestBundle();
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
   bc.setStrict( true );   
   bc.addFilterName( bundleName );
   var bundles = BundleManager.findBundlesByCriteria( bc );
   if ( null != bundles && bundles.size() > 0 ) {
      print( "\n Deleting existing testScriptBundle in order to test a fresh deploy...")
      BundleManager.deleteBundle( bundles.get(0).getId() );      
   }
}

