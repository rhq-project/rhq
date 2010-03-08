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

var bundleName "testScriptBundle"

// note, super-user, will not test any security constraints
var subject = rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

rhq.logout();

function testDeployment() {
   if ( !TestsEnabled ) {
      return;
   }
   
   var c = new BundleCriteria();
   c.addFilterName( bundleName );
   var bundles = BundleManager.findBundlesViaCriteria( c );
   if ( null != bundles && bundles.size > 0 ) {
      print( "\n Deleting existing testScriptBundle in order to test a fresh deploy...")
      BundleManager.deleteBundle( bundles.get(0).getId() );      
   }
   
   var testBundle = createBundle( bundleName, getBundleType() );
   BundleVersion bv1 = createBundleVersion(bundleName, null, getBundleType());
   Configuration config = new Configuration();
   config.put(new PropertySimple("bundletest.property", "bundletest.property value"));
   BundleDeployDefinition bdd1 = createDeployDefinition("one", bv1, config);
   assertNotNull(bdd1);
   Resource platformResource = createTestResource();
   BundleScheduleResponse bsr = bundleManager.scheduleBundleDeployment(overlord, bdd1.getId(), platformResource
       .getId());
   assertNotNull(bsr);
   assertEquals(bdd1.getId(), bsr.getBundleDeployment().getBundleDeployDefinition().getId());
   assertEquals(platformResource.getId(), bsr.getBundleDeployment().getResource().getId());

}

function getBundleType() {
 
   var types = BundleManager.getAllBundleTypes();
   for ( int i=0; ( i < types.size()); ++i ) {
      if ( types.get(i).getName().toUppercase().contains("TEMPLATE") ) {
         return types.get(i).getId();
   }
   
   print( "\n Could not find template bundle type, is the plugin loaded?");
}
