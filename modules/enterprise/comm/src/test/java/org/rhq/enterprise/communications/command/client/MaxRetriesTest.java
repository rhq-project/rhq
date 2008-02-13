/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.communications.command.client;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.echo.EchoCommand;
import org.rhq.enterprise.communications.command.impl.echo.server.EchoCommandService;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommand;

/**
 * Tests limited the number of retries for guaranteed delivery.
 *
 * @author John Mazzitelli
 */
@Test
public class MaxRetriesTest {
    @AfterClass
    public void afterClass() {
        try {
            getPrefs().removeNode();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests max retries.
     *
     * @throws Exception
     */
    public void testMaxRetries() throws Exception {
        ClientCommandSender sender = null;

        Preferences prefs = getPrefs();
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, "127.0.0.1");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        prefs.put(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION, ""
            + ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        prefs.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, "target");
        prefs.put(ServiceContainerConfigurationConstants.CMDSERVICES, EchoCommandService.class.getName());

        ServiceContainer sc = new ServiceContainer();
        sc.start(prefs, new ClientCommandSenderConfiguration());
        Thread.sleep(5000);

        try {
            // numberOfRetries tells jboss remoting effectively the number of seconds before declaring "cannot connect"
            RemoteCommunicator comm = new JBossRemotingRemoteCommunicator(
                "socket://127.0.0.1:11111/?force_remote=true&numberOfRetries=2");
            ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
            config.maxRetries = 5;
            config.retryInterval = 500L;
            config.defaultTimeoutMillis = 4000L;
            sender = new ClientCommandSender(comm, config);
            sender.startSending();

            // sanity check - make sure we can call it
            Command cmd = createNewCommand("hello");
            assert sender.sendSynch(cmd).getResults().toString().equals("hello");

            // try to send a command that can never make it - we should never retry this one
            cmd = createNewCommand(new NotSerializable());
            TestCommandResponseCallback callback = new TestCommandResponseCallback();

            synchronized (callback) {
                sender.sendAsynchGuaranteed(cmd, callback);
                callback.wait(3000L); // should not retry so should go fast
            }

            assert callback.response != null;
            assert callback.response.getCommand() != null;
            assert !callback.response.isSuccessful() : "Command wasn't serializable, should have failed: "
                + callback.response;
            assert !callback.response.getCommand().getConfiguration().containsKey("rhq.retry") : "Should not have retried at all: "
                + callback.response;

            sc.shutdown();
            Thread.sleep(2000L); // give the server container time to shutdown

            // shutdown listener and try to send - should retry forever due to cannot-connect exception (max-retries will be ignored)
            cmd = createNewCommand("forever");
            callback = new TestCommandResponseCallback();

            synchronized (callback) {
                sender.sendAsynchGuaranteed(cmd, callback);
                callback.wait((config.maxRetries * (config.retryInterval + config.defaultTimeoutMillis)) + 1000); // we will not get a callback - this proves max-retries doesn't take effect
            }

            assert callback.response == null : "Server was shut down, and we should have retried forever; should not have received a response yet: "
                + callback.response;

            // now restart the server and the command should immediately get triggered
            synchronized (callback) {
                sc.start(prefs, new ClientCommandSenderConfiguration());
                callback.wait(5000); // should get notified very fast, probably within a second
            }

            assert callback.response != null : "Command should have been finished by now";
            assert callback.response.isSuccessful() : "Command should have been successful: " + callback.response;
            assert callback.response.getResults().toString().equals("forever");
            assert Integer.parseInt(callback.response.getCommand().getConfiguration().getProperty("rhq.retry")) >= config.maxRetries : "Should have retried: "
                + callback.response;
        } finally {
            if (sender != null) {
                sender.stopSending(false);
            }

            sc.shutdown();
        }
    }

    private Preferences getPrefs() {
        Preferences topNode = Preferences.userRoot().node("rhq-agent");
        Preferences preferencesNode = topNode.node("concurrencytest");
        return preferencesNode;
    }

    private Command createNewCommand(Object obj) {
        GenericCommand cmd = new GenericCommand();
        cmd.setCommandType(EchoCommand.COMMAND_TYPE);
        cmd.setParameterValue(EchoCommand.PARAM_MESSAGE.getName(), obj);
        cmd.setCommandInResponse(true); // need this for our callback to test the retry count in cmd config
        return cmd;
    }

    private class TestCommandResponseCallback implements CommandResponseCallback {
        private static final long serialVersionUID = 1L;
        public CommandResponse response = null; // public so tests can get to it easily

        public void commandSent(CommandResponse response) {
            this.response = response;
            synchronized (this) {
                this.notify();
            }
        }
    }

    private class NotSerializable {
    }
}