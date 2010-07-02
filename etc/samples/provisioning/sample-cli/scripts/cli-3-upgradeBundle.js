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
 * This sample script shows how to upgrade a Bundle to a new version.  It upgrades the bundle version
 * deployed by the cli-2-deployBundle.js script. The Bundle uses the Ant Bundle Type provided with
 * RHQ.
 * <pre>
 * 
 * The deployment is to /tmp/sample-bundle.
 * 
 * Prerequisites:
 *   1) run cli-1-createBundle.js and cli-2-deployBundle.js   
 *   2) For successful deployment the RHQ Agents must be running on the "platforms" group platforms.      
 * 
 * Usage:
 *   Note, the CLI must be executed in its own bin directory, so have <path> below be the path to the
 *   sample-cli directory.
 * 
 *   1) start the CLI (can be downloaded from the GUI, Administration->Downloads, RHQ Client)
 *   2) login user password serverHost serverPort
 *   3) exec <path>/cli-3-upgradeBundle.js <path>
 *   4) validate the upgrade deployment and that the config settings are applied correctly 
 * </pre>
 */

var bundleName = 'sample-bundle';
var bundleDistroV2Path = args[0] + '/resources/sample-bundle-v2.zip';

//get the bundle if it exists
var bc = new BundleCriteria();
bc.addFilterName(bundleName);
bc.fetchDestinations( true );
var bundles = BundleManager.findBundlesByCriteria(bc);
Assert.assertTrue( null != bundles && bundles.size() > 0 );
var bundle = bundles.get(0);
Assert.assertTrue( null != bundle.getDestinations() && bundle.getDestinations().size() == 1 );
var dest = bundle.getDestinations().get(0);

//create bundleVersion 2.0 for the sample bundle
var distributionFile = new java.io.File(bundleDistroV2Path);
distributionFile = new java.io.File(distributionFile.getAbsolutePath());
Assert.assertTrue(distributionFile.exists(), "Missing ant bundle distribution file: " + distributionFile);
var bundleVersion2 = BundleManager.createBundleVersionViaFile(distributionFile);

print("\nCreated " + bundleVersion1 + "!")

//create a config for the V2.0 deployment, setting the required properties for recipe in distro 2.0 
var config2 = new Configuration();
var property21 = new PropertySimple("sample.name", "V2 Name");
config2.put( property21 );
var property22 = new PropertySimple("sample.port", "22222");
config2.put( property22 );
var property23 = new PropertySimple("sample.new", "V2.0 ONLY!");
config2.put( property23 );

// upgrade the deployment to 2.0 using the 2.0 config
var deployment = BundleManager.createBundleDeployment(bundleVersion2.getId(), dest.getId(), "Upgrading Sample Ant Bundle to V2", config2);
deployment = BundleManager.scheduleBundleDeployment(deployment.getId(), false);
Assert.assertNotNull( deployment, "Failed to upgrade to 2.0 deployment" );

print("\nBundle Upgrade Deployment Status=" + deployment.getStatus());

