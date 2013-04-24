/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.util.Random;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;

@Test
public class SessionManagerTest {
    private static final String TEST_USERNAME = "SessionManagerTestUsername";
    private SessionManager sessionManager = SessionManager.getInstance();

    private int initialSessionCount;

    // because the SessionManager is a singleton, some outside tests in other test classes/suites
    // might have added sessions to our manager previously. So get the number that we start with.
    @BeforeMethod
    public void getInitialSessionCount() {
        sessionManager.purgeTimedOutSessions();
        initialSessionCount = sessionManager.getSessionCount();
    }

    public void testLoginAndInvalidateBySessionId() throws Exception {
        Subject subject = sessionManager.put(getTestSubject());
        assert subject != null;
        assert subject.getSessionId() != null;
        assert (initialSessionCount + 1) == sessionManager.getSessionCount();

        Integer sessionId = subject.getSessionId();
        assert sessionManager.getLastAccess(sessionId) > 0;

        Subject subject2 = sessionManager.getSubject(subject.getSessionId());
        assert subject.equals(subject2);
        assert subject2.getSessionId() == subject.getSessionId();

        sessionManager.invalidate(sessionId); // invalidate by session ID

        try {
            sessionManager.getSubject(sessionId);
            assert false : "The session should be been invalidated";
        } catch (SessionNotFoundException ok) {
            // to be expected
        }

        assert sessionManager.getLastAccess(sessionId) == -1;
        assert initialSessionCount == sessionManager.getSessionCount(); // back to where we were
    }

    public void testLoginAndInvalidateBySubject() throws Exception {
        Subject subject = sessionManager.put(getTestSubject());
        assert subject != null;
        assert subject.getSessionId() != null;
        assert (initialSessionCount + 1) == sessionManager.getSessionCount();

        Integer sessionId = subject.getSessionId();
        assert sessionManager.getLastAccess(sessionId) > 0;

        Subject subject2 = sessionManager.getSubject(subject.getSessionId());
        assert subject.equals(subject2);
        assert subject2.getSessionId() == subject.getSessionId();

        sessionManager.invalidate(subject2.getName()); // invalidate by subject name

        try {
            sessionManager.getSubject(sessionId);
            assert false : "The session should be been invalidated";
        } catch (SessionNotFoundException ok) {
            // to be expected
        }

        assert sessionManager.getLastAccess(sessionId) == -1;
        assert initialSessionCount == sessionManager.getSessionCount(); // back to where we were
    }

    public void testTimeout() throws Exception {
        Subject subject = sessionManager.put(getTestSubject(), 1000L);
        assert null != sessionManager.getSubject(subject.getSessionId());
        assert (initialSessionCount + 1) == sessionManager.getSessionCount();

        Thread.sleep(1200L);

        try {
            sessionManager.getSubject(subject.getSessionId());
            assert false : "The session should have timed out";
        } catch (SessionTimeoutException e) {
            // to be expected
        }

        assert sessionManager.getLastAccess(subject.getSessionId()) == -1;
        assert initialSessionCount == sessionManager.getSessionCount(); // back to where we were
    }

    public void testTimeoutSomeSessions() throws Exception {
        Subject subject = sessionManager.put(getTestSubject(), 1000L);
        assert null != sessionManager.getSubject(subject.getSessionId());
        Subject subject2 = sessionManager.put(getNewSubject("longLivedUser"), 60000L);
        assert null != sessionManager.getSubject(subject2.getSessionId());
        assert (initialSessionCount + 2) == sessionManager.getSessionCount();
        Thread.sleep(1200L);

        try {
            sessionManager.getSubject(subject.getSessionId());
            assert false : "The session should have timed out";
        } catch (SessionTimeoutException e) {
            // to be expected
        }

        assert sessionManager.getLastAccess(subject.getSessionId()) == -1;

        // second subject should still have a valid session
        assert null != sessionManager.getSubject(subject2.getSessionId());
        assert sessionManager.getLastAccess(subject2.getSessionId()) > 0;
        assert (initialSessionCount + 1) == sessionManager.getSessionCount();

        sessionManager.invalidate(subject2.getSessionId()); // clean up test
        assert initialSessionCount == sessionManager.getSessionCount(); // back to where we were
    }

    public void testPurge() throws Exception {
        Subject subject1 = sessionManager.put(getNewSubject("shortLivedUser1"), 1000L);
        assert null != sessionManager.getSubject(subject1.getSessionId());
        Subject subject2 = sessionManager.put(getNewSubject("shortLivedUser2"), 1200L);
        assert null != sessionManager.getSubject(subject2.getSessionId());
        Subject subject3 = sessionManager.put(getNewSubject("longLivedUser"), 60000L);
        assert null != sessionManager.getSubject(subject3.getSessionId());
        assert (initialSessionCount + 3) == sessionManager.getSessionCount();
        Thread.sleep(1500L);

        sessionManager.purgeTimedOutSessions();
        assert (initialSessionCount + 1) == sessionManager.getSessionCount(); // 2 should have been purged, 1 should still be valid

        try {
            sessionManager.getSubject(subject1.getSessionId());
            assert false : "The session should not have been found - it should have been purged!";
        } catch (SessionNotFoundException e) {
            // to be expected
        }

        try {
            sessionManager.getSubject(subject2.getSessionId());
            assert false : "The session should not have been found - it should have been purged!!";
        } catch (SessionNotFoundException e) {
            // to be expected
        }

        // third subject should still have a valid session
        assert null != sessionManager.getSubject(subject3.getSessionId());

        // last access time should be -1 for the purged sessions
        assert sessionManager.getLastAccess(subject1.getSessionId()) == -1;
        assert sessionManager.getLastAccess(subject2.getSessionId()) == -1;
        assert sessionManager.getLastAccess(subject3.getSessionId()) > 0;

        // clean up test
        sessionManager.invalidate(subject3.getSessionId());
        assert initialSessionCount == sessionManager.getSessionCount(); // back to where we were
    }

    private Subject getTestSubject() {
        return getNewSubject(TEST_USERNAME);
    }

    private Subject getNewSubject(String username) {
        Subject s = new Subject(username, true, false);
        s.setId(new Random(System.currentTimeMillis()).nextInt());
        return s;
    }
}
