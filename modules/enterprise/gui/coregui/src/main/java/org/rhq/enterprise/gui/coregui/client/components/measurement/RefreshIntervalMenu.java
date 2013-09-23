/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.measurement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * Component to handle the Auto-Refresh Interval Menu.
 * @author  Mike Thompson
 */
public class RefreshIntervalMenu extends IMenuButton {
    private static final Messages MSG = CoreGUI.getMessages();

    private HashMap<Integer, String> refreshMenuMappings;
    private MenuItem[] refreshMenuItems;
    private int refreshInterval = 0;

    public RefreshIntervalMenu() {
        super();
        Menu refreshMenu = new Menu();
        refreshMenu.setShowShadow(true);
        refreshMenu.setShadowDepth(10);
        refreshMenu.setAutoWidth();
        refreshMenu.setHeight(15);
        com.smartgwt.client.widgets.menu.events.ClickHandler menuClick = new com.smartgwt.client.widgets.menu.events.ClickHandler() {
            @Override
            public void onClick(MenuItemClickEvent event) {
                String selection = event.getItem().getTitle();
                refreshInterval = 0;
                if (selection != null) {
                    for (RefreshSelectItemData item : RefreshSelectItemData.values()) {
                        if (selection.equals(item.getLabel())) {
                            refreshInterval = item.getTimeSpanInSeconds();
                        }
                    }
                    UserSessionManager.getUserPreferences().setPageRefreshInterval(refreshInterval,
                        new RefreshCallback());
                }
            }
        };

        refreshMenuMappings = new HashMap<Integer, String>();
        refreshMenuItems = new MenuItem[RefreshSelectItemData.values().length];
        List<MenuItem> menuItemList = new ArrayList<MenuItem>(RefreshSelectItemData.values().length);
        int retrievedRefreshInterval = RefreshSelectItemData.refresh1.getTimeSpanInSeconds();
        if (null != UserSessionManager.getUserPreferences()) {
            retrievedRefreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();
        }
        for (RefreshSelectItemData item : RefreshSelectItemData.values()) {
            MenuItem menuItem = new MenuItem(item.getLabel(), "");
            menuItem.addClickHandler(menuClick);
            refreshMenuMappings.put(item.getTimeSpanInSeconds(), item.getLabel());
            if (retrievedRefreshInterval == item.getTimeSpanInSeconds()) {
                menuItem.setIcon(ImageManager.getAvailabilityIcon(true));
            }
            menuItemList.add(menuItem);
        }
        refreshMenuItems = menuItemList.toArray(new MenuItem[menuItemList.size()]);

        refreshMenu.setItems(refreshMenuItems);
        setMenu(refreshMenu);
        refreshMenu.setAutoHeight();
        setTitle(MSG.common_title_change_refresh_time());
        setWidth(140);
        setShowTitle(true);
        setTop(0);
        setIconOrientation("left");
    }

    private final class RefreshCallback implements AsyncCallback<Subject> {
        public void onSuccess(Subject subject) {
            String m;
            if (refreshInterval > 0) {
                m = MSG.view_dashboards_portlets_refresh_success1();
            } else {
                m = MSG.view_dashboards_portlets_refresh_success2();
            }
            CoreGUI.getMessageCenter().notify(new Message(m, Message.Severity.Info));
            updateRefreshMenu();
        }

        public void onFailure(Throwable throwable) {
            String m;
            if (refreshInterval > 0) {
                m = MSG.view_dashboards_portlets_refresh_fail1();
            } else {
                m = MSG.view_dashboards_portlets_refresh_fail2();
            }
            CoreGUI.getMessageCenter().notify(new Message(m, Message.Severity.Error));
            // Revert back to our original favorite status, since the server update failed.
            updateRefreshMenu();
        }
    }

    private void updateRefreshMenu() {
        if (refreshMenuItems != null) {
            int retrievedRefreshInterval = RefreshSelectItemData.refresh1.getTimeSpanInSeconds();
            if (null != UserSessionManager.getUserPreferences()) {
                retrievedRefreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();
            }
            String currentSelection = refreshMenuMappings.get(retrievedRefreshInterval);
            if (currentSelection != null) {
                //iterate over menu items and update icon details
                for (int i = 0; i < refreshMenuItems.length; i++) {
                    MenuItem menu = refreshMenuItems[i];
                    if (currentSelection.equals(menu.getTitle())) {
                        menu.setIcon(ImageManager.getAvailabilityIcon(true));
                    } else {
                        menu.setIcon("");
                    }
                    refreshMenuItems[i] = menu;
                }
                //update the menu
                getMenu().setItems(refreshMenuItems);
            }
        }
        markForRedraw();
    }

    private enum RefreshSelectItemData {

        stop(MSG.view_dashboards_portlets_refresh_none(), 0),
        refresh1(MSG.view_dashboards_portlets_refresh_one_min(), 60000),
        refresh5(MSG.view_dashboards_portlets_refresh_multiple_min(String.valueOf(5)), 5 * 60000),
        refresh10(MSG.view_dashboards_portlets_refresh_multiple_min(String.valueOf(10)), 10 * 60000);

        private final String label;
        private final Integer timeSpanInSeconds;

        RefreshSelectItemData(String label, Integer timeSpanInSeconds) {
            this.label = label;
            this.timeSpanInSeconds = timeSpanInSeconds;
        }

        private String getLabel() {
            return label;
        }

        private Integer getTimeSpanInSeconds() {
            return timeSpanInSeconds;
        }
    }

}
