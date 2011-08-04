package org.rhq.modules.integrationTests.jbossas7plugin;

import java.util.Map;

import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.Address;
import org.rhq.modules.plugins.jbossas7.json.ComplexResult;
import org.rhq.modules.plugins.jbossas7.json.CompositeOperation;
import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.ReadAttribute;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Miscellaneous tests that don't fit well into other test classes
 * @author Heiko W. Rupp
 */
@Test
public class MiscTest extends AbstractIntegrationTest {

    public void testSetRollback() throws Exception {

        Operation op = new Operation("foo", new Address());
        Result res = getASConnection().execute(op);
        assert res != null;
        assert !res.isSuccess();
        assert res.isRolledBack();
        assert res.getFailureDescription().endsWith("rolled-back=true");
    }

    public void testCompositeReadAttribute() throws Exception {

        Address a = new Address("profile=default,subsystem=web,connector=http");
        CompositeOperation cop = new CompositeOperation();
        Operation step1 = new ReadAttribute(a,"maxTime");
        cop.addStep(step1);
        Operation step2 = new ReadAttribute(a,"processingTime");
        cop.addStep(step2);

        ComplexResult res = getASConnection().executeComplex(cop);
        assert res!=null;
        assert res.isSuccess();
        Map<String,Object> resResult = res.getResult();
        assert resResult !=null;
        assert resResult.size()==2;


    }
}
