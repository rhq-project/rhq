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
package org.rhq.enterprise.gui.coregui.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStripSeparator;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.footer.FavoritesButton;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenterView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class Footer extends LocatableToolStrip {
    private static final String LOCATOR_ID = "CoreFooter";

    public Footer() {
        super(LOCATOR_ID);
        setHeight(30);
        setAlign(VerticalAlignment.CENTER);
        //        setPadding(5);
        setWidth100();
        setMembersMargin(15);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        final UserSessionState userSessionState = new UserSessionState("UserSessionState");
        final MessageCenterView recentMessage = new MessageCenterView(extendLocatorId(MessageCenterView.LOCATOR_ID));
        recentMessage.setWidth("*");
        final FavoritesButton favoritesButton = new FavoritesButton(extendLocatorId("Favorites"));
        final AlertsMessage alertsMessage = new AlertsMessage(extendLocatorId("Alerts"));

        addMember(userSessionState);
        addMember(new ToolStripSeparator());
        addMember(recentMessage);
        addMember(new ToolStripSeparator());
        addMember(favoritesButton);
        addMember(alertsMessage);

        userSessionState.schedule(15000);
        alertsMessage.schedule(60000);
    }

    public abstract static class RefreshableLabel extends LocatableLabel {
        public RefreshableLabel(String locatorId) {
            super(locatorId);
        }

        // scheduling refreshes is sub-optimal, really need to move to a message bus architecture
        public void schedule(int millis) {
            new Timer() {
                public void run() {
                    refresh();
                }
            }.scheduleRepeating(millis);
        }

        @Override
        protected void onInit() {
            super.onInit();

            refresh();
        }

        public void refresh() {
            if (UserSessionManager.isLoggedIn()) {
                refreshLoggedIn();
            } else {
                refreshLoggedOut();
            }
        }

        public abstract void refreshLoggedIn();

        public void refreshLoggedOut() {
            setContents("");
            setIcon(null);
        }
    }

    public static class UserSessionState extends RefreshableLabel {
        public UserSessionState(String locatorId) {
            super(locatorId);
            setWrap(false);
            setMargin(5);
            setValign(VerticalAlignment.CENTER);
        }

        public void refreshLoggedIn() {
            setContents("Logged in as " + UserSessionManager.getSessionSubject().getName());
        }

        public void refreshLoggedOut() {
            setContents("Logged out");
        }
    }

    public static class AlertsMessage extends RefreshableLabel {
        public AlertsMessage(String locatorId) {
            super(locatorId);
            setHeight(30);
            setPadding(5);

            setIcon("subsystems/alert/Alert_LOW_16.png");
            setIconSize(16);
            setWrap(false);

            addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    History.newItem("Reports/Inventory/Recent Alerts");
                }
            });
        }

        public void refreshLoggedIn() {
            AlertCriteria alertCriteria = new AlertCriteria();

            // only get one record from the first page, we only really care about the count
            alertCriteria.setPaging(0, 1);

            // last eight hours
            alertCriteria.addFilterStartTime(System.currentTimeMillis() - (1000L * 60 * 60 * 8));

            //check for still logged in before submitting server side request
            if (UserSessionManager.isLoggedIn()) {
                GWTServiceLookup.getAlertService().findAlertsByCriteria(alertCriteria,
                    new AsyncCallback<PageList<Alert>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Latest alerts lookup failed", caught);
                        }

                        public void onSuccess(PageList<Alert> result) {
                            if (result.isEmpty()) {
                                setContents("no recent alerts");
                                setIcon("subsystems/alert/Alert_LOW_16.png");
                            } else {
                                setContents(result.getTotalSize() + " recent alerts");
                                setIcon("subsystems/alert/Alert_HIGH_16.png");
                            }
                        }
                    });
            } else {//dump request
                Log.debug("user not logged in. Not fetching any alerts now.");
            }
        }
    }

}
