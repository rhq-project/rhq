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
 * This sample script shows how to create a Bundle by uploading an existing
 * Bundle Distribution file. The Bundle uses the Ant Bundle Type provided with
 * RHQ.
 * <pre>
 * Prerequisites:
 *   none
 * 
 * Usage:
 *   1) start the CLI (can be downloaded from the GUI, Administration->Downloads, RHQ Client)
 *   2) login user password serverHost serverPort
 *   3) exec <path>/cli-1-createBundle.js 
 */

var bundleName = 'sample-bundle';
var bundleDistroV1Path = '../resources/sample-bundle-v1.zip';

// delete the test bundle if it exists
var bc = new BundleCriteria();
bc.addFilterName(bundleName);
var bundles = BundleManager.findBundlesByCriteria(bc);
if (null != bundles && bundles.size() > 0) {
   print("\nDeleting [" + bundleName + "] to re-run sample scripts...")
   BundleManager.deleteBundle(bundles.get(0).getId());
}

// create bundleVersion 1.0 for the sample bundle
var distributionFile = new java.io.File(bundleDistroV1Path);
distributionFile = new java.io.File(distributionFile.getAbsolutePath());
Assert.assertTrue(distributionFile.exists(), "Missing ant bundle distribution file: " + distributionFile);
var bundleVersion1 = BundleManager.createBundleVersionViaFile(distributionFile);

print("\nCreated Bundle [" + bundleVersion1 + "]!")
