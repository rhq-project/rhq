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
package org.rhq.enterprise.agent;

import java.io.IOException;
import java.io.NotSerializableException;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.MarshalException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.Test;

import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.InvokerLocator;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderStateListener;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.command.client.CommandResponseCallback;
import org.rhq.enterprise.communications.command.client.RemotePojoInvocationFuture;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommandResponse;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryException;

/**
 * This tests the communications layer in the agent.
 *
 * @author John Mazzitelli
 */
@Test(groups = "agent-comm")
public class AgentComm3Test extends AgentCommTestBase {
    private static final boolean ENABLE_TESTS = true;

    /**
     * Tests the command listener framework and that an agent will start its sender once it receives a command from the
     * server.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testCommandListenerAgentSenderStart() throws Exception {
        // make it so the server auto-detection features do not detect anything
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.MULTICASTDETECTOR_PORT, "17777");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.MULTICASTDETECTOR_PORT, "18888");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        m_agent1Test.setConfigurationOverrides(props1);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false); // don't start its sender yet
        agent1.start();

        AgentMain agent2 = m_agent2Test.createAgent(true); // we can start its sender now, we'll use it to send agent1 a command

        try {
            Thread.sleep(5000);
            assert agent1.getClientCommandSender().isSending() : "connectAgent should have triggered our listener and started the sender";
            assert agent2.getClientCommandSender().isSending() : "Should have already started the sender";

            // our canary-in-the-mine is the sending mode, the sender goes into sending mode when it detects the server
            IdentifyCommand command = new IdentifyCommand();
            CommandResponse response;

            response = agent2.getClientCommandSender().sendSynch(command);
            assert response.isSuccessful() : "agent2 should have been able to send to agent1: " + response;
            assert agent1.getClientCommandSender().isSending() : "agent2 message should have started agent1 sender";

            // now let's stop the agent1 sender and try again, just to make sure the command listeners work repeatedly
            agent1.getClientCommandSender().stopSending(false);
            assert !agent1.getClientCommandSender().isSending() : "Should not be in sending mode again";
            assert agent2.getClientCommandSender().isSending() : "Should still be started";

            response = agent2.getClientCommandSender().sendSynch(command);
            assert response.isSuccessful() : "agent2 should have been able to send to agent1: " + response;
            assert agent1.getClientCommandSender().isSending() : "agent2 message should have re-started agent1 sender again";

            // now let's entirely shutdown agent1 and try again, just to make sure the command listeners can be rebuilt
            agent1.shutdown();
            assert agent1.getClientCommandSender() == null : "agent1 should be completely shutdown";
            assert agent2.getClientCommandSender().isSending() : "Should still be started";

            agent1.start();
            assert agent1.getClientCommandSender() != null : "agent1 should be started again";
            assert !agent1.getClientCommandSender().isSending() : "Should still not yet be in sending mode again";
            assert agent2.getClientCommandSender().isSending() : "Should still be started";

            response = agent2.getClientCommandSender().sendSynch(command);
            assert response.isSuccessful() : "agent2 should have been able to send to agent1: " + response;
            assert agent1.getClientCommandSender().isSending() : "agent2 message should have re-started agent1 sender again";
        } finally {
            if (agent1 != null) {
                agent1.shutdown();
            }

            if (agent2 != null) {
                agent2.shutdown();
            }
        }

        return;
    }

    /**
     * Tests a remote POJO invocation that throws declared and undeclared exceptions.
     *
     * @throws Throwable
     */
    @Test(enabled = ENABLE_TESTS)
    public void testRemotePojoExceptions() throws Throwable {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestAnnotatedPojo.class.getName()
            + ':' + ITestAnnotatedPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        ITestAnnotatedPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestAnnotatedPojo.class);

        try {
            // AutoDiscoveryException matches the throws clause - will not be wrapped
            pojo.throwSpecificException(new AutoDiscoveryException("should not be wrapped"));
        } catch (AutoDiscoveryException expected) {
            assert expected.getMessage().equals("should not be wrapped");
        }

        try {
            // IOException matches the throws clause - will not be wrapped
            pojo.throwSpecificException(new IOException("should not be wrapped"));
        } catch (IOException expected) {
            assert expected.getMessage().equals("should not be wrapped");
        }

        try {
            // IllegalArgumentException doesn't match throws but is a java.* runtime exception - will not be wrapped
            pojo.throwSpecificException(new IllegalArgumentException("should not be wrapped"));
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().equals("should not be wrapped");
        }

        try {
            // IllegalArgumentException doesn't match throws but it and its causes are java.* exceptions - will not be wrapped
            pojo.throwSpecificException(new IllegalArgumentException("should not be wrapped", new Exception("inner 1",
                new IllegalStateException("inner 2"))));
        } catch (IllegalArgumentException expected) {
            assert expected.getMessage().equals("should not be wrapped");
            assert expected.getCause() instanceof Exception;
            assert expected.getCause().getMessage().equals("inner 1");
            assert expected.getCause().getCause() instanceof IllegalStateException;
            assert expected.getCause().getCause().getMessage().equals("inner 2");
        }

        try {
            // AutoDiscoveryException match throws so we assume the developer knows what he is doing and only
            // includes causes within it that are allowed to be sent over the wire - it is not wrapped
            pojo.throwSpecificException(new AutoDiscoveryException("should not be wrapped", new CannotConnectException(
                "inner exception")));
        } catch (AutoDiscoveryException expected) {
            assert expected.getMessage().equals("should not be wrapped");
            assert expected.getCause() instanceof CannotConnectException;
            assert expected.getCause().getMessage().equals("inner exception");
        }

        try {
            // IllegalArgumentException doesn't match throws but is a java.* runtime exception - however, its cause
            // is a non-java.* exception - it should be wrapped entirely
            pojo.throwSpecificException(new IllegalArgumentException("should be wrapped", new CannotConnectException(
                "inner exception")));
        } catch (WrappedRemotingException expected) {
            assert expected.getMessage().indexOf("should be wrapped") != -1;
            assert expected.getCause().getMessage().indexOf("inner exception") != -1;
            assert ((WrappedRemotingException) expected.getCause()).getActualException().getExceptionName().equals(
                CannotConnectException.class.getName());
        }

        try {
            // doesn't match throws and is not a java.* exception - will be wrapped
            pojo.throwSpecificException(new CannotConnectException("should be wrapped"));
        } catch (WrappedRemotingException expected) {
            assert expected.getMessage().indexOf("should be wrapped") != -1;
            assert expected.getActualException().getExceptionName().equals(CannotConnectException.class.getName());
        }

        try {
            pojo.returnNonSerializableObject(new Thread()); // opps, Thread is not serializable, never even gets sent over the wire
        } catch (UndeclaredThrowableException expected) {
            assert expected.getCause().getClass().getName().equals(MarshalException.class.getName());
            assert expected.getCause().getCause().getClass().getName().equals(NotSerializableException.class.getName());
        } catch (Throwable t) {
            assert false : "Unexpected exception: " + ThrowableUtil.getAllMessages(t); // this is here so I can put a breakpoint and see what exceptions are thrown
        }

        try {
            pojo.throwNonSerializableException();
        } catch (WrappedRemotingException expected) {
            assert expected.getActualException().getExceptionName() != null;
        } catch (Throwable t) {
            t.printStackTrace();
            assert false : "Unexpected exception: " + ThrowableUtil.getAllMessages(t); // this is here so I can put a breakpoint and see what exceptions are thrown
        }

        return;
    }

    /**
     * Tests client command sender state listeners.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testClientCommandSenderStateListener() throws Exception {
        AgentMain agent1 = m_agent1Test.createAgent(true);
        m_agent2Test.createAgent(true);

        final Boolean[] state = new Boolean[1];
        ClientCommandSenderStateListener listener = new ClientCommandSenderStateListener() {
            public boolean stoppedSending(ClientCommandSender sender) {
                state[0] = Boolean.FALSE;
                return true;
            }

            public boolean startedSending(ClientCommandSender sender) {
                state[0] = Boolean.TRUE;
                return true;
            }
        };

        assert state[0] == null : "This was just to prove we are null at the start - very weird for this to fail";
        assert agent1.getClientCommandSender().isSending() : "Setup of this test should have started the sender";

        agent1.getClientCommandSender().addStateListener(listener, false);
        assert state[0] == null : "We told it not to notify us immediately - should not have called the listener";
        agent1.getClientCommandSender().removeStateListener(listener);

        agent1.getClientCommandSender().addStateListener(listener, true);
        assert state[0].booleanValue() : "Listener should have been told the sender is sending";
        agent1.getClientCommandSender().stopSending(false);
        assert !state[0].booleanValue() : "Listener should have told us we are no longer sending";
        agent1.getClientCommandSender().startSending();
        assert state[0].booleanValue() : "Listener should have told us we are sending again";
        agent1.getClientCommandSender().removeStateListener(listener);

        ClientCommandSenderStateListener one_time_listener = new ClientCommandSenderStateListener() {
            public boolean stoppedSending(ClientCommandSender sender) {
                state[0] = Boolean.FALSE;
                return false;
            }

            public boolean startedSending(ClientCommandSender sender) {
                state[0] = Boolean.TRUE;
                return false;
            }
        };

        state[0] = null;
        agent1.getClientCommandSender().addStateListener(one_time_listener, true);
        assert state[0].booleanValue() : "One-time listener should have been told the sender is sending";
        agent1.getClientCommandSender().stopSending(false);
        assert state[0].booleanValue() : "One-time listener should not have been told the sender is not sending";

        agent1.getClientCommandSender().addStateListener(one_time_listener, true);
        assert !state[0].booleanValue() : "One-time listener should have been told the sender is not sending";
        agent1.getClientCommandSender().startSending();
        assert !state[0].booleanValue() : "One-time listener should not have been told the sender is sending";

        return;
    }

    /**
     * Tests getting results from asynch remote pojo invocation using the invocation future object.
     *
     * @throws Throwable
     */
    @Test(enabled = ENABLE_TESTS)
    public void testRemotePojoInvocationFuture() throws Throwable {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestAnnotatedPojo.class.getName()
            + ':' + ITestAnnotatedPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        RemotePojoInvocationFuture future = new RemotePojoInvocationFuture();
        ClientRemotePojoFactory factory = agent1.getClientCommandSender().getClientRemotePojoFactory();
        factory.setAsynch(false, future); // false should still honor annotations - we'll make sure that's true in this test
        ITestAnnotatedPojo pojo = factory.getRemotePojo(ITestAnnotatedPojo.class);

        pojo.volatileMethod("first test");
        assert "first test".equals(future.get());
        assert future.isDone();

        long stopwatch = System.currentTimeMillis();
        assert "first test".equals(future.get(10, TimeUnit.SECONDS)); // make sure its still there
        long test_duration = System.currentTimeMillis() - stopwatch;
        assert test_duration < 750L : "get should have returned immediately: " + test_duration;

        assert future.isDone();
        future.reset();
        assert !future.isDone();

        stopwatch = System.currentTimeMillis();
        try {
            future.get(2, TimeUnit.SECONDS);
            assert false : "The get should have timed out";
        } catch (TimeoutException toe) {
        }

        test_duration = System.currentTimeMillis() - stopwatch;
        assert (test_duration > 1900L) && (test_duration < 2500L) : "Should have timed out after 2 seconds: "
            + test_duration;

        // we want to call throwThrowable asynchronously but it isn't annotated that way, so force async
        // calling this isn't enough - all existing proxies remain as-is - this call only affects newly created remote pojo proxies
        factory.setAsynch(true, future);
        factory.setIgnoreAnnotations(true);

        // test throwing an Error
        Error err = new Error("bogus error for testing");

        // side-test - show that the proxy isn't forcing all methods to be async.
        try {
            pojo.throwThrowable(err);
            assert false : "Should have called this synchronously which should have thrown Error";
        } catch (Error error) {
            // to be expected, the remote pojo proxy is still configured to call throwThrowable synchronously
        }

        // now let's get a new remote pojo proxy that is forced to call everything asynchronously
        pojo = factory.getRemotePojo(ITestAnnotatedPojo.class);
        pojo.throwThrowable(err);

        try {
            future.get();
            assert false : "Should have thrown an exception";
        } catch (ExecutionException ee) {
            assert "bogus error for testing".equals(ee.getCause().getMessage());
            assert ee.getCause() instanceof Error;
        }

        // test throwing an Error
        future.reset();
        pojo.throwThrowable(new RuntimeException("bogus runtime exception for testing"));

        try {
            future.getAndReset();
            assert false : "Should have thrown an exception";
        } catch (ExecutionException ee) {
            assert "bogus runtime exception for testing".equals(ee.getCause().getMessage());
            assert ee.getCause() instanceof RuntimeException;
        }

        return;
    }

    /**
     * Tests multiple command preprocessors.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testMultipleCommandPreprocessors() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName() + ":" + SimpleCounterCommandPreprocessor.class.getName());
        m_agent1Test.setConfigurationOverrides(props1);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        m_agent2Test.createAgent(true);

        Thread.sleep(2000L); // wait for the initial connectAgent messages to get sent

        assert SimpleCounterCommandPreprocessor.COUNTER.get() == 1L : "Counter should have been reset but equal to 1 due to the agent's initial connectAgent message";
        agent1.getClientCommandSender().sendSynch(new IdentifyCommand());
        assert SimpleCounterCommandPreprocessor.COUNTER.get() == 2L : "Should have counted 2 commands (including the auto-connectAgent at startup)";
        agent1.getClientCommandSender().sendSynch(new IdentifyCommand());
        assert SimpleCounterCommandPreprocessor.COUNTER.get() == 3L : "Should have counted 3 commands (including the auto-connectAgent at startup)";
        agent1.getClientCommandSender().sendSynch(new IdentifyCommand());
        assert SimpleCounterCommandPreprocessor.COUNTER.get() == 4L : "Should have counted 4 commands (including the auto-connectAgent at startup)";

        return;
    }

    /**
     * Sends a message from one remote server to another using command preprocessor/authenticator to perform security
     * checks.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testCommandPreprocessorAuthenticator() throws Exception {
        // agent1 has both preprocessor and authenticator so it can send and receive securely
        // agent2 only has the authenticator - no preprocessor so when it sends to agent1 it won't be authenticatable
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName());
        props1.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS, "");
        props2.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        IdentifyCommandResponse response;

        response = (IdentifyCommandResponse) agent1.getClientCommandSender().sendSynch(new IdentifyCommand());
        assert response.isSuccessful() : "Failed to send command from agent1 to agent2";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        CommandResponse generic_response;
        generic_response = agent2.getClientCommandSender().sendSynch(new IdentifyCommand());
        assert !generic_response.isSuccessful() : "Should have failed to send command from agent2 to agent1 since it didn't preprocess the command";
        assert generic_response.getException() != null;

        return;
    }

    /**
     * Tests a remote POJO interface with send throttling annotation.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testRemotePojoAnnotationsSendThrottled() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SEND_THROTTLING, "5:7000");
        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestAnnotatedPojo.class.getName()
            + ':' + ITestAnnotatedPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        ITestAnnotatedPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestAnnotatedPojo.class);

        // blast 6 calls in a row - we will throttle after the 5th, thus pausing for 7 seconds
        long stopwatch = System.currentTimeMillis();
        for (int i = 1; i <= 6; i++) {
            assert i == pojo.sendThrottled(i);
        }

        long test_duration = System.currentTimeMillis() - stopwatch;
        assert test_duration > 6950L : "Send throttling was on - should have a pause: " + test_duration;

        // test send throttling that has been explicitly turned off but our agent is configured for send throttling
        stopwatch = System.currentTimeMillis();
        for (int i = 1; i <= 6; i++) {
            assert i == pojo.notSendThrottled(i);
        }

        test_duration = System.currentTimeMillis() - stopwatch;
        assert test_duration < 7000L : "Send throttling was off - should have returned very fast: " + test_duration;

        // let's ignore annotations and try that one that wanted to explicitly turn off send throttling
        // since we are ignoring the annotations, our invocation will be send throttled
        ClientRemotePojoFactory factory = agent1.getClientCommandSender().getClientRemotePojoFactory();
        factory.setIgnoreAnnotations(true);
        pojo = factory.getRemotePojo(ITestAnnotatedPojo.class);

        stopwatch = System.currentTimeMillis();
        for (int i = 1; i <= 6; i++) {
            assert i == pojo.notSendThrottled(i);
        }

        test_duration = System.currentTimeMillis() - stopwatch;
        assert test_duration > 6950L : "Annotations were ignored, should have been send throttled: " + test_duration;

        return;
    }

    /**
     * Tests a remote POJO interface with timeout annotation.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testRemotePojoAnnotationsTimeout() throws Exception {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestAnnotatedPojo.class.getName()
            + ':' + ITestAnnotatedPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        ITestAnnotatedPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestAnnotatedPojo.class);

        boolean exception_was_thrown = false;
        try {
            pojo.shortAnnotatedTimeout("first");
        } catch (Throwable t) {
            exception_was_thrown = true;
        }

        assert exception_was_thrown : "The method should have timed out and thrown an exception";

        ClientRemotePojoFactory factory = agent1.getClientCommandSender().getClientRemotePojoFactory();
        factory.setIgnoreAnnotations(true);
        pojo = factory.getRemotePojo(ITestAnnotatedPojo.class);

        // since we are ignoring the annotations, our default timeout should be enough for us to succeed
        assert "second".equals(pojo.shortAnnotatedTimeout("second"));

        return;
    }

    /**
     * Tests guaranteed delivery with a remote POJO interface with annotations.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testRemotePojoAnnotationsGuaranteedDelivery() throws Exception {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestAnnotatedPojo.class.getName()
            + ':' + ITestAnnotatedPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        // set timeout so the auto-connectAgent times out quick 
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_TIMEOUT, "5000");
        m_agent1Test.setConfigurationOverrides(props1);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        assert agent1.isStarted() : "agent1 should have been started";

        final String[] results = new String[] { null };
        final Throwable[] throwable = new Throwable[] { null };
        final Boolean[] success = new Boolean[] { null };
        ClientRemotePojoFactory factory = agent1.getClientCommandSender().getClientRemotePojoFactory();
        factory.setAsynch(false, new CommandResponseCallback() {
            private static final long serialVersionUID = 1L;

            public void commandSent(CommandResponse response) {
                success[0] = Boolean.valueOf(response.isSuccessful());
                results[0] = (String) response.getResults();
                throwable[0] = response.getException();
                synchronized (success) {
                    success.notify();
                }
            }
        });

        ITestAnnotatedPojo pojo = factory.getRemotePojo(ITestAnnotatedPojo.class);

        // see that the volatile method failed because we haven't started our second server agent
        pojo.volatileMethod("call 1");
        synchronized (success) {
            success.wait(30000L); // this will get notified immediately when our volatile method callback is called with the failure
        }

        assert !success[0].booleanValue() : "The volatile method should have failed - there is no remote endpoint to service it";
        assert throwable[0] != null : "The volatile method should have failed - there is no remote endpoint to service it";
        assert results[0] == null : "The volatile method should have failed - there is no remote endpoint to service it";
        throwable[0] = null;
        success[0] = null;
        results[0] = null;

        // make a guaranteed method call - it should get persisted waiting for us to start our second remote endpoint
        pojo.guaranteedMethod("call 2");
        Thread.sleep(5000);
        assert success[0] == null : "The method invocation should not have been performed yet - it should be persisted";

        // start a second agent to be used as our server-side remote endpoint
        synchronized (success) {
            AgentMain agent2 = m_agent2Test.createAgent(true);
            assert agent2.isStarted() : "agent2 should have been started";

            // see that our guaranteed method invocation worked
            success.wait(30000L); // this will get notified immediately when our guaranteed method callback is called with the success
        }

        assert success[0].booleanValue() : "The guaranteed method should have succeeded - there is now a remote endpoint to service it";
        assert throwable[0] == null : "The guaranteed method should have succeeded - there is now a remote endpoint to service it";
        assert "call 2".equals(results[0]) : "The guaranteed method should have succeeded - there is now a remote endpoint to service it";
        throwable[0] = null;
        success[0] = null;
        results[0] = null;

        // for good measure, show that our volatile, non-guaranteed method will work too
        pojo.volatileMethod("call 3");
        synchronized (success) {
            success.wait(30000L); // this will get notified immediately when our volatile method callback is called with the success
        }

        assert success[0].booleanValue() : "The volatile method should have succeeded - there is now a remote endpoint to service it";
        assert throwable[0] == null : "The volatile method should have succeeded - there is now a remote endpoint to service it";
        assert "call 3".equals(results[0]) : "The volatile method should have succeeded - there is now a remote endpoint to service it";

        return;
    }
}