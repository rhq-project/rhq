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
package org.rhq.coregui.client;

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
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.preferences.UserPreferences;

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
 * Additionally, all login checks go through checkLoginStatus where the following happens
 *
 *  1)HTTP POST request sent to portal.war SessionAccessServlet, which, when logged in:
 *    yes)logged in returns (Subject.id):(Server side session id):(Last Accessed time)
 *    no )empty text and Login screen displayed
 *
 *  2)If logged in there are two flavors of Subject logins that will occur
 *    a)Regular RHQ logins and case sensitive LDAP users with established RHQ accounts:         Subject.id > 0
 *    b)LDAP users i)registering new user ii)Existing but case-insensitive ldap username used : Subject.id == 0
 *
 *    Case a) is trivial and well understood. With case b) the credentials are used to retrieve or create RHQ Subject
 *    instances by terminating previous sessions and creating new ones to be used with all future client
 *    side UI requests.  In case b) the WebUser is updated appropriately.
 *
 * @author Joseph Marques
 * @author Jay Shaughnessy
 * @author Simeon Pinder
 */
public class UserSessionManager {
    private static final Messages MSG = CoreGUI.getMessages();

    // The length of CoreGUI inactivity (no call to refresh()) before a CoreGUI session timeout    
    // Initially: 1 hour
    private static int SESSION_TIMEOUT_MINIMUM = 60 * 60 * 1000;
    private static int sessionTimeout = SESSION_TIMEOUT_MINIMUM;

    // After CoreGUI logout, the delay before the server-side logout. This is the period in which we allow in-flight
    // async requests to complete.
    // Currently: 5 seconds
    private static int LOGOUT_DELAY = 5 * 1000; // 5 seconds

    // We want the CoreGUI session timeout to rule them all.  The rhq subject session is refreshed on each SLSB
    // call. But the HTTP session will die after 30 minutes. Keep it alive until logout by pinging SessionAccessServlet
    // occasionally, as specified by this interval (which must be less than the 30 minutes).
    // Currently: 20 minutes
    private static int SESSION_ACCESS_REFRESH = 20 * 60 * 1000;

    // The web session
    public static final String SESSION_NAME = "RHQ_Session";

    // The web session scheduled to be logged out on the server side
    private static final String DOOMED_SESSION_NAME = "RHQ_DoomedSession";

    // HTTP Header indicating to SessionAccessServlet to update the portal war webUser
    private static final String HEADER_WEB_USER_UPDATE = "rhq_webuser_update";

    // HTTP Header indicating to SessionAccessServlet to update the HTTP session access time
    private static final String HEADER_LAST_ACCESS_UPDATE = "rhq_last_access_update";

    private static Subject sessionSubject;
    private static UserPreferences userPreferences;

    private static Timer logoutTimer = new Timer() {
        @Override
        public void run() {
            logoutServerSide();
        }
    };

    private static Timer coreGuiSessionTimer = new Timer() {
        @Override
        public void run() {
            Log.info("Session timer expired.");
            new LoginView().showLoginDialog();
        }
    };

    private static Timer httpSessionTimer = new Timer() {
        @Override
        public void run() {
            Log.info("HTTP Session refresh timer expired.");
            refreshHttpSession();
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
        //initiate request to portal.war(SessionAccessServlet) to retrieve existing session info if exists
        //session has valid user then <subjectId>:<sessionId>:<lastAccess> else ""
        final RequestBuilder b = createSessionAccessRequestBuilder();
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {

                    Log.info("response text = " + response.getText());
                    String sessionIdString = response.getText();

                    if (sessionIdString.startsWith("booting")) {
                        // "booting" is the string we get back from SessionAccessServlet if StartupBean hasn't finished
                        new LoginView().showLoginDialog(MSG.view_core_serverInitializing());
                        return;
                    }

                    // If a session is active it will return valid session strings
                    if (sessionIdString.length() > 0) {

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

                            new LoginView().showLoginDialog();
                            return;
                        }

                        String previousSessionId = getPreviousSessionId(); // may be null
                        Log.info("sessionAccess-previousSessionId: " + previousSessionId);

                        if (previousSessionId == null || previousSessionId.equals(sessionId) == false) {

                            // persist sessionId if different from previously saved sessionId
                            Log.info("sessionAccess-savingSessionId: " + sessionId);
                            saveSessionId(sessionId);

                            // new sessions get the full SESSION_TIMEOUT period prior to expire
                            Log.info("sessionAccess-schedulingSessionTimeout: " + sessionTimeout);
                            coreGuiSessionTimer.schedule(sessionTimeout);
                        } else {

                            // existing sessions should expire SESSION_TIMEOUT minutes from the previous access time
                            long expiryTime = lastAccess + sessionTimeout;
                            long expiryMillis = expiryTime - System.currentTimeMillis();

                            // can not schedule a time with millis less than or equal to 0
                            if (expiryMillis < 1) {
                                expiryMillis = 1; // expire VERY quickly
                            } else if (expiryMillis > sessionTimeout) {
                                expiryMillis = sessionTimeout; // guarantees maximum
                            }

                            Log.info("sessionAccess-reschedulingSessionTimeout: " + expiryMillis);
                            coreGuiSessionTimer.schedule((int) expiryMillis);
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
                        // populate the username for the subject for isUserWithPrincipal check in ldap processing
                        subject.setName(user);
                        sessionSubject = subject;

                        if (subject.getId() == 0) {//either i)ldap new user registration ii)ldap case sensitive match
                            if ((subject.getName() == null) || (subject.getName().trim().isEmpty())) {
                                //we've lost crucial information, probably in a browser refresh. Send them back through login
                                Log.trace("Unable to locate information critical to ldap registration/account lookup. Log back in.");
                                sessionState = State.IS_LOGGED_OUT;
                                new LoginView().showLoginDialog();
                                return;
                            }

                            Log.trace("Proceeding with case insensitive login of ldap user '" + user + "'.");
                            GWTServiceLookup.getSubjectService().processSubjectForLdap(subject, password,
                                new AsyncCallback<Subject>() {
                                    public void onFailure(Throwable caught) {
                                        // this means either: a) we mapped the username to a previously registered LDAP
                                        // user but login via LDAP failed, or b) we were not able to map the username
                                        // to any LDAP users, previously registered or not.
                                        Log.debug("Failed to complete ldap processing for subject: "
                                            + caught.getMessage());
                                        //TODO: pass message to login dialog.
                                        new LoginView().showLoginDialog();
                                        return;
                                    }

                                    public void onSuccess(final Subject processedSubject) {
                                        //Then found case insensitive and returned that logged in user
                                        //Figure out of this is new user registration
                                        boolean isNewUser = false;
                                        if (processedSubject.getUserConfiguration() != null) {
                                            isNewUser = Boolean.valueOf(processedSubject.getUserConfiguration()
                                                .getSimpleValue("isNewUser", "false"));
                                        }
                                        if (!isNewUser) {
                                            // otherwise, we successfully logged in as an existing LDAP user case insensitively.
                                            Log.trace("Logged in case insensitively as ldap user '"
                                                + processedSubject.getName() + "'");
                                            callback.onSuccess(processedSubject);
                                        } else {// if account is still active assume new LDAP user registration.
                                            Log.trace("Proceeding with registration for ldap user '" + user + "'.");
                                            sessionState = State.IS_REGISTERING;
                                            sessionSubject = processedSubject;

                                            new LoginView().showRegistrationDialog(subject.getName(),
                                                String.valueOf(processedSubject.getSessionId()), password, callback);
                                        }

                                        return;
                                    }

                                });//end processSubjectForLdap call
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
                                        new LoginView().showLoginDialog();
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
                                                    callback.onSuccess(validSessionSubject);
                                                    return;
                                                }

                                                public void onSuccess(Subject result) {
                                                    Log.trace("Successfully processed subject '"
                                                        + validSessionSubject.getName() + "' for LDAP.");
                                                    callback.onSuccess(validSessionSubject);
                                                    return;
                                                }
                                            });
                                    }
                                });
                        }//end of server side session check;

                    } else {
                        //invalid client session. Back to login
                        sessionState = State.IS_LOGGED_OUT;
                        new LoginView().showLoginDialog();
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
        }
    }

    /** Takes an updated Subject and signals SessionAccessServlet in portal.war to update the associated WebUser
     *  because for this specific authenticated user.  Currently assumes Subject instances returned from SubjectManagerBean.processSubjectForLdap.
     *  This should only ever be called by UI logic in LDAP logins(case insensitive/new registration) after RHQ sessions have been renewed server side
     *  correctly.
     *
     * @param loggedInSubject Subject with updated session
     */
    private static void scheduleWebUserUpdate(final Subject loggedInSubject) {
        final RequestBuilder b = createSessionAccessRequestBuilder();
        //add header to signal SessionAccessServlet to update the WebUser for the successfully logged in user
        b.setHeader(HEADER_WEB_USER_UPDATE, String.valueOf(loggedInSubject.getSessionId()));
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {
                    Log.trace("Successfully submitted request to update server side WebUser for subject '"
                        + loggedInSubject.getName() + "'.");
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    Log.trace("Failed to submit request to update server side WebUser for subject '"
                        + loggedInSubject.getName() + "'."
                        + ((exception != null ? exception.getMessage() : " Exception ref null.")));
                }
            });
            b.send();
        } catch (RequestException e) {
            Log.trace("Failure submitting update request for WebUser '" + loggedInSubject.getName() + "'."
                + (e != null ? e.getMessage() : "RequestException reference is null."));
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
            public void onSuccess(final Subject loggedInSubject) {
                // will build UI if necessary, then fires history event
                sessionState = State.IS_LOGGED_IN;

                int storedSessionSubjectId = -1;
                if (sessionSubject != null) {
                    sessionSubject.getId();
                }

                //update the sessionSubject appropriately
                sessionSubject = loggedInSubject;
                sessionState = State.IS_LOGGED_IN;
                userPreferences = new UserPreferences(loggedInSubject);
                userPreferences.setAutomaticPersistence(true);

                GWTServiceLookup.getSystemService().getSessionTimeout(new AsyncCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        try {
                            final long millis = Long.parseLong(result);

                            // let's be safe here
                            sessionTimeout = (int) millis;
                            sessionTimeout = (millis != (long) sessionTimeout) ? Integer.MAX_VALUE : sessionTimeout;
                            sessionTimeout = (sessionTimeout < SESSION_TIMEOUT_MINIMUM) ? SESSION_TIMEOUT_MINIMUM
                                : sessionTimeout;

                        } catch (NumberFormatException e) {
                            sessionTimeout = SESSION_TIMEOUT_MINIMUM;
                        }

                        refresh();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_admin_systemSettings_cannotLoadSettings(),
                            caught);
                    }
                });

                httpSessionTimer.schedule(SESSION_ACCESS_REFRESH);

                //conditionally update session information and server side WebUser when updated.
                if ((sessionSubject != null) && (loggedInSubject.getId() != storedSessionSubjectId)) {
                    //update the sessionSubject appropriately
                    sessionSubject = loggedInSubject;
                    //update the sessionId
                    saveSessionId(String.valueOf(loggedInSubject.getSessionId().intValue()));
                    //Update the portal war WebUser now that we've updated subject+session
                    scheduleWebUserUpdate(loggedInSubject);
                }
                
                // invalidate the CustomDateRangeState instance
                CustomDateRangeState.invalidateInstance();

                CoreGUI.get().init();
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
        refresh(sessionTimeout);
    }

    private static void refresh(int millis) {
        // now continue with the rest of the login logic
        sessionState = State.IS_LOGGED_IN;

        Log.info("Refreshing session timer...");
        coreGuiSessionTimer.schedule(millis);
    }

    public static void logout() {
        if (isLoggedOut()) {
            return; // nothing to do, already called
        }

        sessionState = State.IS_LOGGED_OUT;

        Log.info("Destroying session timer...");
        coreGuiSessionTimer.cancel();

        Log.info("Destroying HTTP session refresh timer...");
        httpSessionTimer.cancel();

        //ResourceTypeRepository.Cache.getInstance().clear();

        CoreGUI.get().reset();

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
            // The logout() method should be called against the dying session. This makes sense and
            // also protects against the fact that sessionSubject may be null at the time of this call.
            // The sessionSubject is used to build the server request, so it must be valid.
            Subject tempSubject = sessionSubject;
            sessionSubject = new Subject();
            sessionSubject.setSessionId(doomedSessionId);

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

            // return sessionSubject to its actual value
            sessionSubject = tempSubject;
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

    /**
     * Obtain an object that you can use to add/modify/remove/retrieve user preferences.
     * You can optionally wrap the returned object with a
     * {@link org.rhq.coregui.client.util.preferences.MeasurementUserPreferences}
     * object to work with measurement preferences.
     *
     * @return user preferences object
     */
    public static UserPreferences getUserPreferences() {
        return userPreferences;
    }

    private static void refreshHttpSession() {
        final RequestBuilder b = createSessionAccessRequestBuilder();
        // add header to signal SessionAccessServlet to refresh the http lastAccess time (basically a no-op as the
        // request will make that happen).
        b.setHeader(HEADER_LAST_ACCESS_UPDATE, "dummy");
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {
                    Log.trace("Successfully submitted request to update HTTP accessTime");
                }

                @Override
                public void onError(Request request, Throwable t) {
                    Log.trace("Error updating HTTP accessTime", t);
                }
            });
            b.send();
        } catch (RequestException e) {
            Log.trace("Error requesting update of HTTP accessTime", e);
        } finally {
            httpSessionTimer.schedule(SESSION_ACCESS_REFRESH);
        }
    }

    private static RequestBuilder createSessionAccessRequestBuilder() {
        final RequestBuilder b = new RequestBuilder(RequestBuilder.POST, "/portal/sessionAccess");
        b.setHeader("Accept", "text/plain");
        return b;
    }

}
