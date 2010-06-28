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
 * said platform. It will deploy an original distro file then upgrade it, utilitizing the
 * Ant bundle system.
 */

var TestsEnabled = true;

var bundleName = 'test-bundle-upgrade-ant';
var bundleZipFile1 = 'src/test/resources/test-upgrade-bundle-ant1.zip';
var bundleZipFile2 = 'src/test/resources/test-upgrade-bundle-ant2.zip';

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
   Assert.assertNotNull(bundleType, "need ant bundle plugin installed for this test");

   // delete the test bundle if it exists
   var bc = new BundleCriteria();
   bc.addFilterName( bundleName );
   var bundles = BundleManager.findBundlesByCriteria( bc );
   if ( null != bundles && bundles.size() > 0 ) {
      print( "\nDeleting existing test ant bundle in order to test a fresh deploy...")
      BundleManager.deleteBundle( bundles.get(0).getId() );
   }

   // create bundleVersion 1.0
   var distributionFile1 = new java.io.File(bundleZipFile1);
   distributionFile1 = new java.io.File(distributionFile1.getAbsolutePath());
   Assert.assertTrue(distributionFile1.exists(), "missing ant bundle file 1: " + distributionFile1);
   var testBundleVersion1 = BundleManager.createBundleVersionViaFile( distributionFile1 );

