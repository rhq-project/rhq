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
 * said platform. It will deploy an original distro file then upgrade it.
 */

var TestsEnabled = true;

var bundleName = 'test-bundle-upgrade';
var bundleZipFile1 = 'src/test/resources/test-upgrade-bundle1.zip';
var bundleZipFile2 = 'src/test/resources/test-upgrade-bundle2.zip';

// note, super-user, will not test any security constraints
var subject = rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

rhq.logout();

function testGroupDeployment() {
   if ( !TestsEnabled ) {
      return;
   }

   var groupId = getGroupId();
   var bundleType = getBundleType();
   Assert.assertNotNull(bundleType, "need bundle plugin installed for this test");

   // delete the test bundle if it exists
   var bc = new BundleCriteria();
   bc.addFilterName( bundleName );
   var bundles = BundleManager.findBundlesByCriteria( bc );
   if ( null != bundles && bundles.size() > 0 ) {
      print( "\nDeleting existing test bundle in order to test a fresh deploy...")
      BundleManager.deleteBundle( bundles.get(0).getId() );
   }

   // create bundleVersion 1.0
   var distributionFile1 = new java.io.File(bundleZipFile1);
   distributionFile1 = new java.io.File(distributionFile1.getAbsolutePath());
   Assert.assertTrue(distributionFile1.exists(), "missing bundle file 1: " + distributionFile1);
   var testBundleVersion1 = BundleManager.createBundleVersionViaFile( distributionFile1 );

   // create bundleVersion 2.0
   var distributionFile2 = new java.io.File(bundleZipFile2);
   distributionFile2 = new java.io.File(distributionFile2.getAbsolutePath());
   Assert.assertTrue(distributionFile2.exists(), "missing bundle file 2: " + distributionFile2);
   var testBundleVersion2 = BundleManager.createBundleVersionViaFile( distributionFile2 );

   // create 1.0 config, setting the required properties for recipe in distro 1.0 
   var config1 = new Configuration();
   var property11 = new PropertySimple("upgrade.test.name", "Original Name");
   config1.put( property11 );
   var property12 = new PropertySimple("upgrade.test.port", "12345");
   config1.put( property12 );

   // create 2.0 config, setting the required properties for recipe in distro 2.0 
   var config2 = new Configuration();
   var property21 = new PropertySimple("upgrade.test.name", "UPGRADED NAME!");
   config2.put( property21 );
   var property22 = new PropertySimple("upgrade.test.port", "9876");
   config2.put( property22 );
   var property23 = new PropertySimple("upgrade.test.new", "A NEW REPLACEMENT");
   config2.put( property23 );
   
   // create a destination to deploy to
   var testDest = BundleManager.createBundleDestination(testBundleVersion1.getBundle().getId(), "upgrade destination", "upgrade destination", "/tmp/upgrade-bundle-test", groupId);

   // create a deployment of 1.0 using the 1.0 config
   var testDeployment = BundleManager.createBundleDeployment(testBundleVersion1.getId(), testDest.getId(), "Creating initial deployment to be upgraded", config1);
   var bgd = BundleManager.scheduleBundleDeployment(testDeployment.getId(), true);
   Assert.assertNotNull( bgd, "Failed to create 1.0 deployment" );
   
   // upgrade the deployment to 2.0 using the 2.0 config
   testDeployment = BundleManager.createBundleDeployment(testBundleVersion2.getId(), testDest.getId(), "Testing upgrade deployment", config2);
   bgd = BundleManager.scheduleBundleDeployment(testDeployment.getId(), false);
   Assert.assertNotNull( bgd, "Failed to upgrade to 2.0 deployment" );
}

function getGroupId() {
    // Find a target platform group
    var rgc = new ResourceGroupCriteria();
    rgc.addFilterName("platforms");
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
   return groupId;
}

function getBundleType() {
   var types = BundleManager.getAllBundleTypes();
   for (i=0; ( i < types.size()); ++i ) {
      if ( types.get(i).getName().equals( "File Template Bundle" )) {
         return types.get(i).getId();
      }
   }
   return null;
}
