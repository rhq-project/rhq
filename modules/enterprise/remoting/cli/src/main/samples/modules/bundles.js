/**
 * file: bundles.js
 * 
 * description: this file contains a few helper functions to make working 
 * with the bundle subsystem more convenient.
 *
 * Note that you must login before you can load the scripts.
 *
 * author: lkrejci@redhat.com
 */

var util = require('./util');

/**
 * A simple function to create a new bundle version from a zip file containing
 * the bundle.
 * 
 * @param pathToBundleZipFile the path to the bundle on the local file system
 * 
 * @return an instance of BundleVersion class describing what's been created on 
 * the RHQ server.
 */
exports.createBundleVersion = function(pathToBundleZipFile) {
    var contentHandle = scriptUtil.uploadContent(pathToBundleZipFile);
    return BundleManager.createBundleVersionViaContentHandle(contentHandle);
}

/**
 * This is a helper function that one can use to find out what base directories
 * given resource type defines.
 * <p>
 * These base directories then can be used when specifying bundle destinations.
 * 
 * @param resourceTypeId
 * @returns a java.util.Set of ResourceTypeBundleConfiguration objects
 */
exports.getAllBaseDirectories = function(resourceTypeId) {
	var crit = new ResourceTypeCriteria;
	crit.addFilterId(resourceTypeId);
	crit.fetchBundleConfiguration(true);
	
	var types = ResourceTypeManager.findResourceTypesByCriteria(crit);
	
	if (types.size() == 0) {
		throw "Could not find a resource type with id " + resourceTypeId;
	} else if (types.size() > 1) {
		throw "More than one resource type found with id " + resourceTypeId + "! How did that happen!";
	}
	
	var type = types.get(0);
	
	return type.getResourceTypeBundleConfiguration().getBundleDestinationBaseDirectories();
}

/**
 * Creates a new destination for given bundle. Once a destination exists,
 * actual bundle versions can be deployed to it.
 * <p>
 * Note that this only differs from the <code>BundleManager.createBundleDestination</code>
 * method in the fact that one can provide bundle and resource group names instead of their
 * ids.
 * 
 * @param destinationName the name of the destination to be created
 * @param description the description for the destination
 * @param bundleName the name of the bundle to create the destination for
 * @param groupName name of a group of resources that the destination will handle
 * @param baseDirName the name of the basedir definition that represents where inside the 
 *                    deployment of the individual resources the bundle will get deployed
 * @param deployDir the specific sub directory of the base dir where the bundles will get deployed
 * 
 * @return BundleDestination object
 */
exports.createBundleDestination = function(destinationName, description, bundleName, groupName, baseDirName, deployDir) {
	var groupCrit = new ResourceGroupCriteria;
	groupCrit.addFilterName(groupName);
	var groups = ResourceGroupManager.findResourceGroupsByCriteria(groupCrit);
	
	if (groups.empty) {
		throw "No group called '" + groupName + "' found.";
	}
	
	var group = groups.get(0);
	
	var bundleCrit = new BundleCriteria;
	bundleCrit.addFilterName(bundleName);
	var bundles = BundleManager.findBundlesByCriteria(bundleCrit);
	
	if (bundles.empty) {
		throw "No bundle called '" + bundleName + "' found.";
	}
	
	var bundle = bundles.get(0);
	
	return BundleManager.createBundleDestination(bundle.id, destinationName, description, baseDirName, deployDir, group.id);
}

/**
 * Tries to deploy given bundle version to provided destination using given configuration.
 * <p>
 * This method blocks while waiting for the deployment to complete or fail.
 * 
 * @param destination the bundle destination (or id thereof)
 * @param bundleVersion the bundle version to deploy (or id thereof)
 * @param deploymentConfiguration the deployment configuration. This can be an ordinary
 * javascript object (hash) or an instance of RHQ's Configuration. If it is the former,
 * it is converted to a Configuration instance using the <code>asConfiguration</code>
 * function from <code>util.js</code>. Please consult the documentation of that method
 * to understand the limitations of that approach.
 * @param description the deployment description
 * @param isCleanDeployment if true, perform a wipe of the deploy directory prior to the deployment; if false,
 * perform as an upgrade to the existing deployment, if any
 * 
 * @return the BundleDeployment instance describing the deployment
 */
exports.deployBundle = function(destination, bundleVersion, deploymentConfiguration, description, isCleanDeployment) {
	var destinationId = destination;
	if (typeof(destination) == 'object') {
		destinationId = destination.id;
	}
	
	var bundleVersionId = bundleVersion;
	if (typeof(bundleVersion) == 'object') {
		bundleVersionId = bundleVersion.id;
	}
	
	var deploymentConfig = deploymentConfiguration;
	if (!(deploymentConfiguration instanceof Configuration)) {
		deploymentConfig = util.asConfiguration(deploymentConfiguration);
	}
	
	var deployment = BundleManager.createBundleDeployment(bundleVersionId, destinationId, description, deploymentConfig);
	
	deployment = BundleManager.scheduleBundleDeployment(deployment.id, isCleanDeployment);
	
	var crit = new BundleDeploymentCriteria;
	crit.addFilterId(deployment.id);
	
	while (deployment.status == BundleDeploymentStatus.PENDING || deployment.status == BundleDeploymentStatus.IN_PROGRESS) {
		java.lang.Thread.currentThread().sleep(1000);
		var dps = BundleManager.findBundleDeploymentsByCriteria(crit);
		if (dps.empty) {
			throw "The deployment disappeared while we were waiting for it to complete.";
		}
		
		deployment = dps.get(0);
	}
	
	return deployment;
}

