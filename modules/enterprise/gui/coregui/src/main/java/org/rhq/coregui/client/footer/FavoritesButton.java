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
package org.rhq.coregui.client.footer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.Messages;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.menu.MenuBarView;
import org.rhq.coregui.client.util.message.MessageBar;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class FavoritesButton extends IMenuButton {

    private Messages MSG = CoreGUI.getMessages();
    final Menu favoriteResourcesMenu;
    final Menu favoriteGroupsMenu;
    final Menu recentlyViewedMenu;
    final Menu favoritesMenu;

    public FavoritesButton() {
        super(CoreGUI.getMessages().favorites());

        // this is the main menu - the "favorites" button shown in the UI the user initially clicks 
        favoritesMenu = new Menu();
        favoritesMenu.setSubmenuDirection("left");
        setMenu(favoritesMenu);
        setAutoFit(true);

        // these are the child menus directly under the main favorites button 
        favoriteResourcesMenu = new Menu();
        favoriteGroupsMenu = new Menu();
        recentlyViewedMenu = new Menu();
        favoriteResourcesMenu.setSubmenuDirection("left");
        favoriteResourcesMenu.setAutoWidth();
        favoriteGroupsMenu.setSubmenuDirection("left");
        favoriteGroupsMenu.setAutoWidth();
        recentlyViewedMenu.setSubmenuDirection("left");
        recentlyViewedMenu.setAutoWidth();
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
                    showMenu();
                }
            });
    }

    public void showMenu() {
        setLeft(DOM.getElementById(MenuBarView.BTN_FAV_ID).getAbsoluteLeft());
        final Set<Integer> favoriteResourceIds = UserSessionManager.getUserPreferences().getFavoriteResources();
        final Set<Integer> favoriteGroupIds = UserSessionManager.getUserPreferences()
            .getFavoriteResourceGroups();
        final List<Integer> recentResourceIds = UserSessionManager.getUserPreferences().getRecentResources();
        final List<Integer> recentGroupIds = UserSessionManager.getUserPreferences().getRecentResourceGroups();

        // if we have no menu items at all, then show the empty menu now
        if (favoriteGroupIds.isEmpty() && favoriteResourceIds.isEmpty() && recentResourceIds.isEmpty()
            && recentGroupIds.isEmpty()) {
            favoritesMenu.showNextTo(FavoritesButton.this, "bottom");
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

            public void onSuccess(final Favorites favorites) {
                // For Ancestry we need all the resource types and ancestry resource types loaded
                HashSet<Integer> typesSet = new HashSet<Integer>();
                HashSet<String> ancestries = new HashSet<String>();
                for (Resource resource : favorites.resources) {
                    typesSet.add(resource.getResourceType().getId());
                    ancestries.add(resource.getAncestry());
                }

                // In addition to the types of the result resources, get the types of their ancestry
                typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

                ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
                typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]),
                    new TypesLoadedCallback() {
                        @Override
                        public void onTypesLoaded(Map<Integer, ResourceType> types) {
                            // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.                
                            AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                            // generate the menus
                            buildFavoriteResourcesMenu(favorites, favoriteResourcesMenu, favoriteResourceIds,
                                typesWrapper);
                            buildFavoriteGroupsMenu(favorites, favoriteGroupsMenu, favoriteGroupIds);
                            buildRecentlyViewedMenu(favorites, recentlyViewedMenu, recentResourceIds,
                                recentGroupIds, typesWrapper);

                            favoritesMenu.showNextTo(FavoritesButton.this, "bottom");
                        }
                    });
            }
        });
    }

    private void buildFavoriteResourcesMenu(Favorites favorites, Menu menu, Set<Integer> resourceIds,
        AncestryUtil.MapWrapper typesWrapper) {

        if (resourceIds.isEmpty()) {
            menu.setItems();
            return;
        }

        List<MenuItem> items = new ArrayList<MenuItem>(resourceIds.size());

        for (final Integer resourceId : resourceIds) {
            Resource resource = favorites.getResource(resourceId);
            if (null == resource) {
                // if the resource is gone just skip it
                continue;
            }

            MenuItem item = new MenuItem(resource.getName());
            item.setIcon(ImageManager.getResourceIcon(resource));

            // build a subMenu to display a disambiguated resource
            item.setAttribute(AncestryUtil.RESOURCE_ID, resourceId);
            item.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
            item.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
            item.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());
            item.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);
            Menu ancestryMenu = new Menu();
            MenuItem ancestryItem = new MenuItem(AncestryUtil.getAncestryHoverHTML(item, -1));
            ancestryItem.setEnabled(false);
            ancestryMenu.setItems(ancestryItem);
            ancestryMenu.setSubmenuDirection("left");
            ancestryMenu.setAutoWidth();
            item.setSubmenu(ancestryMenu);

            item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getResourceLink(resourceId));
                }
            });

            items.add(item);
        }

        menu.setCanSelectParentItems(Boolean.TRUE);
        menu.setItems(items.toArray(new MenuItem[items.size()]));
    }

    private void buildFavoriteGroupsMenu(Favorites favorites, Menu menu, Set<Integer> groupIds) {

        if (groupIds.isEmpty()) {
            menu.setItems();
            return;
        }

        List<MenuItem> items = new ArrayList<MenuItem>(groupIds.size());

        for (final Integer groupId : groupIds) {
            ResourceGroupComposite groupComposite = favorites.getGroupComposite(groupId);
            if (null == groupComposite) {
                // if the resource group is gone just skip it
                continue;
            }
            final ResourceGroup group = groupComposite.getResourceGroup();

            MenuItem item = new MenuItem(String.valueOf(groupId));
            item.setTitle(group.getName());
            item.setIcon(ImageManager.getGroupIcon(group.getGroupCategory(),
                groupComposite.getExplicitAvailabilityType()));

            item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getResourceGroupLink(group));
                }
            });
            items.add(item);
        }
        menu.setItems(items.toArray(new MenuItem[items.size()]));
    }

    private void buildRecentlyViewedMenu(Favorites favorites, Menu menu, List<Integer> recentResourceIds,
        List<Integer> recentGroupIds, AncestryUtil.MapWrapper typesWrapper) {

        if (recentResourceIds.isEmpty() && recentGroupIds.isEmpty()) {
            return;
        }
        List<MenuItem> items = new ArrayList<MenuItem>(recentResourceIds.size() + recentGroupIds.size() + 1);

        for (final Integer resourceId : recentResourceIds) {
            Resource resource = favorites.getResource(resourceId);
            if (null == resource) {
                // if the resource is gone just skip it
                continue;
            }

            MenuItem item = new MenuItem(resource.getName());
            item.setIcon(ImageManager.getResourceIcon(resource));

            // build a subMenu to display a disambiguated resource
            item.setAttribute(AncestryUtil.RESOURCE_ID, resourceId);
            item.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
            item.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
            item.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());
            item.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

            Menu ancestryMenu = new Menu();
            MenuItem ancestryItem = new MenuItem(AncestryUtil.getAncestryHoverHTML(item, -1));
            ancestryItem.setEnabled(false);
            ancestryMenu.setItems(ancestryItem);
            ancestryMenu.setSubmenuDirection("left");
            ancestryMenu.setAutoWidth();
            item.setSubmenu(ancestryMenu);

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
            final ResourceGroup group = groupComposite.getResourceGroup();

            MenuItem item = new MenuItem(String.valueOf(groupId));
            item.setTitle(group.getName());
            item.setIcon(ImageManager.getGroupIcon(group.getGroupCategory(),
                groupComposite.getExplicitAvailabilityType()));

            item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                public void onClick(MenuItemClickEvent event) {
                    CoreGUI.goToView(LinkManager.getResourceGroupLink(group));
                }
            });
            items.add(item);
        }

        menu.setCanSelectParentItems(Boolean.TRUE);
        menu.setItems(items.toArray(new MenuItem[items.size()]));
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
