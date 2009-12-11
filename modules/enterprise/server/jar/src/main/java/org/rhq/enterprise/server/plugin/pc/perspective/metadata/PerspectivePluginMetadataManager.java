/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc.perspective.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.perspective.MenuItem;
import org.rhq.enterprise.server.perspective.Tab;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ActionType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ApplicationType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ExtensionPointType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ExtensionType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.MenuItemType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PerspectivePluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PlacementType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PositionType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TabType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TaskType;

/**
 * This loads in all deployed perspective server plugins and maintains the complete set of
 * metadata found in all plugin descriptors. Individual plugin
 * metadata is irrelevant and not retrievable.  The union of the perspective definitions is
 * maintained and made available.  Conflict resolution rules are applied as needed for
 * clashing perspective definitions.
 * 
 * Activators are not processed here, they use session-time information. This is performed at
 * plugin-load-time. 
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class PerspectivePluginMetadataManager {
    private final String CORE_PERSPECTIVE_NAME = "CorePerspective";

    private static final String MENU_ITEM_TARGET_URL = "/rhq/perspective/main.xhtml?targetUrlKey=";
    private static final String TAB_TARGET_URL = "/rhq/perspective/resource.xhtml?targetUrlKey=";

    private static Map<Integer, String> keyUrlMap = new HashMap<Integer, String>();

    private Log log = LogFactory.getLog(PerspectivePluginMetadataManager.class);

    private Map<String, PerspectivePluginDescriptorType> loadedPlugins = new HashMap<String, PerspectivePluginDescriptorType>();

    private boolean isStarted = false;
    private List<Tab> resourceTabs;
    private List<MenuItem> coreMenu;

    public PerspectivePluginMetadataManager() {
    }

    /**
     * Transforms the pluginDescriptor into domain object form and stores those objects.
     *
     * @param  descriptor the descriptor to transform
     */
    public synchronized void loadPlugin(PerspectivePluginDescriptorType descriptor) {
        loadedPlugins.put(descriptor.getName(), descriptor);
    }

    public synchronized void unloadPlugin(PerspectivePluginDescriptorType descriptor) {
        loadedPlugins.remove(descriptor.getName());
    }

    public synchronized void start() {
        if (isStarted)
            return;

        // always process the core perspective first as other perspectives may reference its definitions
        processCorePerspective();
        processNonCorePerspectives();

        isStarted = true;
    }

    private void processCorePerspective() {
        PerspectivePluginDescriptorType cp = loadedPlugins.get(CORE_PERSPECTIVE_NAME);

        if (null == cp) {
            throw new IllegalStateException("Core perspective must be loaded. Invalid startup. Load: "
                + CORE_PERSPECTIVE_NAME);
        }

        // TODO: Init lists of global tasks and resource tasks.

        // Setup core menus
        if (null == this.coreMenu) {
            this.coreMenu = new ArrayList<MenuItem>();
        }

        if (null == this.resourceTabs) {
            this.resourceTabs = new ArrayList<Tab>();
        }

        processPerspective(cp);

        // TODO: remove this debug code
        //printMenu(this.coreMenu, "");
    }

    private void processNonCorePerspectives() {
        for (String key : loadedPlugins.keySet()) {
            if (!CORE_PERSPECTIVE_NAME.equals(key)) {
                processPerspective(loadedPlugins.get(key));
            }
        }

        // TODO: remove this debug code
        //printMenu(this.coreMenu, "");
    }

    private void processPerspective(PerspectivePluginDescriptorType perspective) {
        List<ApplicationType> applications = perspective.getApplication();

        // TODO: Process global tasks and resource tasks.

        // Process MenuItem Extensions 
        for (MenuItemType rawMenuItem : perspective.getMenuItem()) {
            resolveUrls(applications, rawMenuItem, MENU_ITEM_TARGET_URL);
            ActionType action = rawMenuItem.getAction();
            ExtensionPointType extensionPoint = rawMenuItem.getPosition().getExtensionPoint();
            if (ExtensionPointType.CORE_MENU == extensionPoint) {
                switch (action) {
                case ADD:
                    menuAdd(this.coreMenu, rawMenuItem);
                    break;
                case REMOVE:
                    menuRemove(this.coreMenu, rawMenuItem);
                    break;
                case REPLACE:
                    menuReplace(this.coreMenu, rawMenuItem);
                    break;
                }
            }
        }

        // Process Tab Extensions
        for (TabType rawTab : perspective.getTab()) {
            resolveUrls(applications, rawTab, TAB_TARGET_URL);
            ActionType action = rawTab.getAction();
            switch (action) {
            case ADD:
                addTab(rawTab);
                break;
            case REMOVE:
                removeTab(rawTab);
                break;
            case REPLACE:
                replaceTab(rawTab);
                break;
            }
        }
    }

    /**
     * Update the url and iconUrl as needed by applying declared application information.
     *  
     * @param applications
     * @param extension
     */
    private void resolveUrls(List<ApplicationType> applications, ExtensionType extension, String targetUrl) {
        ApplicationType applicationType = getApplicationByName(applications, extension.getApplication());

        if (extension instanceof TaskType) {
            TaskType task = (TaskType) extension;
            String resolvedUrl = resolveUrl(applicationType, task.getUrl(), targetUrl);
            task.setUrl(resolvedUrl);
        } else if (extension instanceof MenuItemType) {
            MenuItemType menuItem = (MenuItemType) extension;
            String resolvedUrl = resolveUrl(applicationType, menuItem.getUrl(), targetUrl);
            menuItem.setUrl(resolvedUrl);
        } else if (extension instanceof TabType) {
            TabType tab = (TabType) extension;
            String resolvedUrl = resolveUrl(applicationType, tab.getUrl(), targetUrl);
            tab.setUrl(resolvedUrl);
        }

        String resolvedIconUrl = resolveUrl(applicationType, extension.getIconUrl(), null);
        extension.setIconUrl(resolvedIconUrl);
    }

    private String resolveUrl(ApplicationType applicationType, String url, String targetUrl) {
        String result = url;

        if (null != url) {
            if (isPerspectiveUrl(url)) {
                if (null == applicationType) {
                    throw new IllegalArgumentException(
                        "Relative URL found without application. Add application attribute to fully resolve url '"
                            + url + "'");
                }

                String baseUrl = applicationType.getBaseUrl();
                // fix the baseUrl if it lacks the separator
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                    applicationType.setBaseUrl(baseUrl);
                }

                result = baseUrl + url;

                // if a targetUrl is supplied then it is assumed that the url will actually
                // be passed as a parameter to a predefined perspective (xhtml) page. And then
                // content will be rendered within that page.  We don't want to pass urls as request
                // parameters (long, could contain characters that would need to be escaped, etc)
                // so instead we'll now replace the actual url with a numeric key for short and
                // safe param passing.  They key can then be used to get back the real url at runtime.
                if (null != targetUrl) {
                    String keyedUrl = baseUrl + url;
                    int key = getUrlKey(keyedUrl);

                    // return the targetUrl with the proper targetUrlKey param 
                    result = targetUrl + key;
                }
            }
        }

        return result;
    }

    /**
     * Determine whether the url is relative to a perspective application or whether the url is
     * for the RHQ Portal War.
     * @param url
     * @return true for a relative url in a declared perspective application
     */
    private boolean isPerspectiveUrl(String url) {
        return (!(url.startsWith("/") || url.startsWith("http")));
    }

    private ApplicationType getApplicationByName(List<ApplicationType> applications, String name) {
        ApplicationType result = null;

        if (null != applications && null != name) {
            for (ApplicationType application : applications) {
                if (application.getName().equals(name)) {
                    result = application;
                    break;
                }
            }
        }

        return result;
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
        String mapEntry = keyUrlMap.get(key);
        while (!((null == mapEntry) || mapEntry.equals(url))) {
            key *= 13;
            mapEntry = keyUrlMap.get(key);
        }

        if (null == mapEntry) {
            keyUrlMap.put(key, url);
        }

        return key;
    }

    /**
     * Return the url for the given key.
     */
    public String getUrlViaKey(int key) {
        return keyUrlMap.get(key);
    }

    // TODO: remove this debug code
    @SuppressWarnings("unused")
    private void printMenu(List<MenuItem> menu, String indent) {
        if (null == menu)
            return;

        for (MenuItem menuItem : menu) {
            System.out.println(indent + menuItem.getItem().getName());
            printMenu(menuItem.getChildren(), indent + "   ");
        }
    }

    private void menuAdd(List<MenuItem> menu, MenuItemType rawMenuItem) {
        PositionType position = rawMenuItem.getPosition();
        List<MenuItem> containingMenu = getContainingMenu(menu, rawMenuItem);
        MenuItem menuItem = new MenuItem(rawMenuItem);

        if (PlacementType.START == position.getPlacement()) {
            containingMenu.add(0, menuItem);
        } else if (PlacementType.END == position.getPlacement()) {
            containingMenu.add(menuItem);
        } else {
            int index = getMenuItemIndex(containingMenu, position.getName());
            if (-1 == index) {
                throw new IllegalArgumentException("Invalid position defined for menuItem [" + rawMenuItem.getName()
                    + "]. Referenced menuItem not found : " + position.getName()
                    + ". Make sure supporting menus are defined for the menuItem.");
            }

            containingMenu.add((PlacementType.AFTER == position.getPlacement() ? ++index : --index), menuItem);
        }
    }

    private void menuRemove(List<MenuItem> menu, MenuItemType rawMenuItem) {
        PositionType position = rawMenuItem.getPosition();
        List<MenuItem> containingMenu;
        try {
            containingMenu = getContainingMenu(menu, rawMenuItem);
            int index = getMenuItemIndex(containingMenu, rawMenuItem.getName());
            if (-1 == index) {
                throw new IllegalArgumentException("Invalid position defined for menuItem [" + rawMenuItem.getName()
                    + "]. Referenced menuItem not found : " + position.getName()
                    + ". Make sure supporting menus are defined for the menuItem.");
            }

            containingMenu.remove(index);

        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.debug("Menu item not found. Ignoring removal of " + rawMenuItem.getName(), e);
            }
        }
    }

    private void menuReplace(List<MenuItem> menu, MenuItemType rawMenuItem) {
        PositionType position = rawMenuItem.getPosition();
        List<MenuItem> containingMenu;
        try {
            containingMenu = getContainingMenu(menu, rawMenuItem);
            int index = getMenuItemIndex(containingMenu, rawMenuItem.getName());
            if (-1 == index) {
                throw new IllegalArgumentException("Invalid position defined for menuItem [" + rawMenuItem.getName()
                    + "]. Referenced menuItem not found : " + position.getName()
                    + ". Make sure supporting menus are defined for the menuItem.");
            }

            containingMenu.set(index, new MenuItem(rawMenuItem));

        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.debug("Menu item not found. Ignoring replacement and attempting add for " + rawMenuItem.getName(),
                    e);
            }
            menuAdd(menu, rawMenuItem);
        }
    }

    private List<MenuItem> getContainingMenu(List<MenuItem> menu, MenuItemType rawMenuItem) {
        String name = rawMenuItem.getName();
        String[] nameTokens = (null != name) ? name.split("\\.") : new String[0];
        List<MenuItem> result = menu;

        for (int i = 0; (i + 1 < nameTokens.length); ++i) {
            int index = this.getMenuItemIndex(result, nameTokens[i]);
            if (-1 == index) {
                throw new IllegalArgumentException("Invalid position defined for menuItem [" + rawMenuItem.getName()
                    + "]. No containing menu found for : " + name
                    + ". Make sure supporting menus are defined for the menuItem.");
            }
            MenuItem menuItem = result.get(index);
            result = menuItem.getChildren();
            if (null == result) {
                result = new ArrayList<MenuItem>();
                menuItem.setChildren(result);
            }
        }

        return result;
    }

    private int getMenuItemIndex(List<MenuItem> menu, String name) {
        int result = -1;

        for (int i = 0, size = menu.size(); (i < size); ++i) {
            if (menu.get(i).getItem().getName().endsWith(name)) {
                result = i;
                break;
            }
        }
        return result;
    }

    private void addTab(TabType rawTab) {
        PositionType position = rawTab.getPosition();
        List<Tab> siblingTabs = getSiblingTabs(rawTab);
        Tab tab = new Tab(rawTab);

        if (PlacementType.START == position.getPlacement()) {
            siblingTabs.add(0, tab);
        } else if (PlacementType.END == position.getPlacement()) {
            siblingTabs.add(tab);
        } else {
            int index = getTabIndex(siblingTabs, position.getName());
            if (index == -1) {
                throw new IllegalArgumentException("Invalid position defined for tab [" + rawTab.getName()
                    + "]. Referenced tab not found : " + position.getName()
                    + ". Make sure supporting tabs are defined for the tab.");
            }
            siblingTabs.add((PlacementType.AFTER == position.getPlacement() ? ++index : --index), tab);
        }
    }

    private void removeTab(TabType rawTab) {
        PositionType position = rawTab.getPosition();
        List<Tab> siblingTabs;
        try {
            siblingTabs = getSiblingTabs(rawTab);
            int index = getTabIndex(siblingTabs, rawTab.getName());
            if (index == -1) {
                throw new IllegalArgumentException("Invalid position defined for menuItem [" + rawTab.getName()
                    + "]. Referenced menuItem not found : " + position.getName()
                    + ". Make sure supporting menus are defined for the menuItem.");
            }
            siblingTabs.remove(index);
        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.debug("Tab not found. Ignoring removal of tab [" + rawTab.getName() + "].", e);
            }
        }
    }

    private void replaceTab(TabType rawTab) {
        PositionType position = rawTab.getPosition();
        List<Tab> siblingTabs;
        try {
            siblingTabs = getSiblingTabs(rawTab);
            int index = getTabIndex(siblingTabs, rawTab.getName());
            if (-1 == index) {
                throw new IllegalArgumentException("Invalid position defined for tab [" + rawTab.getName()
                    + "]. Referenced tab not found : " + position.getName()
                    + ". Make sure supporting tabs are defined for the tab.");
            }
            siblingTabs.set(index, new Tab(rawTab));
        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.debug("Tab not found. Ignoring replacement and attempting add for " + rawTab.getName(), e);
            }
            addTab(rawTab);
        }
    }

    private List<Tab> getSiblingTabs(TabType rawTab) {
        String name = rawTab.getName();
        String[] nameTokens = (name != null) ? name.split("\\.") : new String[0];
        List<Tab> result = this.resourceTabs;

        for (int i = 0; (i + 1) < nameTokens.length; i++) {
            int index = getTabIndex(result, nameTokens[i]);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid position defined for tab [" + rawTab.getName()
                    + "]. No parent tab found for : " + name + ". Make sure supporting tabs are defined for the tab.");
            }
            Tab parentTab = result.get(index);
            result = parentTab.getChildren();
        }

        return result;
    }

    private int getTabIndex(List<Tab> menu, String name) {
        int result = -1;

        for (int i = 0, size = menu.size(); (i < size); ++i) {
            if (menu.get(i).getQualifiedName().endsWith(name)) {
                result = i;
                break;
            }
        }
        return result;
    }

    /**
     * @return the coreMenu
     */
    public List<MenuItem> getCoreMenu() {
        return coreMenu;
    }

    public List<Tab> getResourceTabs() {
        List<Tab> clone = new ArrayList<Tab>(this.resourceTabs.size());
        for (Tab resourceTab : this.resourceTabs) {
            try {
                clone.add(resourceTab.clone());
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException(e);
            }
        }
        return clone;
    }
}
