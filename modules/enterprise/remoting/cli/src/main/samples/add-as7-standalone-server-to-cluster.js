/**
 * This script can be used to add a standalone vJBoss AS7 server into a cluster.
 *
 * All the functions with names starting with an underscore are considered
 * private.
 *
 * @author: Lukas Krejci
 */

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
function addToCluster(newAs7Resource, newNodeName, existingClusterMemberResource, doCopyDeployments) {  
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
function copyDeployments(sourceAS7, targetAS7) {
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