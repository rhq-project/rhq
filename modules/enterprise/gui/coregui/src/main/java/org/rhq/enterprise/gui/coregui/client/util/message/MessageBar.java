/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLabel;

/**
 * A bar for displaying a message at the top of a page - the equivalent of the JSF h:messages component.
 * The message will be displayed for 30 seconds and then will be automatically cleared.
 *
 * @author Ian Springer
 */
public class MessageBar extends LocatableHLayout implements MessageCenter.MessageListener {
    private static final String LOCATOR_ID = "MessageBar";
    private static final int AUTO_HIDE_DELAY_MILLIS = 30000;

    private LocatableLabel label = new LocatableLabel(extendLocatorId("Label"));
    private Message stickyMessage;

    private static final String NON_BREAKING_SPACE = "&nbsp;";

    public MessageBar() {
        super(LOCATOR_ID);

        setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setWidth100();
        setAlign(Alignment.CENTER);

        label.setAlign(Alignment.CENTER);
        label.setWidth("600px");
        label.setHeight("25px");

        setLabelEmpty();
        addMember(label);

        // sometimes it's annoying to have the error message hang around for too long;
        // let the user click the message so it goes away on demand
        addDoubleClickHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                clearMessage(true);
            }
        });

        CoreGUI.getMessageCenter().addMessageListener(this);
    }

    @Override
    public void onMessage(Message message) {
        if (!message.isBackgroundJobResult()) {
            updateLabel(message);

            // Auto-clear the message after some time unless it's been designated as sticky.
            if (message.isSticky()) {
                this.stickyMessage = message;
            } else {
                new Timer() {
                    @Override
                    public void run() {
                        clearMessage(false);
                        if (stickyMessage != null) {
                            updateLabel(stickyMessage);
                        }
                    }
                }.schedule(AUTO_HIDE_DELAY_MILLIS);
            }
        }
    }

    public void clearMessage(boolean clearSticky) {
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
        String contents = (message.getConciseMessage() != null) ? message.getConciseMessage() : message
            .getDetailedMessage();
        label.setContents(contents);

        String styleName = (contents != null) ? message.getSeverity().getStyle() : null;
        label.setStyleName(styleName);

        // TODO: Create some custom edge images in green, yellow, red, etc. so we can add nice rounded corners to the
        //       label.
        //label.setShowEdges(true);

        String icon = (contents != null) ? message.getSeverity().getIcon() : null;
        label.setIcon(icon);

        markForRedraw();
    }
}
