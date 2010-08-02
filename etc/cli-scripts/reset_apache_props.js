/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * This CLI script resets/restores apache connection properties that are not set to their
 * default values during plugin upgrade. This script is only intended to be run once. If
 * run subsequent times, the script will reset these properties to their defaults,
 * overriding whatever the current values might be.
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

functionr resetApachePluginConfigProps() {
  var resourceTypeName = 'Apache HTTP Server';
  var pluginName = 'Apache';
  var servers = findResourcesByTypeAndPlugin(resourceTypeName, pluginName);

  updatePluginConfigs(servers, function(pluginConfig) {
    pluginConfig.put(PropertySimple('augeasEnabled', 'no'));
    pluginConfig.put(PropertySimple('configurationFilesInclusionPatterns', '/etc/httpd/conf/httpd.conf'));
    pluginConfig.put(PropertySimple('vhostCreationPolicy', 'vhost-per-file'));
  });
}

//////////////
// main     //
//////////////
resetApachePluginConfigProps();
