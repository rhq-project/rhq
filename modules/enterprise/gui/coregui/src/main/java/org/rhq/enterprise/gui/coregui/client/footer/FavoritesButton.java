/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.footer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class FavoritesButton extends LocatableIMenuButton {

    public FavoritesButton(String locatorId) {
        super(locatorId, MSG.favorites());

        // this is the main menu - the "favorites" button shown in the UI the user initially clicks 
        final Menu favoritesMenu = new Menu();
        setMenu(favoritesMenu);
        setShowMenuBelow(false);
        setAutoFit(true);

        // these are the child menus directly under the main favorites button 
        final Menu favoriteResourcesMenu = new Menu();
        final Menu favoriteGroupsMenu = new Menu();
        final Menu recentlyViewedMenu = new Menu();
        MenuItem favoriteResourcesMenuItem = new MenuItem(MSG.favorites_resources(), "Favorite_Resource_16.png");
        favoriteResourcesMenuItem.setSubmenu(favoriteResourcesMenu);
        favoriteResourcesMenu.setEmptyMessage(MSG.common_val_none());

        MenuItem favoriteGroupsMenuItem = new MenuItem(MSG.favorites_groups(), "Favorite_Group_16.png");
        favoriteGroupsMenuItem.setSubmenu(favoriteGroupsMenu);
        favoriteGroupsMenu.setEmptyMessage(MSG.common_val_none());

        MenuItem recentlyViewedMenuItem = new MenuItem(MSG.favorites_recentlyViewed(), "global/Recent_16.png");
        recentlyViewedMenuItem.setSubmenu(recentlyViewedMenu);
        recentlyViewedMenu.setEmptyMessage(MSG.common_val_none());

        favoritesMenu.setItems(favoriteResourcesMenuItem, favoriteGroupsMenuItem, recentlyViewedMenuItem);

        addClickHandler(new ClickHandler() {
            private boolean contextMenuIsShown = false;

            public void onClick(ClickEvent clickEvent) {
                // Cancel the click event. We'll call show() on the menu ourselves only if we're able to load the
                // favorite Resources successfully.
                clickEvent.cancel();
                contextMenuIsShown = false; // so at least one of our two callbacks will show it

                Set<Integer> favoriteResources;
                Set<Integer> favoriteGroups;
                Set<Integer> recentlyViewed;

                favoriteResources = UserSessionManager.getUserPreferences().getFavoriteResources();
                favoriteGroups = UserSessionManager.getUserPreferences().getFavoriteResourceGroups();
                recentlyViewed = new HashSet<Integer>(0); // TODO: get the IDs of the recently visited resources

                if (!favoriteResources.isEmpty()) {

                    Integer[] resourceIds = new Integer[favoriteResources.size()];
                    final MenuItem[] items = new MenuItem[favoriteResources.size()];
                    final Map<Integer, MenuItem> resIdToMenuItemMap = new HashMap<Integer, MenuItem>(favoriteResources
                        .size());
                    int i = 0;
                    for (final Integer resourceId : favoriteResources) {
                        resourceIds[i] = resourceId;
                        MenuItem item = new MenuItem(String.valueOf(resourceId));
                        item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                            public void onClick(MenuItemClickEvent event) {
                                CoreGUI.goToView(LinkManager.getResourceLink(resourceId));
                            }
                        });
                        items[i] = item;
                        resIdToMenuItemMap.put(resourceId, item);
                        i++;
                    }

                    ResourceCriteria criteria = new ResourceCriteria();
                    criteria.addFilterIds(resourceIds);
                    GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                        new AsyncCallback<PageList<Resource>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_dashboard_favorites_error1(), caught);
                            }

                            public void onSuccess(PageList<Resource> resources) {
                                for (Resource resource : resources) {
                                    MenuItem item = resIdToMenuItemMap.get(resource.getId());
                                    // TODO: Ideally, we should use ResourceManagerLocal.disambiguate() here to obtain
                                    //       disambiguated Resource names.
                                    item.setTitle(resource.getName());
                                    item.setIcon(ImageManager.getResourceIcon(resource));
                                }
                                favoriteResourcesMenu.setItems(items);
                                if (!contextMenuIsShown) {
                                    contextMenuIsShown = true;
                                    favoritesMenu.showContextMenu();
                                }
                            }
                        });
                }

                if (!favoriteGroups.isEmpty()) {

                    Integer[] groupIds = new Integer[favoriteGroups.size()];
                    final MenuItem[] items = new MenuItem[favoriteGroups.size()];
                    final Map<Integer, MenuItem> grpIdToMenuItemMap = new HashMap<Integer, MenuItem>(favoriteGroups
                        .size());
                    int i = 0;
                    for (final Integer groupId : favoriteGroups) {
                        groupIds[i] = groupId;
                        MenuItem item = new MenuItem(String.valueOf(groupId));
                        item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                            public void onClick(MenuItemClickEvent event) {
                                CoreGUI.goToView(LinkManager.getResourceGroupLink(groupId));
                            }
                        });
                        items[i] = item;
                        grpIdToMenuItemMap.put(groupId, item);
                        i++;
                    }

                    ResourceGroupCriteria criteria = new ResourceGroupCriteria();
                    criteria.addFilterIds(groupIds);
                    GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
                        new AsyncCallback<PageList<ResourceGroupComposite>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_dashboard_favorites_error1(), caught);
                            }

                            public void onSuccess(PageList<ResourceGroupComposite> groups) {
                                for (ResourceGroupComposite groupInfo : groups) {
                                    ResourceGroup theGroup = groupInfo.getResourceGroup();
                                    MenuItem item = grpIdToMenuItemMap.get(theGroup.getId());
                                    item.setTitle(theGroup.getName());
                                    item.setIcon(ImageManager.getGroupIcon(theGroup.getGroupCategory(), groupInfo
                                        .getImplicitAvail()));
                                }
                                favoriteGroupsMenu.setItems(items);
                                if (!contextMenuIsShown) {
                                    contextMenuIsShown = true;
                                    favoritesMenu.showContextMenu();
                                }

                            }
                        });
                }

                if (!recentlyViewed.isEmpty()) {
                    // TODO populate the menu. this is different than the other two because there could be a mix
                    //      of both resources and groups in here. presumably, the user prefs will be able to tell
                    //      us which is which so we can call the proper server API and use the proper icons
                }

                // if we have no menu items at all, then show the menu now
                if (favoriteGroups.isEmpty() && favoriteResources.isEmpty() && recentlyViewed.isEmpty()) {
                    favoritesMenu.showContextMenu();
                }
            }
        });
    }
}
