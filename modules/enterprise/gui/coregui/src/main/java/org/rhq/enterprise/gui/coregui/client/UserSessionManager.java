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

    private static int SESSION_TIMEOUT = 29 * 60 * 1000; // 29 mins, just shorter than the 30-min web session timeout
    private static int LOGOUT_DELAY = 15 * 1000; // wait 15 seconds for in-flight requests to complete before logout

    public static final String SESSION_NAME = "RHQ_Sesssion";
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

    public static void checkLoginStatus(final AsyncCallback<Void> callback) {
        BrowserUtility.forceIe6Hacks();

        RequestBuilder b = new RequestBuilder(RequestBuilder.GET, "/sessionAccess");
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {
                    com.allen_sauer.gwt.log.client.Log.info("response text = " + response.getText());
                    String sessionIdString = response.getText();
                    if (sessionIdString != null && sessionIdString.length() > 0) {

                        String[] parts = sessionIdString.split(":");
                        final int subjectId = Integer.parseInt(parts[0]);
                        final String sessionId = parts[1]; // not null
                        final long lastAccess = Long.parseLong(parts[2]);
                        com.allen_sauer.gwt.log.client.Log.info("sessionAccess-subjectId: " + subjectId);
                        com.allen_sauer.gwt.log.client.Log.info("sessionAccess-sessionId: " + sessionId);
                        com.allen_sauer.gwt.log.client.Log.info("sessionAccess-lastAccess: " + lastAccess);

                        String previousSessionId = getPreviousSessionId(); // may be null  
                        com.allen_sauer.gwt.log.client.Log.info("sessionAccess-previousSessionId: " + previousSessionId);
                        if (previousSessionId == null || previousSessionId.equals(sessionId) == false) {

                            // persist sessionId if different from previously saved sessionId
                            com.allen_sauer.gwt.log.client.Log.info("sessionAccess-savingSessionId: " + sessionId);
                            saveSessionId(sessionId);

                            // new sessions get the full 29 minutes to expire
                            com.allen_sauer.gwt.log.client.Log.info("sessionAccess-schedulingSessionTimeout: " + SESSION_TIMEOUT);
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

                            com.allen_sauer.gwt.log.client.Log.info("sessionAccess-reschedulingSessionTimeout: " + expiryMillis);
                            sessionTimer.schedule((int) expiryMillis);
                        }

                        // set the session subject, so the fetch to load the configuration works
                        Subject subject = new Subject();
                        subject.setId(subjectId);
                        subject.setSessionId(Integer.valueOf(sessionId));
                        sessionSubject = subject;

                        SubjectCriteria criteria = new SubjectCriteria();
                        criteria.fetchConfiguration(true);
                        criteria.addFilterId(subjectId);

                        GWTServiceLookup.getSubjectService().findSubjectsByCriteria(criteria,
                            new AsyncCallback<PageList<Subject>>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(
                                        "UserSessionManager: Failed to load user's subject", caught);
                                    com.allen_sauer.gwt.log.client.Log.info("Failed to load user's subject");
                                    new LoginView().showLoginDialog();
                                }

                                public void onSuccess(PageList<Subject> result) {
                                    Subject subject = result.get(0);
                                    Log.debug("Found subject [" + subject + "].");
                                    subject.setSessionId(Integer.valueOf(sessionId));

                                    // reset the session subject to the latest, for wrapping in user preferences
                                    sessionSubject = subject;
                                    userPreferences = new UserPreferences(sessionSubject);
                                    refresh();

                                    callback.onSuccess((Void) null);
                                }
                            });
                    } else {
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

    public static void login() {
        checkLoginStatus(new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // will build UI if necessary, then fires history event
                CoreGUI.get().buildCoreUI();
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error("Unable to determine login status - check Server status.");
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
            return null;
        }
        Integer sessionId = sessionSubject.getSessionId();
        if (sessionId == null) {
            return null;
        }
        return sessionId.toString();
    }

    public static UserPreferences getUserPreferences() {
        return userPreferences;
    }
}
