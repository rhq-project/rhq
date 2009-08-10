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

rhq.login('rhqadmin', 'rhqadmin');

executeAllTests();

function testFindOperationDefinitionsUnfiltered() {
    var operationDefinitions = OperationManager.findOperationDefinitionsByCriteria(OperationDefinitionCriteria());

    Assert.assertNotNull(operationDefinitions, 'Expected non-null results for criteria search of operation definitions');    
}

function testFindOperationDefinitionsWithFiltering() {
    var criteria = OperationDefinitionCriteria();
    criteria.addFilterDisplayName('Start');
    criteria.addFilterDescription('Start this application server. The script used is specified in the Operations group of connection properties.');
    criteria.addFilterPluginName('JBossAS');
    criteria.addFilterResourceTypeName('JBossAS Server');

    var opDefinitions = OperationManager.findOperationDefinitionsByCriteria(criteria);

    Assert.assertTrue(opDefinitions.size() > 0, 'Expected non-empty result list for criteria search with filters ' +
            ' for operation definitions.');
}

function testFindResourceOperationHistoriesUnfiltered() {
    var histories = OperationManager.findResourceOperationHistoriesByCriteria(ResourceOperationHistoryCriteria());

    Assert.assertNotNull(histories, 'Expected non-null results for criteria search of resource operation histories');
}

function testFindGroupOperationHistoriesUnfiltered() {
    var histories = OperationManager.findGroupOperationHistoriesByCriteria(GroupOperationHistoryCriteria());

    Assert.assertNotNull(histories, 'Expected non-null results for criteria search of group operation histories');
}