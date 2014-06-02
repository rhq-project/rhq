/**
 * file: deploy-to-and-restart-JBAS.js
 *
 * description: this is an example script that defines a function for deploying
 * a new application to a group of JBoss AS 5.1.x servers using the bundle
 * subsystem and then restarting the servers serially.
 *
 * This script depends on a bundles.js and thus that script needs to be included
 * prior to including this script.
 *  $ login <username> <password>
 *  $ exec -f samples/util.js
 *  $ exec -f samples/bundles.js
 *  $ exec -f samples/deploy-to-and-restart-JBAS.js
 *
 * Note that you must login before you can load the scripts. Also please note
 * that if a function is not documented, then it is not intended for public use.
 * It is used only as an internal helper function.
 *
 * author: lkrejci@redhat.com
 */

/**
 * This creates a new destination in the JBoss AS (4,5,6,7) servers from the provided
 * group and deploys the application provided by the bundle to it. Once the app is deployed,
 * the servers in the group are sequentially stopped and started.
 *
 * @param bundleZipFile the path to the zip file with the application bundle
 * @param deploymentConfiguration the deployment configuration required by the bundle.
 *        See the <code>deployBundle</code> in <code>bundles.js</code> to find out about
 *        supported formats.
 * @param groupName the name of the compatible group of JBossAS servers to deploy the bundle to
 * @param destinationName the name of the destination that will be created so that the bundle
 *        can be deployed to it
 * @param destinationDescription the description of the destination
 * @param baseDirName the name of the base directory for the deployment. Each resource type
 *        defines a list of deployment base directories which you can review using, for example,
 *        the <code>getAllBaseDirectories(resourceTypeId)</code> function from <code>bundles.js</code>.
 * @param deployDir the name of the directory under the base dir in which the bundle will be
 *        deployed to.
 *
 * @return the BundleDeployment instance
 */
function createAppAndRestartJBAS(bundleZipFile, deploymentConfiguration, groupName, destinationName, destinationDescription, baseDirName, deployDir) {
    var gcrit = new ResourceGroupCriteria;
    gcrit.addFilterName(groupName);
    gcrit.fetchResourceType(true);

    var groups = ResourceGroupManager.findResourceGroupsByCriteria(gcrit);
    if (groups.empty) {
        throw "Could not find a resource group called " + groupName;
    } else if (groups.size() > 1) {
        throw "There are more than 1 groups called " + groupName;
    }

    var group = groups.get(0);
    var targetResourceType = group.resourceType;

    var deployFn = function(restartFn) {

        var bundleVersion = createBundleVersion(bundleZipFile);
        var destination = BundleManager.createBundleDestination(bundleVersion.bundle.id, destinationName, destinationDescription, baseDirName, deployDir, group.id);

        var deployment = deployBundle(destination, bundleVersion, deploymentConfiguration, null, false);

        if (deployment.status != BundleDeploymentStatus.SUCCESS) {
            throw "Deployment wasn't successful: " + deployment;
        }

        restartFn(group);

        return deployment;
    };

    return deployFn(_restartFunction(targetResourceType));
}

/**
 * Deploys an updated bundle of an enterprise application to a destination that
 * points to a group of JBoss AS(4,5,6,7) servers and then stops all the servers
 * serially and then starts them up again sequentially.
 *
 * @param bundleZipFile
 *            the path to the bundle zip file with the application
 * @param jbasDestination
 *            the bundle destination the bundle should be deployed to (or id
 *            thereof)
 * @param deploymentConfiguration
 *            the deployment configuration. This can be an ordinary javascript
 *            object (hash) or an instance of RHQ's Configuration. If it is the
 *            former, it is converted to the Configuration instance using the
 *            <code>asConfiguration</code> function from <code>util.js</code>.
 *            Please consult the documentation of that method to understand the
 *            limitations of that approach.
 * @param description
 *            the deployment description
 *
 * @return the deployment instance
 */
function updateAppAndRestartJBAS(bundleZipFile, jbasDestination, deploymentConfiguration) {
    // first figure out the jbas version we are deploying to
    var destinationId = jbasDestination;
    if (typeof(jbasDestination) == 'object') {
        destinationId = jbasDestination.id;
    }

    var destCrit = new BundleDestinationCriteria
    destCrit.fetchGroup(true)
    destCrit.addFilterId(destinationId)

    var destinations = BundleManager.findBundleDestinationsByCriteria(destCrit);

    if (destinations.empty) {
        throw "No destinations corresponding to " + jbasDestination + " found on the server.";
    }

    var destination = destinations.get(0);

    var targetResourceType = destination.group.resourceType;

    if (targetResourceType == null) {
        throw "This function expects a compatible group of JBoss AS (4,5,6 or 7) resources but the provided destination is connected with " + destination.group;
    }

    var deployFn = function(restartFn) {
        var bundleVersion = createBundleVersion(bundleZipFile);
        var deployment = deployBundle(destination, bundleVersion, deploymentConfiguration, null, false);

        if (deployment.status != BundleDeploymentStatus.SUCCESS) {
            throw "Deployment wasn't successful: " + deployment;
        }

        restartFn(destination.group);

        return deployment;
    };

    return deployFn(_restartFunction(targetResourceType));
}

/** Helper method to return a correct restart function for given resource type of JBoss AS */
function _restartFunction(asResourceType) {
    if (asResourceType.plugin == "JBossAS" && asResourceType.name == "JBossAS Server") {
        return _restartAS4;
    } else if (asResourceType.plugin == "JBossAS5" && asResourceType.name == "JBossAS Server") {
        return _restartAS5;
    } else if (asResourceType.plugin == "JBossAS7" &&
        (asResourceType.name == "JBossAS7 Standalone Server" ||
         asResourceType.name == "Managed Server")) {
        
    	return _restartAS7;
    } else {
    	throw "The resource group the destination targets doesn't seem to be a JBoss AS server group.";
    }
}

function _loadGroupMembers(group) {
    var crit = new ResourceGroupCriteria;
    crit.addFilterId(group.id);
    crit.fetchExplicitResources(true);
    crit.fetchImplicitResources(true);

    var groups = ResourceGroupManager.findResourceGroupsByCriteria(crit)

    if (groups.empty) {
        throw "Could not find a previously loaded group on the server: " + group;
    }

    return groups.get(0);
}

function _allResourceIds(group) {
    var list = [];

    if (group.explicitResources != null) {
        foreach(group.explicitResources, function(resource) {
            list.push(resource.id);
        });
    }

    return list;
}

function _runOperationSequentially(group, operationName, operationConfig) {
    group = _loadGroupMembers(group);
    var resourceIds = _allResourceIds(group);

    var conf = operationConfig;
    if (!(conf instanceof Configuration)) {
        conf = asConfiguration(conf);
    }

    // by specifying the resource ids explicitly, we define the order in which
    // the operations will be executed on individual resources (in sequence).
    // if the resourceIds were null, the operations would be invoked all at once
    // in no particular order.
    var schedule = OperationManager.scheduleGroupOperation(group.id, resourceIds, false, operationName, conf, 0, 0, 0, 0, null);

    // now we need to wait for all the individual operations on resources to
    // complete
    var crit = new GroupOperationHistoryCriteria;
    crit.addFilterJobId(new JobId(schedule.jobName, schedule.jobGroup));

    var counter = 0;
    while (counter++ < 30) { //30 attempts
        // XXX should this be configurable?
        // we're waiting for an AS to start/shutdown which takes time..
        // Let's check in 2s intervals 30 times at most.
        // We're waiting at the start of this loop to also ensure that the server
        // had time to issue the quartz job and persist the history entry.
        java.lang.Thread.currentThread().sleep(2000);

        var results = OperationManager.findGroupOperationHistoriesByCriteria(crit);

        if (!results.empty) {
            var history = results.get(0);

            if (history.status != OperationRequestStatus.INPROGRESS) {
                return history;
            }
        }
    }

    if (results.empty) {
        throw "Could not find operation history for schedule " + schedule + ". This should not happen.";
    }
}

function _restartAS4(group) {
    _runOperationSequentially(group, "shutdown", new Configuration);
    _runOperationSequentially(group, "start", new Configuration);
}

function _restartAS5(group) {
    _runOperationSequentially(group, "shutdown", new Configuration);
    _runOperationSequentially(group, "start", new Configuration);
}

function _restartAS7(group) {
    var shutdownOp = group.resourceType.name == "JBossAS7 Standalone Server" ? "shutdown" : "stop";
    var startOp = "start";

    _runOperationSequentially(group, shutdownOp, new Configuration);
    _runOperationSequentially(group, startOp, new Configuration);
}
