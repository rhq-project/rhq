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

/**
 * This interface defines an object that is to listen for state changes to a {@link ClientCommandSender}. A state change
 * is either one in which the sender goes from sending-to-not-sending or from not-sending-to-sending.
 *
 * <p>Add implementations of this listener to a sender object via
 * {@link ClientCommandSender#addStateListener(ClientCommandSenderStateListener, boolean)}</p>
 *
 * @author John Mazzitelli
 */
public interface ClientCommandSenderStateListener {
    /**
     * The notification method that is called when the sender this object is listening to has started sending commands
     * to its remote endpoint.
     *
     * <p>If the listener wants to keep listening, it must return <code>true</code>. If this method throws an exception,
     * it is the same as if <code>false</code> is returned; that is, it will be removed and thus no longer receive
     * notifications.</p>
     *
     * @param  sender the sender that emitted the notification
     *
     * @return <code>true</code> if this listener wants to keep listening for state changes; <code>false</code> if the
     *         listener no longer wishes to listen for state changes and should be removed from the sender's list of
     *         listeners.
     */
    boolean startedSending(ClientCommandSender sender);

    /**
     * The notification method that is called when the sender this object is listening to has stopped sending commands
     * to its remote endpoint.
     *
     * <p>If the listener wants to keep listening, it must return <code>true</code>. If this method throws an exception,
     * it is the same as if <code>false</code> is returned; that is, it will be removed and thus no longer receive
     * notifications.</p>
     *
     * @param  sender
     *
     * @return <code>true</code> if this listener wants to keep listening for state changes; <code>false</code> if the
     *         listener no longer wishes to listen for state changes and should be removed from the sender's list of
     *         listeners.
     */
    boolean stoppedSending(ClientCommandSender sender);
}