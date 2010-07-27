/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.search.SearchBar;

/**
 * @author Joseph Marques
 */
public class SearchGUI implements EntryPoint {

    public static SearchGUI singleton = new SearchGUI();
    private static Subject sessionSubject;
    private SearchBar searchBar;

    private SearchGUI() {
    }

    public static SearchGUI get() {
        return singleton;
    }

    public void onModuleLoad() {
        if (SearchBar.existsOnPage() == false) {
            System.out.println("Suppressing load of SearchGUI module");
            return;
        }

        checkLoginStatus();
    }

    public void buildSearchGUI() {
        searchBar = new SearchBar();
    }

    public static void checkLoginStatus() {

        if (CoreGUI.detectIe6()) {
            CoreGUI.forceIe6Hacks();
        }

        RequestBuilder b = new RequestBuilder(RequestBuilder.GET, "/sessionAccess");
        try {
            b.setCallback(new RequestCallback() {
                public void onResponseReceived(final Request request, final Response response) {
                    String sessionIdString = response.getText();
                    if (sessionIdString != null && sessionIdString.length() > 0) {

                        int subjectId = Integer.parseInt(sessionIdString.split(":")[0]);
                        final int sessionId = Integer.parseInt(sessionIdString.split(":")[1]);

                        Subject subject = new Subject();
                        subject.setId(subjectId);
                        subject.setSessionId(sessionId);

                        GWTServiceLookup.registerSession(String.valueOf(subject.getSessionId()));

                        // look up real user prefs

                        SubjectCriteria criteria = new SubjectCriteria();
                        criteria.fetchConfiguration(true);
                        criteria.addFilterId(subjectId);
                        criteria.fetchRoles(true);

                        GWTServiceLookup.getSubjectService().findSubjectsByCriteria(criteria,
                            new AsyncCallback<PageList<Subject>>() {
                                public void onFailure(Throwable caught) {
                                    // can't use this until gwt frame is always present, reserve for futureu
                                    //CoreGUI.getErrorHandler().handleError("Failed to load user's subject", caught);
                                    //SC.say("Failed to load user's subject.");
                                    //new LoginView().showLoginDialog();

                                    System.out.println("Failed to load user's subject");
                                }

                                public void onSuccess(PageList<Subject> result) {

                                    Subject subject = result.get(0);
                                    subject.setSessionId(sessionId);
                                    SearchGUI.sessionSubject = subject;
                                    singleton.buildSearchGUI();

                                }
                            });
                    } else {
                        new LoginView().showLoginDialog();
                    }
                }

                public void onError(Request request, Throwable exception) {
                    SC.say("Unable to determine login status, check server status");
                }
            });
            b.send();
        } catch (RequestException e) {
            SC.say("Unable to determine login status, check server status");
            e.printStackTrace();
        } finally {
            if (CoreGUI.detectIe6()) {
                CoreGUI.unforceIe6Hacks();
            }
        }

    }

    public static Subject getSessionSubject() {
        return sessionSubject;
    }

    public SearchBar getSearchBar() {
        return searchBar;
    }

    /**
     * Detects IE6.
     * <p/>
     * This is a nasty hack; but it's extremely reliable when running with other
     * js libraries on the same page at the same time as gwt.
     */
    public static native boolean detectIe6() /*-{
                                             if (typeof $doc.body.style.maxHeight != "undefined")
                                             return(false);
                                             else
                                             return(true);
                                             }-*/;

    public static native void forceIe6Hacks() /*-{
                                              $wnd.XMLHttpRequestBackup = $wnd.XMLHttpRequest;
                                              $wnd.XMLHttpRequest = null;
                                              }-*/;

    public static native void unforceIe6Hacks() /*-{
                                                $wnd.XMLHttpRequest = $wnd.XMLHttpRequestBackup;
                                                $wnd.XMLHttpRequestBackup = null;
                                                }-*/;

}
