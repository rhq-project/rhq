/**
 * This module contains a variety of utility functions for working with JBoss AS in different versions.
 * 
 * @author Lukas Krejci
 */


var util = require('./util');
var bundles = require("./bundles");

//init the version-specific objects. We'll be adding the functions to them further down below.
exports.as7 = {}

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
exports.createAppAndRestart = function(bundleZipFile, deploymentConfiguration, groupName, destinationName, destinationDescription, baseDirName, deployDir) {
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

        var bundleVersion = bundles.createBundleVersion(bundleZipFile);
        var destination = BundleManager.createBundleDestination(bundleVersion.bundle.id, destinationName, destinationDescription, baseDirName, deployDir, group.id);

        var deployment = bundles.deployBundle(destination, bundleVersion, deploymentConfiguration, null, false);

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
exports.updateAppAndRestart = function(bundleZipFile, jbasDestination, deploymentConfiguration) {
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
        var bundleVersion = bundles.createBundleVersion(bundleZipFile);
        var deployment = bundles.deployBundle(destination, bundleVersion, deploymentConfiguration, null, false);

        if (deployment.status != BundleDeploymentStatus.SUCCESS) {
            throw "Deployment wasn't successful: " + deployment;
        }

        restartFn(destination.group);

        return deployment;
    };

    return deployFn(_restartFunction(targetResourceType));
}

/**
 * This function adds an AS7 server to a cluster with another AS7 instance, using
 * the provided node name as its identifier.
 * <p>
 * This method only makes the new member use the same configuration as the existing
 * member and synchronizes its jgroups, messaging and modcluster socket bindings
 * with the cluster. Optionally it also copies over all the deployments to the
 * new member.
 * <p>
 * The configurations are therefore assumed to be otherwise compatible - i.e.
 * the infinispan cache containers should be configured correctly (or left at
 * default values, which are configured correctly), etc.
 *
 * @param newAs7Resource the resource proxy of the AS7 instance to add
 * @param newNodeName the node name, i.e. unique identification, of the new member
 *        in the cluster
 * @param existingClusterMemberResource an AS7 instance that belongs to a cluster
 *        the new member should join into.
 * @param copyDeployments whether or not to copy the deployments from the existing
 *        member to the new member
 */
exports.as7.addToCluster = function(newAs7Resource, newNodeName, existingClusterMemberResource, doCopyDeployments) {  
    println("Reading config of the existing cluster member");
    var existingMember = {
        'id' : existingClusterMemberResource.id,
	'resourceConfiguration' : _getLiveResourceConfiguration(existingClusterMemberResource),
	'pluginConfiguration' : _getPluginConfiguration(existingClusterMemberResource)
    };
    
    var clusterConfig = _getClusterSignificantConfig(existingClusterMemberResource.children,
        existingMember.pluginConfiguration, existingMember.resourceConfiguration);

    println("Reading config of the new member");
    var newMember = {
        'id' : newAs7Resource.id,
	'resourceConfiguration' : _getLiveResourceConfiguration(newAs7Resource),
	'pluginConfiguration' : _getPluginConfiguration(newAs7Resource)
    };

    var memberConfig = _getClusterSignificantConfig(newAs7Resource.children, newMember.pluginConfiguration, newMember.resourceConfiguration);

    var memberResourceConfiguration = newMember.resourceConfiguration.deepCopy(false);

    if (memberConfig['config'] != clusterConfig['config']) {
        println("The configurations of the servers differ.\n" +
            "The new cluster member's configuration will be changed to match the configuration of the existing member.");

        newAs7Resource.updatePluginConfiguration(_changeConfig(newMember.pluginConfiguration, clusterConfig['config']));

        //we need to restart straight away so that we see the changes to the
        //rest of the configuration caused by the change of current config.
        println("Restarting the new cluster member to switch it to the new configuration.");
        newAs7Resource.restart();

        //refresh the resource
        newAs7Resource = ProxyFactory.getResource(newMember.id);
        newMember = {
            'id' : newAs7Resource.id,
	    'resourceConfiguration' : _getLiveResourceConfiguration(newAs7Resource),
	    'pluginConfiguration' : _getPluginConfiguration(newAs7Resource)
        };
	
        //refresh the cluster specific config after the restart with the new
        //config
        memberConfig = _getClusterSignificantConfig(newAs7Resource.children, newMember.pluginConfiguration, newMember.resourceConfiguration);
        memberResourceConfiguration = newMember.resourceConfiguration;
    }

    //now check what's the node name we see
    if (memberConfig['node-name'] != newNodeName) {
        println("Updating the node name of the new cluster member from '" + memberConfig['node-name'] + "' to '" + newNodeName + "'");
        _updateNodeName(memberResourceConfiguration, newNodeName);

        newAs7Resource.updateResourceConfiguration(memberResourceConfiguration);
        memberResourceConfiguration = newMember.resourceConfiguration.deepCopy(false);
    }

    //now apply the socket binding changes for jgroups and other cluster
    //significant subsystems
    //first find the socket binding group config in the new member
    for(i in newAs7Resource.children) {
        var child = newAs7Resource.children[i];
        if (child.resourceType.name == 'SocketBindingGroup' &&
                child.resourceType.plugin == 'JBossAS7') {

            println("Updating socket bindings of jgroups, messaging and modcluster subsystems");

	    var resourceConfig = _getLiveResourceConfiguration(child);
	
            var portOffset = javascriptString(resourceConfig.getSimpleValue('port-offset', '0'));
            var clusterMemberPortOffset = clusterConfig['port-offset'];

            var newConfig = resourceConfig.deepCopy(false);

            _updateSocketBindings(newConfig, portOffset, clusterMemberPortOffset, clusterConfig['jgroups']);
            _updateSocketBindings(newConfig, portOffset, clusterMemberPortOffset, clusterConfig['messaging']);
            _updateSocketBindings(newConfig, portOffset, clusterMemberPortOffset, clusterConfig['modcluster']);

            child.updateResourceConfiguration(newConfig);
        }
    }

    println("Restarting the new member for the new socket bindings to take effect.");
    newAs7Resource.restart();

    if (doCopyDeployments) {
        println("Copying the deployments to the new cluster member...");
        copyDeployments(existingClusterMemberResource, newAs7Resource);

        println("Restarting the new cluster member.");
        newAs7Resource.restart();
    }
}

/**
 * Copies all deployments from the source to the target. Both resources must
 * be standalone AS7 servers.
 *
 * @param sourceAS7 the resource id or object of the source AS7 standalone instance
 * @param targetAS7 the resource id or object of the target AS7 standalone instance
 */
exports.as7.copyDeployments = function(sourceAS7, targetAS7) {
    if (typeof sourceAS7 == 'object') {
        sourceAS7 = sourceAS7.id;
    }

    if (typeof targetAS7 == 'object') {
        targetAS7 = targetAS7.id;
    }

    var deploymentResourceType = ResourceTypeManager.getResourceTypeByNameAndPlugin('Deployment', 'JBossAS7');

    var deploymentsCrit = new ResourceCriteria;
    deploymentsCrit.addFilterParentResourceId(sourceAS7);
    deploymentsCrit.addFilterResourceTypeId(deploymentResourceType.id);

    var unlimitedPageControl = PageControl.unlimitedInstance;

    var sourceDeployments = ResourceManager.findResourcesByCriteria(deploymentsCrit);
    var iterator = sourceDeployments.iterator();
    while (iterator.hasNext()) {
        var deploymentResource = iterator.next();
        //get a resource proxy for easy access to configurations, etc.
        deploymentResource = ProxyFactory.getResource(deploymentResource.id);

        println("Copying deployment " + deploymentResource.name);

        var installedPackage = ContentManager.getBackingPackageForResource(deploymentResource.id);
        var content = ContentManager.getPackageBytes(deploymentResource.id, installedPackage.id);

        var runtimeName = deploymentResource.resourceConfiguration.getSimpleValue('runtime-name', deploymentResource.name);

        var deploymentConfiguration = new Configuration;
        deploymentConfiguration.put(new PropertySimple('runtimeName', runtimeName));

        //so now we have both metadata and the data of the deployment, let's
        //push a copy of it to the target server
        var history = ResourceFactoryManager.createPackageBackedResource(targetAS7,
            deploymentResourceType.id, deploymentResource.name,
            deploymentResource.pluginConfiguration,
            installedPackage.packageVersion.generalPackage.name,
            installedPackage.packageVersion.version,
            installedPackage.packageVersion.architecture.id,
            deploymentConfiguration, content, null);

        while (history.status.name() == 'IN_PROGRESS') {
            java.lang.Thread.sleep(1000);
            //the API for checking the create histories is kinda weird..
            var histories = ResourceFactoryManager.findCreateChildResourceHistory(targetAS7, null, null, unlimitedPageControl);
            var hit = histories.iterator();
            var found = false;
            while(hit.hasNext()) {
                var h = hit.next();

                if (h.id == history.id) {
                    history = h;
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw "The history object for the deployment seems to have disappeared, this is very strange.";
            }
        }

        println("Deployment finished with status: " + history.status.toString() +
            (history.status.name() == 'SUCCESS' ? "." : (", error message: " + history.errorMessage + ".")));
    }
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

function _getConfigName(as7PluginConfig) {
    var argsValue = as7PluginConfig.getSimpleValue('startScriptArgs', '');

    var args = argsValue.split('\n');

    var possibleParams = ['-c ', '-c=', '--server-config='];
    
    var ret = null;
    for (i in args) {
        var arg = args[i];

	var found = false;
	
	for(j in possibleParams) {
	    var param = possibleParams[j];
	    var pos = arg.indexOf(param);

	    if (pos >= 0) {
	        ret = arg.substring(pos + param.length).trim();
		found = true;
		break;
	    }
	}

	if (found) {
	    break;
	}
    }

    if (ret != null && ret.startsWith('%') && ret.endsWith('%')) {
        var pluginConfRef = ret.substring(1, ret.length - 1);
        ret = as7PluginConfig.getSimpleValue(pluginConfigRef, null);
    }

    return ret;
}

function _changeConfig(pluginConfig, configName) {
    var argsValue = javascriptString(pluginConfig.getSimpleValue('startScriptArgs', ''));

    var args = argsValue.split('\n');

    var possibleParams = ['-c ', '-c=', '--server-config='];
    
    var updated = false;

    for (i in args) {
        var arg = args[i];

	for(j in possibleParams) {
	    var param = possibleParams[j];

	    var pos = arg.indexOf(param);

	    if (pos >= 0) {
	        args[i] = param + configName;
		updated = true;
		break;
	    }
	}

        if (updated) {
            break;
        }
    }

    if (!updated) {
        args.push("-c " + configName);
    }

    //join the config back together
    for(i in args) {
        if (i == 0) {
            argsValue = args[i];
        } else {
            argsValue += "\n" + args[i];
        }
    }

    pluginConfig.put(new PropertySimple('startScriptArgs', argsValue));

    return pluginConfig;
}

/**
 * The config properties of AS7 can have a form of ${propertyName:defaultvalue}.
 * Because we can't figure out the value of the property if it is defined,
 * (or get at its default value), we return the following hash from this function
 * {
 *    property : propertyName,
 *    value    : defaultvalue
 * }
 *
 * At least one of the keys in the above hash will have a value.
 */
function _parseValueFromExpr(expr) {
    var strExpr = expr + '';
    if (strExpr.indexOf('${') == 0) {
        var colonIdx = strExpr.indexOf(':');
        if (colonIdx == -1) {
            return { 'property' : expr };
        } else {
            var property = strExpr.substring(2, colonIdx);
            var value = strExpr.substring(colonIdx + 1, strExpr.length - 1);

            return { 'property' : property, 'value' : value };
        }
    } else {
        return { 'value' : expr };
    }
}

/**
 * This converts the result of _parseValueFromExpr() function back into the
 * ${property:defaultvalue} format.
 */
function _composeValueExpr(parsedValue) {
    if (parsedValue.property == undefined) {
        return parsedValue.value;
    } else {
        return '${' + parsedValue.property + ':' + parsedValue.value + '}';
    }
}

function _updateNodeName(serverResourceConfiguration, newNodeName) {
    //define it as a system prop
    var systemProps = serverResourceConfiguration.getList('*2');
    if (systemProps == null) {
        systemProps = new PropertyList('*2');
        serverResourceConfiguration.put(systemProps);
    }

    var it = systemProps.list.iterator();
    var updated = false;
    while (it.hasNext()) {
        var systemProp = it.next();
        var systemPropName = javascriptString(systemProp.getSimpleValue('name', null));
        if (systemPropName == 'jboss.node.name') {
            var systemPropValue = systemProp.getSimple('value');
            if (systemPropValue == null) {
                systemPropValue = new PropertySimple('value', newNodeName);
                systemProp.put(systemPropValue);
            } else {
                systemPropValue.setValue(newNodeName);
            }

            updated = true;
            break;
        }
    }

    if (!updated) {
        var systemProp = new PropertyMap('*:name');
        systemProp.put(new PropertySimple('name', 'jboss.node.name'));
        systemProp.put(new PropertySimple('value', newNodeName));
        systemProps.add(systemProp);
    }
}

function _updateSocketBindings(socketBindingsConfig, portOffset, clusterMemberPortOffset, socketBindingConfig) {
    portOffset = _parseValueFromExpr(portOffset);
    clusterMemberPortOffset = _parseValueFromExpr(clusterMemberPortOffset);

    var portOffsetValue = portOffset.value != undefined ? portOffset.value : 0;
    var clusterMemberPortOffsetValue = clusterMemberPortOffset.value != undefined ?
    clusterMemberPortOffset.value : 0;

    var portOffsetDiff = clusterMemberPortOffsetValue - portOffsetValue;

    var ports = socketBindingsConfig.get('*');
    var portIterator = ports.list.iterator();

    var appliedProperties = [];

    while (portIterator.hasNext()) {
        var port = portIterator.next();

        var name = port.getSimpleValue('name', null);

        if (socketBindingConfig[name] != undefined) {
            //k, this is a significant config, port it over

            //compute the value of the ports
            if (socketBindingConfig[name]['fixed-port'] == 'true') {
                port.put(new PropertySimple('port:expr', socketBindingConfig[name]['port:expr']));
                port.put(new PropertySimple('multicast-port:expr', socketBindingConfig[name]['multicast-port:expr']));
                port.put(new PropertySimple('multicast-address', socketBindingConfig[name]['multicast-address']));
                port.put(new PropertySimple('fixed-port', 'true'));
            } else {
                var portExprToUse = _parseValueFromExpr(socketBindingConfig[name]['port:expr']);
                if (portExprToUse.value != undefined) {
                    portExprToUse.value = portOffsetDiff + parseInt(portExprToUse.value) + '';
                }

                var multicastPortExprToUse = _parseValueFromExpr(socketBindingConfig[name]['multicast-port:expr']);
                if (multicastPortExprToUse.value != undefined) {
                    multicastPortExprToUse.value = portOffsetDiff + parseInt(multicastPortExprToUse.value) + '';
                }

                port.put(new PropertySimple('port:expr', _composeValueExpr(portExprToUse)));
                port.put(new PropertySimple('multicast-port:expr', _composeValueExpr(multicastPortExprToUse)));
                port.put(new PropertySimple('multicast-address', socketBindingConfig[name]['multicast-address']));
                port.put(new PropertySimple('fixed-port', 'false'));
            }
            appliedProperties.push(name);
        }
    }

    //I need contains() and Rhino's Array object apparently doesn't have indexOf()
    //which I could otherwise use.
    appliedProperties = java.util.Arrays.asList(appliedProperties);

    //now add to the list the props that were not there
    for(name in socketBindingConfig) {
        if (appliedProperties.contains(name)) {
            continue;
        }

        var port = new PropertyMap('binding');
        ports.add(port);

        port.put(new PropertySimple('name', name));
        port.put(new PropertySimple('multicast-address', socketBindingConfig[name]['multicast-address']));
        port.put(new PropertySimple('fixed-port', socketBindingConfig[name]['fixed-port']));

        var portExprToUse = _parseValueFromExpr(socketBindingConfig[name]['port:expr']);
        if (portExprToUse.value != undefined && !socketBindingConfig[name]['fixed-port']) {
            portExprToUse.value = portOffsetDiff + parseInt(portExprToUse.value) + '';
        }

        var multicastPortExprToUse = _parseValueFromExpr(socketBindingConfig[name]['multicast-port:expr']);
        if (multicastPortExprToUse.value != undefined && !socketBindingConfig[name]['fixed-port']) {
            multicastPortExprToUse.value = portOffsetDiff + parseInt(multicastPortExprToUse.value) + '';
        }

        port.put(new PropertySimple('port:expr', _composeValueExpr(portExprToUse)));
        port.put(new PropertySimple('multicast-port:expr', _composeValueExpr(multicastPortExprToUse)));
    }
}


/**
 * Returns a javascript hash of configuration properties significant for the
 * cluster configuration.
 *
 * This method is quite simplistic - it merely reads out the important socket
 * bindings and Infinispan configuration properties.
 *
 * It doesn't try to be smart about specifying which concrete caches and cache
 * containers are used for individual subsystems like EJB3, JPA or web apps.
 *
 * @param resource the resource proxy of the AS7
 */
function _getClusterSignificantConfig(children, pluginConfiguration, resourceConfiguration) {
    var ret = {};

    ret['config'] = javascriptString(_getConfigName(pluginConfiguration));

    ret['node-name'] = javascriptString(resourceConfiguration.getSimpleValue('node-name', null));

    //the standalone server has a single socket binding group
    for(var i in children) {
        var child = children[i];
        if (child.resourceType.plugin != 'JBossAS7') {
            continue;
        }
        if (child.resourceType.name == 'SocketBindingGroup') {

	    var resourceConfig = _getLiveResourceConfiguration(child);
	    
            ret['port-offset'] = javascriptString(resourceConfig.getSimpleValue('port-offset', '0'));
            var ports = resourceConfig.get('*');
            var portIterator = ports.list.iterator();

            var jgroups = {};
            var messaging = {};
            var modcluster = {};

            ret['jgroups'] = jgroups;
            ret['messaging'] = messaging;
            ret['modcluster'] = modcluster;

            while (portIterator.hasNext()) {
                var port = portIterator.next();
                var name = javascriptString(port.getSimpleValue('name', null));

                if (name.indexOf('jgroups') == 0) {
                    jgroups[name] = _getSocketBinding(port);
                } else if (name.indexOf('messaging') == 0) {
                    messaging[name] = _getSocketBinding(port);
                } else if (name.indexOf('modcluster') == 0) {
                    modcluster[name] = _getSocketBinding(port);
                }
            }
        } else if (child.resourceType.name == 'Infinispan') {
            var ispn = {};
            ret['infinispan'] = ispn;

            //This has disappeared
            //ispn['default-cache-container'] = javascriptString(child.defaultCacheContainer.value);
            var cacheContainers = {};
            ispn['cache-containers'] = cacheContainers;

            for(cc in child.children) {
                var cacheContainer = child.children[cc];

                var caches = {};

		var containerConfig = _getLiveResourceConfiguration(cacheContainer);
		
                cacheContainers[cacheContainer.name] = {
                    'default-cache' : javascriptString(containerConfig.getSimpleValue('default-cache', null)),
                    'aliases' : _asArray(containerConfig.get('aliases')),
                    'caches' : caches
                };

                for (c in cacheContainer.children) {
                    var cache = cacheContainer.children[c];

                    caches[cache.name] = {
                        '_flavor' : javascriptString(containerConfig.getSimpleValue('_flavor', null)),
                        'batching' : javascriptString(containerConfig.getSimpleValue('batching', null)),
                        'indexing' : javascriptString(containerConfig.getSimpleValue('indexing', null)),
                        'mode' : javascriptString(containerConfig.getSimpleValue('mode', null))
                    };
                }
            }
        }
    }

    return ret;
}

function _getSocketBinding(port) {
    return {
        'port:expr' : javascriptString(port.getSimpleValue('port:expr', '0')),
        'multicast-port:expr' : javascriptString(port.getSimpleValue('multicast-port:expr', null)),
        'multicast-address' : javascriptString(port.getSimpleValue('multicast-address', null)),
        'fixed-port' : javascriptString(port.getSimpleValue('fixed-port', null))
    };
}

function javascriptString(string) {
    return string == null || string == undefined ? string : string + '';
}

function _asArray(propertyList) {
    var ret = [];
    if (propertyList == undefined || propertyList == null) {
        return ret;
    }

    var it = propertyList.list.iterator();
    while (it.hasNext()) {
        var prop = it.next();

        if (prop instanceof PropertySimple) {
            ret.push(javascriptString(prop.stringValue));
        }
    }

    return ret;
}

function _getPluginConfiguration(resource) {
  if (typeof(resource) == 'number') {
      resource = ProxyFactory.getResource(resource)
  } else {
      resource = ProxyFactory.getResource(resource.id)
  }
  return resource.pluginConfiguration
}

function _getLiveResourceConfiguration(resource) {
    var id;
    if (typeof(resource) == 'number') {
        id = resource;  
    } else {
        id = resource.id
    }

    return ConfigurationManager.getLiveResourceConfiguration(id, false) 
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
        util.foreach(group.explicitResources, function(resource) {
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
