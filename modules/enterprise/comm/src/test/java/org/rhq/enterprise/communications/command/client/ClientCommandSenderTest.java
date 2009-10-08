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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import org.testng.annotations.Test;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommand;

/**
 * Tests the sender.
 *
 * @author John Mazzitelli
 */
@Test(groups = "comm-client")
public class ClientCommandSenderTest {
    /**
     * Tests sending alot of volatile and guaranteed messages - since no errors occur, no persisting will occur.
     *
     * @throws Exception
     */
    public void testSendAlot() throws Exception {
        DummyRemoteCommunicator comm = new DummyRemoteCommunicator();
        ClientCommandSenderConfiguration config = createConfig();

        int num_to_send = 250000;

        config.queueSize = num_to_send;
        config.maxConcurrent = 100;
        config.defaultTimeoutMillis = -1;

        ClientCommandSender sender = new ClientCommandSender(comm, config);

        try {
            sender.startSending();

            for (int i = 0; i < num_to_send; i++) {
                if ((i % 2) == 0) {
                    sender.sendAsynchGuaranteed(createGenericCommand(), null);
                } else {
                    sender.sendAsynch(createGenericCommand(), null);
                }
            }

            // wait for our commands to be sent; but only wait at most a few seconds (so our test doesn't hang if this fails)
            for (int i = 0; (i < 30) && (comm.getSentCount() < num_to_send); i++) {
                Thread.sleep(1000L);
            }

            sender.stopSending(false);
            assert sender.drainQueuedCommands().size() == 0 : "still some commands left to be sent - we didn't wait long enough";
            assert comm.getSentCount() == num_to_send : "did not send all commands: " + comm.getSentCount();
            assert comm.getSentSuccessfulCount() == num_to_send;
        } finally {
            sender.stopSending(false);
        }

        return;
    }

    /**
     * Tests sending commands (guaranteed and volatile), and ensuring they get requeue after draining them and
     * rebuilding the sender.
     *
     * @throws Exception
     */
    public void testReconstituteQueue() throws Exception {
        DummyRemoteCommunicator comm = new DummyRemoteCommunicator();
        ClientCommandSenderConfiguration config = createConfig();

        config.queueSize = 1000;
        config.commandSpoolFileMaxSize = 2000000L;
        config.maxConcurrent = 10;

        File cmd_spool_file = getPersistentFifoFile(true);
        ClientCommandSender sender = new ClientCommandSender(comm, config);
        PersistentFifo fifo = new PersistentFifo(cmd_spool_file, config.commandSpoolFileMaxSize,
            config.commandSpoolFilePurgePercentage, false);

        // sent 500 guaranteed and 500 volatile commands, ensure we send all of them
        try {
            for (int i = 0; i < 1000; i++) {
                Command cmd_to_send = createGenericCommand();
                cmd_to_send.setParameterValue("num", String.valueOf(i));

                if ((i % 2) == 0) {
                    sender.sendAsynchGuaranteed(cmd_to_send, null);
                } else {
                    sender.sendAsynch(cmd_to_send, null);
                }
            }

            // drain the command from this sender - we'll reconstitute a new sender with these
            // only volatile commands are returned - guaranteed commands are spooled (why did I do this? I lost the FIFO order of the commands now)
            // after drained, the only messages this sender will send are the spooled/guaranteed commands
            LinkedList<Runnable> drained_commands = sender.drainQueuedCommands();
            assert fifo.count() == 500 : "did not persist enough: " + fifo.count();
            assert drained_commands.size() == 500 : "missing some volatile commands: " + drained_commands.size();

            // create a new sender and reconsitute the queue with our old volatile commands, persisted file has the rest
            sender = new ClientCommandSender(comm, config, drained_commands);
            sender.startSending();

            // wait for our commands to be sent; but only wait at most a few seconds (so our test doesn't hang if this fails)
            for (int i = 0; (i < 5) && (comm.getSentCount() < 1000); i++) {
                Thread.sleep(2000L);
            }

            sender.stopSending(true);
            assert sender.drainQueuedCommands().size() == 0 : "still some commands left to be sent - we didn't wait long enough";
            assert comm.getSentCount() == 1000 : "should have been able to send all commands with two senders: "
                + comm.getSentCount();
            assert comm.getSentSuccessfulCount() == 1000;
            assert fifo.isEmpty();
            assert fifo.count() == 0; // just checking - making sure isEmpty was right
        } finally {
            sender.stopSending(false);
            getPersistentFifoFile(true);
        }

        return;
    }

    /**
     * Tests sending a guaranteed command that takes too long to complete and therefore times out. We will stop the
     * sender to see that the command will be persisted after the sender is stopped.
     *
     * @throws Exception
     */
    public void testSendGuaranteedThatTimesOutThenStopSending() throws Exception {
        DummyRemoteCommunicator comm = new DummyRemoteCommunicator();
        GenericCommand command = createGenericCommand();
        ClientCommandSenderConfiguration config = createConfig();

        config.retryInterval = 50L; // retry really fast
        config.defaultTimeoutMillis = 500L; // default will be less than the time the comm.send will return
        comm.setSleepPeriod(1000L); // simulate the server taking 1sec to process the request

        File cmd_spool_file = getPersistentFifoFile(true);
        ClientCommandSender sender = new ClientCommandSender(comm, config);
        PersistentFifo fifo = new PersistentFifo(cmd_spool_file, config.commandSpoolFileMaxSize,
            config.commandSpoolFilePurgePercentage, false);

        try {
            sender.startSending();
            assert comm.getSentCount() == 0 : "should not have sent any command yet";
            sender.sendAsynchGuaranteed(command, null);
            Thread.sleep(2000L); // let it retry a few times
            sender.stopSending(false);
            assert comm.getSentSuccessfulCount() == 0 : "should not have been able to send the command successfully";

            long sent_count = comm.getSentCount();
            assert comm.getSentCount() > 1 : "should have attempted to resend the command multiple times";
            Thread.sleep(1000L); // wait a bit and see that we truely did stop sending commands
            assert sent_count == comm.getSentCount() : "should not have continued sending commands while stopped: "
                + sent_count + "!=" + comm.getSentCount();
            assert comm.getSentSuccessfulCount() == 0 : "should not have been able to send the command successfully: "
                + comm.getSentSuccessfulCount();

            assert fifo.count() == 1 : "not sending so we should have spooled that guaranteed command to disk";
        } finally {
            sender.stopSending(false);
            getPersistentFifoFile(true);
        }

        return;
    }

    /**
     * Tests sending a guaranteed command that takes too long to complete and therefore times out.
     *
     * @throws Exception
     */
    public void testSendGuaranteedThatTimesOut() throws Exception {
        DummyRemoteCommunicator comm = new DummyRemoteCommunicator();
        GenericCommand command = createGenericCommand();
        ClientCommandSenderConfiguration config = createConfig();

        config.retryInterval = 50L; // retry really fast
        config.defaultTimeoutMillis = 1000L; // default will be less than the time the comm.send will return
        comm.setSleepPeriod(1500L);

        getPersistentFifoFile(true);
        ClientCommandSender sender = new ClientCommandSender(comm, config);

        try {
            sender.startSending();
            assert comm.getSentCount() == 0 : "should not have sent any command yet";
            sender.sendAsynchGuaranteed(command, null);
            Thread.sleep(2500L); // let it retry a few times
            comm.setSleepPeriod(0L); // let it finish now - it won't timeout anymore
            Thread.sleep(1000L); // wait for it to execute one last time (this time successfully)
            assert comm.getSentCount() > 1 : "should have attempted to resend the command multiple times";
            assert comm.getSentSuccessfulCount() == 1 : "should have sent the command successfully by now";
            Thread.sleep(1000L);
            assert comm.getSentSuccessfulCount() == 1 : "should not have sent the command more than once";
        } finally {
            sender.stopSending(false);
            getPersistentFifoFile(true);
        }

        return;
    }

    /**
     * Tests sending a guaranteed command that takes too long to complete and therefore times out. The retry interval
     * will be long.
     *
     * @throws Exception
     */
    public void testSendGuaranteedThatTimesOutWithLongRetryInterval() throws Exception {
        DummyRemoteCommunicator comm = new DummyRemoteCommunicator();
        GenericCommand command = createGenericCommand();
        ClientCommandSenderConfiguration config = createConfig();

        config.retryInterval = 5000L;
        config.defaultTimeoutMillis = 750L; // default will be less than the time the comm.send will return
        comm.setSleepPeriod(1250L);

        getPersistentFifoFile(true);
        ClientCommandSender sender = new ClientCommandSender(comm, config);

        try {
            sender.startSending();
            assert comm.getSentCount() == 0 : "should not have sent any command yet";
            sender.sendAsynchGuaranteed(command, null);
            Thread.sleep(4000L); // not enough time to retry
            comm.setSleepPeriod(0L); // let it finish - it won't timeout anymore
            assert comm.getSentCount() == 1 : "should not have attempted to resend the command, only sent this once";
            Thread.sleep(3500L); // takes us past the retry interval plus the simulated processing time, it'll retry and be successful
            assert comm.getSentCount() == 2 : "should have attempted to resend the command one time, only sent this twice";
            assert comm.getSentSuccessfulCount() == 1 : "should have sent the command successfully by now";
            Thread.sleep(6000L);
            assert comm.getSentCount() == 2 : "should not have attempted to resend the command any more";
            assert comm.getSentSuccessfulCount() == 1 : "should not have sent the command more than once successfully";
        } finally {
            sender.stopSending(false);
            getPersistentFifoFile(true);
        }

        return;
    }

    /**
     * Tests sending a guaranteed command while the sender is not sending. After starting up the sender, the command
     * (which should have been persisted) will be sent.
     *
     * @throws Exception
     */
    public void testSendGuaranteedWhileNotSending() throws Exception {
        DummyRemoteCommunicator comm = new DummyRemoteCommunicator();
        GenericCommand command = createGenericCommand();
        ClientCommandSenderConfiguration config = createConfig();
        File cmd_spool_file = getPersistentFifoFile(true);
        ClientCommandSender sender = new ClientCommandSender(comm, config);

        try {
            try {
                sender.sendSynch(command);
                assert false : "Not sending yet, should not have been allowed to send a sync command";
            } catch (Exception ok) {
            }

            sender.sendAsynchGuaranteed(command, null);

            PersistentFifo fifo = new PersistentFifo(cmd_spool_file, config.commandSpoolFileMaxSize,
                config.commandSpoolFilePurgePercentage, false);
            assert fifo.count() == 1 : "not sending so we should have spooled that guaranteed command to disk";
            assert comm.getSentCount() == 0 : "should not have sent any command yet";
            sender.startSending();
            Thread.sleep(1000L); // give it time to dequeue and send; there is no throttling enabled so the sending should happen fast
            assert fifo.count() == 0 : "the command should have been unspooled after the sender was started";
            assert comm.getSentCount() == 1 : "should have sent the command by now";
            assert comm.getSentSuccessfulCount() == 1 : "should have sent the command by now";
        } finally {
            sender.stopSending(false);
            getPersistentFifoFile(true);
        }

        return;
    }

    /**
     * Tests sending a guaranteed command while the sender is not sending with a non-serializable callback. The command
     * should still be sent even if the callback cannot be called.
     *
     * @throws Exception
     */
    public void testSendGuaranteedWhileNotSendingWithNonSerializableCallback() throws Exception {
        DummyRemoteCommunicator comm = new DummyRemoteCommunicator();
        GenericCommand command = createGenericCommand();
        ClientCommandSenderConfiguration config = createConfig();
        File cmd_spool_file = getPersistentFifoFile(true);
        ClientCommandSender sender = new ClientCommandSender(comm, config);

        try {
            try {
                sender.sendSynch(command);
                assert false : "Not sending yet, should not have been allowed to send a sync command";
            } catch (Exception ok) {
            }

            sender.sendAsynchGuaranteed(command, new CommandResponseCallback() {
                private static final long serialVersionUID = 1L;
                private final Thread notSerializable = Thread.currentThread();

                public void commandSent(CommandResponse response) {
                    assert notSerializable != null : "This should never get here";
                }
            });

            PersistentFifo fifo = new PersistentFifo(cmd_spool_file, config.commandSpoolFileMaxSize,
                config.commandSpoolFilePurgePercentage, false);
            assert fifo.count() == 1 : "not sending so we should have spooled that guaranteed command to disk";
            assert comm.getSentCount() == 0 : "should not have sent any command yet";
            sender.startSending();
            Thread.sleep(1000L); // give it time to dequeue and send; there is no throttling enabled so the sending should happen fast
            assert fifo.count() == 0 : "the command should have been unspooled after the sender was started";
            assert comm.getSentCount() == 1 : "should have sent the command by now";
            assert comm.getSentSuccessfulCount() == 1 : "should have sent the command by now";
        } finally {
            sender.stopSending(false);
            getPersistentFifoFile(true);
        }

        return;
    }

    /**
     * Creates a config - caller can change settings as appropriate.
     *
     * @return the config
     */
    private ClientCommandSenderConfiguration createConfig() {
        ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();

        config.dataDirectory = new File(System.getProperty("java.io.tmpdir"));
        config.defaultTimeoutMillis = 60000L;
        config.maxConcurrent = 1;
        config.commandSpoolFileName = "command-spool.dat";
        config.commandSpoolFileMaxSize = 100000L;
        config.commandSpoolFilePurgePercentage = 90;
        config.serverPollingIntervalMillis = 0L;
        config.commandSpoolFileCompressData = false;
        config.retryInterval = 10000L;
        config.queueSize = 5;

        // queue throttling
        config.enableQueueThrottling = false;
        config.queueThrottleMaxCommands = 2L;
        config.queueThrottleBurstPeriodMillis = 1000L;

        // send throttling
        config.enableSendThrottling = false;
        config.sendThrottleMaxCommands = 2L;
        config.sendThrottleQuietPeriodDurationMillis = 1000L;

        return config;
    }

    /**
     * Creates a simple test command.
     *
     * @return test command
     */
    private GenericCommand createGenericCommand() {
        GenericCommand cmd = new GenericCommand();
        cmd.setCommandType(new CommandType("test", 1));
        return cmd;
    }

    /**
     * Returns the spool file. Caller can ask to start with an empty one by passing in <code>true</code>.
     *
     * @param  delete_it if caller wants to make sure the persistent file is empty, set this to <code>true</code>
     *
     * @return the spool file where command/callback pairs are stored
     */
    private File getPersistentFifoFile(boolean delete_it) {
        // we know the file name that the command spool file is - that's always the same and its in the data directory
        File ret_file = new File(createConfig().dataDirectory, "command-spool.dat");

        if (delete_it) {
            // in case we can't outright delete it, let's first empty it
            try {
                new PersistentFifo(ret_file, 10000L, 0, false).initializeEmptyFile();
            } catch (IOException ignore) {
            }

            // now try to delete it
            ret_file.delete();
        }

        return ret_file;
    }
}