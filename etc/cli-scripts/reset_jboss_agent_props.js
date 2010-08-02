/**
 * This is a CLI script to be used for https://bugzilla.redhat.com/show_bug.cgi?id=573034.
 * The script resets/restores the snapshot report plugin configuration properties for JBoss servers
 * and agents to their default values. This is a one-off script intended only to be run
 * once to assign values to required plugin properties that are unset. Running it subsequent times
 * cause the current connection settings for the snapshot report properties to be overridden and
 * reset to their default vales
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

function updatePluginConfigs(resources, applyDefaults) {
  iterate(resources, function(resource) {
    log("Preparing to update plugin configuration for " + resource)
    applyDefaults(resource.pluginConfiguration);
    ConfigurationManager.updatePluginConfiguration(resource.id, resource.pluginConfiguration);
    log("Updated plugin configuration for " + resource);
  });
}

function resetJBossPluginConfigProps() {
  var jbossResourceTypeName = 'JBossAS Server';
  var jbossPluginName = 'JBossAS';
  var jbossServers = findResourcesByTypeAndPlugin(jbossResourceTypeName, jbossPluginName);

  updatePluginConfigs(jbossServers, function(pluginConfig) {
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

  updatePluginConfigs(agents, function(pluginConfig) {
    pluginConfig.put(PropertySimple('snapshotConfigEnabled', true));
    pluginConfig.put(PropertySimple('snapshotLogEnabled', true));
    pluginConfig.put(PropertySimple('snapshotDataEnabled', true));
  });
}

//////////////
// main     //        
//////////////
exec -f
resetJBossPluginConfigProps();
resetAgentPluginConfigProps();
