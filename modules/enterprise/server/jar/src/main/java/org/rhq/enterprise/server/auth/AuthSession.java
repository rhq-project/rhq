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
package org.rhq.enterprise.server.auth;

import org.rhq.core.domain.auth.Subject;

/**
 * This object is stored in the {@link SessionManager} - each {@link Subject} that gets a session will have one of these
 * objects assigned to it and stored in the {@link SessionManager}.
 *
 * <p>This object keeps track of the last time it was accessed; if it hasn't been accessed in a given amount of
 * milliseconds (i.e. the timeout), it is considered {@link #isExpired() expired}.</p>
 */
class AuthSession {
    /**
     * The session timeout.
     */
    private long _timeout;

    /**
     * The subject, aka the user.
     */
    private Subject _subject;

    /**
     * The last access time for this object.
     */
    private long _lastAccess;

    /**
     * Constructor for {@link AuthSession}.
     *
     * @param subject The subject to store
     * @param timeout The timeout for this session in milliseconds
     */
    protected AuthSession(Subject subject, long timeout) {
        _subject = subject;
        _timeout = timeout;
        _lastAccess = System.currentTimeMillis();
    }

    /**
     * Return the {@link Subject} associated with this session. Calling this method resets its last access time.
     *
     * @param  updateLastAccessTime if <code>true</code>, the last access time for this session is set to the current
     *                              time
     *
     * @return the {@link Subject}
     */
    protected Subject getSubject(boolean updateLastAccessTime) {
        if (updateLastAccessTime) {
            _lastAccess = System.currentTimeMillis();
        }

        return _subject;
    }

    /**
     * Check if this session is expired. A session is considered expired if {@link #getSubject(boolean)} hasn't been
     * called in the given timeout time period.
     *
     * @return <code>true</code> if this object hasn't been accessed in the timeout time period
     */
    protected boolean isExpired() {
        return System.currentTimeMillis() > (_lastAccess + _timeout);
    }

    public long getLastAccess() {
        return _lastAccess;
    }
}