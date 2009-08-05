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

setUp();

testFilterByResource();

rhq.logout();


function setUp() {
    parentServer = findServer("server-omega-0");
    alphaService0 = findService("service-alpha-0");
    alphaService1 = findService("service-alpha-1");
    betaService0 = findService("service-beta-0");

    alphaService0Details = java.util.Date() + " >> events created for " + alphaService0.name;
    alphaService1Details = java.util.Date() + " >> events created for " + alphaService1.name;
    betaService0Details = java.util.Date() + " >> events created for " + betaService0.name;

    operationSchedule = fireEvent(alphaService0, "WARN", 1, alphaService0Details);
    fireEvent(alphaService1, "ERROR", 1, alphaService1Details);
    fireEvent(betaService0, "FATAL", 1, betaService0Details);
}

function testFilterByResource() {
    var criteria = new EventCriteria();
    criteria.caseSensitive = true;
    //criteria.addFilterResourceId(alphaService0.id);
    //criteria.addFilterSeverity(EventSeverity.WARN);
    //criteria.addFilterDetail(alphaService0Details);
    //criteria.addFilterSourceName(alphaService0.name);
    criteria.addFilterSourceName("service-alpha-event");

    result = waitForScheduledOperationToComplete(operationSchedule);

    Assert.assertNotNull(result, "Failed to get result for scheduled operation");

    var events = EventManager.findEventsByCriteria(criteria);
    //var events = findEventsByResource(alphaService0);

    //Assert.assertNumberEqualsJS(events.size(), 1, "Expected to find one event but found " + events.size());
    Assert.assertTrue(events.size() > 0, "Expected to find events when filtering by resource id for " + alphaService0);
    var foundEvent = false;
    for (i = 0; i < events.size(); ++i) {
        if (events.get(i).detail == alphaService0Details) {
            foundEvent = true;
            break;
        }
    }
    Assert.assertTrue(foundEvent, "Failed to find event with details, '" + alphaService0Details + "'");
    
    events = findEventsByResource(alphaService1);
    Assert.assertTrue(events.size() > 0, "Expected to find events when filtering by resource id for " + alphaService1);

    events = findEventsByResource(betaService0);
    Assert.assertTrue(events.size() > 0, "Expected to find events when filtering by resource id for " + betaService0);
}

function findServer(name) {
    var criteria = new ResourceCriteria();
    criteria.addFilterName(name)

    resources = ResourceManager.findResourcesByCriteria(criteria);

    assertNumberEqualsJS(resources.size(), 1, "Expected to find only one resource named '" + name + "'");

    return resources.get(0);
}

function findService(name) {
    var criteria = new ResourceCriteria()
    criteria.addFilterName(name);
    criteria.addFilterParentResourceId(parentServer.id);

    resources = ResourceManager.findResourcesByCriteria(criteria);

    assertNumberEqualsJS(resources.size(), 1, "Expected to find only one service named '" + name + "' having parent, '" +
            parentServer.name + "'");

    return resources.get(0);
}

function fireEvent(resource, severity, numberOfEvents, details) {
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