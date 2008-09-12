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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandResponse;

/**
 * A remote communicator that doesn't really communicate with anything. Its connection flag is maintained so calling
 * {@link #connect()} and {@link #disconnect()} will affect {@link #isConnected()}. Upon instantiation,
 * {@link #isConnected()} will return <code>true</code>.
 *
 * <p>You can simulate exceptions thrown during the {@link #send(Command)} by calling
 * {@link #simulateSendException(Exception)}.</p>
 *
 * @author John Mazzitelli
 */
public class DummyRemoteCommunicator implements RemoteCommunicator {
    private AtomicBoolean m_connected = new AtomicBoolean(true);
    private Exception m_sendException = null;
    private AtomicLong m_sent = new AtomicLong(0L);
    private AtomicLong m_sentSuccessful = new AtomicLong(0L);
    private AtomicLong m_sleepPeriod = new AtomicLong(0L);

    /**
     * If you want the {@link #send(Command)} to simulate an error, pass in the exception you want it to throw. If you
     * want to simulate a successful send, set the exception to <code>null</code>.
     *
     * @param e the exception that {@link #send(Command)} should throw (may be <code>null</code>)
     */
    public void simulateSendException(Exception e) {
        m_sendException = e;
    }

    /**
     * Sets the amount of milliseconds you want the {@link #send(Command)} to sleep before returning or throwing a
     * simulated exception. If the sleep period is less than or equal to 0, the send will not sleep.
     *
     * @param sleep_period time for send to sleep, in milliseconds
     */
    public void setSleepPeriod(long sleep_period) {
        m_sleepPeriod.set(sleep_period);
    }

    /**
     * Resets the counters that count how many times {@link #send(Command)} was called.
     */
    public void resetSentCount() {
        m_sent.set(0L);
        m_sentSuccessful.set(0L);
    }

    /**
     * Returns the number of times {@link #send(Command)} was called. You can reset the counter by calling
     * {@link #resetSentCount()}.
     *
     * @return number of times {@link #send(Command)} was called.
     */
    public long getSentCount() {
        return m_sent.get();
    }

    /**
     * Returns the number of times {@link #send(Command)} was called and simulated a successful command. You can reset
     * the counter by calling {@link #resetSentCount()}.
     *
     * @return number of times {@link #send(Command)} was called without it throwing an exception.
     */
    public long getSentSuccessfulCount() {
        return m_sentSuccessful.get();
    }

    /**
     * @see RemoteCommunicator#connect()
     */
    public void connect() throws Exception {
        m_connected.set(true);
    }

    /**
     * @see RemoteCommunicator#disconnect()
     */
    public void disconnect() {
        m_connected.set(false);
    }

    /**
     * @see RemoteCommunicator#isConnected()
     */
    public boolean isConnected() {
        return m_connected.get();
    }

    /**
     * @see RemoteCommunicator#send(Command)
     */
    public CommandResponse send(Command command) throws Exception {
        m_sent.incrementAndGet();

        long sleep_period = m_sleepPeriod.get();
        if (sleep_period > 0L) {
            Thread.sleep(sleep_period);
        }

        if (m_sendException != null) {
            throw m_sendException;
        }

        m_sentSuccessful.incrementAndGet();

        return new GenericCommandResponse(command, true, null, null);
    }

    @Override
    public FailureCallback getFailureCallback() {
        return null;
    }

    @Override
    public void setFailureCallback(FailureCallback callback) {
    }
}