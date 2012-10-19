package org.rhq.modules.plugins.jbossas7.itest.nonpc;

import java.util.Map;

import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

import static org.testng.Assert.*;

/**
 * Miscellaneous tests that don't fit well into other test classes
 *
 * @author Heiko W. Rupp
 */
public class MiscTest extends AbstractIntegrationTest {

    public void testSetRollback() throws Exception {
        Operation op = new Operation("foo", new Address());
        Result res = getASConnection().execute(op);
        assertNotNull(res);
        assertFalse(res.isSuccess(), "Response outcome was success.");
        assertTrue(res.isRolledBack(), "Response was not rolled back: " + res);
        assertTrue(res.getFailureDescription().endsWith("rolled-back=true"), "Unexpected failure description: " + res);
    }

    public void testCompositeReadAttribute() throws Exception {
        Address a = new Address("profile=default,subsystem=web,connector=http");
        CompositeOperation cop = new CompositeOperation();
        Operation step1 = new ReadAttribute(a,"maxTime");
        cop.addStep(step1);
        Operation step2 = new ReadAttribute(a,"processingTime");
        cop.addStep(step2);

        ComplexResult res = getASConnection().executeComplex(cop);
        assertNotNull(res);
        assertTrue(res.isSuccess(), "Response outcome was failure.");
        Map<String, Object> resResult = res.getResult();
        assertNotNull(resResult);
        assertEquals(resResult.size(), 2);
    }

}
