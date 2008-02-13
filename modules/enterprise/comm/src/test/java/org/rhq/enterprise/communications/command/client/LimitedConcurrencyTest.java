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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.util.ConcurrencyManager;

/**
 * Tests limited concurrency.
 *
 * @author John Mazzitelli
 */
@Test
public class LimitedConcurrencyTest {
    @AfterClass
    public void afterClass() {
        try {
            getPrefs().removeNode();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * This tests the actual concurrency limitation feature. This is the important test.
     *
     * @throws Exception
     */
    public void testLimited() throws Exception {
        ClientCommandSender sender = null;

        Preferences prefs = getPrefs();
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, "127.0.0.1");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        prefs.put(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION, ""
            + ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        prefs.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, "target");
        prefs.remove(ServiceContainerConfigurationConstants.GLOBAL_CONCURRENCY_LIMIT); // make sure we do not have a global limit

        ServiceContainer sc = new ServiceContainer();
        sc.start(prefs, new ClientCommandSenderConfiguration());
        Map<String, Integer> map = sc.getConcurrencyManager().getAllConfiguredNumberOfPermitsAllowed();
        map.put("limited", 10);
        sc.setConcurrencyManager(new ConcurrencyManager(map));

        Thread.sleep(5000);

        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        LimitedConcurrencyPojo pojoImpl = new LimitedConcurrencyPojo(counter, latch);

        sc.addRemotePojo(pojoImpl, ILimitedConcurrencyPojo.class);
        try {
            // default for clientMaxPoolSize is 50, but we don't want remoting to be the limit for this test
            RemoteCommunicator comm = new JBossRemotingRemoteCommunicator(
                "socket://127.0.0.1:11111/?clientMaxPoolSize=100");
            ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
            config.maxConcurrent = Integer.MAX_VALUE; // let the sender send as fast as it can
            config.defaultTimeoutMillis = 60000L;
            sender = new ClientCommandSender(comm, config);
            sender.startSending();

            final ILimitedConcurrencyPojo pojo = sender.getClientRemotePojoFactory().getRemotePojo(
                ILimitedConcurrencyPojo.class);

            // sanity check - make sure we can call it - this is needed to get the jboss/remoting client initialized with thread safety
            assert pojo.ping();

            for (int i = 0; i < 10; i++) {
                new Thread(new Runnable() {
                    public void run() {
                        pojo.limitedMethod("foo1");
                    }
                }).start();
            }

            int loopMax = 10; // should not take us over 10s for our threads to start
            while ((counter.get() < 10) && (loopMax-- > 0)) {
                Thread.sleep(1000);
            }

            assert counter.get() == 10 : "For some reason, we didn't invoke our pojo 10 times: " + counter;

            // so far so good - there should now be 10 concurrent threads in the limited method, no more are allowed
            new Thread(new Runnable() {
                public void run() {
                    pojo.limitedMethod("notallowed1");
                }
            }).start();

            Thread.sleep(5000);
            assert counter.get() == 10 : "Should not have been permitted, counter should still be 10: " + counter;

            latch.countDown(); // release the hounds! all threads should finish now
            loopMax = 10; // should not take us over 10s for our threads to finish
            while ((counter.get() > 0) && (loopMax-- > 0)) {
                Thread.sleep(1000);
            }

            assert counter.get() == 0 : "All the threads should have finished, counter should be 0: " + counter;

            // see that we can call it - we can call it synchronously, it shouldn't block and we can finish quick
            assert "allowed1".equals(pojo.limitedMethod("allowed1"));
        } finally {
            latch.countDown(); // in case we got exceptions, let's flush our threads that might be hung

            if (sender != null) {
                sender.stopSending(false);
            }

            sc.shutdown();
        }
    }

    /**
     * This tests shows that we can define a timeout and see the concurrency limitation feature abort after the timeout
     * expires.
     *
     * @throws Exception
     */
    public void testLimitedTimeout() throws Exception {
        ClientCommandSender sender = null;

        Preferences prefs = getPrefs();
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, "127.0.0.1");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        prefs.put(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION, ""
            + ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        prefs.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, "target");
        prefs.remove(ServiceContainerConfigurationConstants.GLOBAL_CONCURRENCY_LIMIT); // make sure we do not have a global limit

        ServiceContainer sc = new ServiceContainer();
        sc.start(prefs, new ClientCommandSenderConfiguration());
        Map<String, Integer> map = sc.getConcurrencyManager().getAllConfiguredNumberOfPermitsAllowed();
        map.put("limitedTimeout", 10);
        sc.setConcurrencyManager(new ConcurrencyManager(map));
        Thread.sleep(5000);

        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        LimitedConcurrencyPojo pojoImpl = new LimitedConcurrencyPojo(counter, latch);

        sc.addRemotePojo(pojoImpl, ILimitedConcurrencyPojo.class);
        try {
            // default for clientMaxPoolSize is 50, but we don't want remoting to be the limit for this test
            RemoteCommunicator comm = new JBossRemotingRemoteCommunicator(
                "socket://127.0.0.1:11111/?clientMaxPoolSize=100");
            ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
            config.maxConcurrent = Integer.MAX_VALUE; // let the sender send as fast as it can
            config.defaultTimeoutMillis = 60000L; // @Timeout will override this
            sender = new ClientCommandSender(comm, config);
            sender.startSending();

            final ILimitedConcurrencyPojo pojo = sender.getClientRemotePojoFactory().getRemotePojo(
                ILimitedConcurrencyPojo.class);

            // sanity check - make sure we can call it - this is needed to get the jboss/remoting client initialized with thread safety
            assert pojo.ping();

            final AtomicInteger timedOutThreads = new AtomicInteger(0);

            // 10 will timeout because of the latch, and 5 will timeout while waiting to get in due to concurrency limit
            for (int i = 0; i < 15; i++) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            pojo.limitedWithTimeoutMethod("foo2");
                        } catch (Exception e) {
                            timedOutThreads.incrementAndGet();
                        }
                    }
                }).start();
            }

            int loopMax = 10; // should not take us over 10s for our threads to start

            // note that only 10 threads will be able to increment counter; the other 5 will block and timeout before they have a chance to
            while ((counter.get() < 10) && (loopMax-- > 0)) {
                Thread.sleep(1000);
            }

            assert counter.get() == 10 : "For some reason, we didn't invoke our pojo 10 times: " + counter;

            // do not open latch, show that the threads do die (which could only be due to timeout)
            loopMax = 10; // should not take us over 10s for our threads to finish
            while ((timedOutThreads.get() < 15) && (loopMax-- > 0)) {
                Thread.sleep(1000);
            }

            assert timedOutThreads.get() == 15 : "All the threads should have timed out, counter should be 15: "
                + counter;

            // see that we can call it - we can call it synchronously, it shouldn't block and we can finish quick
            latch.countDown();
            assert "allowed2".equals(pojo.limitedWithTimeoutMethod("allowed2"));
        } finally {
            latch.countDown(); // in case we got exceptions, let's flush our threads that might be hung

            if (sender != null) {
                sender.stopSending(false);
            }

            sc.shutdown();
        }
    }

    /**
     * This tests that we can still have unlimited concurrency.
     *
     * @throws Exception
     */
    public void testUnlimited() throws Exception {
        ClientCommandSender sender = null;

        Preferences prefs = getPrefs();
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, "127.0.0.1");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        prefs.put(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION, ""
            + ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        prefs.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, "target");
        prefs.remove(ServiceContainerConfigurationConstants.GLOBAL_CONCURRENCY_LIMIT); // make sure we do not have a global limit

        ServiceContainer sc = new ServiceContainer();
        sc.start(prefs, new ClientCommandSenderConfiguration());
        Thread.sleep(5000);

        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        LimitedConcurrencyPojo pojoImpl = new LimitedConcurrencyPojo(counter, latch);

        sc.addRemotePojo(pojoImpl, ILimitedConcurrencyPojo.class);
        try {
            // default for clientMaxPoolSize is 50, but we don't want remoting to be the limit for this test
            RemoteCommunicator comm = new JBossRemotingRemoteCommunicator(
                "socket://127.0.0.1:11111/?clientMaxPoolSize=100");
            ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
            config.maxConcurrent = Integer.MAX_VALUE; // let the sender send as fast as it can
            config.defaultTimeoutMillis = 60000L;
            sender = new ClientCommandSender(comm, config);
            sender.startSending();

            final ILimitedConcurrencyPojo pojo = sender.getClientRemotePojoFactory().getRemotePojo(
                ILimitedConcurrencyPojo.class);

            // sanity check - make sure we can call it - this is needed to get the jboss/remoting client initialized with thread safety
            assert pojo.ping();

            for (int i = 0; i < 60; i++) // 60 is larger than the default concurrent limit (when @LimitedConcurrency is used)
            {
                new Thread(new Runnable() {
                    public void run() {
                        pojo.unlimitedMethod("unlimited3"); // unlimited number of concurrent calls are allowed here
                    }
                }).start();
            }

            int loopMax = 30; // should not take us over 30s for our threads to start
            while ((counter.get() < 60) && (loopMax-- > 0)) {
                Thread.sleep(1000);
            }

            assert counter.get() == 60 : "For some reason, we didn't invoke our pojo 60 times: " + counter;

            latch.countDown(); // release the hounds! all threads should finish now

            loopMax = 30; // should not take us over 30s for our threads to finish
            while ((counter.get() > 0) && (loopMax-- > 0)) {
                Thread.sleep(1000);
            }

            assert counter.get() == 0 : "All the threads should have finished, counter should be 0: " + counter;
        } finally {
            latch.countDown(); // in case we got exceptions, let's flush our threads that might be hung

            if (sender != null) {
                sender.stopSending(false);
            }

            sc.shutdown();
        }
    }

    /**
     * This tests that we can define a global limit.
     *
     * @throws Exception
     */
    public void testGlobalLimit() throws Exception {
        ClientCommandSender sender = null;

        Preferences prefs = getPrefs();
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_ADDRESS, "127.0.0.1");
        prefs.put(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        prefs.put(ServiceContainerConfigurationConstants.CONFIG_SCHEMA_VERSION, ""
            + ServiceContainerConfigurationConstants.CURRENT_CONFIG_SCHEMA_VERSION);
        prefs.put(ServiceContainerConfigurationConstants.DATA_DIRECTORY, "target");
        prefs.put(ServiceContainerConfigurationConstants.GLOBAL_CONCURRENCY_LIMIT, "5");

        ServiceContainer sc = new ServiceContainer();
        sc.start(prefs, new ClientCommandSenderConfiguration());
        Thread.sleep(5000);

        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        LimitedConcurrencyPojo pojoImpl = new LimitedConcurrencyPojo(counter, latch);

        sc.addRemotePojo(pojoImpl, ILimitedConcurrencyPojo.class);
        try {
            // default for clientMaxPoolSize is 50, but we don't want remoting to be the limit for this test
            RemoteCommunicator comm = new JBossRemotingRemoteCommunicator(
                "socket://127.0.0.1:11111/?clientMaxPoolSize=100");
            ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
            config.maxConcurrent = Integer.MAX_VALUE; // let the sender send as fast as it can
            config.defaultTimeoutMillis = 6000000L;
            sender = new ClientCommandSender(comm, config);
            sender.startSending();

            final ILimitedConcurrencyPojo pojo = sender.getClientRemotePojoFactory().getRemotePojo(
                ILimitedConcurrencyPojo.class);

            // sanity check - make sure we can call it - this is needed to get the jboss/remoting client initialized with thread safety
            assert pojo.ping();

            // 5 will timeout because of the latch, and 10 will timeout while waiting to get in due to concurrency limit
            for (int i = 0; i < 15; i++) {
                new Thread(new Runnable() {
                    public void run() {
                        pojo.unlimitedMethod("unlimited3"); // unlimited number of concurrent calls are allowed here
                    }
                }).start();
            }

            int loopMax = 10; // should not take us over 10s for our threads to start

            // note that only 5 threads will be able to increment counter; the other 10 will block due to concurrency limit
            while ((counter.get() < 5) && (loopMax-- > 0)) {
                Thread.sleep(1000);
            }

            assert counter.get() == 5 : "For some reason, we didn't invoke our pojo 5 times: " + counter;

            // release the hounds! see that we can call it now - we can call it synchronously, it shouldn't block and we can finish quick
            latch.countDown();
            assert "unlimited3".equals(pojo.unlimitedMethod("unlimited3"));
        } finally {
            latch.countDown(); // in case we got exceptions, let's flush our threads that might be hung

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
}