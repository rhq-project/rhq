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

import java.util.Set;

import com.google.gwt.user.client.History;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;

/**
 * @author Greg Hinkle
 */
public class FavoritesButton extends IMenuButton {

    public FavoritesButton() {
        super("Favorites");

        final Menu favoritesMenu = new Menu();
        setMenu(favoritesMenu);
        setShowMenuBelow(false);
        setAutoFit(true);

        addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {

                Set<Integer> favorites = CoreGUI.getUserPreferences().getFavoriteResources();

                // todo: lookup resource names
//
//                ResourceCriteria criteria = new ResourceCriteria();
//                criteria.addFilter

                MenuItem[] items = new MenuItem[favorites.size()];
                int i = 0;
                for (final Integer resourceId : favorites) {
                    MenuItem item = new MenuItem(String.valueOf(resourceId));
                    item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                        public void onClick(MenuItemClickEvent event) {
                            History.newItem("Resource/" + resourceId);
                        }
                    });
                    items[i++] = item;
                }

                favoritesMenu.setItems(items);
            }
        });
    }



}
