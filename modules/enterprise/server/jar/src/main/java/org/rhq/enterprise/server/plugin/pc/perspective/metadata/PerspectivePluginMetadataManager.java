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
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ActionType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ApplicationType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ExtensionPointType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.MenuItemType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PerspectivePluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PlacementType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PositionType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TabType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TaskType;

/**
 * This loads in all deployed perspective server plugins and maintains the complete set of
 * {@link #getMetadataManager() metadata} found in all plugin descriptors. Individual plugin
 * metadata is irrelevant and not retrievable.  The union of the perspective definitions is
 * maintained and made available.  Conflict resolution rules are applied as needed for
 * clashing perspective definitions.
 * 
 * Activators are not processed here, they use session-time information. This is performed at
 * plugin-load-time. 
 *
 * @author Jay Shaughnessy
 */
public class PerspectivePluginMetadataManager {
    private final String CORE_PERSPECTIVE_NAME = "CorePerspective";

    private Log log = LogFactory.getLog(PerspectivePluginMetadataManager.class);

    //private static PerspectivePluginMetadataManager manager = new PerspectivePluginMetadataManager(); 

    private Map<String, PerspectivePluginDescriptorType> loadedPlugins = new HashMap<String, PerspectivePluginDescriptorType>();

    private boolean isStarted = false;
    private List<TaskType> globalTasks = null;
    private List<TaskType> resourceTasks = null;
    private List<TabType> tabs;
    private List<MenuItem> coreMenu = null;

    public PerspectivePluginMetadataManager() {
    }

    /**
     * Transforms the pluginDescriptor into domain object form and stores those objects.
     *
     * @param  descriptor the descriptor to transform
     *
     * @return void
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

        this.globalTasks = cp.getGlobalTask();
        this.resourceTasks = cp.getResourceTask();
        this.tabs = cp.getTab(); // TODO

        // Setup core menus
        if (null == this.coreMenu) {
            this.coreMenu = new ArrayList<MenuItem>();
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

        for (MenuItemType rawMenuItem : perspective.getMenuItem()) {
            resolveUrls(applications, rawMenuItem);
            ActionType action = rawMenuItem.getAction();
            ExtensionPointType extensionPoint = rawMenuItem.getPosition().getExtensionPoint();
            if (ExtensionPointType.CORE_MENU == extensionPoint) {
                if (ActionType.ADD == action) {
                    menuAdd(this.coreMenu, rawMenuItem);
                } else if (ActionType.REMOVE == action) {
                    menuRemove(this.coreMenu, rawMenuItem);
                } else if (ActionType.REPLACE == action) {
                    menuReplace(this.coreMenu, rawMenuItem);
                }
            } else {
                throw new IllegalArgumentException("Unknown Extension Point defined in menuItem ["
                    + rawMenuItem.getName() + "] : " + extensionPoint);
            }
        }
    }

    /**
     * Update the url and iconUrl as needed by applying delclard application information.
     *  
     * @param applications
     * @param rawMenuItem
     */
    private void resolveUrls(List<ApplicationType> applications, MenuItemType rawMenuItem) {
        ApplicationType applicationType = getApplicationByName(applications, rawMenuItem.getApplication());

        rawMenuItem.setUrl(resolveUrl(applicationType, rawMenuItem.getUrl()));
        rawMenuItem.setIconUrl(resolveUrl(applicationType, rawMenuItem.getIconUrl()));
    }

    private String resolveUrl(ApplicationType applicationType, String url) {
        String result = url;

        if (null != url) {
            if (isRelativeUrl(url)) {
                if (null == applicationType) {
                    throw new IllegalArgumentException(
                        "Relative URL found without application. Add application attribute to fully resolve url '"
                            + url + "'");
                }

                String baseUrl = applicationType.getBaseUrl();
                // fix the baseUrl if it lacks the separator
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                    applicationType.setBaseUrl(baseUrl);
                }
                result = baseUrl + url;
            }
        }

        return result;
    }

    private boolean isRelativeUrl(String url) {
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

    /**
     * @return the coreMenu
     */
    public List<MenuItem> getCoreMenu() {
        return coreMenu;
    }

    public List<TaskType> getGlobalTasks()
    {
        return globalTasks;
    }

    public List<TaskType> getResourceTasks()
    {
        return resourceTasks;
    }

    public List<TabType> getTabs()
    {
        return tabs;
    }
}
