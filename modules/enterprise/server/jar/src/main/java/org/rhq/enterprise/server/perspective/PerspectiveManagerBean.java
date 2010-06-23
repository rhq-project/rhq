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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.cloud.instance.ServerManagerLocal;
import org.rhq.enterprise.server.perspective.activator.Activator;
import org.rhq.enterprise.server.perspective.activator.context.ActivationContext;
import org.rhq.enterprise.server.perspective.activator.context.ActivationContextScope;
import org.rhq.enterprise.server.perspective.activator.context.GlobalActivationContext;
import org.rhq.enterprise.server.perspective.activator.context.ResourceActivationContext;
import org.rhq.enterprise.server.plugin.pc.perspective.metadata.PerspectivePluginMetadataManager;

@Stateless
// @WebService(endpointInterface = "org.rhq.enterprise.server.perspective.PerspectiveManagerRemote")
/**
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class PerspectiveManagerBean implements PerspectiveManagerLocal {

    // Map of sessionId to cached menu entry.  The cached menu is re-used for the same sessionId.
    // This should more appropriately use Subject as the key, but since Subject equality is
    // based on username, it's not quite appropriate.
    // The cache is cleaned anytime there is a new entry.
    static private final Map<Integer, CacheEntry> CACHE = new HashMap<Integer, CacheEntry>();

    static private Server server = null;

    private final Log log = LogFactory.getLog(PerspectiveManagerBean.class);

    @EJB
    private ServerManagerLocal serverManager;

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

    public String getPageLink(Subject subject, String pageName, String linkName, String defaultValue) {
        CacheEntry cacheEntry = getCacheEntry(subject);
        List<PageLink> pageLinks = cacheEntry.getPageLinks();

        String result = defaultValue;

        for (PageLink pageLink : pageLinks) {
            if (pageLink.getPageName().equals(pageName) && pageLink.getName().equals(linkName)) {
                result = pageLink.getUrl();
                break;
            }
        }

        return result;
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

    /**
     * Applies activators, based on the specified contexts, to a list of PageLinks, and returns a
     * filtered list. The supplied <List> is unmodified.
     */
    private List<PageLink> applyActivatorsToPageLinks(ActivationContext context,
        EnumSet<ActivationContextScope> scopes, List<PageLink> pageLinks) {

        List<PageLink> filteredPageLinks = new ArrayList<PageLink>();
        for (PageLink pageLink : pageLinks) {
            if (isActive(context, scopes, pageLink)) {
                filteredPageLinks.add(pageLink);
            }
        }

        return filteredPageLinks;
    }

    @SuppressWarnings("unchecked")
    private boolean isActive(ActivationContext context, EnumSet<ActivationContextScope> scopes, Extension extension) {
        List<Activator<?>> activators = extension.getActivators();
        for (Activator activator : activators) {
            if (scopes.contains(activator.getScope()) && !activator.isActive(context)) {
                return false;
            }
        }
        return true;
    }

    private CacheEntry getCacheEntry(Subject subject) {
        Integer sessionId = subject.getSessionId();
        CacheEntry cacheEntry;
        synchronized (CACHE) {
            cacheEntry = CACHE.get(sessionId);
        }
        long metadataLastModifiedTime = getPluginMetadataManager().getLastModifiedTime();
        if (cacheEntry == null || cacheEntry.getMetadataLastModifiedTime() < metadataLastModifiedTime) {
            // Take this opportunity to clean expired sessions from the cache.
            cleanCache();

            GlobalActivationContext context = new GlobalActivationContext(subject);
            EnumSet<ActivationContextScope> scopes = EnumSet.of(ActivationContextScope.GLOBAL);

            List<MenuItem> baseMenu = getPluginMetadataManager().getMenu();
            List<MenuItem> filteredMenu = applyActivatorsToMenu(context, scopes, baseMenu);

            List<Tab> baseTabs = getPluginMetadataManager().getResourceTabs();
            List<Tab> filteredTabs = applyActivatorsToTabs(context, scopes, baseTabs);

            List<PageLink> basePageLinks = getPluginMetadataManager().getPageLinks();
            List<PageLink> filteredPageLinks = applyActivatorsToPageLinks(context, scopes, basePageLinks);

            cacheEntry = new CacheEntry(metadataLastModifiedTime, filteredMenu, filteredTabs, filteredPageLinks);
            synchronized (CACHE) {
                CACHE.put(sessionId, cacheEntry);
            }
        }
        return cacheEntry;
    }

    private PerspectivePluginMetadataManager getPluginMetadataManager() {
        return PerspectiveManagerHelper.getPluginMetadataManager();
    }

    // TODO: Is there any sort of listener approach we could use to clear an individual cache entry
    // for various events like: change to role defs, change to inventory? Perhaps even a manual or
    // automated refresh for the session?
    private void cleanCache() {
        Subject subject;

        synchronized (CACHE) {
            Iterator<Integer> iterator = CACHE.keySet().iterator(); // so we can use iterator.remove and avoid concurrent-mod-exception
            while (iterator.hasNext()) {
                Integer sessionId = iterator.next();
                try {
                    subject = subjectManager.getSubjectBySessionId(sessionId);
                    if (null == subject) {
                        log.debug("Removing perspective cache entry for session. " + sessionId);
                        iterator.remove();
                    }
                } catch (Exception e) {
                    log.debug("Removing perspective cache entry for session: " + sessionId);
                    iterator.remove();
                }
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
        private long metadataLastModifiedTime;

        // This is a copy of the base menu that has had all global-scoped activators already applied to it.
        // We cache it because the variables used by the global activators do not change very often.
        private List<MenuItem> menu;

        // This is a copy of the base tabs that has had all global-scoped activators already applied to it.
        // We cache it because the variables used by the global activators do not change very often.
        private List<Tab> tabs;

        // This is a list of references into the base pageLinks that has had all global-scoped activators
        // already applied to it. We cache it because the variables used by the global activators do not
        // change very often.
        private List<PageLink> pageLinks;

        public CacheEntry(long metadataLastModifiedTime, List<MenuItem> menu, List<Tab> tabs, List<PageLink> pageLinks) {
            this.metadataLastModifiedTime = metadataLastModifiedTime;
            this.menu = menu;
            this.tabs = tabs;
            this.pageLinks = pageLinks;
        }

        public long getMetadataLastModifiedTime() {
            return metadataLastModifiedTime;
        }

        public List<MenuItem> getMenu() {
            return menu;
        }

        public List<Tab> getTabs() {
            return tabs;
        }

        public List<PageLink> getPageLinks() {
            return pageLinks;
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
            return getPluginMetadataManager().getUrlViaKey(key);
        } catch (Exception e) {
            throw new PerspectiveException("Failed to get URL for key: " + key, e);
        }
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getMenuUrl(org.rhq.core.domain.auth.Subject, java.lang.String)
     */
    public String getMenuItemUrl(Subject subject, String menuItemName, boolean makeExplicit, boolean makeSecure) {
        if (null == menuItemName) {
            throw new IllegalArgumentException("Invalid menuItemName: null ");
        }

        String result = null;

        try {
            result = getMenuItemUrlByName(menuItemName, getMenu(subject));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid menuItemName: " + menuItemName, e);
        }

        return (null == result) ? result : makeUrl(result, makeExplicit, makeSecure);
    }

    private String getMenuItemUrlByName(String menuItemName, List<MenuItem> menuItems) {
        if (null == menuItems) {
            return null;
        }

        String result = null;

        for (MenuItem menuItem : menuItems) {
            String url = menuItem.getUrl();
            if (null != url && menuItemName.equals(menuItem.getName())) {
                result = url;
                break;
            } else {
                result = getMenuItemUrlByName(menuItemName, menuItem.getChildren());
                if (null != result) {
                    break;
                }
            }
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getResourceTabUrl(org.rhq.core.domain.auth.Subject, java.lang.String)
     */
    public String getResourceTabUrl(Subject subject, String tabName, int resourceId, boolean makeExplicit,
        boolean makeSecure) {
        if (null == tabName) {
            throw new IllegalArgumentException("Invalid tabName: null ");
        }

        String result = null;

        try {
            result = getResourceTabUrlByName(tabName, this.getPluginMetadataManager().getResourceTabs());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid tabName: " + tabName, e);
        }

        return (null == result) ? result : makeUrl(result, makeExplicit, makeSecure);
    }

    private String getResourceTabUrlByName(String tabName, List<Tab> tabs) {
        if (null == tabs) {
            return null;
        }

        String result = null;

        for (Tab tab : tabs) {
            String url = tab.getUrl();
            if (null != url && tabName.equals(tab.getName())) {
                result = url;
                break;
            } else {
                result = getResourceTabUrlByName(tabName, tab.getChildren());
                if (null != result) {
                    break;
                }
            }
        }

        return result;

    }

    /*
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getRootUrl(org.rhq.core.domain.auth.Subject)
     */
    public String getRootUrl(Subject subject, boolean makeExplicit, boolean makeSecure) {
        return makeUrl("/", makeExplicit, makeSecure);
    }

    /*
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getTargetUrl(org.rhq.core.domain.auth.Subject, org.rhq.enterprise.server.perspective.PerspectiveTarget, int, boolean, boolean)
     */
    public String getTargetUrl(Subject subject, PerspectiveTarget target, int targetId, boolean makeExplicit,
        boolean makeSecure) {

        return makeUrl(target.getTargetUrl(targetId), makeExplicit, makeSecure);
    }

    /*
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getTargetUrls(org.rhq.core.domain.auth.Subject, org.rhq.enterprise.server.perspective.PerspectiveTarget, int[], boolean, boolean)
     */
    public Map<Integer, String> getTargetUrls(Subject subject, PerspectiveTarget target, int[] targetIds,
        boolean makeExplicit, boolean makeSecure) {

        Map<Integer, String> result = new HashMap<Integer, String>(targetIds.length);

        for (int targetId : targetIds) {
            result.put(targetId, makeUrl(target.getTargetUrl(targetId), makeExplicit, makeSecure));
        }

        return result;
    }

    /*
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getResourceTargetUrl(org.rhq.core.domain.auth.Subject, int, org.rhq.enterprise.server.perspective.PerspectiveTarget, int, boolean, boolean)
     */
    public String getResourceTargetUrl(Subject subject, int resourceId, PerspectiveTarget target, int targetId,
        boolean makeExplicit, boolean makeSecure) {

        return makeUrl(target.getResourceTargetUrl(resourceId, targetId), makeExplicit, makeSecure);
    }

    /*
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getResourceTargetUrls(org.rhq.core.domain.auth.Subject, int, org.rhq.enterprise.server.perspective.PerspectiveTarget, int[], boolean, boolean)
     */
    public Map<Integer, String> getResourceTargetUrls(Subject subject, int resourceId, PerspectiveTarget target,
        int[] targetIds, boolean makeExplicit, boolean makeSecure) {

        Map<Integer, String> result = new HashMap<Integer, String>(targetIds.length);

        for (int targetId : targetIds) {
            result.put(targetId, makeUrl(target.getResourceTargetUrl(resourceId, targetId), makeExplicit, makeSecure));
        }

        return result;
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.server.perspective.PerspectiveManagerLocal#getTemplateTargetUrl(org.rhq.core.domain.auth.Subject, int, org.rhq.enterprise.server.perspective.PerspectiveTarget, int, boolean, boolean)
     */
    public String getTemplateTargetUrl(Subject subject, int resourceId, PerspectiveTarget target, int targetId,
        boolean makeExplicit, boolean makeSecure) {

        return makeUrl(target.getTemplateTargetUrl(resourceId, targetId), makeExplicit, makeSecure);
    }

    private String makeUrl(String url, boolean makeExplicit, boolean makeSecure) {
        if (null == url || !makeExplicit || url.startsWith("http")) {
            return url;
        }

        if (null == server) {
            server = serverManager.getServer();
        }

        String protocol = (makeSecure) ? "https://" : "http://";
        int port = (makeSecure) ? server.getSecurePort() : server.getPort();
        String result = protocol + server.getAddress() + ":" + port + url;
        return result;
    }
}