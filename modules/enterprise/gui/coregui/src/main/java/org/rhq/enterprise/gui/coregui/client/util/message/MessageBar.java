/*
 * RHQ Management Platform
 * Copyright 2010-2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.events.RightMouseDownEvent;
import com.smartgwt.client.widgets.events.RightMouseDownHandler;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemIfFunction;
import com.smartgwt.client.widgets.menu.events.ClickHandler;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.selenium.EnhancedHLayout;


/**
 * A bar for displaying a message at the top of a page - the equivalent of the JSF h:messages component.
 * The message will be displayed for 30 seconds and then will be automatically cleared unless
 * it is a sticky message.
 *
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
public class MessageBar extends EnhancedHLayout implements MessageCenter.MessageListener {

    private static final int AUTO_HIDE_DELAY_MILLIS = 30000;
    private static final String NON_BREAKING_SPACE = "&nbsp;";

    private Label label;
    private Message currentMessage;
    private Message stickyMessage; // this message will always be shown until dismissed by user.
    private Menu showDetailsMenu;
    private Timer messageClearingTimer;

    public MessageBar() {
        super();
        setOverflow(Overflow.VISIBLE);
        setWidth100();
        setAlign(Alignment.CENTER);

        label = new Label();
        label.setAlign(Alignment.CENTER);
        label.setWidth100();
        label.setHeight("30px");
        label.setCanSelectText(true);
        addMember(label);

        showDetailsMenu = new Menu();
        MenuItem showDetailsMenuItem = new MenuItem(MSG.view_messageCenter_messageBarShowDetails());
        showDetailsMenuItem.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(MenuItemClickEvent event) {
                MessageCenterView.showDetails(MessageBar.this.currentMessage);
            }
        });

        MenuItem showRootCauseMenuItem = new MenuItem(MSG.view_messageCenter_messageRootCause());
        showRootCauseMenuItem.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(MenuItemClickEvent event) {
                Message msg = MessageBar.this.currentMessage;
                if (msg != null) {
                    String rootCause = msg.getRootCauseMessage();
                    if (rootCause != null) {
                        SC.say(MSG.view_messageCenter_messageRootCause(), rootCause);
                    }
                }
            }
        });
        showRootCauseMenuItem.setEnableIfCondition(new MenuItemIfFunction() {
            public boolean execute(Canvas target, Menu menu, MenuItem item) {
                Message msg = MessageBar.this.currentMessage;
                return msg != null && msg.getRootCauseMessage() != null;
            }
        });

        showDetailsMenu.setItems(showRootCauseMenuItem, showDetailsMenuItem);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setLabelEmpty();

        // sometimes it's annoying to have the error message hang around for too long;
        // let the user click the message so it goes away on demand
        addDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                clearMessage(true);
            }
        });

        addRightMouseDownHandler(new RightMouseDownHandler() {
            @Override
            public void onRightMouseDown(RightMouseDownEvent event) {
                if (MessageBar.this.currentMessage != null) {
                    showDetailsMenu.showContextMenu();
                }
            }
        });

        CoreGUI.getMessageCenter().addMessageListener(this);
    }

    @Override
    public void onMessage(Message message) {
        if (!message.isBackgroundJobResult()) {
            if (this.messageClearingTimer != null) {
                this.messageClearingTimer.cancel();
            }

            this.currentMessage = message;
            updateLabel(message);

            // Auto-clear the message after some time unless it's been designated as sticky.
            if (message.isSticky()) {
                this.stickyMessage = message;
            } else {
                this.messageClearingTimer = new Timer() {
                    public void run() {
                        clearMessage(false);
                        // if we had a sticky message before, show it again, now that our more recent message has gone away
                        if (stickyMessage != null) {
                            updateLabel(stickyMessage);
                        }
                    }
                };
                this.messageClearingTimer.schedule(AUTO_HIDE_DELAY_MILLIS);
            }
        }
    }

    public void reset() {
        clearMessage(true);
    }

    public void clearMessage(boolean clearSticky) {
        this.currentMessage = null;
        setLabelEmpty();
        markForRedraw();
        if (clearSticky) {
            this.stickyMessage = null;
        }
    }

    private void setLabelEmpty() {
        label.setContents(NON_BREAKING_SPACE);
        label.setIcon(Message.Severity.Blank.getIcon());
        label.setStyleName(Message.Severity.Blank.getStyle());
    }

    private void updateLabel(Message message) {
        label.hide();

        String contents;

        if (message.getConciseMessage() != null) {
            contents = message.getConciseMessage();
        } else if (message.getRootCauseMessage() != null) {
            contents = message.getRootCauseMessage();
        } else {
            contents = message.getDetailedMessage();
        }

        label.setContents(contents);

        String styleName = (contents != null) ? message.getSeverity().getStyle() : null;
        label.setStyleName(styleName);

        String icon = (contents != null) ? message.getSeverity().getIcon() : null;
        label.setIcon(icon);

        label.redraw();
        label.show();
    }

}
