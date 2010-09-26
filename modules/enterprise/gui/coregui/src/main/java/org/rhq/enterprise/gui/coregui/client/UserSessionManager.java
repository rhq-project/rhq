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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.preferences.UserPreferences;

/**
 * First time this class is loaded, local loggedIn bit will be false, which implies user is not logged in.
 * 
 * If login successful, CoreGUI/SearchGUI will call setSessionSubject, which sets local loggedIn bit to true.
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
    private static int LOGOUT_DELAY = 30 * 1000; // 30 seconds

    private static Subject sessionSubject;
    private static UserPreferences userPreferences;

    private UserSessionManager() {
        // static access only
    }

    private static boolean loggedIn = false;
    private static Timer sessionTimer = new Timer() {
        @Override
        public void run() {
            System.out.println("Session Timer Expired");
            new LoginView().showLoginDialog(); // log user out, show login dialog
        }
    };
    private static Timer logoutTimer = new Timer() {
        @Override
        public void run() {
            logoutServerSide();
        }
    };

    public static void refresh() {
        // if quickly logging back in, first cancel the logout timer so that we
        // don't have race conditions to the server where the login request beats
        // the logout request, which would appear to the user for the login to
        // have failed since it will boot them back to the login screen.
        logoutTimer.cancel();

        // now continue with the rest of the login logic
        loggedIn = true;
        System.out.println("Refreshing Session Timer");
        sessionTimer.schedule(SESSION_TIMEOUT);
    }

    public static void logout() {
        if (!loggedIn) {
            return; // nothing to do, already called
        }

        loggedIn = false;
        System.out.println("Destroying Session Timer");
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
        System.out.println("isLoggedIn = " + loggedIn);
        return loggedIn;
    }

    public static Subject getSessionSubject() {
        return sessionSubject;
    }

    public static void setSessionSubject(Subject subject) {
        sessionSubject = subject;
        userPreferences = new UserPreferences(sessionSubject);
        refresh();
    }

    public static UserPreferences getUserPreferences() {
        return userPreferences;
    }
}
