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

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.Criteria.Restriction;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.alert.AlertHistoryView;
import org.rhq.enterprise.gui.coregui.client.footer.FavoritesButton;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.report.ReportTopView;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageBar;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenterView;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class Footer extends LocatableHLayout {
    private static final String LOCATOR_ID = "CoreFooter";

    private MessageBar messageBar;
    private MessageCenterView messageCenter;

    public Footer() {
        super(LOCATOR_ID);
        setHeight(30);
        setAlign(Alignment.LEFT);
        setWidth100();
        setMembersMargin(5);
        setBackgroundColor("#F1F2F3");
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        messageCenter = new MessageCenterView(extendLocatorId(MessageCenterView.LOCATOR_ID));
        final FavoritesButton favoritesButton = new FavoritesButton(extendLocatorId("Favorites"));
        final AlertsMessage alertsMessage = new AlertsMessage(extendLocatorId("Alerts"));
        messageBar = new MessageBar();

        // leave space for the RPC Activity Spinner 
        addMember(createHSpacer(16));

        addMember(alertsMessage);

        addMember(messageBar);

        VLayout favoritesLayout = new VLayout();
        favoritesLayout.setHeight100();
        favoritesLayout.setAutoWidth();
        favoritesLayout.setAlign(Alignment.CENTER);
        favoritesLayout.addMember(favoritesButton);
        addMember(favoritesLayout);

        addMember(messageCenter.getMessageCenterButton());

        addMember(createHSpacer(0));

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

    public static class AlertsMessage extends RefreshableLabel {
        public AlertsMessage(String locatorId) {
            super(locatorId);
            setHeight100();
            setPadding(5);

            setIcon("subsystems/alert/Alert_LOW_16.png");
            setIconSize(16);
            setWrap(false);

            addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    History.newItem(ReportTopView.VIEW_ID.getName() + "/" + ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID
                        + "/" + AlertHistoryView.SUBSYSTEM_VIEW_ID);
                }
            });
        }

        public void refreshLoggedIn() {
            AlertCriteria alertCriteria = new AlertCriteria();
            alertCriteria.addFilterStartTime(System.currentTimeMillis() - (1000L * 60 * 60 * 8)); // last 8 hrs
            alertCriteria.setRestriction(Restriction.COUNT_ONLY);

            GWTServiceLookup.getAlertService().findAlertsByCriteria(alertCriteria,
                new AsyncCallback<PageList<Alert>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_core_error_1(), caught);
                    }

                    public void onSuccess(PageList<Alert> result) {
                        if (result.getTotalSize() == 0) {
                            setContents(MSG.view_core_recentAlerts("0"));
                            setIcon("subsystems/alert/Alert_LOW_16.png");
                        } else {
                            setContents(MSG.view_core_recentAlerts(String.valueOf(result.getTotalSize())));
                            setIcon("subsystems/alert/Alert_HIGH_16.png");
                        }
                    }
                });
        }
    }

    public MessageBar getMessageBar() {
        return messageBar;
    }

    public MessageCenterView getMessageCenter() {
        return messageCenter;
    }

    private HLayout createHSpacer(int width) {
        HLayout spacer = new HLayout();
        spacer.setWidth(width);
        return spacer;
    }

}
