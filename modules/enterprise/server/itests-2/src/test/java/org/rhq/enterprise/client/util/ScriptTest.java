package org.rhq.enterprise.client.util;

import java.io.IOException;
import java.util.Random;

import javax.persistence.EntityManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.client.ScriptableAbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**Exercise some of the methods available via cli an integration tests/using a running server.

 * @author Simeon Pinder
 */
@Test
public class ScriptTest extends ScriptableAbstractEJB3Test {

    /** Exercise the ScriptUtil.findResoruces
     * 
     * @throws ScriptException
     * @throws IOException
     * @throws NotSupportedException
     * @throws SystemException
     * @throws SecurityException
     * @throws IllegalStateException
     * @throws RollbackException
     * @throws HeuristicMixedException
     * @throws HeuristicRollbackException
     */
    @Test
    public void testScriptUtilFindResources() throws ScriptException, IOException, NotSupportedException,
        SystemException,
        SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
        HeuristicRollbackException {

        //Instantiate ScriptEngine.
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        ScriptEngine engine = getEngine(overlord);

        //create resources to query.
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();
        String tuid = "" + new Random().nextInt();
        String prefix = "CLI-TEST-" + tuid + "-";
        int resourceCount = 201; //assuming 200 per page at least 2 pages of results.
        int[] resourceIds = new int[resourceCount];
        try {
            System.out.println("-------- Creating " + resourceCount + " resource(s). This may take a while ....");
            long start = System.currentTimeMillis();
            for (int i = 0; i < resourceCount; i++) {
                String name = prefix + i;
                Resource r = SessionTestHelper.createNewResource(entityMgr, name);
                resourceIds[i] = r.getId();
            }
            entityMgr.flush();

            System.out.println("----------- Created " + resourceCount + " resource(s) in "
                + (System.currentTimeMillis() - start) + " ms.");

            //now get the Resources back by CLI
            PageList<Resource> result = (PageList<Resource>) engine.eval("findResources('" + prefix + "');");
            assert result.size() == resourceCount : "Expected to get '" + resourceCount
                + "' result(s) from across two pages but instead got '" + result.size() + "'.";
        } finally {
            getTransactionManager().rollback();
        }
    }
}
