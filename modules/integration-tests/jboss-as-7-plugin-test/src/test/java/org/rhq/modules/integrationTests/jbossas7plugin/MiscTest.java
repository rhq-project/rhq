package org.rhq.modules.integrationTests.jbossas7plugin;

import java.util.Collections;

import org.testng.annotations.Test;

import org.rhq.modules.plugins.jbossas7.json.Operation;
import org.rhq.modules.plugins.jbossas7.json.PROPERTY_VALUE;
import org.rhq.modules.plugins.jbossas7.json.Result;

/**
 * Miscellaneous tests that don't fit well into other test classes
 * @author Heiko W. Rupp
 */
@Test
public class MiscTest extends AbstractIntegrationTest {

    public void testSetRollback() throws Exception {

        Operation op = new Operation("foo", Collections.<PROPERTY_VALUE>emptyList());
        Result res = getASConnection().execute(op);
        assert res != null;
        assert !res.isSuccess();
        assert res.isRolledBack();
        assert res.getFailureDescription().endsWith("rolled-back=true");
    }

}
