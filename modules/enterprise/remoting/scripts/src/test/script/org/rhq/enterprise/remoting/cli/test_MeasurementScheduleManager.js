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

function testFindWithFiltering() {
    var measurementDef = findMeasurementDefinition();
    var resource = findAlphaService();

    var criteria = MeasurementScheduleCriteria();
    criteria.addFilterDefinitionIds([measurementDef.id])
    criteria.addFilterResourceId(resource.id);

    var measurementSchedules = MeasurementScheduleManager.findSchedulesByCriteria(criteria);

    Assert.assertNumberEqualsJS(measurementSchedules.size(), 1, 'Failed to find measurement schedules when filtering');
}

function testFindWithFetchingAssociations() {
    var measurementDef = findMeasurementDefinition();
    var resource = findAlphaService();

    var criteria = MeasurementScheduleCriteria();
    criteria.addFilterDefinitionIds([measurementDef.id])
    criteria.addFilterResourceId(resource.id);
    criteria.fetchBaseline(true);
    criteria.fetchDefinition(true);
    criteria.fetchResource(true);

    var measurementSchedules = MeasurementScheduleManager.findSchedulesByCriteria(criteria);

    Assert.assertNumberEqualsJS(measurementSchedules.size(), 1, 'Failed to find measurement schedules when fetching associations');
}

function testFindWithSorting() {
    var measurementDef = findMeasurementDefinition();
    var resource = findAlphaService();

    var criteria = MeasurementScheduleCriteria();
    criteria.addSortName(PageOrdering.ASC);

    var measurementSchedules = MeasurementScheduleManager.findSchedulesByCriteria(criteria);

    Assert.assertTrue(measurementSchedules.size() > 0, 'Failed to find measurement schedules when sorting');
}

function testEnablingAndDisablingMeasurementSchedules() {
    var service = findAlphaService();

    var criteria = MeasurementScheduleCriteria();
    criteria.addFilterResourceId(service.id);

    var schedules = MeasurementScheduleManager.findSchedulesByCriteria(criteria);

    Assert.assertNumberEqualsJS(schedules.size(), 6, 'Failed to retrieve measurement schedules for ' + service.name);

    enableSchedules(schedules, service);

    var scheduleIds = getMeasurementDefinitionIds(schedules);

    var schedulesFromAgent = MeasurementScheduleManager.getResourceMeasurementSchedulesFromAgent(service.id);

    Assert.assertNumberEqualsJS(schedulesFromAgent.size(), schedules.size(), 'Expected the number of schedules coming from ' +
            'the agent to be the same as the number on the server');

    disableSchedules(schedules, service);

    var verifySchedulesDisabledInDB = function() {
        var schedules = MeasurementScheduleManager.findSchedulesByCriteria(criteria);
        for (i = 0; i < schedules.size(); ++i) {
            Assert.assertFalse(schedules.get(i).enabled, 'Failed to persist the disabling of schedules');
        }
    }

    var verifySchedulesDisabledOnAgent = function() {
        var schedulesFromAgent = MeasurementScheduleManager.getResourceMeasurementSchedulesFromAgent(service.id);
        Assert.assertNumberEqualsJS(schedulesFromAgent.size(), 0, 'Schedules have been disabled but failed to sync changes with agent');
    }

    verifySchedulesDisabledInDB();
    verifySchedulesDisabledOnAgent();
}

function findMeasurementDefinition() {
    var criteria = MeasurementDefinitionCriteria();
    criteria.addFilterName('alpha-metric0');
    criteria.addFilterResourceTypeName('service-alpha');

    var measurementDefs = MeasurementDefinitionManager.findMeasurementDefinitionsByCriteria(criteria);

    return measurementDefs.get(0);
}

function findAlphaService() {
    var criteria = ResourceCriteria();
    criteria.addFilterName('service-alpha-0');
    criteria.addFilterParentResourceName('server-omega-0');

    return ResourceManager.findResourcesByCriteria(criteria).get(0);
}

function getMeasurementDefinitionIds(schedules) {
    var ids = [];
    for (i = 0; i < schedules.size(); ++i) {
        ids.push(schedules.get(i).definition.id);
    }
    return ids;
}

function enableSchedules(schedules, resource) {
    setSchedulesEnabled(schedules, true);
    MeasurementScheduleManager.enableSchedulesForResource(resource.id, getMeasurementDefinitionIds(schedules));
}

function disableSchedules(schedules, resource) {
    setSchedulesEnabled(schedules, false);
    MeasurementScheduleManager.disableSchedulesForResource(resource.id, getMeasurementDefinitionIds(schedules));
}

function setSchedulesEnabled(schedules, enabled) {
    println('ENABLED = ' + enabled);
    for (i = 0; i < schedules.size(); ++i) {
        schedules.get(i).enabled = enabled;
    }

}