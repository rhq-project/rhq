 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.pc.agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * Those plugin container managers that need to expose their interfaces remotely (to the JON Server for example) need to
 * extend this class. This class will know what its remote client interface is (see the constructor) and will be able
 * notify a set of listeners when it is started and stopped (to presumably tell the listeners that they should remote
 * and "unremote" its client interface).
 */
public abstract class AgentService {
    private Class clientInterface;
    private Collection<AgentServiceLifecycleListener> listeners;
    private AgentServiceStreamRemoter streamRemoter;

    /**
     * Creates a new {@link AgentService} object.
     *
     * @param clientInterface the client interface that this agent service wants to make remotely accessible to external
     *                        clients
     */
    protected AgentService(Class clientInterface) {
        this.clientInterface = clientInterface;
        this.listeners = new LinkedHashSet<AgentServiceLifecycleListener>();
    }

    /**
     * This is called when the agent service changes its {@link LifecycleState state} - it will notify all listeners of
     * the changed state.
     *
     * @param newState
     */
    public void notifyLifecycleListenersOfNewState(LifecycleState newState) {
        for (AgentServiceLifecycleListener agentServiceLifecycleListener : listeners) {
            switch (newState) {
            case STARTED: {
                agentServiceLifecycleListener.started(this);
                break;
            }

            case STOPPED: {
                agentServiceLifecycleListener.stopped(this);
                break;
            }
            }
        }
    }

    /**
     * Given any input stream, this will attempt to remote it using the
     * {@link #setAgentServiceStreamRemoter(AgentServiceStreamRemoter) stream remoter}, thus providing access to
     * external clients. If there is no remoter available, the same input stream instance passed into this method is
     * returned as-is.
     *
     * <p>If <code>inputStream</code> is <code>null</code>, <code>null</code> is returned.</p>
     *
     * @param  inputStream the input stream to remote
     *
     * @return the input stream possibly wrapped in a remote proxy to enable it for remote access.
     */
    protected InputStream remoteInputStream(InputStream inputStream) {
        if ((inputStream == null) || (streamRemoter == null)) {
            return inputStream;
        }

        return streamRemoter.prepareInputStream(inputStream);
    }

    /**
     * Given any output stream, this will attempt to remote it using the
     * {@link #setAgentServiceStreamRemoter(AgentServiceStreamRemoter) stream remoter}, thus providing access to
     * external clients. If there is no remoter available, the same output stream instance passed into this method is
     * returned as-is.
     *
     * <p>If <code>outputStream</code> is <code>null</code>, <code>null</code> is returned.</p>
     *
     * @param  outputStream the output stream to remote
     *
     * @return the output stream possibly wrapped in a remote proxy to enable it for remote access.
     */
    protected OutputStream remoteOutputStream(OutputStream outputStream) {
        if ((outputStream == null) || (streamRemoter == null)) {
            return outputStream;
        }

        return streamRemoter.prepareOutputStream(outputStream);
    }

    /**
     * Adds the given listener to the list of listeners that will be notified when this agent service changes its state
     * (i.e. is started or stopped).
     *
     * @param listener
     */
    public void addLifecycleListener(AgentServiceLifecycleListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given listener so it is no longer notified of changed states.
     *
     * @param listener
     */
    public void removeLifecycleListener(AgentServiceLifecycleListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets the remoter object that is responsible for remoting streams. If <code>null</code>, the agent service will
     * not be able to remote streams to external clients, as in the case when the plugin container is not running inside
     * an agent (i.e. embedded mode).
     *
     * @param remoter
     */
    public void setAgentServiceStreamRemoter(AgentServiceStreamRemoter remoter) {
        streamRemoter = remoter;
    }

    /**
     * Returns the interface that this agent service wants to be made remotely accessible to external clients. In other
     * words, this is the interface the agent will expose as the service's remote POJO interface.
     *
     * @return the interface that should be remoted so external clients can access it
     */
    public Class getClientInterface() {
        return clientInterface;
    }

    /**
     * The different states agent services can be in. Listeners will be notified when agent services enter one of these
     * states.
     */
    public enum LifecycleState {
        STARTED, STOPPED
    }
}