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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.AllowRhqServerInternalsAccessPermission;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is the JON Server's own session ID generator. It is outside any container-provided session mechanism. Its sole
 * purpose is to provide session IDs to logged in {@link Subject}s. It will timeout those sessions regardless of any
 * container-provided session-timeout mechanism.
 * <p>
 * Because this is a very security-sensitive class, any public method requires the caller to 
 * have the {@link AllowEjbAccessPermission} as any other calls to the EJB layer. This is so that the
 * malicious users can't trick the EJB layer into thinking that some users are logged in or log out other
 * users.
 * <p>
 * Also, for security reasons, this class is final so that malicious code can't subclass it and modify its
 * behavior.
 * 
 * <p>This object is a {@link #getInstance() singleton}.</p>
 */
public final class SessionManager {
    
    private static final AllowRhqServerInternalsAccessPermission ACCESS_PERMISSION = new AllowRhqServerInternalsAccessPermission();
    
    /**
     * Our source for random session IDs.
     */
    private static final Random _random = new Random();

    /**
     * Our session cache that is keyed on the session ID.
     */
    private static final Map<Integer, AuthSession> _cache = new HashMap<Integer, AuthSession>();

    /**
     * The singleton instance
     */
    private static final SessionManager _manager = new SessionManager();

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
     * Returns the number of sessions that are currently held by this manager.
     * This count includes those sessions that may have already timed out but not yet invalidated and purged.
     *
     * @return total number of sessions
     */
    public synchronized int getSessionCount() {
        return _cache.size();
    }

    /**
     * Associates a {@link Subject} with a new session id. The new session will use the
     * {@link #DEFAULT_TIMEOUT default timeout}.
     *
     * @param  subject
     *
     * @return the Subject associated with session. Note, this may be a copy of the Subject passed into the method. The
     * sessionId will be assigned.
     */
    public Subject put(Subject subject) {
        checkPermission();
        return put(subject, DEFAULT_TIMEOUT);
    }

    /**
     * Associates a {@link Subject} with a new session id with the given session timeout.
     *
     * @param  subject
     * @param  timeout the timeout for the session, in milliseconds
     *
     * @return the Subject associated with session. This will be a copy of the Subject passed into the method (unless
     * that Subject is overlord). The sessionId will be assigned.
     */
    public synchronized Subject put(Subject subject, long timeout) {
        checkPermission();
        Integer key;

        do {
            key = new Integer(_random.nextInt());
        } while (_cache.containsKey(key));

        // Each session should have its own POJO Subject so that each can store a separate sessionId. The exception
        // is our special-case shared singleton for overlord.
        Subject sessionSubject;
        if (subject.equals(overlordSubject)) {
            sessionSubject = overlordSubject;
            sessionSubject.setSessionId(key);

        } else {
            sessionSubject = getSessionSubject(subject, key);
        }

        _cache.put(key, new AuthSession(sessionSubject, timeout));

        return sessionSubject;
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
        checkPermission();
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
        checkPermission();
        _cache.remove(new Integer(sessionId));

        // while we are here, let's go through the entire session cache and remove expired sessions
        purgeTimedOutSessions();

        return;
    }

    /**
     * Asks the session manager to examine all sessions and invalidate those sessions that have timed out.
     */
    public synchronized void purgeTimedOutSessions() {
        checkPermission();

        Iterator<Entry<Integer, AuthSession>> iterator = _cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, AuthSession> map_entry = iterator.next();
            AuthSession session = map_entry.getValue();
            if (session.isExpired()) {
                iterator.remove();
            }
        }

        return;
    }

    /**
     * Invalidates all sessions for the given username. This is for testing purposes ONLY.
     *
     * @param username username for the sessions to be invalidated
     */
    public synchronized void invalidate(String username) {
        checkPermission();

        Iterator<Entry<Integer, AuthSession>> iterator = _cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, AuthSession> map_entry = iterator.next();
            AuthSession session = map_entry.getValue();
            if (username.equals(session.getSubject(false).getName())) {
                iterator.remove();
            }
        }

        return;
    }

    public synchronized long getLastAccess(int sessionId) {
        checkPermission();
        AuthSession session = _cache.get(sessionId);
        if (session == null) {
            return -1;
        }
        return session.getLastAccess();
    }

    public Subject getOverlord() {
        checkPermission();
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
            session_id = put(overlordSubject, OVERLORD_TIMEOUT).getSessionId();
            overlordSubject.setSessionId(session_id);
        }

        // we create a separate and detached Subject for each caller - do not share the copy this singleton holds internally
        Subject copy = getSessionSubject(overlordSubject, session_id);
        return copy;
    }

    private Subject getSessionSubject(Subject subject, Integer sessionId) {
        Subject copy = new Subject(subject.getName(), subject.getFactive(), subject.getFsystem());
        copy.setId(subject.getId());
        copy.setSessionId(sessionId);

        copy.setDepartment(subject.getDepartment());
        copy.setEmailAddress(subject.getEmailAddress());
        copy.setFirstName(subject.getFirstName());
        copy.setLastName(subject.getLastName());
        copy.setLdapRoles(subject.getLdapRoles());
        copy.setOwnedGroups(subject.getOwnedGroups());
        copy.setPhoneNumber(subject.getPhoneNumber());
        copy.setRoles(subject.getRoles());
        copy.setUserConfiguration(subject.getUserConfiguration());

        return copy;
    }
    
    private static void checkPermission() {
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) sm.checkPermission(ACCESS_PERMISSION);
    }
}