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
 * This sample script shows how to deploy a Bundle.  It deploys the bundle created
 * by the cli-1-createBundle.js script. The Bundle uses the Ant Bundle Type provided with
 * RHQ.
 * <pre>
 * 
 * The deployment is to /tmp/sample-bundle.
 * 
 * Prerequisites:
 *   1) run cli-1-createBundle.js   
 *   2) At least one platform must be in inventory. If the "platforms" group does not exist it will
 *      be created with all imported platforms as members. The sample bundle will be deployed to the
 *      "platforms" group.  
 *   3) For successful deployment the RHQ Agents must be running on the "platforms" group platforms.
 * 
 * Usage:
 *   Note, the CLI must be executed in its own bin directory, so have <path> below be the path to the
 *   sample-cli directory.
 * 
 *   1) start the CLI (can be downloaded from the GUI, Administration->Downloads, RHQ Client)
 *   2) login user password serverHost serverPort
 *   3) exec <path>/scripts/cli-2-deployBundle.js
 *   4) validate the deployment and that the config settings are applied correctly 
 * </pre>
 */

var bundleName = 'sample-bundle';

// get the bundle if it exists
var bc = new BundleCriteria();
bc.addFilterName(bundleName);
bc.fetchBundleVersions( true );
var bundles = BundleManager.findBundlesByCriteria(bc);
Assert.assertTrue( null != bundles && bundles.size() > 0 );
var bundle = bundles.get(0);
Assert.assertTrue( null != bundle.getBundleVersions() && bundle.getBundleVersions().size() == 1 );
var bundleVersion = bundle.getBundleVersions().get(0);


// Find or create the "platforms" group
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

// create a destination for the deployment
var dest = BundleManager.createBundleDestination(bundle.getId(), "sample destination", "sample destination", "/tmp/sample-bundle", groupId);

// create a config for the V1.0 deployment, setting the required properties for recipe in distro 1.0 
var config1 = new Configuration();
var property11 = new PropertySimple("sample.name", "V1 Name");
config1.put( property11 );
var property12 = new PropertySimple("sample.port", "11111");
config1.put( property12 );


// create a deployment for sample bundle 1.0 using the 1.0 config
var deployment = BundleManager.createBundleDeployment(bundleVersion.getId(), dest.getId(), "Deploying Sample Ant Bundle V1", config1);
deployment = BundleManager.scheduleBundleDeployment(deployment.getId(), true);
Assert.assertNotNull( deployment, "Failed to create 1.0 deployment" );

print("\nBundle Deployment Status=" + deployment.getStatus());

