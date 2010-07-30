/**
 * This is a CLI script to be used for https://bugzilla.redhat.com/show_bug.cgi?id=573034.
 * The script resets the snapshot report plugin configuration properties for JBoss servers
 * and agents to their default values. This is a one-off script intended only to be run
 * once to assign values to required plugin properties that are unset.
 *
 * NOTE: This script can probably be refactored and generalized a bit to support
 * resetting any specified plugin configuration properties to their default values.
 * And specifying candidate properties could be done via a callback function to
 * allow for arbitrarily complex logic in determining the properties.
 */

function iterate(iterable, callback) {
  var iterator = iterable.iterator();
  while (iterator.hasNext()) {
    callback(iterator.next());
  }
}

function filter(iterable, predicate) {
  var matches = java.util.ArrayList();
  iterate(iterable, function(obj) {
    if (predicate(obj)) {
      matches.add(obj);
    }
  });
  return matches;
}

function log(msg) {
  println("DEBUG   " + msg);
}

function loadPluginConfiguration(resource) {
  log("Loading plugin configuration for " + resource);
  return ConfigurationManager.getPluginConfiguration(resource.id);
}

function loadPluginConfigDef(resourceTypeName, pluginName) {
  log("Loading plugin configuration definition for [resourceType=" + resourceTypeName + ", plugin=" +
      pluginName + "]");
  var resourceType = ResourceTypeManager.getResourceTypeByNameAndPlugin(resourceTypeName,
      pluginName);

  if (resourceType == null) {
    throw "Failed to find resource type '" + resourceTypeName + "'";
  }
  
  configDef = ConfigurationManager.getPluginConfigurationDefinitionForResourceType(resourceType.id);

  if (configDef == null) {
    throw "Failed to load plugin configuration for [resourceType=" + resourceTypeName +
        ", plugin=" + pluginName + "]";
  }

  return configDef;
}

function findResourcesByTypeAndPlugin(resourceType, plugin) {
  var criteria = ResourceCriteria();
  criteria.addFilterResourceTypeName(resourceType);
  criteria.addFilterPluginName(plugin);
  criteria.fetchResourceType(true);
  criteria.fetchPluginConfiguration(true);
  criteria.caseSensitive = true;
  criteria.strict = true;

  var resources = ResourceManager.findResourcesByCriteria(criteria);

  log("Found " + resources.size() + " " + resourceType + " resources ");

  iterate(resources, function(resource) {
    resource.pluginConfiguration = loadPluginConfiguration(resource);
  });

  return resources;
}

/**
 * This function takes a Configuration object and "flattens" its properties into a list of
 * all simple (i.e., PropertySimple) objects contained in the configuration. This includes
 * nested properties. The properties are returned as a java.util.List.
 *
 * param: configuration The Configuration object from which the property list will be built
 *
 * return: A List object of all PropertySimple objects, including those nested in complex
 *         properties
 */
function flattenProperties(configuration) {
  var simpleProperties = java.util.ArrayList();
  
  iterate(configuration.map.values(), function(property) {
    if (property instanceof PropertySimple) {
      simpleProperties.add(property);
    }
    else if (property instanceof PropertyList) {
      simpleProperties.addAll(flattenPropertyList(property));
    }
  });

  return simpleProperties;
}

function flattenPropertyList(propertyList) {
  var simpleProperties = java.util.ArrayList();

  iterate(propertyList.list, function(property) {
    if (property instanceof PropertySimple) {
      simpleProperty.add(property);
    }
    else if (property instanceof PropertyMap) {
      simpleProperties.addAll(flattenPropertyMap(property));
    }
    else if (property instanceof PropertyList) {
      simpleProperties.addAll(flattenPropertyList(property));
    }
  });

  return simpleProperties;
}

function flattenPropertyMap(propertyMap) {
  var simpleProperties = java.util.ArrayList();

  iterate(propertyMap.map.values(), function(property) {
    if (property instanceof PropertySimple) {
      simpleProperties.add(property);
    }
    else if (property instanceof PropertyMap) {
      simpleProperties.addAll(flattenPropertyMap(property));
    }
    else if (property instanceof PropertyList) {
      simpleProperties.addAll(flattPropertyList(property));
    }
  });

  return simpleProperties;
}

/**
 * Loads the specified default plugin properties for a given plugin configuration definition.
 * The defaults are stored in and retrieved from the default template of the plugin
 * configuration.
 *
 * param: pluginConfigDef A ConfigurationDefinition object which is the plugin config def
 *        from which to retrieve the defaults
 *
 * param: defaultPropertyNames A java.util.Collection of the property names to include
 *
 * return: A java.util.Map of the default properties. Property names as strings are mapped
 *         to the PropertySimple objects.
 */
function loadSnapshotDefaults(pluginConfigDef, defaultPropertyNames) {
  var snapshotDefaultsMap = java.util.HashMap();
  var defaults = flattenProperties(pluginConfigDef.defaultTemplate.configuration);

  var isSnapshotProperty = function(property) {
    return defaultPropertyNames.contains(property.name);
  }

  var snapshotDefaults = filter(defaults, isSnapshotProperty);

  iterate(snapshotDefaults, function(property) {
    snapshotDefaultsMap.put(property.name, property);
  });

  return snapshotDefaultsMap;
}

function getJBossSnapshotPropertyNames() {
  var set = java.util.HashSet();
  set.add('snapshotConfigEnabled');
  set.add('snapshotConfigDirectory');
  set.add('snapshotConfigRecursive');
  set.add('snapshotLogEnabled');
  set.add('snapshotLogDirectory');
  set.add('snapshotLogRecursive');
  set.add('snapshotDataEnabled');
  set.add('snapshotDataDirectory');
  set.add('snapshotDataRecursive');
  set.add('snapshotAdditionalFilesEnabled');
  set.add('snapshotAdditionalFilesDirectory');
  set.add('snapshotAdditionalFilesRecursive');

  return set; 
}

function getAgentSnapshotPropertyNames() {
  var set = java.util.HashSet();
  set.add('snapshotConfigEnabled');
  set.add('snapshotLogEnabled');
  set.add('snapshotDataEnabled');

  return set;
}

/**
 * Resets the values of specified plugin configuration properties back to their default values.
 * The configuration is modified locally, in memory and then a request is sent to
 * ConfigurationManager.
 *
 * param: resources The resources whose plugin configurations are to be updated. This is
 *        expected to be a java.util.Collection.
 *
 * param: snapshotDefaults: The names of the plugin configuration properties to reset. This
 *        is expected to be a java.util.Collection.
 */
function resetPluginConfigPropsFromTemplate(pluginConfigDef, resources, snapshotDefaults) {
  iterate(resources, function(resource) {
    log("Preparing to reset plugin configuration properties for " + resource);
    var simpleProperties = flattenProperties(resource.pluginConfiguration);
    var snapshotProperties = filter(simpleProperties, function(property) {
      return snapshotDefaults.containsKey(property.name);
    });

    iterate(snapshotProperties, function(property) {
      var defaultProperty = snapshotDefaults.get(property.name);
      property.stringValue = defaultProperty.stringValue;
      log("Set property " + property.name + " to " + property.stringValue);
    });

    ConfigurationManager.updatePluginConfiguration(resource.id, resource.pluginConfiguration);
    log("Updated plugin configuration for " + resource);
  });
}

function resetPluginConfigProps(resources, applyDefaults) {
  iterate(resources, function(resource) {
    applyDefaults(resource.pluginConfiguration);
    ConfigurationManager.updatePluginConfiguration(resource.id, resource.pluginConfiguration);
    log("Updated plugin configuration for " + resource);
  });
}

function resetJBossPluginConfigProps() {
  var jbossResourceTypeName = 'JBossAS Server';
  var jbossPluginName = 'JBossAS';
  var jbossServers = findResourcesByTypeAndPlugin(jbossResourceTypeName, jbossPluginName);
  var jbossPluginConfigDef = loadPluginConfigDef(jbossResourceTypeName, jbossPluginName);

  //resetPluginConfigProps(jbossServers, loadSnapshotDefaults(jbossPluginConfigDef,
  //    getJBossSnapshotPropertyNames()));

  resetPluginConfigProps(jbossServers, function(pluginConfig) {
    pluginConfig.put(PropertySimple('snapshotConfigEnabled', true));
    pluginConfig.put(PropertySimple('snapshotConfigDirectory', 'config'));
    pluginConfig.put(PropertySimple('snapshotConfigRecursive', true));
    pluginConfig.put(PropertySimple('snapshotLogEnabled', true));
    pluginConfig.put(PropertySimple('snapshotLogDirectory', 'log'));
    pluginConfig.put(PropertySimple('snapshotLogRecursive', false));
    pluginConfig.put(PropertySimple('snapshotDataEnabled', false));
    pluginConfig.put(PropertySimple('snapshotDataDirectory', 'data'));
    pluginConfig.put(PropertySimple('snapshotDataRecursive', true));

    var propertyMap = PropertyMap('snapshotAdditionalFilesMap');
    propertyMap.put(PropertySimple('snapshotAdditionalFilesEnabled', true));
    propertyMap.put(PropertySimple('snapshotAdditionalFilesDirectory', ''));
    propertyMap.put(PropertySimple('snapshotAdditionalFilesRecursive', true));

    var propertyList = PropertyList('snapshotAdditionalFilesList');
    propertyList.list.add(propertyMap);

    pluginConfig.put(propertyList);
  });
}

function resetAgentPluginConfigProps() {
  var agentResourceTypeName = 'RHQ Agent';
  var agentPluginName = 'RHQAgent';
  var agents = findResourcesByTypeAndPlugin(agentResourceTypeName, agentPluginName);
  var agentPluginConfigDef = loadPluginConfigDef(agentResourceTypeName, agentPluginName);

  //resetPluginConfigProps(agents, loadSnapshotDefaults(agentPluginConfigDef,
  //    getAgentSnapshotPropertyNames()));

  resetPluginConfigProps(agents, function(pluginConfig) {
    pluginConfig.put(PropertySimple('snapshotConfigEnabled', true));
    pluginConfig.put(PropertySimple('snapshotLogEnabled', true));
    pluginConfig.put(PropertySimple('snapshotDataEnabled', true));
  });
}

//////////////
// main     //        
//////////////
resetJBossPluginConfigProps();
resetAgentPluginConfigProps();
