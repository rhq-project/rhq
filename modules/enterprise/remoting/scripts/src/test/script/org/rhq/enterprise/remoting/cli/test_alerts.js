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

skippedTests.push('testFindAlertsWithFiltering');

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
    criteria.addFilterDeleted(false);

    var alertDefs = AlertDefinitionManager.findAlertDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(alertDefs.size(), 1, "Expected to get back one alert definition but got '" +
        getNames(alertDefs) + "'");
}

function testFindMultipleAlertDefinitionsWithFiltering() {
    var serviceAlpha = findService('service-alpha-0', 'server-omega-0');
    var serviceBeta = findService('service-alpha-1', 'server-omega-0');

    var criteria = AlertDefinitionCriteria();
    criteria.addFilterPriority(AlertPriority.MEDIUM);
    criteria.addFilterResourceIds([serviceAlpha.id, serviceBeta.id]);
    criteria.addFilterDeleted(false);

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
    criteria.addFilterDeleted(false);

    var alertDefs = AlertDefinitionManager.findAlertDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(alertDefs.size(), 1, 'Expected to get back one alert when filtering and fetching associations');
}

function testFindAlertDefinitionsWithFilteringAndSortingAndFetchingAssociations() {
    var serviceAlpha = findService('service-alpha-0', 'server-omega-0');
    var serviceBeta = findService('service-alpha-1', 'server-omega-0');

    var criteria = AlertDefinitionCriteria();
    criteria.addFilterPriority(AlertPriority.MEDIUM);
    criteria.addFilterResourceIds([serviceAlpha.id, serviceBeta.id]);
    criteria.addFilterDeleted(false);

    criteria.addSortName(PageOrdering.ASC);
    criteria.addSortPriority(PageOrdering.DESC);

    var alertDefs = AlertDefinitionManager.findAlertDefinitionsByCriteria(criteria);

    Assert.assertNumberEqualsJS(alertDefs.size(), 2, 'Expected to get back two alert definitions when filtering, ' +
            'sorting, and fetching associations');

    // TODO verify sort order
}

function testFindAlertsWithoutFiltering() {
    var criteria = AlertCriteria();

    var alerts = AlertManager.findAlertsByCriteria(criteria);

    Assert.assertNotNull(alerts, 'Expected to get back non-null results when fetching alerts without filtering');
}

function testFindAlertsWithFiltering() {
    var service = findService('service-alpha-0', 'server-omega-0');
    var eventDetails = java.util.Date() + " >> events created for " + service.name;
    var severity = 'WARN';
    var numberOfEvents = 1;

    fireEvents(service, severity, numberOfEvents, eventDetails);

    var pauseLength = 1000; // in milliseconds
    var numberOfIntervals = 10;

    var eventCriteria = EventCriteria();
    eventCriteria.addFilterDetail(eventDetails);

    var events = waitForEventsToBeCommitted(pauseLength, numberOfIntervals, eventCriteria, numberOfEvents);

    Assert.assertNumberEqualsJS(events.size(), numberOfEvents, 'Failed to find all fired events when finding alerts ' +
        'with filtering. This could just be a timeout. You may want to check your database and server logs to be sure though');

    var alertDef1Name = 'service-alpha-0-alert-def-1';

    var alertCriteria = AlertCriteria();
    alertCriteria.addFilterName(alertDef1Name);
    alertCriteria.addFilterDescription('Test alert definition 1 for service-alpha-0');
    alertCriteria.addFilterPriority(AlertPriority.MEDIUM);
    alertCriteria.addFilterResourceTypeName('service-alpha-0');

    var alerts = AlertManager.findAlertsByCriteria(alertCriteria);

    Assert.assertNumberEqualsJS(alerts.size(), 1, "Expected to get back one alert for alert definition '" + alertDef1Name + "'");
}

function getNames(entities) {
    var names = [];
    for (i = 0; i < entities.size(); ++i) {
        names.push(entities.get(i).name);
    }

    return names;
}

function findService(name, parentName) {
    var criteria = ResourceCriteria();
    criteria.addFilterName(name);
    criteria.addFilterParentResourceName(parentName);

    return ResourceManager.findResourcesByCriteria(criteria).get(0);
}

function fireEvents(resource, severity, numberOfEvents, details) {
    var operationName = "createEvents";
    var delay = 0;
    var repeatInterval = 0;
    var repeatCount = 0;
    var timeout = 0;
    var parameters = createParameters(resource, severity, numberOfEvents, details);
    var description = "Test script event for " + resource.name;

    return OperationManager.scheduleResourceOperation(
        resource.id,
        operationName,
        delay,
        repeatInterval,
        repeatCount,
        timeout,
        parameters,
        description
    );
}

function createParameters(resource, severity, numberOfEvents, details) {
    var params = new Configuration();
    params.put(new PropertySimple("source", resource.name));
    params.put(new PropertySimple("details", details));
    params.put(new PropertySimple("severity", severity));
    params.put(new PropertySimple("count", java.lang.Integer(numberOfEvents)));

    return params;
}

function findEventsByResource(resource) {
    var criteria = new EventCriteria();
    criteria.addFilterResourceId(resource.id);

    return EventManager.findEventsByCriteria(criteria);
}

function waitForEventsToBeCommitted(intervalLength, numberOfIntervals, eventCriteria, numberOfEvents) {
    for (i = 0; i < numberOfIntervals; ++i) {
        events = EventManager.findEventsByCriteria(eventCriteria);
        if (events.size() == numberOfEvents) {
            return events;
        }
        java.lang.Thread.sleep(intervalLength);
    }
    return null;
}

