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
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.search.SearchBar;

/**
 * @author Joseph Marques
 */
public class SearchGUI implements EntryPoint {

    public static SearchGUI singleton = new SearchGUI();
    private SearchBar searchBar;

    private SearchGUI() {
    }

    public static SearchGUI get() {
        return singleton;
    }

    public void onModuleLoad() {
        if (SearchBar.existsOnPage() == false) {
            com.allen_sauer.gwt.log.client.Log.info("Suppressing load of SearchGUI module");
            return;
        }

        UserSessionManager.checkLoginStatus(Cookies.getCookie("username"), null, new AsyncCallback<Subject>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say("Unable to determine login status, check server status");
            }

            @Override
            public void onSuccess(Subject result) {
                singleton.buildSearchGUI();
            }
        });
    }

    public void buildSearchGUI() {
        searchBar = new SearchBar();
    }

    public SearchBar getSearchBar() {
        return searchBar;
    }

}
