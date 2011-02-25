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
package org.rhq.enterprise.gui.coregui.client.util.message;

import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIMenuButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableMenu;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Greg Hinkle
 */
public class MessageCenterView extends LocatableVLayout implements MessageCenter.MessageListener {

    public static final String LOCATOR_ID = "MessageCenter";

    public MessageCenterView(String locatorId) {
        super(locatorId, 5);
        setHeight100();
        setAlign(Alignment.CENTER);
        setAutoWidth();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        CoreGUI.getMessageCenter().addMessageListener(this);

        final Menu recentEventsMenu = new LocatableMenu(this.extendLocatorId("Messages"));

        IMenuButton recentEventsButton = new LocatableIMenuButton(extendLocatorId("RecentEvents"), MSG
            .view_messageCenter_messageTitle(), recentEventsMenu);
        recentEventsButton.setAutoFit(true);

        recentEventsButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                List<Message> messages = CoreGUI.getMessageCenter().getMessages();
                if (messages.isEmpty()) {
                    recentEventsMenu.setItems(new MenuItem(MSG.view_messageCenter_noRecentMessages()));
                } else {
                    MenuItem[] items = new MenuItem[messages.size()];
                    for (int i = 0, messagesSize = messages.size(); i < messagesSize; i++) {
                        final Message message = messages.get(i);
                        MenuItem messageItem = new MenuItem(message.conciseMessage, getSeverityIcon(message.severity));

                        items[i] = messageItem;

                        messageItem.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
                            public void onClick(MenuItemClickEvent event) {
                                showDetails(message);
                            }
                        });
                    }
                    recentEventsMenu.setItems(items);
                }
            }
        });

        addMember(recentEventsButton);
    }

    private void showDetails(Message message) {
        DynamicForm form = new LocatableDynamicForm(extendLocatorId("Details"));
        form.setWrapItemTitles(false);

        StaticTextItem title = new StaticTextItem("title", MSG.view_messageCenter_messageTitle());
        title.setValue(message.conciseMessage);

        StaticTextItem severity = new StaticTextItem("severity", MSG.view_messageCenter_messageSeverity());
        FormItemIcon severityIcon = new FormItemIcon();
        severityIcon.setSrc(getSeverityIcon(message.severity));
        severity.setIcons(severityIcon);
        severity.setValue(message.severity.name());

        StaticTextItem date = new StaticTextItem("time", MSG.view_messageCenter_messageTime());
        date.setValue(message.fired);

        StaticTextItem detail = new StaticTextItem("detail", MSG.view_messageCenter_messageDetail());
        detail.setTitleOrientation(TitleOrientation.TOP);
        detail.setValue(message.detailedMessage);
        detail.setColSpan(2);

        ButtonItem okButton = new ButtonItem("ok", MSG.common_button_ok());
        okButton.setColSpan(2);
        okButton.setAlign(Alignment.CENTER);

        form.setItems(title, severity, date, detail, okButton);

        final Window window = new LocatableWindow(this.extendLocatorId("Message"));
        window.setTitle(message.conciseMessage);
        window.setWidth(600);
        window.setHeight(400);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(form);
        window.show();
        okButton.focusInItem();
        okButton.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent clickEvent) {
                window.destroy();
            }
        });
    }

    public void onMessage(final Message message) {
        if (!message.isTransient()) {
            logMessage(message);
        }
    }

    private void logMessage(Message message) {
        // TODO: Format the message better.
        String logMessage = message.toString();
        switch (message.getSeverity()) {
        case Info:
            Log.info(logMessage);
            break;
        case Warning:
            Log.warn(logMessage);
            break;
        case Error:
            Log.error(logMessage);
            break;
        case Fatal:
            Log.fatal(logMessage);
            break;
        }
    }

    private String getSeverityIcon(Message.Severity severity) {
        String iconSrc = null;
        switch (severity) {
        case Info:
            iconSrc = "info/icn_info_blue.png";
            break;
        case Warning:
            iconSrc = "info/icn_info_orange.png";
            break;
        case Error:
        case Fatal:
            iconSrc = "info/icn_info_red.png";
            break;
        }
        return iconSrc;
    }

}
