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

var serverName = '';
var waitInterval = 100;
var STRUCTURED_SERVER = 'Structured Server';
var STRUCTURED_ANR_RAW_SERVER = 'Structured and Raw Server'

if (cmdLineArgsValid()) {
    updateConfiguration();
}

function cmdLineArgsValid() {
    var isValid = true;

    if (!scriptUtil.isDefined('server')) {
        println("'server' is a required argument");
        isValid = false;
    }

    serverName = server;
    if (!(serverName == 'structured' || serverName == 'structuredandraw')) {
        println("server argument must be 'structured' or 'structuredandraw'");
        isValid = false;
    }

    if (!scriptUtil.isDefined('propertyName')) {
        println("'propertyName' is a required argument");
        isValid = false;
    }

    if (!scriptUtil.isDefined('propertyValue')) {
        println("'propertyValue' is a required argument\n");
        isValid = false;
    }

    return isValid;
}

function updateConfiguration() {
    setServerName();

    println("\nPreapring to update configuration for " + serverName);
    
    var resource = findServer();
    var configuration = getLatestConfiguration(resource);

    printConfiguration(resource, configuration);

    if (propertyToUpdateIsUndefined(configuration)) {
        println("The property '" + propertyName + "' is undefined. Update will abort.");
        return;
    }

    applyUpdate(resource, configuration);
    waitForResourceConfigurationUpdateToComplete(resource.id);

    println("Configuration update has completed.");
    configuration = getLatestConfiguration(resource);
    printConfiguration(resource, configuration);
}

function setServerName() {
    if (server == 'structured') {
        serverName = STRUCTURED_SERVER;
    }
    else {
        serverName = STRUCTURED_ANR_RAW_SERVER;
    }
}

function findServer() {
    var criteria = ResourceCriteria();
    criteria.strict = true;
    criteria.addFilterName(serverName);

    var resources = ResourceManager.findResourcesByCriteria(criteria);

    Assert.assertTrue(resources.size() == 1, "Expected to get back one resource but got back " + resources.size());

    return resources.get(0);
}

function getLatestConfiguration(resource) {
    var latestUpdate = ConfigurationManager.getLatestResourceConfigurationUpdate(resource.id);

    Assert.assertNotNull(latestUpdate, "Failed to load the latest resource configuration update for " + resource.name);
    Assert.assertNotNull(latestUpdate.configuration, "No configuration is attached to the latest config update object.");
    
    return latestUpdate.configuration;
}

function printConfiguration(resource, configuration) {
    println("Fetching the latest resource configuration for " + resource.name);
    pretty.print(configuration);
    println('');
}

function propertyToUpdateIsUndefined(configuration) {
    return configuration.getSimple(propertyName) == null;
}

function applyUpdate(resource, configuration) {
    var property = configuration.getSimple(propertyName);
    property.stringValue = propertyValue;

    println("Apply resource configuration update...");

    ConfigurationManager.updateStructuredOrRawConfiguration(resource.id, configuration, true);
}

function waitForResourceConfigurationUpdateToComplete(resourceId) {
    print("Waiting for configuration update to complete...")
    while (ConfigurationManager.isResourceConfigurationUpdateInProgress(resourceId)) {
        print(".");
        java.lang.Thread.sleep(waitInterval);
    }
    println('');
}