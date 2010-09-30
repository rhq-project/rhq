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

import com.smartgwt.client.widgets.Label;

/**
 * A bar for displaying a message at the top of a page - the equivalent of the JSF h:messages component.
 *
 * @author Ian Springer
 */
public class MessageBar extends Label {
    public static final Map<Message.Severity, String> SEVERITY_TO_STYLE_NAME_MAP = new HashMap();
    static {
        SEVERITY_TO_STYLE_NAME_MAP.put(Message.Severity.Info, "InfoBlock");
        SEVERITY_TO_STYLE_NAME_MAP.put(Message.Severity.Warning, "WarnBlock");
        SEVERITY_TO_STYLE_NAME_MAP.put(Message.Severity.Error, "ErrorBlock");
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setWidth100();
        setAutoHeight();

        hide();
    }

    public void setMessage(Message message) {

        String contents;
        if (message != null) {
            contents = message.getTitle();
            if (message.getDetail() != null) {
                contents += ": " + message.getDetail();
            }
        } else {
            contents = null;
        }
        setContents(contents);        
        if (contents != null) {
            String styleName = SEVERITY_TO_STYLE_NAME_MAP.get(message.getSeverity());
            setStyleName(styleName);
        }
        markForRedraw();
        if (contents != null) {
            show();
        } else {
            hide();
        }
    }
}
