/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.itest.nonpc;

import static org.rhq.modules.plugins.wildfly10.test.util.ASConnectionFactory.getDomainControllerASConnection;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;

import org.rhq.modules.plugins.wildfly10.json.Address;
import org.rhq.modules.plugins.wildfly10.json.ComplexResult;
import org.rhq.modules.plugins.wildfly10.json.CompositeOperation;
import org.rhq.modules.plugins.wildfly10.json.Operation;
import org.rhq.modules.plugins.wildfly10.json.ReadAttribute;
import org.rhq.modules.plugins.wildfly10.json.Result;

/**
 * Miscellaneous tests that don't fit well into other test classes
 *
 * @author Heiko W. Rupp
 */
public class MiscTest extends AbstractIntegrationTest {

    public void testSetRollback() throws Exception {
        Operation op = new Operation("foo", new Address());
        Result res = getDomainControllerASConnection().execute(op);
        assertNotNull(res);
        assertFalse(res.isSuccess(), "Response outcome was success.");
        assertTrue(res.isRolledBack(), "Response was not rolled back: " + res);
        assertTrue(res.getFailureDescription().endsWith("rolled-back=true"), "Unexpected failure description: " + res);
    }

    public void testCompositeReadAttribute() throws Exception {
        Address a = new Address("profile=default,subsystem=web,connector=http");
        CompositeOperation cop = new CompositeOperation();
        Operation step1 = new ReadAttribute(a, "maxTime");
        cop.addStep(step1);
        Operation step2 = new ReadAttribute(a, "processingTime");
        cop.addStep(step2);

        ComplexResult res = getDomainControllerASConnection().executeComplex(cop);
        assertNotNull(res);
        assertTrue(res.isSuccess(), "Response outcome was failure.");
        Map<String, Object> resResult = res.getResult();
        assertNotNull(resResult);
        assertEquals(resResult.size(), 2);
    }

}
