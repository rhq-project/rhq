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
 * This sample script shows how to revert the "live" deployment for a destination. The revert
 * will return to the previous "replaced" deployment.  So, this will undo the V2 deployment
 * performed in cli-3-upgradeBundle.js and return to the V1 deployment performed in
 * cli-2-deployBundle.js. The Bundle uses the Ant Bundle Type provided with RHQ.
 * <pre>
 * Prerequisites:
 *   1) run cli-1-createBundle.js, cli-2-deployBundle.js and cli-3-upgradeBundle.js   
 *   2) For successful deployment the RHQ Agents must be running on the "platforms" group platforms.      
 * 
 * Usage:
 *   Note, the CLI must be executed in its own bin directory, so have <path> below be the path to the
 *   sample-cli directory.
 *   
 *   1) start the CLI (can be downloaded from the GUI, Administration->Downloads, RHQ Client)
 *   2) login user password serverHost serverPort
 *   3) exec <path>/cli-4-revertBundle.js
 *   4) validate the revert deployment and that V1 is again the live deployment. 
 * </pre>
 */

var bundleName = 'sample-bundle';

//get the bundle if it exists
var bc = new BundleCriteria();
bc.addFilterName(bundleName);
bc.fetchDestinations( true );
var bundles = BundleManager.findBundlesByCriteria(bc);
Assert.assertTrue( null != bundles && bundles.size() > 0 );
var bundle = bundles.get(0);
Assert.assertTrue( null != bundle.getDestinations() && bundle.getDestinations().size() == 1 );
var dest = bundle.getDestinations().get(0);

//revert the live V2 deployment to the replaced V1 deployment
var deployment = BundleManager.scheduleRevertBundleDeployment(dest.getId(), "Reverting Sample Ant Bundle from V2 to V1", false);
Assert.assertNotNull( deployment, "Failed to revert 2.0 deployment" );

print("\nBundle Revert Status=" + deployment.getStatus());
