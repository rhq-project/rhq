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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
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

            public void onClick(ClickEvent clickEvent) {
                // Cancel the click event. We'll call show() on the menu ourselves only if we're able to load the
                // favorite Resources successfully.
                clickEvent.cancel();

                final Set<Integer> favoriteResourceIds = UserSessionManager.getUserPreferences().getFavoriteResources();
                final Set<Integer> favoriteGroupIds = UserSessionManager.getUserPreferences()
                    .getFavoriteResourceGroups();
                final List<Integer> recentResourceIds = UserSessionManager.getUserPreferences().getRecentResources();
                final List<Integer> recentGroupIds = UserSessionManager.getUserPreferences().getRecentResourceGroups();

                // if we have no menu items at all, then show the empty menu now
                if (favoriteGroupIds.isEmpty() && favoriteResourceIds.isEmpty() && recentResourceIds.isEmpty()
                    && recentGroupIds.isEmpty()) {
                    favoritesMenu.showContextMenu();
                    return;
                }

                // keep a list of all the ids we need to pull from the db. combine favs and recents to minimize
                // db round trips.
                Set<Integer> resourceIds = new HashSet<Integer>();
                Set<Integer> groupIds = new HashSet<Integer>();

                resourceIds.addAll(favoriteResourceIds);
                resourceIds.addAll(recentResourceIds);
                groupIds.addAll(favoriteGroupIds);
                groupIds.addAll(recentGroupIds);

                fetchFavorites(resourceIds, groupIds, new AsyncCallback<Favorites>() {

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_dashboard_favorites_error1(), caught);
                    }

                    public void onSuccess(Favorites favorites) {
                        // generate the menus
                        if (!favoriteResourceIds.isEmpty()) {
                            List<MenuItem> items = new ArrayList<MenuItem>(favoriteResourceIds.size());

                            for (final Integer resourceId : favoriteResourceIds) {
                                Resource resource = favorites.getResource(resourceId);
                                if (null == resource) {
                                    // if the resource is gone just skip it
                                    continue;
                                }

                                MenuItem item = new MenuItem(String.valueOf(resourceId));
                                // TODO: Ideally, we should use ResourceManagerLocal.disambiguate() here to obtain
                                //       disambiguated Resource names.
                                item.setTitle(resource.getName());
                                item.setIcon(ImageManager.getResourceIcon(resource));
                                item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                                    public void onClick(MenuItemClickEvent event) {
                                        CoreGUI.goToView(LinkManager.getResourceLink(resourceId));
                                    }
                                });
                                items.add(item);
                            }
                            favoriteResourcesMenu.setItems(items.toArray(new MenuItem[items.size()]));
                        }

                        if (!favoriteGroupIds.isEmpty()) {
                            List<MenuItem> items = new ArrayList<MenuItem>(favoriteGroupIds.size());

                            for (final Integer groupId : favoriteGroupIds) {
                                ResourceGroupComposite groupComposite = favorites.getGroupComposite(groupId);
                                if (null == groupComposite) {
                                    // if the resource group is gone just skip it
                                    continue;
                                }
                                ResourceGroup group = groupComposite.getResourceGroup();

                                MenuItem item = new MenuItem(String.valueOf(groupId));
                                item.setTitle(group.getName());
                                item.setIcon(ImageManager.getGroupIcon(group.getGroupCategory(), groupComposite
                                    .getImplicitAvail()));

                                item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                                    public void onClick(MenuItemClickEvent event) {
                                        CoreGUI.goToView(LinkManager.getResourceGroupLink(groupId));
                                    }
                                });
                                items.add(item);
                            }
                            favoriteGroupsMenu.setItems(items.toArray(new MenuItem[items.size()]));
                        }

                        if (!(recentResourceIds.isEmpty() && recentGroupIds.isEmpty())) {
                            List<MenuItem> items = new ArrayList<MenuItem>(recentResourceIds.size()
                                + recentGroupIds.size());

                            for (final Integer resourceId : recentResourceIds) {
                                Resource resource = favorites.getResource(resourceId);
                                if (null == resource) {
                                    // if the resource is gone just skip it
                                    continue;
                                }

                                MenuItem item = new MenuItem(String.valueOf(resourceId));
                                // TODO: Ideally, we should use ResourceManagerLocal.disambiguate() here to obtain
                                //       disambiguated Resource names.
                                item.setTitle(resource.getName());
                                item.setIcon(ImageManager.getResourceIcon(resource));
                                item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                                    public void onClick(MenuItemClickEvent event) {
                                        CoreGUI.goToView(LinkManager.getResourceLink(resourceId));
                                    }
                                });
                                items.add(item);
                            }
                            if (!recentResourceIds.isEmpty() && !recentGroupIds.isEmpty()) {
                                items.add(new MenuItemSeparator());
                            }
                            for (final Integer groupId : recentGroupIds) {
                                ResourceGroupComposite groupComposite = favorites.getGroupComposite(groupId);
                                if (null == groupComposite) {
                                    // if the resource group is gone just skip it
                                    continue;
                                }
                                ResourceGroup group = groupComposite.getResourceGroup();

                                MenuItem item = new MenuItem(String.valueOf(groupId));
                                item.setTitle(group.getName());
                                item.setIcon(ImageManager.getGroupIcon(group.getGroupCategory(), groupComposite
                                    .getImplicitAvail()));

                                item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                                    public void onClick(MenuItemClickEvent event) {
                                        CoreGUI.goToView(LinkManager.getResourceGroupLink(groupId));
                                    }
                                });
                                items.add(item);
                            }

                            recentlyViewedMenu.setItems(items.toArray(new MenuItem[items.size()]));
                        }

                        favoritesMenu.showContextMenu();
                    }
                });
            }
        });
    }

    private void fetchFavorites(Set<Integer> resourceIds, final Set<Integer> groupIds,
        final AsyncCallback<Favorites> callback) {

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterIds(resourceIds.toArray(new Integer[resourceIds.size()]));
        GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
            new AsyncCallback<PageList<Resource>>() {
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                }

                public void onSuccess(final PageList<Resource> resources) {

                    if (groupIds.isEmpty()) {
                        callback.onSuccess(new Favorites(resources, new PageList<ResourceGroupComposite>()));
                        return;
                    }

                    ResourceGroupCriteria criteria = new ResourceGroupCriteria();
                    criteria.addFilterIds(groupIds.toArray(new Integer[groupIds.size()]));
                    GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
                        new AsyncCallback<PageList<ResourceGroupComposite>>() {
                            public void onFailure(Throwable caught) {
                                callback.onFailure(caught);
                            }

                            public void onSuccess(PageList<ResourceGroupComposite> groups) {
                                callback.onSuccess(new Favorites(resources, groups));
                            }
                        });

                }
            });
    }

    private static class Favorites {
        private PageList<Resource> resources;
        private PageList<ResourceGroupComposite> groupComposites;

        public Favorites(PageList<Resource> resources, PageList<ResourceGroupComposite> groupComposites) {
            this.resources = resources;
            this.groupComposites = groupComposites;
        }

        public Resource getResource(int resourceId) {
            for (Resource resource : resources) {
                if (resourceId == resource.getId()) {
                    return resource;
                }
            }
            return null;
        }

        public ResourceGroupComposite getGroupComposite(int groupId) {
            for (ResourceGroupComposite groupComposite : groupComposites) {
                if (groupId == groupComposite.getResourceGroup().getId()) {
                    return groupComposite;
                }
            }
            return null;
        }
    }

}
