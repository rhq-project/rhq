/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.perspective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.GlobalPermissionActivatorSetType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.GlobalPermissionActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.GlobalPermissionType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.MenuItemType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourceActivatorSetType;

@Stateless
// @WebService(endpointInterface = "org.rhq.enterprise.server.perspective.PerspectiveManagerRemote")
public class PerspectiveManagerBean implements PerspectiveManagerLocal, PerspectiveManagerRemote {

    private final Log log = LogFactory.getLog(PerspectiveManagerBean.class);

    // Map of sessionId to cached menu entry.  The cached menu is re-used for the same sessionId.
    // This should more appropriately use Subject as the key but since Subject equality is
    // based on username it's not quite appropriate.
    // The cache is cleaned anytime there is a new entry.
    static private Map<Integer, CacheEntry> cache = new HashMap<Integer, CacheEntry>();

    static private Map<Integer, String> urlMap = new HashMap<Integer, String>();

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getCoreMenu(org.rhq.core.domain.auth.Subject)
     */
    public synchronized List<MenuItem> getCoreMenu(Subject subject) throws PerspectiveException {
        Integer sessionId = subject.getSessionId();
        CacheEntry cacheEntry = cache.get(sessionId);

        if (null == cacheEntry) {
            // take this opportunity to clean the cache
            cleanCache();

            cacheEntry = new CacheEntry();
            cache.put(sessionId, cacheEntry);
        }

        List<MenuItem> coreMenu = cacheEntry.getCoreMenu();

        if (null == coreMenu) {
            try {
                coreMenu = PerspectiveManagerHelper.getPluginMetadataManager().getCoreMenu();

                // We have to be careful not to mess with the core menu as it is returned from
                // the perspective manager. The core menu for each subject/sessionid could
                // differ based on activation checks. So, get a new menu structure when applying
                // activation filters.
                coreMenu = getActivatedMenu(subject, coreMenu);

                cacheEntry.setCoreMenu(coreMenu);
            } catch (Exception e) {
                throw new PerspectiveException("Failed to get Core Menu.", e);
            }
        }

        return coreMenu;
    }

    @NotNull
    public List<Tab> getResourceTabs(Subject subject, Resource resource) {
        // TODO: implement
        return Collections.emptyList();
    }

    /**
     * Return a unique key for the url.
     * @param url
     * @return
     */
    public synchronized int getUrlKey(String url) {
        int key = url.hashCode();

        // in the very unlikely case that we have multiple urls with the same hashcode, protect
        // ourselves. This will mess up user bookmarking of the url but saves us from internal confusion.
        String mapEntry = urlMap.get(key);
        while (!((null == mapEntry) || mapEntry.equals(url))) {
            key *= 13;
            mapEntry = urlMap.get(key);
        }

        if (null == mapEntry) {
            urlMap.put(key, url);
        }

        return key;
    }

    /**
     * Return the url for the given key.
     */
    public String getUrlViaKey(int key) {
        return urlMap.get(key);
    }

    // TODO: Is there any sort of listener approach we could use to clear an individual cache entry
    // for various events like: change to role defs, change to inventory? Perhaps even a manual or
    // automated refresh for the session?
    private void cleanCache() {
        Subject subject;

        for (Integer sessionId : cache.keySet()) {
            try {
                subject = subjectManager.getSessionSubject(sessionId);
                if (null == subject) {
                    log.debug("Removing perspective cache entry for session " + sessionId);
                    cache.remove(sessionId);
                }
            } catch (Exception e) {
                log.debug("Removing perspective cache entry for session " + sessionId);
                cache.remove(sessionId);
            }
        }
    }

    /**
     * Given a menu return a filtered copy such that inactive menu items are not present. Recursively
     * handles children menus.
     * 
     * @param subject
     * @param coreMenu
     * @return A filtered copy of the menu structure. This results in new Lists and new MenuItems
     * that refer to the original MenuItemType objects.
     */
    private List<MenuItem> getActivatedMenu(Subject subject, List<MenuItem> menu) {
        List<MenuItem> result = new ArrayList<MenuItem>(menu.size());

        for (MenuItem menuItem : menu) {
            MenuItemType item = menuItem.getItem();
            List<ResourceActivatorSetType> inventoriedResourceActivatorSet = item.getInventoriedResourceActivatorSet();
            List<GlobalPermissionActivatorSetType> globalPermissionActivatorSet = item
                .getGlobalPermissionActivatorSet();

            // Make sure activators are satisfied before copying
            if (checkActivators(subject, inventoriedResourceActivatorSet, globalPermissionActivatorSet)) {
                MenuItem copy = new MenuItem(menuItem.getItem());

                result.add(copy);
                if (menuItem.isMenuGroup()) {
                    copy.setChildren(getActivatedMenu(subject, menuItem.getChildren()));
                }
            }
        }

        return result;
    }

    private boolean checkActivators(Subject subject, List<ResourceActivatorSetType> resourceActivatorSets,
        List<GlobalPermissionActivatorSetType> permissionActivatorSets) {

        // global perm checking is relatively fast, make sure these pass before checking inventory activators
        for (GlobalPermissionActivatorSetType permissionActivatorSet : permissionActivatorSets) {
            boolean any = permissionActivatorSet.isAny();
            boolean anyPassed = false;
            boolean anyFailed = false;

            for (GlobalPermissionActivatorType permissionActivator : permissionActivatorSet
                .getGlobalPermissionActivator()) {
                boolean hasPermission = hasGlobalPermission(subject, permissionActivator.getPermission());
                anyPassed = hasPermission;
                anyFailed = !hasPermission;

                if (any && anyPassed) {
                    break;
                }
                if (!any && anyFailed) {
                    break;
                }
            }

            if ((any && !anyPassed) || (!any && anyFailed)) {
                return false;
            }
        }

        for (ResourceActivatorSetType resourceActivatorSet : resourceActivatorSets) {
            //TODO: impl
        }

        return true;
    }

    private boolean hasGlobalPermission(Subject subject, GlobalPermissionType permissionType) {
        if (permissionType == GlobalPermissionType.SUPERUSER) {
            return authorizationManager.isSystemSuperuser(subject);
        }

        return authorizationManager.hasGlobalPermission(subject, Permission.valueOf(permissionType.name()));
    }

    // TODO: remove this debug code
    @SuppressWarnings("unused")
    private void printMenu(List<MenuItem> menu, String indent) {
        if (null == menu)
            return;

        for (MenuItem menuItem : menu) {
            System.out.println(indent + menuItem.getItem().getName());
            printMenu(menuItem.getChildren(), indent + "..");
        }
    }

    private static class CacheEntry {
        private List<MenuItem> coreMenu = null;

        public CacheEntry() {
        }

        /**
         * @return the coreMenu
         */
        public List<MenuItem> getCoreMenu() {
            return coreMenu;
        }

        /**
         * @param coreMenu the coreMenu to set
         */
        public void setCoreMenu(List<MenuItem> coreMenu) {
            this.coreMenu = coreMenu;
        }
    }

}