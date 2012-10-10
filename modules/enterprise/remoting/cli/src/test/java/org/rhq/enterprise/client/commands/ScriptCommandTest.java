package org.rhq.enterprise.client.commands;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.testng.annotations.Test;

import org.rhq.bindings.client.RhqManager;
import org.rhq.bindings.output.TabularWriter;
import org.rhq.bindings.util.ScriptUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.clientapi.RemoteClient;

public class ScriptCommandTest {

    ScriptCommand cmd;
    ClientMain client;
    
    @Test(enabled=false)
    public void executeShouldSetDefaultBindings() throws Exception {
        client = createClient();
        cmd = new ScriptCommand();
        String script = "exec var x = 1;";
        String[] args = asList(script).toArray(new String[] {});

        cmd.execute(client, args);

        assertSubjectBoundToScript();
        assertManagersBoundToScript();
        assertPrettyWriterBoundToScript();
        assertScriptUtilsBoundToScript();
        assertIsDefinedFunctionBoundToScript();
    }

    ClientMain createClient() throws Exception {
        ClientMain client = new ClientMain();
        //client.setSubject(new Subject("rhqadmin", true, true));
        client.setRemoteClient(new RemoteClientStub("localhost", 7080));

        return client;
    }

    void assertSubjectBoundToScript() {
        ScriptEngine scriptEngine = client.getScriptEngine();
        Object subject = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).get("subject");

        assertNotNull(subject, "Expected variable 'subject' to be bound to script in global scope");
        assertTrue(
                (subject instanceof Subject),
                "Expected variable 'subject' to be of type " + Subject.class.getName()
        );
    }

    void assertManagersBoundToScript() {
        ScriptEngine scriptEngine = client.getScriptEngine();
        List<Class<?>> mgrsNotBound = new ArrayList<Class<?>>();

        for (RhqManager mgr : RhqManager.values()) {
            if (!scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(mgr.name())) {
                mgrsNotBound.add(mgr.remote());
            }
        }

        assertTrue(
                mgrsNotBound.size() == 0,
                "Expected the following managers to be bound in global scope to the script, " + mgrsNotBound
        );
    }

    void assertPrettyWriterBoundToScript() {
        ScriptEngine scriptEngine = client.getScriptEngine();
        Object writer = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).get("pretty");

        assertNotNull(writer, "Expected variable 'pretty' to be bound to script in global scope");
        assertTrue(
                (writer instanceof TabularWriter),
                "Expected variable 'pretty' to be of type " + TabularWriter.class.getName()
        );
    }

    void assertScriptUtilsBoundToScript() {
        ScriptEngine scriptEngine = client.getScriptEngine();
        Object scriptUtil = scriptEngine.get("scriptUtil");

        assertNotNull(scriptUtil, "Expected variable 'scriptUtil' to be bound to script in engine scope");
        assertTrue(
                (scriptUtil instanceof ScriptUtil),
                "Expected variable 'scriptUtil' to be of type " + ScriptUtil.class.getName()
        );
    }

    void assertIsDefinedFunctionBoundToScript() {
        ScriptEngine scriptEngine = client.getScriptEngine();
        Object function = scriptEngine.get("isDefined");

        assertNotNull(function, "Expected function 'isDefined' to be bound to script in engine scope");
    }

    public static class RemoteClientStub extends RemoteClient {

        public RemoteClientStub(String host, int port) {
            super(host, port);
        }
    }

}
