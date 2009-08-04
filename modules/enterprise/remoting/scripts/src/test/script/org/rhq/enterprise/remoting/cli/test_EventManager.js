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

alphaService0 = findService("service-alpha-0");
alphaService1 = findService("service-alpha-1");
betaService0 = findService("service-beta-0");

fireEvent(alphaService0, "WARNING", 1);
fireEvent(alphaService1, "ERROR", 1);
fireEvent(betaService0, "FATAL", 1);

rhq.logout();


function findService(name) {
    var criteria = new ResourceCriteria()
    criteria.addFilterName(name);

    resources = ResourceManager.findResourcesByCriteria(criteria);

    assertTrue(resources.size() > 0, "Expected to find a resource named '" + name + "'");

    return resources.get(0);
}

function fireEvent(resource, severity, numberOfEvents) {
    var operationName = "createEvents";
    var delay = 0;
    var repeatInterval = 0;
    var repeatCount = 0;
    var timeout = 0;
    var parameters = createParameters(resource, severity, numberOfEvents);
    var description = "Test script event for " + resource.name;

    OperationManager.scheduleResourceOperation(
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

function createParameters(resource, severity, numberOfEvents) {
    var params = new Configuration();
    params.put(new PropertySimple("source", resource.name));
    params.put(new PropertySimple("details", "Test event for " + resource.name));
    params.put(new PropertySimple("severity", severity));
    params.put(new PropertySimple("count", new java.lang.Integer(numberOfEvents)));

    return params;
}