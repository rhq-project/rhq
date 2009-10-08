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
package org.rhq.core.clientapi.agent.operation;

import java.io.Serializable;

/**
 * Encapsulates the results of a cancelation request.
 *
 * @author John Mazzitelli
 */
public class CancelResults implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Defines the different states an operation job can be in when being canceled.
     */
    public enum InterruptedState {
        /**
         * The operation job has already finished.
         * This is not cancelable since the operation has already been invoked
         * and it completed, i.e. there is nothing to cancel.
         */
        FINISHED,

        /**
         * The operation job was running at the time it was canceled.
         * This means the plugin was currently performing the operation and thus
         * the operation may or may not be cancelable (depends on the plugin
         * implementation as to whether it allows the invocation to be canceled).
         */
        RUNNING,

        /**
         * The operation job was queued but not yet handed off to the plugin to be invoked.
         * This is normally a state that is always cancelable since
         * the operation has not actually been invoked yet.
         */
        QUEUED,

        /**
         * The operation invocation's job ID is not known and therefore nothing
         * can be canceled.
         */
        UNKNOWN
    }

    private final InterruptedState interruptedState;

    /**
     * Constructor for {@link CancelResults}.
     *
     * @param interruptedState
     */
    public CancelResults(InterruptedState interruptedState) {
        this.interruptedState = interruptedState;
    }

    /**
     * Returns the state the operation was in when it was canceled. See the javadoc on the {@link InterruptedState} enum
     * for the definitions of each state.
     *
     * @return the state the operation invocation was in when it was canceled
     */
    public InterruptedState getInterruptedState() {
        return interruptedState;
    }
}