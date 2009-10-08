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
package org.rhq.enterprise.communications.util;

/**
 * Thrown when the {@link ConcurrencyManager} determines that a thread is not permitted to continue.
 *
 * <p>This exception has a sleep-before-retry attribute that is used by the client where this exception bubbles up to.
 * It is a hint to the client that it should wait the given number of milliseconds before it should attempt to retry, if
 * it wants to retry at all.</p>
 *
 * @author John Mazzitelli
 */
public class NotPermittedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private long sleepBeforeRetry;

    public NotPermittedException(long sleepBeforeRetry) {
        super();
        this.sleepBeforeRetry = sleepBeforeRetry;
    }

    /**
     * A catcher of this exception should not attempt to retry what it just did to cause this exception until after this
     * given amount of milliseconds elapses.
     *
     * @return time, in milliseconds, that the client should wait before doing its thing again
     */
    public long getSleepBeforeRetry() {
        return sleepBeforeRetry;
    }
}