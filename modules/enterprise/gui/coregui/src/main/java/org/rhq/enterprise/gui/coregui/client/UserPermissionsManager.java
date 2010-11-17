/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.gwt.AuthorizationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;

/**
 * A singleton that is in charge of fetching and caching the current user's global permissions and their permissions for
 * the currently selected Resource or group. The permissions are reloaded from the Server once per minute to ensure the
 * permissions used by the GUI for button enablement etc. do not get too stale.
 *
 * @author Ian Springer
 */
public class UserPermissionsManager {

    private static final UserPermissionsManager INSTANCE = new UserPermissionsManager();
    private static final AuthorizationGWTServiceAsync AUTHORIZATION_SERVICE = GWTServiceLookup
        .getAuthorizationService();
    private static final MessageCenter MESSAGE_CENTER = CoreGUI.getMessageCenter();
    private static final int REFRESH_INTERVAL = 60 * 1000; // 1 minute

    private Context currentContext;
    private int currentId;

    /** This is a reference to the last set of global perms returned to a caller. */
    private Set<Permission> globalPermissions = new HashSet<Permission>(Permission.GLOBAL_ALL.size());
    private boolean globalCacheDirty = true;
    private PermissionsLoadedListener globalPermissionsLoadedListener;

    /** This is a reference to the last set of Resource perms returned to a caller. */
    private Set<Permission> resourcePermissions;
    private boolean resourceCacheDirty = true;
    private PermissionsLoadedListener resourcePermissionsLoadedListener;

    private enum Context {
        RESOURCE, GROUP;
    };

    public static UserPermissionsManager getInstance() {
        return INSTANCE;
    }

    public void loadGlobalPermissions(PermissionsLoadedListener permissionsLoadedListener) {
        if (this.globalCacheDirty) {
            // Permissions are not cached. Kick off an async load and let it notify the caller when the load completes.
            this.globalPermissionsLoadedListener = permissionsLoadedListener;
            loadGlobalPermissions();
        } else {
            // Permissions are cached - shoot em back to the caller.
            permissionsLoadedListener.onPermissionsLoaded(this.globalPermissions);
        }
    }

    public void loadResourcePermissions(ResourceComposite currentResource,
        PermissionsLoadedListener permissionsLoadedListener) {
        if ((this.currentContext != Context.RESOURCE) || (this.currentId != currentResource.getResource().getId())) {
            this.resourceCacheDirty = true;
        }
        this.currentContext = Context.RESOURCE;
        this.currentId = currentResource.getResource().getId();
        if (currentResource.getResourcePermission() == null) {
            ResourcePermission resourcePermission = new ResourcePermission(new HashSet<Permission>(
                Permission.RESOURCE_ALL.size()));
            currentResource.setResourcePermission(resourcePermission);
        }
        this.resourcePermissions = currentResource.getResourcePermission().getPermissions();

        // TODO: This code is not working correctly, it's returning no perms.  Also, why are we going to the
        // server when we basically just fetched a resource composite. Its perms should be valid.
        // Anyway, for now, bypass this...
        //if (this.resourceCacheDirty) {
        // Permissions are not cached. Kick off an async load and let it notify the caller when the load completes.
        //    this.resourcePermissionsLoadedListener = permissionsLoadedListener;
        //    loadCurrentResourcePermissions();
        //} else {
        // Permissions are cached - shoot em back to the caller.
        permissionsLoadedListener.onPermissionsLoaded(this.resourcePermissions);
        // }
    }

    /**
     * TODO
     *
     * @param currentGroup
     * @param permissionsLoadedListener
     */
    public void loadGroupPermissions(ResourceGroupComposite currentGroup,
        PermissionsLoadedListener permissionsLoadedListener) {
        if ((this.currentContext != Context.GROUP) || (this.currentId != currentGroup.getResourceGroup().getId())) {
            this.resourceCacheDirty = true;
        }
        this.currentContext = Context.GROUP;
        this.currentId = currentGroup.getResourceGroup().getId();
        if (currentGroup.getResourcePermission() == null) {
            ResourcePermission resourcePermission = new ResourcePermission(new HashSet<Permission>(
                Permission.RESOURCE_ALL.size()));
            currentGroup.setResourcePermission(resourcePermission);
        }
        this.resourcePermissions = currentGroup.getResourcePermission().getPermissions();

        if (this.resourceCacheDirty) {
            // Permissions are not cached. Kick off an async load and let it notify the caller when the load completes.
            this.resourcePermissionsLoadedListener = permissionsLoadedListener;
            loadCurrentGroupPermissions();
        } else {
            // Permissions are cached - shoot em back to the caller.
            permissionsLoadedListener.onPermissionsLoaded(this.resourcePermissions);
        }
    }

    /**
     * Clear all cached permissions.
     */
    public void clearCache() {
        // Clear global cache.
        this.globalCacheDirty = true;
        this.globalPermissions.clear();

        // Clear Resource/group cache.
        this.resourceCacheDirty = true;
        this.currentContext = null;
        this.currentId = 0;
    }

    private UserPermissionsManager() {
        Timer timer = new Timer() {
            public void run() {
                Log.debug("Refreshing cached user permissions...");
                loadGlobalPermissions();
                if (UserPermissionsManager.this.currentContext != null) {
                    switch (UserPermissionsManager.this.currentContext) {
                    case RESOURCE:
                        loadCurrentResourcePermissions();
                        break;
                    case GROUP:
                        loadCurrentGroupPermissions();
                        break;
                    }
                }
            }
        };

        // Cache is automatically refreshed once per minute.
        timer.scheduleRepeating(REFRESH_INTERVAL);
    }

    private void loadGlobalPermissions() {
        AUTHORIZATION_SERVICE.getExplicitGlobalPermissions(new AsyncCallback<Set<Permission>>() {
            public void onFailure(Throwable throwable) {
                MESSAGE_CENTER.notify(new Message("Failed to load your global permissions - assuming none for now.",
                    throwable, Message.Severity.Error, EnumSet.of(Message.Option.BackgroundJobResult)));
                UserPermissionsManager.this.globalPermissions.clear();
                UserPermissionsManager.this.globalCacheDirty = true;
                notifyGlobalPermissionsLoadedListener();
            }

            public void onSuccess(Set<Permission> permissions) {
                // Always update the existing Set, so callers won't end up with stale references.
                setTo(UserPermissionsManager.this.globalPermissions, permissions);
                UserPermissionsManager.this.globalCacheDirty = false;
                notifyGlobalPermissionsLoadedListener();
            }
        });
    }

    private void notifyGlobalPermissionsLoadedListener() {
        if (this.globalPermissionsLoadedListener != null) {
            this.globalPermissionsLoadedListener.onPermissionsLoaded(this.globalPermissions);
        }
    }

    private void loadCurrentResourcePermissions() {
        AUTHORIZATION_SERVICE.getExplicitResourcePermissions(this.currentId, new AsyncCallback<Set<Permission>>() {
            public void onFailure(Throwable throwable) {
                MESSAGE_CENTER.notify(new Message("Failed to load your permissions for Resource with id ["
                    + UserPermissionsManager.this.currentId + "] - assuming none for now.", throwable,
                    Message.Severity.Error, EnumSet.of(Message.Option.BackgroundJobResult)));
                UserPermissionsManager.this.resourcePermissions.clear();
                UserPermissionsManager.this.resourceCacheDirty = true;
                notifyResourcePermissionsLoadedListener();
            }

            public void onSuccess(Set<Permission> permissions) {
                setTo(UserPermissionsManager.this.resourcePermissions, permissions);
                UserPermissionsManager.this.resourceCacheDirty = false;
                notifyResourcePermissionsLoadedListener();
            }
        });
    }

    private void loadCurrentGroupPermissions() {
        AUTHORIZATION_SERVICE.getExplicitGroupPermissions(this.currentId, new AsyncCallback<Set<Permission>>() {
            public void onFailure(Throwable throwable) {
                MESSAGE_CENTER.notify(new Message("Failed to load your permissions for group with id ["
                    + UserPermissionsManager.this.currentId + "] - assuming none for now.", throwable,
                    Message.Severity.Error, EnumSet.of(Message.Option.BackgroundJobResult)));
                UserPermissionsManager.this.resourcePermissions.clear();
                UserPermissionsManager.this.resourceCacheDirty = true;
                notifyResourcePermissionsLoadedListener();
            }

            public void onSuccess(Set<Permission> permissions) {
                setTo(UserPermissionsManager.this.resourcePermissions, permissions);
                UserPermissionsManager.this.resourceCacheDirty = false;
                notifyResourcePermissionsLoadedListener();
            }
        });
    }

    private void notifyResourcePermissionsLoadedListener() {
        if (this.resourcePermissionsLoadedListener != null) {
            this.resourcePermissionsLoadedListener.onPermissionsLoaded(this.resourcePermissions);
        }
    }

    private static void setTo(Set<Permission> permissions1, Set<Permission> permissions2) {
        permissions1.clear();
        permissions1.addAll(permissions2);
    }
}
