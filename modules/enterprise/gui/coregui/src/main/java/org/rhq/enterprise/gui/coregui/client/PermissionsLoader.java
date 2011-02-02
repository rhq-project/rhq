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

import java.util.EnumSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Option;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * Provides convenience methods that loads permissions and notifies you via a callback when done.
 * If an error occurs, the error will be logged in the message center.
 * This stores the last error received, in case a caller wants more information on failures.
 * 
 * Usage of this ensures consistency of error messages that occur when permission loading fails and
 * allows callers to implement a smaller callback object ({@link PermissionsLoadedListener}).
 *  
 * @author John Mazzitelli
 */
public class PermissionsLoader {

    private static Messages MSG = CoreGUI.getMessages();
    private Throwable lastError;

    /**
     * Returns the last error that occurred while trying to load permissions.
     * 
     * @return last error that occurred during permissions loading
     */
    public Throwable getLastError() {
        return lastError;
    }

    public void loadExplicitGlobalPermissions(final PermissionsLoadedListener callback) {
        GWTServiceLookup.getAuthorizationService().getExplicitGlobalPermissions(new AsyncCallback<Set<Permission>>() {

            @Override
            public void onSuccess(Set<Permission> result) {
                callback.onPermissionsLoaded(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                processFailure(MSG.util_userPerm_loadFailGlobal(), caught);
                callback.onPermissionsLoaded(null); // indicate an error by passing in null
            }
        });
    }

    public void loadExplicitGroupPermissions(final int groupId, final PermissionsLoadedListener callback) {
        GWTServiceLookup.getAuthorizationService().getExplicitGroupPermissions(groupId,
            new AsyncCallback<Set<Permission>>() {

                @Override
                public void onSuccess(Set<Permission> result) {
                    callback.onPermissionsLoaded(result);
                }

                @Override
                public void onFailure(Throwable caught) {
                    processFailure(MSG.util_userPerm_loadFailGroup(String.valueOf(groupId)), caught);
                    callback.onPermissionsLoaded(null); // indicate an error by passing in null
                }
            });
    }

    public void loadExplicitResourcePermissions(final int resourceId, final PermissionsLoadedListener callback) {
        GWTServiceLookup.getAuthorizationService().getExplicitResourcePermissions(resourceId,
            new AsyncCallback<Set<Permission>>() {

                @Override
                public void onSuccess(Set<Permission> result) {
                    callback.onPermissionsLoaded(result);
                }

                @Override
                public void onFailure(Throwable caught) {
                    processFailure(MSG.util_userPerm_loadFailResource(String.valueOf(resourceId)), caught);
                    callback.onPermissionsLoaded(null); // indicate an error by passing in null
                }
            });
    }

    protected void processFailure(String msg, Throwable caught) {
        this.lastError = caught;
        EnumSet<Option> options = EnumSet.of(Message.Option.BackgroundJobResult);
        Severity severity = Message.Severity.Error;
        CoreGUI.getMessageCenter().notify(new Message(msg, caught, severity, options));
    }
}
