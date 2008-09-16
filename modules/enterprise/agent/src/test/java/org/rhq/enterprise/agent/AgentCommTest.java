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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.MarshalException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.enterprise.communications.ServiceContainerConfiguration;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.ServiceContainerMetricsMBean;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderMetrics;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderStateListener;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.command.client.CommandResponseCallback;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemotePojoInvocationFuture;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommand;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommandResponse;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryException;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryListener;
import org.rhq.enterprise.communications.util.SecurityUtil;

/**
 * This tests the communications layer in the agent.
 *
 * @author John Mazzitelli
 */
@Test(groups = "agent-comm")
public class AgentCommTest {
    private static final boolean ENABLE_TESTS = true;

    private static final int LARGE_STRING_SIZE = 100000;
    private static final String LARGE_STRING;
    private static final byte[] LARGE_STRING_BYTES;

    static {
        StringBuffer stream_data_buf = new StringBuffer(LARGE_STRING_SIZE);

        for (int i = 0; i < LARGE_STRING_SIZE; i += 10) {
            stream_data_buf.append(".123456789");
        }

        LARGE_STRING = stream_data_buf.toString();
        LARGE_STRING_BYTES = LARGE_STRING.getBytes();
    }

    private File m_veryLargeFile = null;

    private AgentTestClass m_agent1Test;
    private AgentTestClass m_agent2Test;

    /**
     * Creates new agent test classes.
     *
     * @throws Exception
     */
    @BeforeMethod
    public void setUp() throws Exception {
        m_agent1Test = new AgentTestClass();
        m_agent1Test.setConfigurationFile("test-agent-configuration.xml", "test");

        m_agent2Test = new AgentTestClass();
        m_agent2Test.setConfigurationFile("test-agent2-configuration.xml", "test2");

        // make sure we create keystore files - some tests needs both existing at the same time as soon as they begin
        File keystore_file1 = new File("target/testdata/keystore.dat");

        if (!keystore_file1.exists()) {
            keystore_file1.getParentFile().mkdirs();
            m_agent1Test.createAgent(false);
            AgentConfiguration agent_config1 = m_agent1Test.getAgent().getConfiguration();
            ServiceContainerConfiguration server_config1 = agent_config1.getServiceContainerPreferences();

            SecurityUtil.createKeyStore(server_config1.getConnectorSecurityKeystoreFile(), server_config1
                .getConnectorSecurityKeystoreAlias(), "CN=JBoss ON Communications Server, OU=JBoss, O=jboss.com, C=US",
                server_config1.getConnectorSecurityKeystorePassword(), server_config1
                    .getConnectorSecurityKeystoreKeyPassword(), "DSA", 36500);
        }

        File keystore_file2 = new File("target/testdata2/keystore.dat");
        if (!keystore_file2.exists()) {
            keystore_file2.getParentFile().mkdirs();
            m_agent2Test.createAgent(false);
            AgentConfiguration agent_config2 = m_agent2Test.getAgent().getConfiguration();
            ServiceContainerConfiguration server_config2 = agent_config2.getServiceContainerPreferences();

            SecurityUtil.createKeyStore(server_config2.getConnectorSecurityKeystoreFile(), server_config2
                .getConnectorSecurityKeystoreAlias(), "CN=JBoss ON Communications Server, OU=JBoss, O=jboss.com, C=US",
                server_config2.getConnectorSecurityKeystorePassword(), server_config2
                    .getConnectorSecurityKeystoreKeyPassword(), "DSA", 36500);
        }
    }

    /**
     * Ensures any agent that was started is shutdown and all configuration is cleared so as not to retain overridden
     * preferences left over by the tests.
     */
    @AfterMethod
    public void tearDown() {
        if (m_agent1Test != null) {
            AgentMain agent = m_agent1Test.getAgent();
            if (agent != null) {
                agent.shutdown();
                m_agent1Test.clearAgentConfiguration();
                m_agent1Test.cleanUpFiles();
            }
        }

        if (m_agent2Test != null) {
            AgentMain agent = m_agent2Test.getAgent();
            if (agent != null) {
                agent.shutdown();
                m_agent2Test.clearAgentConfiguration();
                m_agent2Test.cleanUpFiles();
            }
        }

        // Because overrides (-D arguments to AgentMain) also set System properties, you can't run multiple agents in a single VM
        // when you override arguments that are NOT to be the same on all agents in the VM (like bind port).  You can override
        // things like queue size and other params that can be the same across all agents in the VM.
        // Here, I just clear all system properties that any test might have set so we don't run into this problem.
        Properties sysprops = System.getProperties();
        Properties to_remove = new Properties();
        for (Iterator iter = sysprops.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (((String) entry.getKey()).startsWith(ServiceContainerConfigurationConstants.PROPERTY_NAME_PREFIX)
                || ((String) entry.getKey()).startsWith(AgentConfigurationConstants.PROPERTY_NAME_PREFIX)) {
                to_remove.put(entry.getKey(), entry.getValue());
            }
        }

        for (Iterator iter = to_remove.keySet().iterator(); iter.hasNext();) {
            System.getProperties().remove(iter.next());
        }

        return;
    }

    /**
     * Cleans up after all the tests in this class have executed.
     */
    @AfterClass
    public void cleanUp() {
        if (m_veryLargeFile != null) {
            m_veryLargeFile.delete();
        }
    }

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
            assert !agent1.getClientCommandSender().isSending() : "Should not be in sending mode yet";
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
     * Tests the data streaming feature when remotely accessing a POJO with preprocessor/authenticator installed.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataToPojoWithPreprocessorAuthenticator() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName());
        props1.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName());
        props2.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestStreamPojo.class.getName()
            + ':' + ITestStreamPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data = "Stream this string to a POJO...";
        InputStream in = new ByteArrayInputStream(stream_data.getBytes());
        Long stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
        InputStream remote_stream = new RemoteInputStream(stream_id, agent1.getServiceContainer());
        ITestStreamPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestStreamPojo.class);

        // make a call where the stream is the only argument
        String results;
        try {
            results = pojo.streamData(remote_stream);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert stream_data.equals(results) : "The test should have sent the stream data back in the response";

        return;
    }

    /**
     * Tests the data streaming feature when remotely accessing a POJO with preprocessor/authenticator installed and
     * using SSL.
     *
     * @throws Exception
     */
    @Test(enabled = false)
    // WHY IS THIS FAILING?
    public void testAgentStreamingDataToPojoWithPreprocessorAuthenticatorOverSSL() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName());
        props1.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName());
        props2.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestStreamPojo.class.getName()
            + ':' + ITestStreamPojo.class.getName());

        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data = "Stream this string to a POJO...";
        InputStream in = new ByteArrayInputStream(stream_data.getBytes());
        Long stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
        InputStream remote_stream = new RemoteInputStream(stream_id, agent1.getServiceContainer());
        ITestStreamPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestStreamPojo.class);

        // make a call where the stream is the only argument
        String results;
        try {
            results = pojo.streamData(remote_stream);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert stream_data.equals(results) : "The test should have sent the stream data back in the response";

        return;
    }

    /**
     * Tests the data streaming feature when remotely accessing a POJO with preprocessor/authenticator installed.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataToPojoWithInvalidPreprocessorAuthenticator() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName());
        props1.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        props1.setProperty(ServiceContainerConfigurationConstants.REMOTE_STREAM_MAX_IDLE_TIME, "2000");
        m_agent1Test.setConfigurationOverrides(props1);

        // there is no preprocessor here so when it tries to call back to agent1 to get the stream data it will fail
        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS, "");
        props2.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestStreamPojo.class.getName()
            + ':' + ITestStreamPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data = "Stream this string to a POJO...";
        InputStream in = new ByteArrayInputStream(stream_data.getBytes());
        Long stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
        InputStream remote_stream = new RemoteInputStream(stream_id, agent1.getServiceContainer());
        ITestStreamPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestStreamPojo.class);

        // make a call where the stream is the only argument
        boolean exception_occurred = false;
        try {
            pojo.streamData(remote_stream);
        } catch (Throwable t) {
            exception_occurred = true;
        }

        assert exception_occurred : "Should have failed to stream from the POJO - agent1 should have failed the authentication check";

        // let's wait for the idle timer task to run and make sure it removes the stream
        // this is an important test - if a client fails to come ask for the stream and fails to close it, we need to make sure
        // these unused streams eventually get cleaned up.  Our timeout is 2sec but the reaper runs every 5secs
        Thread.sleep(5250L);
        assert !agent1.getServiceContainer().removeRemoteInputStream(stream_id) : "This should be false because the stream should have been removed by the task already";

        return;
    }

    /**
     * Tests the data streaming feature with preprocessor/authenticator installed but one agent is configured such that
     * it will fail to authenticate properly
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataWithPreprocessorAuthenticator() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName());
        props1.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_COMMAND_PREPROCESSORS,
            SimpleCommandPreprocessor.class.getName());
        props2.setProperty(ServiceContainerConfigurationConstants.COMMAND_AUTHENTICATOR,
            SimpleCommandAuthenticator.class.getName());
        props2.setProperty(ServiceContainerConfigurationConstants.CMDSERVICES, SimpleTestStreamService.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data = "Stream this string...";
        InputStream in = new ByteArrayInputStream(stream_data.getBytes());
        GenericCommand test_command = new GenericCommand(SimpleTestStreamService.COMMAND_TYPE, null);
        CommandResponse results = null;
        Long stream_id = null;

        try {
            stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
            test_command.setParameterValue(SimpleTestStreamService.INPUT_STREAM_PARAM, new RemoteInputStream(stream_id,
                agent1.getServiceContainer()));

            results = agent1.getClientCommandSender().sendSynch(test_command);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert stream_data.equals(results.getResults()) : "The test should have sent the stream data back in the response";

        // let's wait for the idle timer task to run and make sure it removes the stream now that we have closed it
        Thread.sleep(5250L);
        assert !agent1.getServiceContainer().removeRemoteInputStream(stream_id) : "This should be false because the stream should have been removed by the task already";

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

    /**
     * Tests the data streaming feature where the stream times out before a command could be sent to it.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataTimeout() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(ServiceContainerConfigurationConstants.REMOTE_STREAM_MAX_IDLE_TIME, "1000");
        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.CMDSERVICES, SimpleTestStreamService.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data = "Streaming this string should fail";
        InputStream in = new ByteArrayInputStream(stream_data.getBytes());
        GenericCommand test_command = new GenericCommand(SimpleTestStreamService.COMMAND_TYPE, null);
        CommandResponse results = null;

        try {
            test_command.setParameterValue(SimpleTestStreamService.INPUT_STREAM_PARAM, new RemoteInputStream(in, agent1
                .getServiceContainer()));
            Thread.sleep(5500L); // the timer only runs the task every 5 secs even though our timeout is 1 sec
            results = agent1.getClientCommandSender().sendSynch(test_command);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert !results.isSuccessful() : "The test should not have sent the stream data back in the response - the stream service should have timed out";

        return;
    }

    /**
     * Tests the data streaming feature.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingData() throws Exception {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.CMDSERVICES, SimpleTestStreamService.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data = "Stream this string...";
        InputStream in = new ByteArrayInputStream(stream_data.getBytes());
        GenericCommand test_command = new GenericCommand(SimpleTestStreamService.COMMAND_TYPE, null);
        CommandResponse results = null;
        Long stream_id = null;

        try {
            stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
            test_command.setParameterValue(SimpleTestStreamService.INPUT_STREAM_PARAM, new RemoteInputStream(stream_id,
                agent1.getServiceContainer()));

            results = agent1.getClientCommandSender().sendSynch(test_command);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert stream_data.equals(results.getResults()) : "The test should have sent the stream data back in the response";

        // let's wait for the idle timer task to run and make sure it removes the stream now that we have closed it
        Thread.sleep(5250L);
        assert !agent1.getServiceContainer().removeRemoteInputStream(stream_id) : "This should be false because the stream should have been removed by the task already";

        return;
    }

    /**
     * Tests the data streaming feature when remotely accessing a POJO.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataToPojo() throws Exception {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestStreamPojo.class.getName()
            + ':' + ITestStreamPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data = "Stream this string to a POJO...";
        InputStream in = new ByteArrayInputStream(stream_data.getBytes());
        Long stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
        InputStream remote_stream = new RemoteInputStream(stream_id, agent1.getServiceContainer());
        ITestStreamPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestStreamPojo.class);

        // make a call where the stream is the only argument
        String results;
        try {
            results = pojo.streamData(remote_stream);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert stream_data.equals(results) : "The test should have sent the stream data back in the response";

        return;
    }

    /**
     * Tests the data streaming feature when remotely accessing a POJO passing in more than one input stream. This tests
     * that multiple streams can be served up concurrently.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataToPojoMultipleStreams() throws Exception {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestStreamPojo.class.getName()
            + ':' + ITestStreamPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data1 = "FIRST Stream this string to a POJO... 1111111111";
        InputStream in1 = new ByteArrayInputStream(stream_data1.getBytes());
        Long stream_id1 = agent1.getServiceContainer().addRemoteInputStream(in1);
        InputStream remote_stream1 = new RemoteInputStream(stream_id1, agent1.getServiceContainer());

        String stream_data2 = "TWO Stream this string to a POJO... 2222222222";
        InputStream in2 = new ByteArrayInputStream(stream_data2.getBytes());
        Long stream_id2 = agent1.getServiceContainer().addRemoteInputStream(in2);
        InputStream remote_stream2 = new RemoteInputStream(stream_id2, agent1.getServiceContainer());

        ITestStreamPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestStreamPojo.class);

        String[] results;
        try {
            results = pojo.streamData(remote_stream1, remote_stream2);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert stream_data1.equals(results[0]) : "The test should have sent the #1 stream data back in the response";
        assert stream_data2.equals(results[1]) : "The test should have sent the #2 stream data back in the response";

        // let's wait for the idle timer task to run and make sure it removes the streams now that we have closed them
        Thread.sleep(5250L);
        assert !agent1.getServiceContainer().removeRemoteInputStream(stream_id1) : "This should be false because the stream1 should have been removed by the task already";
        assert !agent1.getServiceContainer().removeRemoteInputStream(stream_id2) : "This should be false because the stream2 should have been removed by the task already";

        return;
    }

    /**
     * Tests the data streaming feature over SSL via a POJO where the POJO method signature includes other arguments in
     * addition to the stream itself.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataToPojoOverSSL() throws Exception {
        // set up the SSL configuration.
        // note a side test - see that the internal remote pojo service is created even with dynamic discovery turned off
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        props1.setProperty(ServiceContainerConfigurationConstants.CMDSERVICE_DIRECTORY_DYNAMIC_DISCOVERY, "false");

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestStreamPojo.class.getName()
            + ':' + ITestStreamPojo.class.getName());
        props2.setProperty(ServiceContainerConfigurationConstants.CMDSERVICE_DIRECTORY_DYNAMIC_DISCOVERY, "false");

        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        String stream_data = "Stream this string to a POJO over SSL...";
        InputStream in = new ByteArrayInputStream(stream_data.getBytes());
        Long stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
        InputStream remote_stream = new RemoteInputStream(stream_id, agent1.getServiceContainer());
        ITestStreamPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestStreamPojo.class);

        // make a call where the stream is a middle argument
        String results;

        try {
            results = pojo.streamData("abc", 1, remote_stream, "xyz");
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert (stream_data + "abc" + 1 + "xyz").equals(results) : "The test should have sent the stream data back in the response";

        return;
    }

    /**
     * Tests the data streaming feature where there is a large amount of data to stream.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataBigData() throws Exception {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.CMDSERVICES, SimpleTestStreamService.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        InputStream in = new ByteArrayInputStream(LARGE_STRING_BYTES);
        GenericCommand test_command = new GenericCommand(SimpleTestStreamService.COMMAND_TYPE, null);
        CommandResponse results = null;
        Long stream_id = null;

        try {
            test_command.setParameterValue(SimpleTestStreamService.INPUT_STREAM_PARAM, new RemoteInputStream(in, agent1
                .getServiceContainer()));

            results = agent1.getClientCommandSender().sendSynch(test_command);
        } catch (Throwable t) {
            throw new Exception(ThrowableUtil.getAllMessages(t), t);
        }

        assert LARGE_STRING.equals(results.getResults()) : "The test should have sent the stream data back in the response";

        // let's wait for the idle timer task to run and make sure it removes the stream now that we have closed it
        Thread.sleep(5250L);
        assert !agent1.getServiceContainer().removeRemoteInputStream(stream_id) : "This should be false because the stream should have been removed by the task already";

        return;
    }

    /**
     * Tests the data streaming feature where there is a <b>very large</b> amount of data to stream.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataVeryBigData() throws Exception {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.CMDSERVICES, SimpleTestStreamService.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        InputStream in = new FileInputStream(createVeryLargeFile()); // creates a large file in tmp directory
        GenericCommand test_command = new GenericCommand(SimpleTestStreamService.COMMAND_TYPE, null);
        CommandResponse results = null;
        Long stream_id = null;

        try {
            test_command.setParameterValue(SimpleTestStreamService.RETURN_COUNT_ONLY_PARAM, "");
            test_command.setParameterValue(SimpleTestStreamService.INPUT_STREAM_PARAM, new RemoteInputStream(in, agent1
                .getServiceContainer()));

            results = agent1.getClientCommandSender().sendSynch(test_command);
        } catch (Throwable t) {
            throw new Exception(ThrowableUtil.getAllMessages(t), t);
        }

        long response_count = ((Long) results.getResults()).longValue();
        long true_count = m_veryLargeFile.length();
        assert response_count == true_count : "The test should have sent the correct size of the large file ("
            + true_count + "); instead it returned (" + response_count + ")";

        // let's wait for the idle timer task to run and make sure it removes the stream now that we have closed it
        Thread.sleep(5250L);
        assert !agent1.getServiceContainer().removeRemoteInputStream(stream_id) : "This should be false because the stream should have been removed by the task already";

        return;
    }

    /**
     * Tests the data streaming feature when remotely accessing a POJO where there is a large amount of data to stream.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataToPojoBigData() throws Exception {
        Properties props2 = new Properties();
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestStreamPojo.class.getName()
            + ':' + ITestStreamPojo.class.getName());
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        InputStream in = new ByteArrayInputStream(LARGE_STRING_BYTES);
        Long stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
        InputStream remote_stream = new RemoteInputStream(stream_id, agent1.getServiceContainer());
        ITestStreamPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestStreamPojo.class);

        // make a call where the stream is the only argument
        String results;
        try {
            results = pojo.streamData(remote_stream);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert LARGE_STRING.length() == results.length() : "The large string came back with a different length that what we sent";
        assert LARGE_STRING.equals(results) : "The test should have sent the stream data back in the response";

        return;
    }

    /**
     * Tests the data streaming feature over SSL via a POJO where there is a large amount of data to stream.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStreamingDataToPojoOverSSLBigData() throws Exception {
        // set up the SSL configuration.
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        props2.setProperty(ServiceContainerConfigurationConstants.REMOTE_POJOS, SimpleTestStreamPojo.class.getName()
            + ':' + ITestStreamPojo.class.getName());

        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        InputStream in = new ByteArrayInputStream(LARGE_STRING_BYTES);
        Long stream_id = agent1.getServiceContainer().addRemoteInputStream(in);
        InputStream remote_stream = new RemoteInputStream(stream_id, agent1.getServiceContainer());
        ITestStreamPojo pojo = agent1.getClientCommandSender().getClientRemotePojoFactory().getRemotePojo(
            ITestStreamPojo.class);

        // make a call where the stream is the only argument
        String results;
        try {
            results = pojo.streamData(remote_stream);
        } catch (Throwable t) {
            throw new Exception(t);
        }

        assert LARGE_STRING.length() == results.length() : "The large string came back with a different length that what we sent";
        assert LARGE_STRING.equals(results) : "The test should have sent the stream data back in the response";

        return;
    }

    /**
     * Starts and immediately stops the agents - showing that you can have more than one agent running on the box at any
     * one time.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStartStop() throws Exception {
        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        agent1.shutdown();
        agent2.shutdown();

        assert !agent1.isStarted() : "agent1 should have been shutdown";
        assert !agent2.isStarted() : "agent2 should have been shutdown";

        return;
    }

    /**
     * Checks that you can override configuration properties.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentConfigurationOverride() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "socket", "127.0.0.1", 22345, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "12345");
        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "socket", "127.0.0.1", 12345, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22345");
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        assert agent1.getServiceContainer().getConfiguration().getConnectorBindPort() == 12345;
        assert agent2.getServiceContainer().getConfiguration().getConnectorBindPort() == 22345;

        agent1.shutdown();
        agent2.shutdown();

        assert !agent1.isStarted() : "agent1 should have been shutdown";
        assert !agent2.isStarted() : "agent2 should have been shutdown";

        return;
    }

    /**
     * Sends a message from one remote server to another.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendMessage() throws Exception {
        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        IdentifyCommand command = new IdentifyCommand();
        IdentifyCommandResponse response;

        response = (IdentifyCommandResponse) agent1.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent1 to agent2";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        response = (IdentifyCommandResponse) agent2.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent2 to agent1";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent1
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * Sends messages from one remote server to another and makes sure the metrics are collected properly.
     *
     * @throws Exception
     */
    @Test(enabled = false)
    // WHY IS THIS FAILING?
    public void testMetrics() throws Exception {
        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        Thread.sleep(2000); // let's wait for the senders to start and send the connectAgent messages (which will fail)

        ServiceContainerMetricsMBean server_metrics1 = m_agent1Test.getServerMetrics();
        ServiceContainerMetricsMBean server_metrics2 = m_agent2Test.getServerMetrics();
        ClientCommandSenderMetrics client_metrics1 = m_agent1Test.getClientMetrics();
        ClientCommandSenderMetrics client_metrics2 = m_agent2Test.getClientMetrics();

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 0;
        assert client_metrics1.getNumberFailedCommandsSent() == 1; // the connectAgent at sender startup
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 0;
        assert client_metrics2.getNumberFailedCommandsSent() == 1; // the connectAgent at sender startup
        assert client_metrics2.getAverageExecutionTimeSent() == 0;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 0;
        assert server_metrics1.getNumberFailedCommandsReceived() == 1;
        assert server_metrics1.getNumberTotalCommandsReceived() == 1;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 0;
        assert server_metrics2.getNumberFailedCommandsReceived() == 1;
        assert server_metrics2.getNumberTotalCommandsReceived() == 1;
        assert server_metrics2.getAverageExecutionTimeReceived() == 0;

        IdentifyCommand command = new IdentifyCommand();
        IdentifyCommandResponse response;

        response = (IdentifyCommandResponse) agent1.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent1 to agent2";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 1;
        assert client_metrics1.getNumberFailedCommandsSent() == 1;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 0;
        assert client_metrics2.getNumberFailedCommandsSent() == 1;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 0;
        assert server_metrics1.getNumberFailedCommandsReceived() == 1;
        assert server_metrics1.getNumberTotalCommandsReceived() == 1;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 1;
        assert server_metrics2.getNumberFailedCommandsReceived() == 1;
        assert server_metrics2.getNumberTotalCommandsReceived() == 2;

        response = (IdentifyCommandResponse) agent1.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent1 to agent2";

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 2;
        assert client_metrics1.getNumberFailedCommandsSent() == 1;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 0;
        assert client_metrics2.getNumberFailedCommandsSent() == 1;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 0;
        assert server_metrics1.getNumberFailedCommandsReceived() == 1;
        assert server_metrics1.getNumberTotalCommandsReceived() == 1;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 2;
        assert server_metrics2.getNumberFailedCommandsReceived() == 1;
        assert server_metrics2.getNumberTotalCommandsReceived() == 3;

        response = (IdentifyCommandResponse) agent2.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent2 to agent1";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent1
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 2;
        assert client_metrics1.getNumberFailedCommandsSent() == 1;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 1;
        assert client_metrics2.getNumberFailedCommandsSent() == 1;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 1;
        assert server_metrics1.getNumberFailedCommandsReceived() == 1;
        assert server_metrics1.getNumberTotalCommandsReceived() == 2;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 2;
        assert server_metrics2.getNumberFailedCommandsReceived() == 1;
        assert server_metrics2.getNumberTotalCommandsReceived() == 3;

        response = (IdentifyCommandResponse) agent2.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent2 to agent1";

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 2;
        assert client_metrics1.getNumberFailedCommandsSent() == 1;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 2;
        assert client_metrics2.getNumberFailedCommandsSent() == 1;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 2;
        assert server_metrics1.getNumberFailedCommandsReceived() == 1;
        assert server_metrics1.getNumberTotalCommandsReceived() == 3;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 2;
        assert server_metrics2.getNumberFailedCommandsReceived() == 1;
        assert server_metrics2.getNumberTotalCommandsReceived() == 3;

        return;
    }

    /**
     * Tests auto-discovery listeners.
     *
     * @throws Exception
     */
    @Test(enabled = false)
    // WHY DOES THIS ALWAYS FAIL?
    public void testAutoDiscoveryListeners() throws Exception {
        System.out.println("testAutoDiscoveryListeners - WHY DOES THIS PERIODICALLY FAIL?");

        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "true");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "false");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        m_agent1Test.setConfigurationOverrides(props1);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false);
        agent1.start();

        final Boolean[] online = new Boolean[] { null };
        agent1.getServiceContainer().addDiscoveryListener(new AutoDiscoveryListener() {
            public void serverOnline(InvokerLocator locator) {
                online[0] = Boolean.TRUE;
            }

            public void serverOffline(InvokerLocator locator) {
                online[0] = Boolean.FALSE;
            }
        });

        AgentMain agent2 = null;
        try {
            // now start agent2 which creates the remoting server that agent1 should auto-detect
            agent2 = m_agent2Test.createAgent(true);

            for (int i = 0; (i < 20) && (online[0] == null); i++) {
                Thread.sleep(1000L);
            }

            assert online[0].booleanValue() : "The server running in agent2 was never auto-discovered by agent1";

            // now shutdown agent2 and see that we auto-discovery it going down
            agent2.shutdown();

            for (int i = 0; (i < 20) && (online[0].booleanValue()); i++) {
                Thread.sleep(1000L);
            }

            assert !online[0].booleanValue() : "The server running in agent2 went down but that was not auto-detected by agent1";
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
     * Tests auto-discovery.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAutoDiscovery() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "true");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "false");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        m_agent1Test.setConfigurationOverrides(props1);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false);
        agent1.start();

        AgentMain agent2 = null;
        try {
            assert !agent1.getClientCommandSender().isSending() : "Should not be in sending mode yet";

            // now start agent2 which creates the remoting server that agent1 should auto-detect
            agent2 = m_agent2Test.createAgent(true);

            // our canary-in-the-mine is the sending mode - when auto-discovery works, the sender goes into sending mode
            boolean discovered = false;
            for (int i = 0; (i < 20) && !discovered; i++) {
                Thread.sleep(1000L);
                discovered = agent1.getClientCommandSender().isSending();
            }

            assert discovered : "The server running in agent2 was never auto-discovered by agent1";

            // now shutdown agent2 and see that we auto-discovery it going down
            agent2.shutdown();

            boolean gone_down = false;
            for (int i = 0; (i < 20) && !gone_down; i++) {
                Thread.sleep(1000L);
                gone_down = !agent1.getClientCommandSender().isSending();
            }

            assert gone_down : "The server running in agent2 went down but that was not auto-detected by agent1";
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
     * Tests server polling (which is like auto-discovery).
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testServerPolling() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "false");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "1000");

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "false");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        m_agent1Test.setConfigurationOverrides(props1);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false);
        agent1.start();

        AgentMain agent2 = null;
        try {
            assert !agent1.getClientCommandSender().isSending() : "Should not be in sending mode yet";

            // now start agent2 which creates the remoting server that agent1 should auto-detect
            agent2 = m_agent2Test.createAgent(true);

            // our canary-in-the-mine is the sending mode - when server polling detects the server, the sender goes into sending mode
            boolean discovered = false;
            for (int i = 0; (i < 5) && !discovered; i++) {
                Thread.sleep(1000L);
                discovered = agent1.getClientCommandSender().isSending();
            }

            assert discovered : "The server running in agent2 was never detected by agent1's server polling";

            // now shutdown agent2 and see that we discover it going down via server polling
            agent2.shutdown();

            boolean gone_down = false;
            for (int i = 0; (i < 60) && !gone_down; i++) {
                Thread.sleep(1000L);
                gone_down = !agent1.getClientCommandSender().isSending();
            }

            assert gone_down : "The server running in agent2 went down but that was not detected by agent1's server polling";
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
     * Show that the agent will create the keystore when just the agent is secured (but the server is not).
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testCreateKeystoreSecureAgent() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "socket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");

        m_agent1Test.setConfigurationOverrides(props1);

        File keystore1 = new File("target/testdata/keystore.dat");
        keystore1.delete();
        assert !keystore1.exists() : "Strange - we deleted the keystore but it still exists";

        m_agent1Test.createAgent(true);

        assert keystore1.exists() : "The agent failed to create its keystore upon startup";
    }

    /**
     * Show that the agent will create the keystore when just the server is secured (but the agent is not).
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testCreateKeystoreSecureServer() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");

        m_agent1Test.setConfigurationOverrides(props1);

        File keystore1 = new File("target/testdata/keystore.dat");
        keystore1.delete();
        assert !keystore1.exists() : "Strange - we deleted the keystore but it still exists";

        m_agent1Test.createAgent(true);

        assert keystore1.exists() : "The agent failed to create its keystore upon startup";
    }

    /**
     * Sends a secure message from one remote server to another with server authentication on without a truststore to
     * see the failure actually occurs.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageServerAuthFailure() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");

        assert agent1.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");
        assert agent2.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");

        assert !agent1.getConfiguration().isClientSenderSecurityServerAuthMode();
        assert agent2.getConfiguration().isClientSenderSecurityServerAuthMode();

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Should have been able to send - agent1 does not need to auth the server: "
            + cmdresponse;

        cmdresponse = agent2.getClientCommandSender().sendSynch(command);
        assert !cmdresponse.isSuccessful() : "Should not have been able to send - agent2 don't have a truststore to authenticate the server";

        return;
    }

    /**
     * Sends a secure message from one remote server to another with client authentication on without a keystore to see
     * the failure actually occurs.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageClientAuthFailure() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_FILE, "");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");

        assert agent1.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");
        assert agent2.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Should have been able to send - agent2 only 'wants' to auth clients, but is not required to: "
            + cmdresponse;

        cmdresponse = agent2.getClientCommandSender().sendSynch(command);
        assert !cmdresponse.isSuccessful() : "Should not have been able to send - agent1 'needs' client auth but it doesn't have a truststore to authenticate clients";

        return;
    }

    /**
     * Sends a secure message from one remote server to another - there will be no authentication, just encryption.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageNoAuth() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        m_agent2Test.setConfigurationOverrides(props2);

        // side test - see the agent create the keystore when it starts
        File keystore1 = new File("target/testdata/keystore.dat");
        keystore1.delete();
        assert !keystore1.exists() : "Strange - we deleted the keystore but it still exists";

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert keystore1.exists() : "The agent failed to create its keystore upon startup";

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");

        assert agent1.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");
        assert agent2.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;
        IdentifyCommandResponse response;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent1 to agent2: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        cmdresponse = agent2.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent2 to agent1: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent1
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * Sends a secure message from one remote server to another - there will be full authentication including server and
     * client authentication.
     *
     * @throws Exception
     */
    @Test(enabled = false)
    // WHY IS THIS FAILING?
    public void testSendSecureMessageFullAuth() throws Exception {
        // each keystore is the other's truststore
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getConfiguration().isClientSenderSecurityServerAuthMode();
        assert agent2.getConfiguration().isClientSenderSecurityServerAuthMode();

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;
        IdentifyCommandResponse response;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent1 to agent2: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        // JBoss Remoting 2.2.SP4 doesn't seem to like me doing bi-directional messaging within the same VM
        //      cmdresponse = agent2.getClientCommandSender().sendSynch( command );
        //      assert cmdresponse.isSuccessful() : "Failed to send command from agent2 to agent1: " + cmdresponse;
        //      response = (IdentifyCommandResponse) cmdresponse;
        //      assert new InvokerLocator( response.getIdentification().getInvokerLocator() ).getPort()
        //             == agent1.getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * Prepares to send a secure message from one remote server to another - there will be full authentication including
     * server and client authentication but the keystore passwords will be invalid thus causing errors.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageFullAuthWrongKeystorePassword() throws Exception {
        // each keystore is the other's truststore
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_PASSWORD,
            "invalidpassword");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_PASSWORD,
            "invalidpassword2");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false);
        AgentMain agent2 = m_agent2Test.createAgent(false);

        try {
            agent1.start();
            assert false : "Should not have been able to start agent1 - the wrong keystore password should cause problems starting";
        } catch (Exception e) {
        }

        try {
            agent2.start();
            assert false : "Should not have been able to start agent2 - the wrong keystore password should cause problems starting";
        } catch (Exception e) {
        }

        return;
    }

    /**
     * Sends a secure message from one remote server to another where both servers want client authentication and both
     * clients have truststores with the appropriate keys (therefore, the clients should be able to successfully send).
     *
     * @throws Exception
     */
    @Test(enabled = false)
    // WHY IS THIS FAILING?
    public void testSendSecureMessageClientAuthWantWithTruststore() throws Exception {
        // each keystore is the other's truststore
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getConfiguration().isClientSenderSecurityServerAuthMode();
        assert agent2.getConfiguration().isClientSenderSecurityServerAuthMode();

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;
        IdentifyCommandResponse response;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent1 to agent2: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        // JBoss Remoting 2.2.SP4 doesn't seem to like me doing bi-directional messaging within the same VM
        //      cmdresponse = agent2.getClientCommandSender().sendSynch( command );
        //      assert cmdresponse.isSuccessful() : "Failed to send command from agent2 to agent1: " + cmdresponse;
        //      response = (IdentifyCommandResponse) cmdresponse;
        //      assert new InvokerLocator( response.getIdentification().getInvokerLocator() ).getPort()
        //             == agent1.getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * Sends a secure message from one remote server to another with the secure protocol being different for each remote
     * server (cool, huh?) - there will be no authentication, just encryption.
     *
     * <p>What this tests:<br>
     * agent1 ---- TLS ---> agent2<br>
     * agent1 <--- SSL ---- agent2<br>
     * </p>
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageNoAuthDifferentProtocol() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "TLS");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "TLS");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("TLS");

        assert agent1.getConfiguration().getClientSenderSecuritySocketProtocol().equals("TLS");
        assert agent2.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;
        IdentifyCommandResponse response;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent1 to agent2: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        cmdresponse = agent2.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent2 to agent1: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent1
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * This will set the appropriate properties in <code>props</code> to define the server locator URI.
     *
     * @param props
     * @param transport
     * @param addr
     * @param port
     * @param transport_params
     */
    private void setServerLocatorUriProperties(Properties props, String transport, String addr, int port,
        String transport_params) {
        props.setProperty(AgentConfigurationConstants.SERVER_TRANSPORT, transport);
        props.setProperty(AgentConfigurationConstants.SERVER_BIND_ADDRESS, addr);
        props.setProperty(AgentConfigurationConstants.SERVER_BIND_PORT, Integer.toString(port));
        if (transport_params != null) {
            props.setProperty(AgentConfigurationConstants.SERVER_TRANSPORT_PARAMS, transport_params);
        }
    }

    /**
     * Creates a very large file that we can use for testing streaming of large data.
     *
     * @return the file
     *
     * @throws Exception
     */
    private File createVeryLargeFile() throws Exception {
        if (m_veryLargeFile == null) {
            File file = File.createTempFile("agent-comm-test-large-file", null);
            FileOutputStream o = new FileOutputStream(file);

            // creates a 1MB file
            for (int i = 0; i < 1000000; i++) {
                o.write("123456789\n".getBytes());
            }

            o.close();

            m_veryLargeFile = file;
        }

        return m_veryLargeFile;
    }
}