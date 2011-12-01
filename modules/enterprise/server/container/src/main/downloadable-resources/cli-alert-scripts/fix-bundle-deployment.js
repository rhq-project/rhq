/**
 * Using this script one can automatically reset a bundle deployment to a specified version.
 * 
 * This script consists of several utility methods originally included in standalone files
 * in the samples of the CLI distribution, which have been included in this file so that it
 * is usable on its own even in the server environment.
 * 
 * The configuration of the bundle and its version to reset to is located at the top of this file. 
 * Please provide the values to the variables as instructed to make this script work in 
 * a concrete situation.
 */

//----------------------------- script configuration

/**
 * Replace the "null" with the ID of the bundle destination you want to reset.
 * 
 * The ID of the destination can be determined from the URL. If you navigate to 
 * some destination of a bundle, the destination ID is the number at the very
 * end of the URL in the address bar of your browser. 
 * 
 * It can also be determined programmatically using the remote API of the RHQ 
 * server but that is beyond the scope of this comment. 
 */
var destinationId = null; 

/**
 * Replace the "null" with the ID of the bundle version the deployment should be reset to.
 * 
 * As with the destinations, the ID of a bundle version can be determined in the GUI by
 * examining the URL. If you navigate to some version of a bundle, the bundle version ID is
 * the very last number in the URL in the address bar of the browser. 
 */
var bundleVersionId = null;

/**
 * A deployment of the bundle version may require configuration. Normally, this can be
 * provided as a simple javascript object (using the curly braces notation). Please see
 * the documentation of the <code>asConfiguration</code> function further down to understand
 * the limitations of this approach. Should the simple format using the javascript object
 * be not possible, you have to build up a full org.rhq.core.domain.configuration.Configuration
 * object.
 */
var deploymentConfig = null;

/**
 * The description of the deployment that will be performed.
 */
var description = "redeploy due to drift";

// DO NOT TOUCH THIS UNLESS YOU FULLY UNDERSTAND WHAT YOU ARE DOING.
// NOTE: It's essential that isCleanDeployment=true, otherwise files that have drifted will not be replaced with their
//       original versions from the bundle.
var isCleanDeployment = true;

//-------------------- util.js 

/**
 * If obj is a JS array or a java.util.Collection, each element is passed to
 * the callback function. If obj is a java.util.Map, each map entry is passed
 * to the callback function as a key/value pair. If obj is none of the
 * aforementioned types, it is treated as a generic object and each of its
 * properties is passed to the callback function as a name/value pair.
 */
function foreach(obj, fn) {
  if (obj instanceof Array) {
    for (i in obj) {
      fn(obj[i]);
    }
  }
  else if (obj instanceof java.util.Collection) {
    var iterator = obj.iterator();
    while (iterator.hasNext()) {
      fn(iterator.next());
    }
  }
  else if (obj instanceof java.util.Map) {
    var iterator = obj.entrySet().iterator()
    while (iterator.hasNext()) {
      var entry = iterator.next();
      fn(entry.key, entry.value);
    }
  }
  else {   // assume we have a generic object
    for (i in obj) {
      fn(i, obj[i]);
    }
  }
}

/**
 * Iterates over obj similar to foreach. fn should be a predicate that evaluates
 * to true or false. The first match that is found is returned.
 */
function find(obj, fn) {
  if (obj instanceof Array) {
    for (i in obj) {
      if (fn(obj[i])) {
        return obj[i]
      }
    }
  }
  else if (obj instanceof java.util.Collection) {
    var iterator = obj.iterator();
    while (iterator.hasNext()) {
      var next = iterator.next();
      if (fn(next)) {
        return next;
      }
    }
  }
  else if (obj instanceof java.util.Map) {
    var iterator = obj.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      if (fn(entry.key, entry.value)) {
        return {key: entry.key, value: entry.value};
      }
    }
  }
  else {
    for (i in obj) {
      if (fn(i, obj[i])) {
        return {key: i, value: obj[i]};
      }
    }
  }
  return null;
}

/**
 * Iterates over obj similar to foreach. fn should be a predicate that evaluates
 * to true or false. All of the matches are returned in a java.util.List.
 */
function findAll(obj, fn) {
  var matches = java.util.ArrayList();
  if ((obj instanceof Array) || (obj instanceof java.util.Collection)) {
    foreach(obj, function(element) {
      if (fn(element)) {
        matches.add(element);
      }
    });
  }
  else {
    foreach(obj, function(key, value) {
      if (fn(theKey, theValue)) {
        matches.add({key: theKey, value: theValue});
      }
    });
  }
  return matches;
}

/**
 * A convenience function to convert javascript hashes into RHQ's configuration
 * objects.
 * <p>
 * The conversion of individual keys in the hash follows these rules:
 * <ol>
 * <li> if a value of a key is a javascript array, it is interpreted as PropertyList
 * <li> if a value is a hash, it is interpreted as a PropertyMap
 * <li> otherwise it is interpreted as a PropertySimple
 * <li> a null or undefined value is ignored
 * </ol>
 * <p>
 * Note that the conversion isn't perfect, because the hash does not contain enough
 * information to restore the names of the list members.
 * <p>
 * Example: <br/>
 * <pre><code>
 * {
 *   simple : "value",
 *   list : [ "value1", "value2"],
 *   listOfMaps : [ { k1 : "value", k2 : "value" }, { k1 : "value2", k2 : "value2" } ]
 * }
 * </code></pre>
 * gets converted to a configuration object:
 * Configuration:
 * <ul>
 * <li> PropertySimple(name = "simple", value = "value")
 * <li> PropertyList(name = "list")
 *      <ol>
 *      <li>PropertySimple(name = "list", value = "value1")
 *      <li>PropertySimple(name = "list", value = "value2")
 *      </ol>
 * <li> PropertyList(name = "listOfMaps")
 *      <ol>
 *      <li> PropertyMap(name = "listOfMaps")
 *           <ul>
 *           <li>PropertySimple(name = "k1", value = "value")
 *           <li>PropertySimple(name = "k2", value = "value")
 *           </ul>
 *      <li> PropertyMap(name = "listOfMaps")
 *           <ul>
 *           <li>PropertySimple(name = "k1", value = "value2")
 *           <li>PropertySimple(name = "k2", value = "value2")
 *           </ul>
 *      </ol>
 * </ul>
 * Notice that the members of the list have the same name as the list itself
 * which generally is not the case.
 */
function asConfiguration(hash) {

	config = new Configuration;

	for(key in hash) {
		value = hash[key];

		if (value == null) {
			continue;
		}

		(function(parent, key, value) {
			function isArray(obj) {
				return typeof(obj) == 'object' && (obj instanceof Array);
			}

			function isHash(obj) {
				return typeof(obj) == 'object' && !(obj instanceof Array);
			}

			function isPrimitive(obj) {
				return typeof(obj) != 'object';
			}

			//this is an anonymous function, so the only way it can call itself
			//is by getting its reference via argument.callee. Let's just assign
			//a shorter name for it.
			var me = arguments.callee;

			var prop = null;

			if (isPrimitive(value)) {
				prop = new PropertySimple(key, new java.lang.String(value));
			} else if (isArray(value)) {
				prop = new PropertyList(key);
				for(var i = 0; i < value.length; ++i) {
					var v = value[i];
					if (v != null) {
						me(prop, key, v);
					}
				}
			} else if (isHash(value)) {
				prop = new PropertyMap(key);
				for(var i in value) {
					var v = value[i];
					if (value != null) {
						me(prop, i, v);
					}
				}
			}

			if (parent instanceof PropertyList) {
				parent.add(prop);
			} else {
				parent.put(prop);
			}
		})(config, key, value);
	}

	return config;
}

/**
 * Opposite of <code>asConfiguration</code>. Converts an RHQ's configuration object
 * into a javascript hash.
 *
 * @param configuration
 */
function asHash(configuration) {
	ret = {}

	iterator = configuration.getMap().values().iterator();
	while(iterator.hasNext()) {
		prop = iterator.next();

		(function(parent, prop) {
			function isArray(obj) {
				return typeof(obj) == 'object' && (obj instanceof Array);
			}

			function isHash(obj) {
				return typeof(obj) == 'object' && !(obj instanceof Array);
			}

			var me = arguments.callee;

			var representation = null;

			if (prop instanceof PropertySimple) {
				representation = prop.stringValue;
			} else if (prop instanceof PropertyList) {
				representation = [];

				for(var i = 0; i < prop.list.size(); ++i) {
					var child = prop.list.get(i);
					me(representation, child);
				}
			} else if (prop instanceof PropertyMap) {
				representation = {};

				var childIterator = prop.getMap().values().iterator();
				while(childIterator.hasNext()) {
					var child = childIterator.next();

					me(representation, child);
				}
			}

			if (isArray(parent)) {
				parent.push(representation);
			} else if (isHash(parent)) {
				parent[prop.name] = representation;
			}
		})(ret, prop);
	}
	(function(parent) {

	})(configuration);

	return ret;
}

//------------------------------ bundles.js

/**
 * A simple function to create a new bundle version from a zip file containing
 * the bundle.
 * 
 * @param pathToBundleZipFile the path to the bundle on the local file system
 * 
 * @return an instance of BundleVersion class describing what's been created on 
 * the RHQ server.
 */
function createBundleVersion(pathToBundleZipFile) {
	var bytes = getFileBytes(pathToBundleZipFile)
	return BundleManager.createBundleVersionViaByteArray(bytes)
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
function getAllBaseDirectories(resourceTypeId) {
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
function createBundleDestination(destinationName, description, bundleName, groupName, baseDirName, deployDir) {
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
function deployBundle(destination, bundleVersion, deploymentConfiguration, description, isCleanDeployment) {
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
		deploymentConfig = asConfiguration(deploymentConfiguration);
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

//------------------- script body

deployBundle(destinationId, bundleVersionId, deploymentConfig, description, isCleanDeployment);

