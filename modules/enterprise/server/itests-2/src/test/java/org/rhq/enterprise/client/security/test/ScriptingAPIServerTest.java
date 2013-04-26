/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.enterprise.client.security.test;

import java.util.UUID;

import javax.script.ScriptEngine;

import org.testng.annotations.Test;

import org.rhq.bindings.client.RhqManager;
import org.rhq.bindings.util.SimplifiedClass;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.client.ScriptableAbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;

import static org.testng.Assert.assertNotEquals;

/**
 * @author Lukas Krejci
 */
public class ScriptingAPIServerTest extends ScriptableAbstractEJB3Test {

    @Test
    public void testProxyFactoryCorrectlyDefinesMethodsAndProperties() throws Exception {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceType rt = createResourceType();
                Resource r = createResource(rt);

                Subject overlord = LookupUtil.getSubjectManager().getOverlord();
                ScriptEngine engine = getEngine(overlord);

                engine.eval("var r = ProxyFactory.getResource(" + r.getId() + ");");

                String detectedMeasurementType = (String) engine.eval("typeof(r.measurement);");
                String detectedOperationType = (String) engine.eval("typeof(r.operation);");

                assertEquals("object", detectedMeasurementType);
                assertEquals("function", detectedOperationType);
            }
        });
    }

    @Test
    public void testManagersHaveSimplifiedSignatures() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        ScriptEngine engine = getEngine(overlord);

        //we know that interface simplification worked if the full class name is different from the original
        //Here, we want to test the simplification worked within the classloading environment of the RHQ server.
        //Simplification itself is unit tested separately.

        for (RhqManager m : RhqManager.values()) {
            String name = m.name();
            // Only check for the TagManager if it is enabled at all
            if (name.contains("TagManager") && !RhqManager.TagManager.enabled()) {
                continue;
            }
            Object scriptedManager = engine.eval(name);
            assertNotNull(scriptedManager);

            Class<?> implementedIface = scriptedManager.getClass().getInterfaces()[0];

            assertNotEquals(m.remote(), implementedIface);
            assertNotNull(implementedIface.getAnnotation(SimplifiedClass.class));
        }
    }

    private ResourceType createResourceType() {
        ResourceType resourceType = new ResourceType("ScriptingAPITestType", "dummy", ResourceCategory.PLATFORM, null);

        MeasurementDefinition measurement = new MeasurementDefinition("measurement", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.BYTES,
            DataType.MEASUREMENT, true, 1, DisplayType.DETAIL);
        measurement.setDisplayName("measurement");

        resourceType.addMetricDefinition(measurement);

        ConfigurationDefinition params = new ConfigurationDefinition("dummy", null);
        params.put(new PropertyDefinitionSimple("parameter", null, true, PropertySimpleType.BOOLEAN));

        OperationDefinition operation = new OperationDefinition(resourceType, "operation");
        operation.setDisplayName("operation");
        operation.setParametersConfigurationDefinition(params);

        resourceType.addOperationDefinition(operation);

        getEntityManager().persist(resourceType);

        return resourceType;
    }

    private Resource createResource(ResourceType resourceType) {
        Resource resource = new Resource("key", "resource", resourceType);
        resource.setUuid(UUID.randomUUID().toString());

        getEntityManager().persist(resource);

        return resource;
    }
}
