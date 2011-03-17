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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
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

    private Menu messagesMenu;
    private IMenuButton messageCenterButton;
    private int messageCount;

    public MessageCenterView(String locatorId) {
        super(locatorId, 5);
        setHeight100();
        setAlign(Alignment.CENTER);
        setAutoWidth();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        messagesMenu = new LocatableMenu(this.extendLocatorId("Messages"));

        messageCenterButton = new LocatableIMenuButton(extendLocatorId("RecentEvents"), MSG
            .view_messageCenter_messageTitle(), messagesMenu);
        messageCenterButton.setAutoFit(true);

        emptyMenu();
        addMember(messageCenterButton);

        CoreGUI.getMessageCenter().addMessageListener(this);
    }

    public void onMessage(final Message message) {
        if (!message.isTransient()) {
            if (messageCount == 0) {
                addClearMenuItem();
            }
            messageCount++;

            MenuItem messageItem = new MenuItem(message.conciseMessage, getSeverityIcon(message.severity));
            messageItem.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(MenuItemClickEvent event) {
                    showDetails(message);
                }
            });
            messagesMenu.addItem(messageItem, 1);

            // to avoid flooding the message center, clip old messages
            final int maxMessages = 25;
            if (messageCount > maxMessages) {
                MenuItem[] items = messagesMenu.getItems();
                MenuItem[] clippedItems = new MenuItem[maxMessages + 1]; // +1 to take into account the Clear All Messages item
                System.arraycopy(items, 0, clippedItems, 0, clippedItems.length);
                messagesMenu.setItems(clippedItems);
            }
        }
    }

    private void addClearMenuItem() {
        MenuItem clearItem = new MenuItem(MSG.view_messageCenter_clearAllMessages());
        clearItem.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
            @Override
            public void onClick(MenuItemClickEvent event) {
                emptyMenu();
            }
        });
        messagesMenu.setItems(clearItem); // setItems making this the only item in the menu
        markForRedraw();
    }

    private void emptyMenu() {
        CoreGUI.getMessageCenter().getMessages().clear();
        messageCount = 0;
        messagesMenu.setItems(new MenuItem(MSG.view_messageCenter_noRecentMessages()));
        markForRedraw();
    }

    private void showDetails(Message message) {
        DynamicForm form = new LocatableDynamicForm(extendLocatorId("Details"));
        form.setWrapItemTitles(false);
        form.setAlign(Alignment.LEFT);

        StaticTextItem title = new StaticTextItem("theMessage", MSG.common_title_message());
        title.setValue(message.conciseMessage);

        StaticTextItem severity = new StaticTextItem("severity", MSG.view_messageCenter_messageSeverity());
        FormItemIcon severityIcon = new FormItemIcon();
        severityIcon.setSrc(getSeverityIcon(message.severity));
        severity.setIcons(severityIcon);
        severity.setValue(message.severity.name());

        StaticTextItem date = new StaticTextItem("time", MSG.view_messageCenter_messageTime());
        date.setValue(message.fired);

        StaticTextItem detail = new StaticTextItem("detail", MSG.view_messageCenter_messageDetail());
        detail.setTitleVAlign(VerticalAlignment.TOP);
        detail.setValue(message.detailedMessage);

        form.setItems(title, severity, date, detail);

        final Window window = new LocatableWindow(this.extendLocatorId("MessageWindow"));
        window.setTitle(MSG.view_messageCenter_messageTitle());
        window.setWidth(600);
        window.setHeight(400);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.setShowMaximizeButton(true);
        window.setShowMinimizeButton(false);
        window.centerInPage();
        window.addItem(form);
        window.show();
        window.addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClientEvent event) {
                window.destroy();
            }
        });
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
