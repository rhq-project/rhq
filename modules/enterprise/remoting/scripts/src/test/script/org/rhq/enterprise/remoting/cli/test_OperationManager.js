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
 * These tests assume that there is a jopr server and an agent running on localhost.
 */

rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

function testFindOperationDefinitionsUnfiltered() {
    var operationDefinitions = OperationManager.findOperationDefinitionsByCriteria(OperationDefinitionCriteria());

    Assert.assertNotNull(operationDefinitions, 'Expected non-null results for criteria search of operation definitions');    
}

function testFindSingleOperationDefinitionsWithFiltering() {
    var criteria = OperationDefinitionCriteria();
    criteria.addFilterName('start');
    criteria.addFilterDisplayName('Start');
    criteria.addFilterDescription('Start this application server. The script used is specified in the Operations group of connection properties.');
    criteria.addFilterPluginName('JBossAS');
    criteria.addFilterResourceTypeName('JBossAS Server');

    var opDefinitions = OperationManager.findOperationDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(opDefinitions.size(), 1, "Expected to get back a single operation definition that " +
            "corresponds to the Start operation but got back, '" + getNames(opDefinitions) + "'");
}

function testFindOperationDefinitionsWithOptionalFiltering() {
    var criteria = OperationDefinitionCriteria();
    criteria.filtersOptional = true;

    criteria.addFilterDisplayName('Start');
    criteria.addFilterDescription('_non-existent description_');
    criteria.addFilterPluginName('JBossAS');
    criteria.addFilterResourceTypeName('JBossAS Server');

    var opDefinitions = OperationManager.findOperationDefinitionsByCriteria(criteria);

    Assert.assertTrue(opDefinitions.size() > 0, "Expected non-empty result list for criteria search with optional " +
            "filters for operation definitions");
}

function testFindSingleOperationDefinitionWithFilteringAndFetchingAssociations() {
    var criteria = OperationDefinitionCriteria();
    criteria.addFilterPluginName('JBossAS');
    criteria.addFilterName('start');
    criteria.addFilterDisplayName('Start');
    criteria.addFilterResourceTypeName('JBossAS Server');
    criteria.fetchParametersConfigurationDefinition(true);
    criteria.fetchResultsConfigurationDefinition(true);

    var opDefinitions = OperationManager.findOperationDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(opDefinitions.size(), 1, "Expected to get back one operation definition when " +
            "filtering and fetching associations but got back, '" + getNames(opDefinitions) + "'");

    var opDefinition = opDefinitions.get(0);

    Assert.assertNotNull(opDefinition.parametersConfigurationDefinition, 'operationDefinition.parametersConfigurationDefinition should have been loaded');
    Assert.assertNotNull(opDefinition.resultsConfigurationDefinition, 'operationDefinition.resultsConfigurationDefinition should have been loaded');
}

function testFindMultipleOperationDefinitionsWithFilteringAndFetchingAssociations() {
    var criteria = OperationDefinitionCriteria();
    criteria.addFilterPluginName('JBossAS');
    criteria.addFilterResourceTypeName('JBossAS Server');
    criteria.fetchParametersConfigurationDefinition(true);
    criteria.fetchResultsConfigurationDefinition(true);

    var opDefinitions = OperationManager.findOperationDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(opDefinitions.size(), 3, "Expected to get back three operation definitions when " +
            "filtering and fetching associations but got back, '" + getNames(opDefinitions) + "'");
}

function testFindMultipleOperationDefinitionsWithSorting() {
    var criteria = OperationDefinitionCriteria();
    criteria.addFilterPluginName('JBossAS');
    criteria.addFilterResourceTypeName('JBossAS Server');
    criteria.fetchParametersConfigurationDefinition(true);
    criteria.fetchResultsConfigurationDefinition(true);
    criteria.addSortName(PageOrdering.DESC);

    var opDefinitions = OperationManager.findOperationDefinitionsByCriteria(criteria);

    Assert.assertTrue(opDefinitions.size() > 0, "Expected to get back operation definitions when sorting");

    // TODO verify sorting
}

function testFindResourceOperationHistoriesUnfiltered() {
    var histories = OperationManager.findResourceOperationHistoriesByCriteria(ResourceOperationHistoryCriteria());

    Assert.assertNotNull(histories, 'Expected non-null results for criteria search of resource operation histories');
}

function testFindResourceOperationHistoriesWithFiltering() {
    var criteria = ResourceOperationHistoryCriteria();
    criteria.addFilterId(1);
    criteria.addFilterJobId(JobId(['job_id_1', 'job_id_2']));
    criteria.addFilterErrorMessage('failed');
    criteria.addFilterOperationDefinitionId(1);
    criteria.addFilterStatus(OperationRequestStatus.SUCCESS);
    criteria.addFilterOperationName('start');

    var histories = OperationManager.findResourceOperationHistoriesByCriteria(criteria);

    // TODO add verification
}

function testFindGroupOperationHistoriesUnfiltered() {
    var histories = OperationManager.findGroupOperationHistoriesByCriteria(GroupOperationHistoryCriteria());

    Assert.assertNotNull(histories, 'Expected non-null results for criteria search of group operation histories');
}

function getNames(list) {
    var names = [];
    for (i = 0; i < list.size(); ++i) {
        names.push(list.get(i).name);
    }
    return names;
}