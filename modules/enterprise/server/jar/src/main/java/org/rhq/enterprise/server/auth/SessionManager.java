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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the JON Server's own session ID generator. It is outside any container-provided session mechanism. Its sole
 * purpose is to provide session IDs to logged in {@link Subject}s. It will timeout those sessions regardless of any
 * container-provided session-timeout mechanism.
 *
 * <p>This object is a {@link #getInstance() singleton}.</p>
 */
public class SessionManager {
    /**
     * Our source for random session IDs.
     */
    private static Random _random = new Random();

    /**
     * Our session cache that is keyed on the session ID.
     */
    private static Map<Integer, AuthSession> _cache = new HashMap<Integer, AuthSession>();

    /**
     * The singleton instance
     */
    private static SessionManager _manager = new SessionManager();

    /**
     * The timeout for all user sessions.
     */
    private static final long DEFAULT_TIMEOUT = 1000 * 60 * 90;

    /**
     * The timeout for overlord sessions.
     */
    private static final long OVERLORD_TIMEOUT = 1000 * 60 * 2;

    /**
     * We know that our overlord user always has a subject ID of this value.
     */
    private static final int OVERLORD_SUBJECT_ID = 1;

    /**
     * The overlord never, ever gets updated - it is static and the same forever, so we cache it here.
     */
    private static Subject overlordSubject = null;

    /**
     * Default private constructor to prevent instantiation.
     */
    private SessionManager() {
    }

    /**
     * Return the singleton object.
     *
     * @return the {@link SessionManager}
     */
    public static SessionManager getInstance() {
        return _manager;
    }

    /**
     * Associates a {@link Subject} with a new session id. The new session will use the
     * {@link #DEFAULT_TIMEOUT default timeout}.
     *
     * @param  subject
     *
     * @return the session id assigned to the new session
     */
    public int put(Subject subject) {
        return put(subject, DEFAULT_TIMEOUT);
    }

    /**
     * Associates a {@link Subject} with a new session id with the given session timeout.
     *
     * @param  subject
     * @param  timeout the timeout for the session, in milliseconds
     *
     * @return the session id assigned to the new session
     */
    public synchronized int put(Subject subject, long timeout) {
        Integer key;

        do {
            key = new Integer(_random.nextInt());
        } while (_cache.containsKey(key));

        subject.setSessionId(key);

        _cache.put(key, new AuthSession(subject, timeout));

        return key.intValue();
    }

    /**
     * Lookup and return the session ID that is associated with the given username.
     *
     * @param  username the username of the {@link Subject} that has a valid session
     *
     * @return session ID for the session of the give user
     *
     * @throws SessionNotFoundException
     * @throws SessionTimeoutException
     */
    public synchronized int getSessionIdFromUsername(String username) throws SessionNotFoundException,
        SessionTimeoutException {
        for (Map.Entry<Integer, AuthSession> map_entry : _cache.entrySet()) {
            int session_id = map_entry.getKey().intValue();
            AuthSession session = map_entry.getValue();

            if (session.getSubject(false).getName().equals(username)) {
                if (session.isExpired()) {
                    throw new SessionTimeoutException();
                }

                session.getSubject(true); // this is our session - update the last access time

                return session_id;
            }
        }

        throw new SessionNotFoundException();
    }

    /**
     * Returns the {@link Subject} associated with the given session id.
     *
     * @param  sessionId The session id
     *
     * @return the {@link Subject} associated with the session id
     *
     * @throws SessionNotFoundException
     * @throws SessionTimeoutException
     */
    public synchronized Subject getSubject(int sessionId) throws SessionNotFoundException, SessionTimeoutException {
        Integer id = new Integer(sessionId);
        AuthSession session = _cache.get(id);

        if (session == null) {
            throw new SessionNotFoundException();
        }

        if (session.isExpired()) {
            invalidate(sessionId);
            throw new SessionTimeoutException();
        }

        return session.getSubject(true);
    }

    /**
     * Invalidates the session associated with the given session ID.
     *
     * @param sessionId session id to invalidate
     */
    public synchronized void invalidate(int sessionId) {
        _cache.remove(new Integer(sessionId));

        // while we are here, let's go through the entire session cache and remove expired sessions
        List<Integer> expired_session_ids = new ArrayList<Integer>();

        for (Map.Entry<Integer, AuthSession> map_entry : _cache.entrySet()) {
            AuthSession session = map_entry.getValue();
            if (session.isExpired()) {
                expired_session_ids.add(map_entry.getKey());
            }
        }

        for (Integer expired_session_id : expired_session_ids) {
            _cache.remove(expired_session_id);
        }

        return;
    }

    public Subject getOverlord() {
        if (overlordSubject == null) {
            overlordSubject = LookupUtil.getSubjectManager().getSubjectById(OVERLORD_SUBJECT_ID);

            if (overlordSubject == null) {
                String err = "Cannot find the system's superuser - the database might be corrupted";
                throw new IllegalStateException(err);
            }

            put(overlordSubject, OVERLORD_TIMEOUT);
        }

        int session_id = overlordSubject.getSessionId().intValue();

        try {
            // validate that the superuser session is still valid and update its LAT
            getSubject(session_id);
        } catch (SessionException e) {
            // its been a while since the overlord has been needed - its session has expired.
            // We need to create a new session and assign that new session ID to the instance this singleton holds internally
            // no need to synchronize here - its OK if we concurrently create more than one session, they will eventually expire
            session_id = put(overlordSubject, OVERLORD_TIMEOUT);
        }

        // we create a separate and detached Subject for each caller - do not share the copy this singleton holds internally
        Subject copy = new Subject();
        copy.setSessionId(session_id);
        copy.setId(overlordSubject.getId());
        copy.setFsystem(overlordSubject.getFsystem());
        copy.setFactive(overlordSubject.getFactive());
        copy.setName(overlordSubject.getName());
        copy.setFirstName(overlordSubject.getFirstName());
        copy.setLastName(overlordSubject.getLastName());
        copy.setRoles(overlordSubject.getRoles());

        return copy;
    }
}