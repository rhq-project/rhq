package org.rhq.enterprise.client.commands;

import static java.util.Collections.*;
import static java.util.Arrays.asList;
import static org.testng.Assert.*;

import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.testng.annotations.Test;
import org.rhq.enterprise.client.ClientMain;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.client.commands.ScriptCommand;
import org.rhq.enterprise.server.alert.AlertManagerRemote;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerRemote;
import org.rhq.enterprise.server.configuration.ConfigurationManagerRemote;
import org.rhq.enterprise.server.content.RepoManagerRemote;
import org.rhq.enterprise.server.content.ContentManagerRemote;
import org.rhq.enterprise.server.operation.OperationManagerRemote;
import org.rhq.enterprise.server.authz.RoleManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerRemote;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.bindings.client.RhqManagers;
import org.rhq.bindings.output.TabularWriter;
import org.rhq.bindings.util.ScriptUtil;
import org.rhq.core.domain.auth.Subject;

import javax.script.ScriptEngine;
import javax.script.ScriptContext;
import javax.script.Bindings;

public class ScriptCommandTest {

    ScriptCommand cmd;
    
    @Test(enabled=false)
    public void executeShouldSetDefaultBindings() throws Exception {
        ClientMain client = createClient();
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
        ScriptEngine scriptEngine = cmd.getScriptEngine();
        Object subject = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).get("subject");

        assertNotNull(subject, "Expected variable 'subject' to be bound to script in global scope");
        assertTrue(
                (subject instanceof Subject),
                "Expected variable 'subject' to be of type " + Subject.class.getName()
        );
    }

    void assertManagersBoundToScript() {
        ScriptEngine scriptEngine = cmd.getScriptEngine();
        List<String> mgrsNotBound = new ArrayList<String>();

        for (RhqManagers mgr : RhqManagers.values()) {
            if (!scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).containsKey(mgr.name())) {
                mgrsNotBound.add(mgr.remoteName());
            }
        }

        assertTrue(
                mgrsNotBound.size() == 0,
                "Expected the following managers to be bound in global scope to the script, " + mgrsNotBound
        );
    }

    void assertPrettyWriterBoundToScript() {
        ScriptEngine scriptEngine = cmd.getScriptEngine();
        Object writer = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).get("pretty");

        assertNotNull(writer, "Expected variable 'pretty' to be bound to script in global scope");
        assertTrue(
                (writer instanceof TabularWriter),
                "Expected variable 'pretty' to be of type " + TabularWriter.class.getName()
        );
    }

    void assertScriptUtilsBoundToScript() {
        ScriptEngine scriptEngine = cmd.getScriptEngine();
        Object scriptUtil = scriptEngine.get("scriptUtil");

        assertNotNull(scriptUtil, "Expected variable 'scriptUtil' to be bound to script in engine scope");
        assertTrue(
                (scriptUtil instanceof ScriptUtil),
                "Expected variable 'scriptUtil' to be of type " + ScriptUtil.class.getName()
        );
    }

    void assertIsDefinedFunctionBoundToScript() {
        ScriptEngine scriptEngine = cmd.getScriptEngine();
        Object function = scriptEngine.get("isDefined");

        assertNotNull(function, "Expected function 'isDefined' to be bound to script in engine scope");
    }

    public static class RemoteClientStub extends RemoteClient {

        public RemoteClientStub(String host, int port) {
            super(host, port);
        }

        @Override
        public AlertManagerRemote getAlertManager() {
            return null;
        }

        @Override
        public AlertDefinitionManagerRemote getAlertDefinitionManager() {
            return null;
        }

        @Override
        public ConfigurationManagerRemote getConfigurationManager() {
            return null;
        }

        @Override
        public RepoManagerRemote getRepoManager() {
            return null;
        }

        @Override
        public ContentManagerRemote getContentManager() {
            return null;
        }

        @Override
        public OperationManagerRemote getOperationManager() {
            return null;
        }

        @Override
        public RoleManagerRemote getRoleManager() {
            return null;
        }

        @Override
        public ResourceManagerRemote getResourceManager() {
            return null;
        }

        @Override
        public ResourceGroupManagerRemote getResourceGroupManager() {
            return null;
        }

        @Override
        public SubjectManagerRemote getSubjectManager() {
            return null;
        }
    }

}
