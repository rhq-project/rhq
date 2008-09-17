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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.testng.annotations.Test;

import org.jboss.remoting.security.SSLSocketBuilder;

import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommand;

/**
 * This tests the communications layer in the agent.
 *
 * @author John Mazzitelli
 */
@Test(groups = "agent-comm")
public class AgentComm1Test extends AgentCommTestBase {
    private static final boolean ENABLE_TESTS = true;

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
}