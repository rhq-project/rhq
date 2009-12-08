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

var waitInterval = 100;
var structured = true;
var raw = false;

loadResourceTypes();

function loadResourceTypes() {
    var resourceTypes = context.getAttribute('resourceTypes');
    if (resourceTypes != null && !resourceTypes.isEmpty()) {
        return;
    }

    var criteria = ResourceTypeCriteria();
    criteria.fetchResourceConfigurationDefinition(true);

    resourceTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);
    context.setAttribute('resourceTypes', resourceTypes, javax.script.ScriptContext.ENGINE_SCOPE);
}

function ConfigUtil(resourceName) {
    var util = this;

    this.resource = findServer(resourceName);

    this.getLatestConfiguration = function () {
        var latestUpdate = ConfigurationManager.getLatestResourceConfigurationUpdate(this.resource.id);

        Assert.assertNotNull(
            latestUpdate,
            "Failed to load the latest resource configuration update for " + this.resource.name
        );
        Assert.assertNotNull(
            latestUpdate.configuration,
            "No configuration is attached to the latest config update object."
        );

        return latestUpdate.configuration;
    }

    this.printLatestConfiguration = function() {
        var configuration = this.getLatestConfiguration();
        printConfiguration(configuration);
    }

    function printConfiguration(configuration)  {
        pretty.print(configuration);

        if (util.isRawSupported() || util.isStructuredAndRawSupported()) {
            println("\nRaw configuration files:");
            var rawConfigs = java.util.ArrayList(configuration.rawConfigurations);
            for (i = 0; i < rawConfigs.size(); i++) {
                var rawConfig = rawConfigs.get(i);
                println("\n[" + rawConfig.path + "]");
                println(java.lang.String(rawConfig.contents) + "\n\n---------------------");
            }
        }
    }

    this.updateConfiguration = function(update, doStructured) {
        println("Loading latest configuration for " + this.resource.name);
        var configuration = this.getLatestConfiguration();

        printConfiguration(configuration);
        println('');

        if (this.isStructuredSupported()) {
            applyStructuredUpdate(this.resource, configuration, update);
        }
        else if (this.isRawSupported()) {
            applyRawUpdate(this.resource, configuration, update);
        }
        else if (this.isStructuredAndRawSupported()) {
            if (doStructured == null) {
                throw "\nMust specify structured or raw when updating a configuration that supports both " +
                    "structured and raw. Update will abort.";
            }

            if (doStructured) {
                applyStructuredUpdate(this.resource, configuration, update);
            }
            else {
                applyRawUpdate(this.resource, configuration, update);
            }
        }

        this.waitForResourceConfigurationUpdateToComplete();
        println("Configuration update has completed.");

        println("Loading latest, updated configuration");
        this.printLatestConfiguration();
    }

    this.waitForResourceConfigurationUpdateToComplete = function() {
        print("Waiting for configuration update to complete...")
        while (ConfigurationManager.isResourceConfigurationUpdateInProgress(this.resource.id)) {
            print(".");
            java.lang.Thread.sleep(waitInterval);
        }
        println('');
    }

    this.isRawSupported = function() {
        var configFormat = this.resource.resourceType.resourceConfigurationDefinition.configurationFormat;
        return ConfigurationFormat.RAW == configFormat;
    }

    this.isStructuredSupported = function() {
        var configFormat = this.resource.resourceType.resourceConfigurationDefinition.configurationFormat;
        return ConfigurationFormat.STRUCTURED == configFormat;    
    }

    this.isStructuredAndRawSupported = function() {
        var configFormat = this.resource.resourceType.resourceConfigurationDefinition.configurationFormat;
        return ConfigurationFormat.STRUCTURED_AND_RAW == configFormat;
    }

    function verifyPropertyToUpdateExists(configuration) {
        if (!isDefined('propertyName')) {
            throw "\nThe 'propertyName' variable must be defined. Update will abort.";
        }

        if (!isDefined('propertyValue')) {
            throw "\nThe 'propertyValue' variable must be defined. Update will abort.";
        }

        if(configuration.getSimple(propertyName) == null) {
            throw "\nThe property '" + propertyName + "' is undefined. Update will abort.";
        }
    }

    function verifyRawContentExists() {
        if (!isDefined('contents')) {
            throw "\n'contents' argument is required when updating " + serverName;
        }
    }

    function applyStructuredUpdate(resource, configuration, update) {
        println("Applying resource configuration update...");

        for (key in update) {
            var property = configuration.getSimple(key.toString());
            if (property == null) {
                throw "\nThe property '" + key + "' does not exist in the configuration. Update will abort.";
            }
            property.stringValue = update[key];
        }

        ConfigurationManager.updateStructuredOrRawConfiguration(resource.id, configuration, structured);
    }

    function applyRawUpdate(resource, configuration, update) {
        var updates = 0;
        var rawConfigs = java.util.ArrayList(configuration.rawConfigurations);
        for (path in update) {
            var rawConfig = findRawConfig(path, rawConfigs);
            if (rawConfig == null) {
                println("Failed to find raw config with path ending with " + path);
            }
            else {
                configuration.rawConfigurations.remove(rawConfig);
                var contents = java.lang.String(update[path]);
                rawConfig.contents = contents.bytes;
                configuration.rawConfigurations.add(rawConfig);
                updates++;
            }
        }

        if (updates == 0) {
            throw "\nNo matching paths were found. Update will abort.";
        }

        println("Applying resource configuration update...");
        ConfigurationManager.updateStructuredOrRawConfiguration(resource.id, configuration, raw);
    }

    function findRawConfig(path, rawConfigs) {
        for (i = 0; i < rawConfigs.size(); i++) {
            var rawConfig = rawConfigs.get(i);
            if (rawConfig.path.endsWith(path.toString())) {
                return rawConfig;
            }
        }
        return null;
    }

}

function findServer(name) {
    var criteria = ResourceCriteria();
    criteria.strict = true;
    criteria.addFilterName(name);
    criteria.fetchResourceType(true);

    var resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertTrue(resources.size() == 1, "Expected to get back one resource but got back " + resources.size());

    var resource = resources.get(0);
    var resourceType = findResourceType(resource);

    if (resourceType == null) {
        resourceType = loadResourceType(resource);

        if (resourceType == null) {
            throw "Failed to initialize resource type for " + resource.name;
        }
    }
    resource.resourceType = resourceType;

    return resource;
}

function loadResourceType(resource) {
    return ResourceTypeManager.getResourceTypeById(resource.resourceType.id);
}

function findResourceType(resource) {
    for (i = 0; i < resourceTypes.size(); i++) {
        var resourceType = resourceTypes.get(i);
        if (resourceType.id == resource.resourceType.id) {
            return resourceType;
        }
    }
    return null;
}
