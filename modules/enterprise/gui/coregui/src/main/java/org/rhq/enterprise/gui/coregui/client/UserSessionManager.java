/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.preferences.UserPreferences;

/**
 * Upon application load, if already loggedIn on the server-side, local loggedIn bit will be set to true.
 * 
 * If login successful, the local loggedIn bit will be set to true.
 * If user clicks logout explicitly, LoginView will be shown, which sets local loggedIn bit to false.
 * If count down timer expires, LoginView will be shown, which sets local loggedIn bit to false.
 * 
 * If error during GWT RPC Service, check local loggedIn status
 *    If loggedIn bit false, display LoginView
 *    Else check server-side logged in state
 *       If logged out on server-side, LoginView will be shown, which sets local loggedIn bit to false.
 * 
 * @author Joseph Marques
 * @author Jay Shaughnessy
 */
public class UserSessionManager {
    private static final Messages MSG = CoreGUI.getMessages();

    public static int SESSION_TIMEOUT = 29 * 60 * 1000; // 29 mins, just shorter than the 30-min web session timeout
    private static int LOGOUT_DELAY = 5 * 1000; // wait 5 seconds for in-flight requests to complete before logout

    // The web session
    public static final String SESSION_NAME = "RHQ_Session";

    // The web session scheduled to be logged out on the server side 
    private static final String DOOMED_SESSION_NAME = "RHQ_DoomedSession";
    private static final String LOCATOR_ID = "SessionManagerLogin";

    private static Subject sessionSubject;
    private static UserPreferences userPreferences;

    private static Timer logoutTimer = new Timer() {
        @Override
        public void run() {
            logoutServerSide();
        }
    };

    private static Timer sessionTimer = new Timer() {
        @Override
        public void run() {
            Log.info("Session timer expired.");
            new LoginView(LOCATOR_ID).showLoginDialog();
        }
    };

    enum State {
        IS_LOGGED_IN, //
        IS_REGISTERING, //
        IS_LOGGED_OUT, //
        IS_UNKNOWN;
    }

    // At entry or browser refresh set state IS_UNKNOWN
    private static State sessionState = State.IS_UNKNOWN;

    private UserSessionManager() {
        // static access only        
    }

    public static void checkLoginStatus(final String user, final String password, final AsyncCallback<Subject> callback) {
        BrowserUtility.forceIe6Hacks();

        final RequestBuilder b = new RequestBuilder(RequestBuilder.GET, "/sessionAccess");
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {

                    Log.info("response text = " + response.getText());
                    String sessionIdString = response.getText();

                    // If a session is active it will return valid session strings
                    if (sessionIdString != null && sessionIdString.length() > 0) {

                        String[] parts = sessionIdString.split(":");
                        final int subjectId = Integer.parseInt(parts[0]);
                        final String sessionId = parts[1]; // not null
                        final long lastAccess = Long.parseLong(parts[2]);
                        Log.info("sessionAccess-subjectId: " + subjectId);
                        Log.info("sessionAccess-sessionId: " + sessionId);
                        Log.info("sessionAccess-lastAccess: " + lastAccess);

                        // There is a window of LOGOUT_DELAY ms where the coreGui session is logged out but the
                        // server session is valid (to allow in-flight requests to process successfully). During
                        // this window prevent a browser refresh (F5) from being able to bypass the
                        // loginView and hijack the still-valid server session. We need to allow:
                        // 1) a browser refresh when coreGui is logged in (no doomedSession)
                        // 2) a valid, quick re-login (sessionState loggedOut, not unknown)
                        // Being careful of these scenarios, catch the bad refresh situation and
                        // redirect back to loginView
                        if (State.IS_UNKNOWN == sessionState && sessionId.equals(getDoomedSessionId())) {

                            // a browser refresh kills any existing logoutTimer. Reschedule the logout.
                            sessionState = State.IS_LOGGED_OUT;
                            scheduleLogoutServerSide(sessionId);

                            new LoginView(LOCATOR_ID).showLoginDialog();
                            return;
                        }

                        String previousSessionId = getPreviousSessionId(); // may be null  
                        Log.info("sessionAccess-previousSessionId: " + previousSessionId);

                        if (previousSessionId == null || previousSessionId.equals(sessionId) == false) {

                            // persist sessionId if different from previously saved sessionId
                            Log.info("sessionAccess-savingSessionId: " + sessionId);
                            saveSessionId(sessionId);

                            // new sessions get the full 29 minutes to expire
                            Log.info("sessionAccess-schedulingSessionTimeout: " + SESSION_TIMEOUT);
                            sessionTimer.schedule(SESSION_TIMEOUT);
                        } else {

                            // existing sessions should expire 29 minutes from the previous access time
                            long expiryTime = lastAccess + SESSION_TIMEOUT;
                            long expiryMillis = expiryTime - System.currentTimeMillis();

                            // can not schedule a time with millis less than or equal to 0
                            if (expiryMillis < 1) {
                                expiryMillis = 1; // expire VERY quickly
                            } else if (expiryMillis > SESSION_TIMEOUT) {
                                expiryMillis = SESSION_TIMEOUT; // guarantees maximum is 29 minutes
                            }

                            Log.info("sessionAccess-reschedulingSessionTimeout: " + expiryMillis);
                            sessionTimer.schedule((int) expiryMillis);
                        }

                        // Certain logins may not follow a "LogOut" history item. Specifically, if the session timer
                        // causes a logout the History token will be the user's current view.  If the same user
                        // logs in again his view should be maintained, but if the subsequent login is for a
                        // different user we want him to start fresh, so in this case ensure a proper
                        // History token is set.
                        if (!History.getToken().equals("LogOut")) {

                            if (null != sessionSubject && sessionSubject.getId() != subjectId) {

                                // on user change register the logout                                
                                History.newItem("LogOut", false);

                            }
                            // TODO else {

                            // We don't currently capture enough state info to solve this scenario:
                            // 1) session expires
                            // 2) browser refresh
                            // 3) log in as different user.
                            // In this case the previous user's path will be the initial view for the new user. To
                            // solve this we'd need to somehow flag that a browser refresh has occurred. This may
                            // be doable by looking for state transitions from UNKNOWN to other states.
                            // }
                        }

                        // set the session subject, so the fetch to load the configuration works
                        final Subject subject = new Subject();
                        subject.setId(subjectId);
                        subject.setSessionId(Integer.valueOf(sessionId));
                        sessionSubject = subject;

                        // populate the username for the subject for isUserWithPrincipal check in ldap processing
                        subject.setName(user);

                        if (subject.getId() == 0) {//either i)ldap new user registration ii)ldap case sensitive match
                            if ((subject.getName() == null) || (subject.getName().trim().isEmpty())) {
                                //we've lost crucial information, probably in a browser refresh. Send them back through login
                                Log
                                    .trace("Unable to locate information critical to ldap registration/account lookup. Log back in.");
                                sessionState = State.IS_LOGGED_OUT;
                                new LoginView(LOCATOR_ID).showLoginDialog();
                                return;
                            }

                            // BZ-586435: insert case insensitivity for usernames with ldap auth
                            // locate first matching subject and attach.
                            SubjectCriteria subjectCriteria = new SubjectCriteria();
                            subjectCriteria.setCaseSensitive(false);
                            subjectCriteria.setStrict(true);
                            subjectCriteria.fetchRoles(false);
                            subjectCriteria.fetchConfiguration(false);
                            subjectCriteria.addFilterName(subject.getName());

                            //check for case insensitive matches.
                            GWTServiceLookup.getSubjectService().findSubjectsByCriteria(subjectCriteria,
                                new AsyncCallback<PageList<Subject>>() {

                                    public void onFailure(Throwable caught) {//none found, launch registration
                                        //TODO: log to Login.error
                                        Log.warn("Problem querying subjects by criteria during loginStatus check."
                                            + caught.getMessage());
                                        return;
                                    }

                                    //pipe through method to handle case insensitive
                                    public void onSuccess(PageList<Subject> results) {
                                        if (results.size() == 0) {//no case insensitive matches found, launch registration
                                            Log.trace("Proceeding with registration for ldap user '" + user + "'.");
                                            sessionState = State.IS_REGISTERING;
                                            new LoginView(LOCATOR_ID).showRegistrationDialog(subject.getName(),
                                                sessionId, password, callback);
                                            return;
                                        } else {//launch case sensitive code handling
                                            Subject locatedSubject = results.get(0);
                                            Log
                                                .trace("Checked credentials and determined that ldap case insensitive login '"
                                                    + locatedSubject.getName()
                                                    + "' should be used instead of '"
                                                    + user
                                                    + "'");
                                            //use the original username to pass session check.
                                            subject.setName(user);
                                            GWTServiceLookup.getSubjectService().processSubjectForLdap(subject,
                                                password, new AsyncCallback<Subject>() {
                                                    public void onFailure(Throwable caught) {
                                                        Log.debug("Failed to complete ldap processing for subject:"
                                                            + caught.getMessage());
                                                        //TODO: pass message to login dialog.
                                                        new LoginView(LOCATOR_ID).showLoginDialog();
                                                        return;
                                                    }

                                                    public void onSuccess(Subject checked) {
                                                        Log.trace("Proceeding with registration for ldap user '" + user
                                                            + "'.");
                                                        sessionState = State.IS_LOGGED_IN;
                                                        userPreferences = new UserPreferences(checked);
                                                        refresh();

                                                        callback.onSuccess(checked);
                                                        return;
                                                    }
                                                });//end processSubjectForLdap call
                                        }
                                    }
                                });//end findSubjectsByCriteria

                        } else {//else send through regular session check 

                            SubjectCriteria criteria = new SubjectCriteria();
                            criteria.fetchConfiguration(true);
                            criteria.addFilterId(subjectId);

                            GWTServiceLookup.getSubjectService().findSubjectsByCriteria(criteria,
                                new AsyncCallback<PageList<Subject>>() {
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(MSG.util_userSession_loadFailSubject(),
                                            caught);
                                        Log.info("Failed to load user's subject");
                                        //TODO: pass message to login ui.
                                        new LoginView(LOCATOR_ID).showLoginDialog();
                                        return;
                                    }

                                    public void onSuccess(PageList<Subject> results) {
                                        final Subject validSessionSubject = results.get(0);
                                        //update the returned subject with current session id
                                        validSessionSubject.setSessionId(Integer.valueOf(sessionId));

                                        Log.trace("Completed session check for subject '" + validSessionSubject + "'.");

                                        //initiate ldap check for ldap authz update(wrt roles) of subject with silent update
                                        //as the subject.id > 0 then only group authorization updates will occur if ldap configured.
                                        GWTServiceLookup.getSubjectService().processSubjectForLdap(validSessionSubject,
                                            "", new AsyncCallback<Subject>() {
                                                public void onFailure(Throwable caught) {
                                                    Log.warn("Errors occurred processing subject for LDAP."
                                                        + caught.getMessage());
                                                    //TODO: pass informative message to Login UI.
                                                    return;
                                                }

                                                public void onSuccess(Subject result) {
                                                    Log.trace("Succesfully processed subject '"
                                                        + validSessionSubject.getName() + "' for LDAP.");
                                                    return;
                                                }
                                            });

                                        // reset the session subject to the latest, for wrapping in user preferences
                                        sessionSubject = validSessionSubject;
                                        userPreferences = new UserPreferences(sessionSubject);
                                        refresh();
                                        sessionState = State.IS_LOGGED_IN;
                                        callback.onSuccess(validSessionSubject);
                                        return;
                                    }
                                });
                        }//end of server side session check;
                    } else {

                        //invalid client session. Back to login
                        sessionState = State.IS_LOGGED_OUT;
                        new LoginView(LOCATOR_ID).showLoginDialog();
                        return;
                    }
                }

                public void onError(Request request, Throwable exception) {
                    callback.onFailure(exception);
                }
            });
            b.send();
        } catch (RequestException e) {
            callback.onFailure(e);
        } finally {
            BrowserUtility.unforceIe6Hacks();
        }
    }

    public static void login() {
        login(null, null);
    }

    /**Same as login, but passes in credentials optionally needed during ldap[i)new user registration or ii)case insensitive] logins.
     * 
     * @param user
     * @param password
     */
    public static void login(String user, String password) {

        checkLoginStatus(user, password, new AsyncCallback<Subject>() {
            public void onSuccess(Subject result) {
                // will build UI if necessary, then fires history event
                sessionState = State.IS_LOGGED_IN;

                CoreGUI.get().buildCoreUI();
            }

            public void onFailure(Throwable caught) {
                Log.error("Unable to determine login status - check Server status.");
            }

            public String toString() {//attempt to identify call back
                return super.toString() + " UserSessionManager.checkLoginStatus()";
            }
        });
    }

    private static void saveSessionId(String sessionId) {
        Cookies.setCookie(SESSION_NAME, sessionId);
    }

    private static String getPreviousSessionId() {
        return Cookies.getCookie(SESSION_NAME);
    }

    private static void saveDoomedSessionId(String doomedSessionId) {
        Cookies.setCookie(DOOMED_SESSION_NAME, doomedSessionId);
    }

    private static String getDoomedSessionId() {
        return Cookies.getCookie(DOOMED_SESSION_NAME);
    }

    private static void removeDoomedSessionId() {
        Cookies.removeCookie(DOOMED_SESSION_NAME);
    }

    public static void refresh() {
        refresh(SESSION_TIMEOUT);
    }

    private static void refresh(int millis) {
        // now continue with the rest of the login logic
        sessionState = State.IS_LOGGED_IN;

        Log.info("Refreshing session timer...");
        sessionTimer.schedule(millis);
    }

    public static void logout() {
        if (isLoggedOut()) {
            return; // nothing to do, already called
        }

        sessionState = State.IS_LOGGED_OUT;
        Log.info("Destroying session timer...");
        sessionTimer.cancel();

        // log out the web session on the server-side in a delayed fashion,
        // allowing enough time to pass to let in-flight requests complete
        scheduleLogoutServerSide(String.valueOf(sessionSubject.getSessionId()));

    }

    private static void scheduleLogoutServerSide(String sessionId) {
        if (null == sessionId) {
            return;
        }

        String doomedSessionId = getDoomedSessionId();

        // if we are requesting an already doomed sessionId be logged out then cancel any existing timer
        if (sessionId.equals(doomedSessionId)) {
            logoutTimer.cancel();
        }

        saveDoomedSessionId(sessionId);
        logoutTimer.schedule(LOGOUT_DELAY);
    }

    // only call this from the logoutTimer, all logout requests should be scheduled via logoutTimer(String)  
    private static void logoutServerSide() {
        final Integer doomedSessionId = Integer.valueOf(getDoomedSessionId());
        removeDoomedSessionId();

        if (null == doomedSessionId) {
            return;
        }

        // if the subject scheduled for logout is now logged in again, don't log him out. Unlikely but
        // possible for a quick re-login of the same user.
        if (State.IS_LOGGED_IN == sessionState && null != sessionSubject
            && doomedSessionId.equals(sessionSubject.getSessionId())) {
            return;
        }

        try {
            GWTServiceLookup.getSubjectService().logout(doomedSessionId, new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.util_userSession_logoutFail(), caught);
                }

                public void onSuccess(Void result) {
                    if (Log.isTraceEnabled()) {
                        Log.trace("Logged out: " + doomedSessionId);
                    }
                }
            });
        } catch (Throwable caught) {
            CoreGUI.getErrorHandler().handleError(MSG.util_userSession_logoutFail(), caught);
        }
    }

    public static boolean isLoggedIn() {
        if (Log.isTraceEnabled()) {
            Log.trace("isLoggedIn = " + sessionState);
        }
        return sessionState == State.IS_LOGGED_IN;
    }

    public static boolean isLoggedOut() {
        return sessionState == State.IS_LOGGED_OUT;
    }

    public static boolean isRegistering() {
        return sessionState == State.IS_REGISTERING;
    }

    public static Subject getSessionSubject() {
        return sessionSubject;
    }

    public static String getSessionId() {
        if (sessionSubject == null) {
            Log.error("UserSessionManager: sessionSubject is null");
            return null;
        }
        Integer sessionId = sessionSubject.getSessionId();
        if (sessionId == null) {
            Log.error("UserSessionManager: sessionId is null");
            return null;
        }
        return sessionId.toString();
    }

    public static UserPreferences getUserPreferences() {
        return userPreferences;
    }
}
