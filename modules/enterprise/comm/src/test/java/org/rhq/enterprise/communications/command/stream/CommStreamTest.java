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
package org.rhq.enterprise.communications.command.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.JBossRemotingRemoteCommunicator;
import org.rhq.enterprise.communications.command.client.RemoteCommunicator;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemoteOutputStream;

/**
 * Tests remote streams. This will create two "servers" - #1 listening on one port and #2 listening on another. The
 * remote pojo will be installed on server #2. Streams will be sent to/from the remote pojo on server #2. Remote streams
 * (both input and output) will be hosted in both server #1 and server #2 to test sending remote streams and receiving
 * remote streams.
 *
 * @author John Mazzitelli
 */
@Test
public class CommStreamTest {
    private static final boolean ENABLE_TESTS = true;

    private ClientCommandSender sender1;
    private ServiceContainer serviceContainer1;
    private ServiceContainer serviceContainer2;
    private CommTestStreamPojo pojoImpl;
    private ICommTestStreamPojo pojo;

    @AfterClass
    public void afterClass() {
        try {
            getPrefs1().removeNode();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }

        try {
            getPrefs2().removeNode();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        // setup the server #1
        Preferences prefs1 = getPrefs1();
        prefs1.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        prefs1.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, "127.0.0.1");
        prefs1.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        prefs1.put(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION, ""
            + ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        prefs1.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, "target/data1");
        prefs1.put(ServiceContainerConfigurationConstants.MBEANSERVER_NAME, "commstreamtest1");
        prefs1.put(ServiceContainerConfigurationConstants.CMDSERVICE_DIRECTORY_DYNAMIC_DISCOVERY, "false");

        serviceContainer1 = new ServiceContainer();
        serviceContainer1.start(prefs1, new ClientCommandSenderConfiguration());

        // setup the server #2
        Preferences prefs2 = getPrefs1();
        prefs2.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        prefs2.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, "127.0.0.1");
        prefs2.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        prefs2.put(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION, ""
            + ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        prefs2.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, "target/data2");
        prefs2.put(ServiceContainerConfigurationConstants.MBEANSERVER_NAME, "commstreamtest2");
        prefs2.put(ServiceContainerConfigurationConstants.CMDSERVICE_DIRECTORY_DYNAMIC_DISCOVERY, "false");

        serviceContainer2 = new ServiceContainer();
        serviceContainer2.start(prefs2, new ClientCommandSenderConfiguration());

        Thread.sleep(5000);

        // install our streaming pojo in our server #2
        pojoImpl = new CommTestStreamPojo(this);
        serviceContainer2.addRemotePojo(pojoImpl, ICommTestStreamPojo.class);

        // setup the client to server #2
        RemoteCommunicator comm = new JBossRemotingRemoteCommunicator("socket://127.0.0.1:22222/?force_remote=true");
        ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
        config.maxConcurrent = Integer.MAX_VALUE; // let the sender send as fast as it can
        config.defaultTimeoutMillis = 60000L;
        config.commandSpoolFileName = null;
        config.enableQueueThrottling = false;
        config.enableSendThrottling = false;
        config.serverPollingIntervalMillis = 0;
        sender1 = new ClientCommandSender(comm, config);
        sender1.startSending();
        pojo = sender1.getClientRemotePojoFactory().getRemotePojo(ICommTestStreamPojo.class);

        return;
    }

    @AfterMethod
    public void afterMethod() {
        if (sender1 != null) {
            sender1.stopSending(false);
        }

        if (serviceContainer1 != null) {
            serviceContainer1.shutdown();
        }

        if (serviceContainer2 != null) {
            serviceContainer2.shutdown();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testInputStreamReturn() throws Exception {
        assert pojo.ping();

        InputStream in;
        int expectedLength = CommTestStreamPojo.INPUT_STREAM_STRING.getBytes().length;
        byte[] bytes = new byte[expectedLength];

        // try the different read methods
        in = pojo.returnInputStream();
        assert in instanceof RemoteInputStream;
        assert expectedLength == in.read(bytes);
        assert new String(bytes).equals(CommTestStreamPojo.INPUT_STREAM_STRING);
        in.close();

        in = pojo.returnInputStream();
        assert in instanceof RemoteInputStream;
        assert 2 == in.read(bytes, 0, 2);
        assert bytes[0] == CommTestStreamPojo.INPUT_STREAM_STRING.getBytes()[0];
        assert bytes[1] == CommTestStreamPojo.INPUT_STREAM_STRING.getBytes()[1];
        in.close();

        in = pojo.returnInputStream();
        assert in instanceof RemoteInputStream;
        assert CommTestStreamPojo.INPUT_STREAM_STRING.getBytes()[0] == in.read();
        in.close();

        in = pojo.returnInputStream();
        assert in instanceof RemoteInputStream;
        assert CommTestStreamPojo.INPUT_STREAM_STRING.getBytes().length == in.available();
        in.close();
    }

    @Test(enabled = ENABLE_TESTS)
    public void testInputStreamParam() throws Exception {
        assert pojo.ping();

        final String streamString = "Comm Stream Test String";

        // this will throw an exception if it failed
        InputStream in = prepareRemoteStreamInServer1(new ByteArrayInputStream(streamString.getBytes()));
        assert in instanceof RemoteInputStream;
        assert pojo.slurpInputStream(in, streamString);
    }

    @Test(enabled = ENABLE_TESTS)
    public void testOutputStreamReturn() throws Exception {
        assert pojo.ping();

        OutputStream out;
        String str;

        // try the different write methods
        out = pojo.returnOutputStream();
        assert out instanceof RemoteOutputStream;
        str = "first write test";
        out.write(str.getBytes());
        out.close();
        assert str.equals(pojoImpl.byteArrayOutputStream.toString());

        out = pojo.returnOutputStream();
        assert out instanceof RemoteOutputStream;
        str = "second write test";
        out.write(str.getBytes(), 0, str.length());
        out.close();
        assert str.equals(pojoImpl.byteArrayOutputStream.toString());

        out = pojo.returnOutputStream();
        assert out instanceof RemoteOutputStream;
        out.write('z');
        out.close();
        assert 'z' == pojoImpl.byteArrayOutputStream.toByteArray()[0];
    }

    @Test(enabled = ENABLE_TESTS)
    public void testOutputStreamParam() throws Exception {
        assert pojo.ping();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assert baos.toByteArray().length == 0;

        OutputStream out = prepareRemoteStreamInServer1(baos);
        assert out instanceof RemoteOutputStream;

        String contentsToWrite = "remote write test!";
        assert pojo.slurpOutputStream(out, contentsToWrite);
        assert baos.toByteArray().length == contentsToWrite.length();
        assert baos.toString().equals(contentsToWrite);
    }

    @Test(enabled = ENABLE_TESTS)
    public void testOutputStreamPerformance() throws Exception {
        assert pojo.ping();

        OutputStream out;

        out = pojo.returnOutputStream();
        assert out instanceof RemoteOutputStream;

        byte[] data = new byte[32768];
        Arrays.fill(data, (byte) 0xff);

        int loopCount = 100;
        long start = System.currentTimeMillis();
        for (int i = 0; i < loopCount; i++) {
            out.write(data);
        }

        out.close();

        long end = System.currentTimeMillis();

        assert (loopCount * data.length) == pojoImpl.byteArrayOutputStream.toByteArray().length;

        System.out.println("---> REMOTE OUTPUT STREAM: wrote [" + (loopCount * data.length) + "] byte in ["
            + (end - start) + "]ms");

        pojoImpl.byteArrayOutputStream = null; // free up memory

        // for giggles, let's see how fast true in-mem streaming it
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        start = System.currentTimeMillis();
        for (int i = 0; i < loopCount; i++) {
            baos.write(data);
        }

        baos.close();

        end = System.currentTimeMillis();

        assert (loopCount * data.length) == baos.toByteArray().length;

        System.out.println("---> NORMAL BYTE OUTPUT STREAM: wrote [" + (loopCount * data.length) + "] bytes in ["
            + (end - start) + "]ms");

        baos = null;

        // i wonder how fast file I/O is compared to the above
        File tmpFile = File.createTempFile("commStreamTest", ".dat");
        FileOutputStream fos = new FileOutputStream(tmpFile);
        try {
            start = System.currentTimeMillis();
            for (int i = 0; i < loopCount; i++) {
                fos.write(data);
            }

            fos.flush();
            fos.close();

            end = System.currentTimeMillis();

            assert (loopCount * data.length) == tmpFile.length();
            System.out.println("---> FILE OUTPUT STREAM: wrote [" + (loopCount * data.length) + "] bytes in ["
                + (end - start) + "]ms");
        } finally {
            tmpFile.delete();
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testOutputStreamMultiRequests() throws Exception {
        assert pojo.ping();

        OutputStream out;

        out = pojo.returnOutputStream();
        assert out instanceof RemoteOutputStream;

        // make sure we send the requests serialially - maxConcurrent should be forced to 1
        byte loopCount = Byte.MAX_VALUE;
        for (byte i = 0; i < loopCount; i++) {
            out.write(i);
        }

        out.close();

        byte[] results = pojoImpl.byteArrayOutputStream.toByteArray();
        assert loopCount == results.length : "loopCount=" + loopCount + "!=results.length=" + results.length;
        for (byte i = 0; i < loopCount; i++) {
            assert i == results[i];
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testSocketOutputStream() throws Exception {
        assert pojo.ping();

        String contentsToWrite = "remote write test!";

        SocketThread thread = new SocketThread();
        thread.contentsToExpect = contentsToWrite;
        thread.start();

        Thread.sleep(1000L);
        assert thread.port != 0;
        Socket socket = new Socket("127.0.0.1", thread.port);
        OutputStream os = null;
        try {
            os = socket.getOutputStream();
            assert os != null;

            OutputStream out = prepareRemoteStreamInServer1(os);
            assert out instanceof RemoteOutputStream;
            assert pojo.slurpOutputStream(out, contentsToWrite);
            os.close();
            Thread.sleep(250L);
            assert thread.results != null;
            assert thread.results.equals(contentsToWrite);
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (os != null) {
                os.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testSocketOutputStreamRange() throws Exception {
        assert pojo.ping();

        String contents = "0123456789abcde0123456789";
        int startByte = 10;
        int endByte = 14;

        SocketThread thread = new SocketThread();
        thread.contentsToExpect = "abcde";
        thread.start();

        Thread.sleep(1000L);
        assert thread.port != 0;
        Socket socket = new Socket("127.0.0.1", thread.port);
        OutputStream os = null;
        try {
            os = socket.getOutputStream();
            assert os != null;

            OutputStream out = prepareRemoteStreamInServer1(os);
            assert out instanceof RemoteOutputStream;
            assert pojo.slurpOutputStreamRange(out, contents, startByte, endByte) == ((endByte - startByte) + 1);
            os.close();
            Thread.sleep(250L);
            assert thread.results != null;
            assert thread.results.equals(contents.substring(startByte, endByte + 1));
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (os != null) {
                os.close();
            }
        }
    }

    @Test(enabled = ENABLE_TESTS)
    public void testSocketOutputStreamRange2() throws Exception {
        // this test's a very specific range that was seen to fail
        assert pojo.ping();

        String contents;
        int startByte = 280;
        int endByte = 7861;

        // this is the length of the string we expect to copy from the stream
        int expectedLength = endByte - startByte + 1;

        // let's build a big test string
        int bigLength = endByte * 2;
        Random rand = new Random();
        StringBuilder contentsBuilder = new StringBuilder(bigLength);
        for (int i = 0; i < bigLength; i++) {
            contentsBuilder.append((char) ('a' + rand.nextInt(26)));
        }

        contents = contentsBuilder.toString();

        SocketThread thread = new SocketThread();
        thread.contentsToExpect = contents.substring(startByte, endByte + 1);
        assert thread.contentsToExpect.length() == expectedLength : "sanity check failed!?";
        thread.start();

        Thread.sleep(1000L);
        assert thread.port != 0;
        Socket socket = new Socket("127.0.0.1", thread.port);
        OutputStream os = null;
        try {
            os = socket.getOutputStream();
            assert os != null;

            OutputStream out = prepareRemoteStreamInServer1(os);
            assert out instanceof RemoteOutputStream;
            assert pojo.slurpOutputStreamRange(out, contents, startByte, endByte) == ((endByte - startByte) + 1);
            os.close();
            Thread.sleep(250L);
            assert thread.results != null;
            assert thread.results.length() == expectedLength : "->" + thread.results.length() + ":" + expectedLength;
            assert thread.results.equals(contents.substring(startByte, endByte + 1));
        } finally {
            if (socket != null) {
                socket.close();
            }

            if (os != null) {
                os.close();
            }
        }
    }

    private Preferences getPrefs1() {
        Preferences topNode = Preferences.userRoot().node("rhq-agent");
        Preferences preferencesNode = topNode.node("commstream1test");
        return preferencesNode;
    }

    private Preferences getPrefs2() {
        Preferences topNode = Preferences.userRoot().node("rhq-agent");
        Preferences preferencesNode = topNode.node("commstream2test");
        return preferencesNode;
    }

    private InputStream prepareRemoteStreamInServer1(InputStream in) throws Exception {
        return new RemoteInputStream(in, serviceContainer1);
    }

    private OutputStream prepareRemoteStreamInServer1(OutputStream out) throws Exception {
        return new RemoteOutputStream(out, serviceContainer1);
    }

    // package scoped so it can be used by the pojo class
    InputStream prepareRemoteStreamInServer2(InputStream in) throws Exception {
        return new RemoteInputStream(in, serviceContainer2);
    }

    // package scoped so it can be used by the pojo class
    OutputStream prepareRemoteStreamInServer2(OutputStream out) throws Exception {
        return new RemoteOutputStream(out, serviceContainer2);
    }

    private class SocketThread extends Thread {
        public String contentsToExpect; // test will set this before starting thread
        public String results; // public so tests can access it
        public int port;

        @Override
        public void run() {
            ServerSocket ss = null;
            Socket s = null;

            try {
                ss = new ServerSocket(0);
                port = ss.getLocalPort();
                s = ss.accept();
                InputStream in = s.getInputStream();
                byte[] bytes = new byte[contentsToExpect.length()];
                assert in.read(bytes) == contentsToExpect.length();
                results = new String(bytes);
            } catch (Throwable t) {
                System.out.println("CANNOT CREATE/USE SERVER SOCKET FOR TESTING!!!");
                t.printStackTrace();
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                    }
                }

                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
}