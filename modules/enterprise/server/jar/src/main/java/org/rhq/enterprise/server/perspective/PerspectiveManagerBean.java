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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.perspective.activator.Activator;
import org.rhq.enterprise.server.perspective.activator.context.ActivationContext;
import org.rhq.enterprise.server.perspective.activator.context.ActivationContextScope;
import org.rhq.enterprise.server.perspective.activator.context.GlobalActivationContext;
import org.rhq.enterprise.server.perspective.activator.context.ResourceActivationContext;

@Stateless
// @WebService(endpointInterface = "org.rhq.enterprise.server.perspective.PerspectiveManagerRemote")
/**
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class PerspectiveManagerBean implements PerspectiveManagerLocal, PerspectiveManagerRemote {

    private final Log log = LogFactory.getLog(PerspectiveManagerBean.class);

    // Map of sessionId to cached menu entry.  The cached menu is re-used for the same sessionId.
    // This should more appropriately use Subject as the key, but since Subject equality is
    // based on username, it's not quite appropriate.
    // The cache is cleaned anytime there is a new entry.
    static final private Map<Integer, CacheEntry> CACHE = new HashMap<Integer, CacheEntry>();

    // @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    // private EntityManager entityManager;

    // @EJB
    // private AuthorizationManagerLocal authorizationManager;

    // @EJB
    // private ResourceTypeManagerLocal resourceTypeManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getCoreMenu(org.rhq.core.domain.auth.Subject)
     */
    public synchronized List<MenuItem> getMenu(Subject subject) throws PerspectiveException {
        CacheEntry cacheEntry = getCacheEntry(subject);
        List<MenuItem> menu = cacheEntry.getMenu();
        return menu;
    }

    @NotNull
    public List<Tab> getResourceTabs(Subject subject, Resource resource) {
        // First get a cached copy of the tabs that has the global-scoped activators already applied in the context of
        // the current Subject.
        CacheEntry cacheEntry = getCacheEntry(subject);
        List<Tab> tabs = cacheEntry.getTabs();

        // Now apply the Resource-scoped activators in the context of the current Resource.
        ResourceActivationContext context = new ResourceActivationContext(subject, resource);
        EnumSet<ActivationContextScope> scopes = EnumSet.of(ActivationContextScope.RESOURCE_OR_GROUP);
        List<Tab> filteredTabs = applyActivatorsToTabs(context, scopes, tabs);

        return filteredTabs;
    }

    /**
     * Recursively applies activators, based on the specified contexts, to a menu, and returns a
     * filtered, deep copy of the menu. The supplied <menu> is unmodified.
     */
    private List<MenuItem> applyActivatorsToMenu(ActivationContext context, EnumSet<ActivationContextScope> scopes,
        List<MenuItem> menu) {

        List<MenuItem> filteredMenu = new ArrayList<MenuItem>();
        for (MenuItem menuItem : menu) {
            if (isActive(context, scopes, menuItem)) {
                MenuItem clone = null;
                try {
                    clone = (MenuItem) menuItem.clone();
                } catch (CloneNotSupportedException e) {
                    log.error("Invalid Clone - This should not happen: " + e);
                }

                filteredMenu.add(clone);
                // Recurse...
                List<MenuItem> filteredChildren = applyActivatorsToMenu(context, scopes, clone.getChildren());
                clone.setChildren(filteredChildren);
            }
        }
        return filteredMenu;
    }

    /**
     * Recursively applies activators, based on the specified contexts, to a list of tabs, and returns a
     * filtered, deep copy of the list. The supplied <tabs> are unmodified.
     */
    private List<Tab> applyActivatorsToTabs(ActivationContext context, EnumSet<ActivationContextScope> scopes,
        List<Tab> tabs) {

        List<Tab> filteredTabs = new ArrayList<Tab>();
        for (Tab tab : tabs) {
            if (isActive(context, scopes, tab)) {
                Tab clone = null;
                try {
                    clone = (Tab) tab.clone();
                } catch (CloneNotSupportedException e) {
                    log.error("Invalid Clone - This should not happen: " + e);
                }
                filteredTabs.add(clone);
                // Recurse...
                List<Tab> filteredChildren = applyActivatorsToTabs(context, scopes, clone.getChildren());
                clone.setChildren(filteredChildren);
            }
        }
        return filteredTabs;
    }

    @SuppressWarnings("unchecked")
    private boolean isActive(ActivationContext context, EnumSet<ActivationContextScope> scopes, Extension extension) {
        List<Activator> activators = extension.getActivators();
        for (Activator activator : activators) {
            if (scopes.contains(activator.getScope()) && !activator.matches(context)) {
                return false;
            }
        }
        return true;
    }

    private CacheEntry getCacheEntry(Subject subject) {
        Integer sessionId = subject.getSessionId();
        CacheEntry cacheEntry = CACHE.get(sessionId);
        if (cacheEntry == null) {
            // Take this opportunity to clean the cache.
            cleanCache();

            GlobalActivationContext context = new GlobalActivationContext(subject);
            EnumSet<ActivationContextScope> scopes = EnumSet.of(ActivationContextScope.GLOBAL);

            List<MenuItem> baseMenu = PerspectiveManagerHelper.getPluginMetadataManager().getMenu();
            List<MenuItem> filteredMenu = applyActivatorsToMenu(context, scopes, baseMenu);

            List<Tab> baseTabs = PerspectiveManagerHelper.getPluginMetadataManager().getResourceTabs();
            List<Tab> filteredTabs = applyActivatorsToTabs(context, scopes, baseTabs);

            cacheEntry = new CacheEntry(filteredMenu, filteredTabs);
            CACHE.put(sessionId, cacheEntry);
        }
        return cacheEntry;
    }

    // TODO: Is there any sort of listener approach we could use to clear an individual cache entry
    // for various events like: change to role defs, change to inventory? Perhaps even a manual or
    // automated refresh for the session?
    private void cleanCache() {
        Subject subject;

        for (Integer sessionId : CACHE.keySet()) {
            try {
                subject = subjectManager.getSubjectBySessionId(sessionId);
                if (null == subject) {
                    log.debug("Removing perspective cache entry for session " + sessionId);
                    CACHE.remove(sessionId);
                }
            } catch (Exception e) {
                log.debug("Removing perspective cache entry for session " + sessionId);
                CACHE.remove(sessionId);
            }
        }
    }

    // TODO: remove this debug code
    @SuppressWarnings("unused")
    private void printMenu(List<MenuItem> menu, String indent) {
        if (null == menu)
            return;

        for (MenuItem menuItem : menu) {
            System.out.println(indent + menuItem.getName());
            printMenu(menuItem.getChildren(), indent + "..");
        }
    }

    private static class CacheEntry {
        // This is a copy of the base menu that has had all global-scoped activators already applied to it.
        // We cache it because the variables used by the global activators do not change very often.
        private List<MenuItem> menu;

        // This is a copy of the base tabs that has had all global-scoped activators already applied to it.
        // We cache it because the variables used by the global activators do not change very often.
        private List<Tab> tabs;

        public CacheEntry(List<MenuItem> menu, List<Tab> tabs) {
            this.menu = menu;
            this.tabs = tabs;
        }

        public List<MenuItem> getMenu() {
            return menu;
        }

        public List<Tab> getTabs() {
            return tabs;
        }
    }

    /**
     * Given a targetUrlKey parameter value, as set in the extension, resolve that key into the targetUrl
     * for the extension's content.
     * 
     * @param key, a valid key
     * @return the target url
     * 
     */
    public String getUrlViaKey(int key) throws PerspectiveException {
        try {
            return PerspectiveManagerHelper.getPluginMetadataManager().getUrlViaKey(key);
        } catch (Exception e) {
            throw new PerspectiveException("Failed to get URL for key: " + key, e);
        }
    }

}