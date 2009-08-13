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

rhq.logout();


function testFindAlertDefinitionsWithoutFiltering() {
    var alertDefs = AlertDefinitionManager.findAlertDefinitionsByCriteria(AlertDefinitionCriteria());

    Assert.assertNotNull(alertDefs, 'Expected to get back non-null results when fetch alert definitions without filtering');
}

function testFindSingleAlertDefinitionWithFiltering() {
    var service = findService('service-alpha-0', 'server-omega-0');

    var criteria = AlertDefinitionCriteria();
    criteria.addFilterName('service-alpha-0-alert-def-1');
    criteria.addFilterDescription('Test alert definition 1 for service-alpha-0');
    criteria.addFilterPriority(AlertPriority.MEDIUM);
    criteria.addFilterEnabled(true);
    criteria.addFilterResourceIds([service.id]);

    var alertDefs = AlertDefinitionManager.findAlertDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(alertDefs.size(), 1, 'Expected to get back one alert definition but got ' +
        alertDefs.size());
}

function testFindMultipleAlertDefinitionsWithFiltering() {
    var serviceAlpha = findService('service-alpha-0', 'server-omega-0');
    var serviceBeta = findService('service-alpha-1', 'server-omega-0');

    var criteria = AlertDefinitionCriteria();
    criteria.addFilterPriority(AlertPriority.MEDIUM);
    criteria.addFilterResourceIds([serviceAlpha.id, serviceBeta.id]);

    var alertDefs = AlertDefinitionManager.findAlertDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(alertDefs.size(), 2, 'Expected to get back two alert definitions but got ' + alertDefs.size());
}

function testFindAlertDefinitionWithFilteringAndFetchingAssociations() {
    var criteria = AlertDefinitionCriteria();
    criteria.addFilterName('service-alpha-0');
    criteria.addFilterDescription('Test alert definition 1 for service-alpha-0');

    criteria.fetchAlerts(true);
    criteria.fetchConditions(true);
    criteria.fetchAlertNotifications(true);

    var alertDefs = AlertDefinitionManager.findAlertDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(alertDefs.size(), 1, 'Expected to get back one alert when filtering and fetching associations');
}

function findService(name, parentName) {
    var criteria = ResourceCriteria();
    criteria.addFilterName(name);
    criteria.addFilterParentResourceName(parentName);

    return ResourceManager.findResourcesByCriteria(criteria).get(0);
}

