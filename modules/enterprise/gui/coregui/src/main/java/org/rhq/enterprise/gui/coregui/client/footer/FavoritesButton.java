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
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class FavoritesButton extends LocatableIMenuButton {

    public FavoritesButton(String locatorId) {
        super(locatorId, "Favorites");

        final Menu favoritesMenu = new Menu();
        setMenu(favoritesMenu);
        setShowMenuBelow(false);
        setAutoFit(true);

        addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                // Cancel the click event. We'll call show() on the menu ourselves only if we're able to load the
                // favorite Resources successfully.
                clickEvent.cancel();

                Set<Integer> favorites = UserSessionManager.getUserPreferences().getFavoriteResources();

                if (!favorites.isEmpty()) {

                    Integer[] resourceIds = new Integer[favorites.size()];
                    final MenuItem[] items = new MenuItem[favorites.size()];
                    final Map<Integer, MenuItem> idToMenuItemMap = new HashMap<Integer, MenuItem>(favorites.size());
                    int i = 0;
                    for (final Integer resourceId : favorites) {
                        resourceIds[i] = resourceId;
                        MenuItem item = new MenuItem(String.valueOf(resourceId));
                        item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                            public void onClick(MenuItemClickEvent event) {
                                CoreGUI.goToView("Resource/" + resourceId);
                            }
                        });
                        items[i] = item;
                        idToMenuItemMap.put(resourceId, item);
                        i++;
                    }

                    ResourceCriteria criteria = new ResourceCriteria();
                    criteria.addFilterIds(resourceIds);
                    GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                        new AsyncCallback<PageList<Resource>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to load favorite Resources.", caught);
                            }

                            public void onSuccess(PageList<Resource> resources) {
                                for (Resource resource : resources) {
                                    MenuItem item = idToMenuItemMap.get(resource.getId());
                                    // TODO: Ideally, we should use ResourceManagerLocal.disambiguate() here to obtain
                                    //       disambiguated Resource names.
                                    item.setTitle(resource.getName());

                                    String category = resource.getResourceType().getCategory().getDisplayName();

                                    String avail = (resource.getCurrentAvailability() != null && resource
                                        .getCurrentAvailability().getAvailabilityType() != null) ? (resource
                                        .getCurrentAvailability().getAvailabilityType().name().toLowerCase()) : "down";
                                    item.setIcon("types/" + category + "_" + avail + "_16.png");
                                }
                                favoritesMenu.setItems(items);
                                favoritesMenu.showContextMenu();
                            }
                        });
                }
            }
        });
    }
}
