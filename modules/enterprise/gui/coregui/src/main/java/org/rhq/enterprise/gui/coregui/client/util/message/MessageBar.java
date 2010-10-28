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
import com.smartgwt.client.widgets.Label;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * A bar for displaying a message at the top of a page - the equivalent of the JSF h:messages component.
 * The message will be displayed for 30 seconds and then will be automatically cleared.
 *
 * @author Ian Springer
 */
public class MessageBar extends LocatableHLayout implements MessageCenter.MessageListener {
    private static final String LOCATOR_ID = "MessageBar";
    private static final int AUTO_HIDE_DELAY_MILLIS = 15000; // 15 seconds

    private Label label;
    private Message stickyMessage;

    public MessageBar() {
        super(LOCATOR_ID);

        setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setWidth100();
        setHeight(35);

        setAlign(Alignment.CENTER);

        CoreGUI.getMessageCenter().addMessageListener(this);
    }

    @Override
    public void onMessage(Message message) {
        if (!message.isBackgroundJobResult()) {
            // First clear any previous message.
            clearMessage(message.isSticky());
            displayMessage(message);

            // Auto-clear the message after 15 seconds unless it's been designated as sticky.
            if (message.isSticky()) {
                this.stickyMessage = message;
            } else {
                Timer hideTimer = new Timer() {
                    @Override
                    public void run() {
                        clearMessage(false);
                        if (stickyMessage != null) {
                            displayMessage(stickyMessage);
                        }
                    }
                };
                hideTimer.schedule(AUTO_HIDE_DELAY_MILLIS);
            }
        }
    }

    public void clearMessage() {
        clearMessage(true);
    }

    private void displayMessage(Message message) {
        this.label = createLabel(message);
        addMember(this.label);
        markForRedraw();
    }

    private void clearMessage(boolean clearSticky) {
        if (this.label != null) {
            this.label.destroy();
            markForRedraw();
        }
        if (clearSticky) {
            this.stickyMessage = null;
        }
    }

    private Label createLabel(Message message) {
        Label label = new Label();

        String contents = (message.getConciseMessage() != null) ? message.getConciseMessage() : message
            .getDetailedMessage();
        label.setContents(contents);
        label.setAlign(Alignment.CENTER);

        String styleName = (contents != null) ? message.getSeverity().getStyle() : null;
        label.setStyleName(styleName);

        label.setWidth(400);

        // TODO: Create some custom edge images in greed, yellow, red, etc. so we can add nice rounded corners to the
        //       label.
        //label.setShowEdges(true);

        String icon = (contents != null) ? message.getSeverity().getIcon() : null;
        label.setIcon(icon);

        return label;
    }
}
