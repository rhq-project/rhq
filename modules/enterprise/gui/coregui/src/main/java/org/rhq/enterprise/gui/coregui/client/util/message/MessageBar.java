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

import java.util.HashMap;
import java.util.Map;

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
    private static final int AUTO_HIDE_DELAY_MILLIS = 30000; // 30 seconds

    private static final Map<Message.Severity, String> SEVERITY_TO_STYLE_NAME_MAP =
        new HashMap<Message.Severity, String>();
    static {
        SEVERITY_TO_STYLE_NAME_MAP.put(Message.Severity.Info, "InfoBlock");
        SEVERITY_TO_STYLE_NAME_MAP.put(Message.Severity.Warning, "WarnBlock");
        SEVERITY_TO_STYLE_NAME_MAP.put(Message.Severity.Error, "ErrorBlock");
    }

    private static final Map<Message.Severity, String> SEVERITY_TO_ICON_MAP =
        new HashMap<Message.Severity, String>();
    static {
        SEVERITY_TO_ICON_MAP.put(Message.Severity.Info, "info/icn_info_blue.png");
        SEVERITY_TO_ICON_MAP.put(Message.Severity.Warning, "info/icn_info_orange.png");
        SEVERITY_TO_ICON_MAP.put(Message.Severity.Error, "info/icn_info_red.png");
    }

    private Label label;

    public MessageBar() {
        super(LOCATOR_ID);

        setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setWidth100();
        setAutoHeight();
        setHeight(40);

        setAlign(Alignment.CENTER);

        CoreGUI.getMessageCenter().addMessageListener(this);
    }

    @Override
    public void onMessage(Message message) {
        if (message instanceof TransientMessage) {
            TransientMessage transientMessage = (TransientMessage)message;

            // First clear any previous message.
            clearMessage();

            this.label = createLabel(transientMessage);
            addMember(this.label);            
            markForRedraw();

            // Auto-clear the message after 30 seconds unless it's been designated as sticky.
            if (!transientMessage.isSticky()) {
                Timer hideTimer = new Timer() {
                    @Override
                    public void run() {
                        clearMessage();
                    }
                };
                hideTimer.schedule(AUTO_HIDE_DELAY_MILLIS);
            }
        }
    }

    public void clearMessage() {
        if (this.label != null) {
            this.label.destroy();
            removeMember(this.label);
            markForRedraw();
        }
    }

    private Label createLabel(Message message) {
        Label label = new Label();

        String contents = message.getTitle();
        if (message.getDetail() != null) {
            contents += ": " + message.getDetail();
        }
        label.setContents(contents);

        String styleName = (contents != null) ? SEVERITY_TO_STYLE_NAME_MAP.get(message.getSeverity()) : null;
        label.setStyleName(styleName);

        label.setAutoHeight();
        label.setHeight(35);
        label.setAutoWidth();
        label.setWidth("75%");

        String icon = (contents != null) ? SEVERITY_TO_ICON_MAP.get(message.getSeverity()) : null;
        label.setIcon(icon);

        return label;
    }
}
