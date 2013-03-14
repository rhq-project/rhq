package org.rhq.enterprise.client;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PermissionCollection;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.annotations.Test;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.StandardScriptPermissions;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

/**
 * 
 * @author Simeon Pinder
 *
 */
@Test
public class ScriptableAbstractEJB3Test extends AbstractEJB3Test {

    protected ScriptEngine getEngine(Subject subject) throws ScriptException, IOException {
        StandardBindings bindings = new StandardBindings(new PrintWriter(System.out), new LocalClient(subject));

        PermissionCollection perms = new StandardScriptPermissions();

        return ScriptEngineFactory.getSecuredScriptEngine("javascript",
            new PackageFinder(Collections.<File> emptyList()), bindings, perms);
    }
}
