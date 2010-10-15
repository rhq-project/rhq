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
 */
public class UserSessionManager {
    public static int SESSION_TIMEOUT = 29 * 60 * 1000; // 29 mins, just shorter than the 30-min web session timeout
    private static int LOGOUT_DELAY = 5 * 1000; // wait 5 seconds for in-flight requests to complete before logout

    public static final String SESSION_NAME = "RHQ_Sesssion";
    public static final String SESSION_LAST_ACCESS = SESSION_NAME + ".LAST_ACCESS";

    private static Subject sessionSubject;
    private static UserPreferences userPreferences;

    private static boolean loggedIn = false;
    private static Timer sessionTimer = new Timer() {
        @Override
        public void run() {
            Log.info("Session timer expired.");
            new LoginView().showLoginDialog(); // log user out, show login dialog
        }
    };
    private static Timer logoutTimer = new Timer() {
        @Override
        public void run() {
            logoutServerSide();
        }
    };

    private UserSessionManager() {
        // static access only
    }

    private static Boolean needsRegistration = false;

    public static void checkLoginStatus(final String user, final String password, final AsyncCallback<Subject> callback) {
        BrowserUtility.forceIe6Hacks();

        RequestBuilder b = new RequestBuilder(RequestBuilder.GET, "/sessionAccess");
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {
                    Log.info("response text = " + response.getText());
                    String sessionIdString = response.getText();

                    //Checks for valid session strings
                    if (sessionIdString != null && sessionIdString.length() > 0) {

                        String[] parts = sessionIdString.split(":");
                        final int subjectId = Integer.parseInt(parts[0]);
                        final String sessionId = parts[1]; // not null
                        final long lastAccess = Long.parseLong(parts[2]);
                        Cookies.setCookie(SESSION_LAST_ACCESS, String.valueOf(lastAccess));
                        Log.info("sessionAccess-subjectId: " + subjectId);
                        Log.info("sessionAccess-sessionId: " + sessionId);
                        Log.info("sessionAccess-lastAccess: " + lastAccess);

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

                        // set the session subject, so the fetch to load the configuration works
                        Subject subject = new Subject();
                        subject.setId(subjectId);
                        subject.setSessionId(Integer.valueOf(sessionId));
                        sessionSubject = subject;

                        //checks to see if this user needs registration.
                        if (subject.getId() == 0) {
                            // Subject with a ID of 0 means the subject wasn't in the database but the login succeeded.
                            // This means the login method detected that LDAP authenticated the user and just gave us a dummy subject.
                            // Set the needs-registration flag so we can eventually steer the user to the LDAP registration workflow.
                            //                            needsRegistration = true;
                            needsRegistration = true;
                        }

                        // figure out if ldap auth is used and whether case insenitive ldap auth requests should be handled.
                        GWTServiceLookup.getLdapService().checkSubjectForLdapAuth(subject, user, password,
                            new AsyncCallback<Subject>() {
                                public void onFailure(Throwable caught) {
                                    Log.warn("Unable to check subject for LDAP authorization - check Server status."
                                        + caught.getMessage());
                                    //TODO: how/what to display in LoginView when unexpected communication with server occurs?
                                    //                                    LoginView
                                    //                                        .displayFormError("UserSessionManager: Unable to check subject for LDAP authorization "
                                    //                                            + "- check Server status.");
                                    new LoginView().showLoginDialog();
                                }

                                public void onSuccess(Subject checked) {
                                    //now pull the flags/information back out of this subject
                                    if (checked == null) {//no new subject was returned.
                                        Log.trace("No alternative case insensitive LDAP accounts located.");
                                        locateSubjectOrLogin(subjectId, sessionId, user, password, callback);
                                    } else {//alternative Subject returned meaning we located
                                        Log.trace("Case insensitive matching LDAP account located.");
                                        needsRegistration = false;
                                        //change the subject.sessionId
                                        sessionSubject = checked;
                                        locateSubjectOrLogin(checked.getId(), String.valueOf(checked.getSessionId()),
                                            checked.getName(), password, callback);
                                    }
                                    Log.trace("Subject registration required:" + needsRegistration);
                                }
                            });
                    } else {//invalid session. Back to login
                        loggedIn = false;
                        new LoginView().showLoginDialog();
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

    /**
     * 
     * @param subjectId
     * @param sessionId
     * @param user
     * @param callback
     */
    private static void locateSubjectOrLogin(int subjectId, final String sessionId, final String user, String password,
        final AsyncCallback<Subject> callback) {
        if (subjectId > 0) {//registration not needed
            Log.trace("SubjectCriteria search with subjectId:" + subjectId);
            SubjectCriteria criteria = new SubjectCriteria();
            criteria.fetchConfiguration(true);
            criteria.addFilterId(subjectId);

            //pipe into next asynchronous call.
            GWTServiceLookup.getSubjectService().findSubjectsByCriteria(criteria,
                new AsyncCallback<PageList<Subject>>() {
                    public void onFailure(Throwable caught) {
                        //TODO: how/what to display in LoginView when unexpected communication with server occurs?
                        //                                    LoginView
                        //                                        .displayFormError("UserSessionManager: Unable to check subject for LDAP authorization "
                        //                                            + "- check Server status.");
                        Log.debug("Failed to load user's subject");
                        //show login dialog
                        new LoginView().showLoginDialog();
                    }

                    public void onSuccess(PageList<Subject> result) {
                        Subject subject = result.get(0);
                        Log.trace("Found subject [" + subject + "].");
                        subject.setSessionId(Integer.valueOf(sessionId));

                        // reset the session subject to the latest, for wrapping in user preferences
                        sessionSubject = subject;
                        //insert ldap check logic
                        userPreferences = new UserPreferences(sessionSubject);
                        refresh();

                        callback.onSuccess(subject);
                    }
                });
        } else {
            Log.trace("Proceeding with registration for ldap user '" + user + "'.");
            new LoginView().showRegistrationDialog(user, sessionId, password, callback);
        }
    }

    public static void login() {
        login(null, null);
    }

    /**Same as login, but passes in credentials optionally needed during new LDAP user registration.
     * 
     * @param user
     * @param password
     */
    public static void login(String user, String password) {
        checkLoginStatus(user, password, new AsyncCallback<Subject>() {
            public void onSuccess(Subject result) {
                // will build UI if necessary, then fires history event
                loggedIn = true;
                if (result != null) {// subject and session has been updated during this login request
                    Log.trace("A new subject and session has been returned. Updating sessionSubject.");
                    sessionSubject = result;
                }
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

    public static void refresh() {
        refresh(SESSION_TIMEOUT);
    }

    private static void refresh(int millis) {
        // if quickly logging back in, first cancel the logout timer so that we
        // don't have race conditions to the server where the login request beats
        // the logout request, which would appear to the user for the login to
        // have failed since it will boot them back to the login screen.
        logoutTimer.cancel();

        // now continue with the rest of the login logic
        loggedIn = true;
        Log.info("Refreshing session timer...");
        sessionTimer.schedule(millis);
    }

    public static void logout() {
        if (!loggedIn) {
            return; // nothing to do, already called
        }

        invalidateSession();
    }

    /** There are times when you're logged in but you don't want the application to proceed as if you are. 
     *  In these cases, like LDAP new user registration, the session only needs
     *  to be invalidated to reset the user back to the beginning.
     */
    public static void invalidateSession() {

        loggedIn = false;
        Log.info("Destroying session timer...");
        sessionTimer.cancel();

        // log out the web session on the server-side in a delayed fashion,
        // allowing enough time to pass to let in-flight requests complete
        logoutTimer.schedule(LOGOUT_DELAY);
    }

    private static void logoutServerSide() {
        try {
            if (getSessionSubject() != null) {
                GWTServiceLookup.getSubjectService().logout(UserSessionManager.getSessionSubject(),
                    new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to logout", caught);
                        }

                        public void onSuccess(Void result) {
                        }
                    });
            }
        } catch (Throwable caught) {
            CoreGUI.getErrorHandler().handleError("Failed to logout", caught);
        }
    }

    public static boolean isLoggedIn() {
        Log.trace("isLoggedIn = " + loggedIn);
        return loggedIn;
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

    public static String getLastAccessTime() {
        return Cookies.getCookie(SESSION_LAST_ACCESS);
    }
}
