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
package org.rhq.enterprise.gui.coregui.client.components;

import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * A title bar to be displayed at the top of a content view - contains a label and/or an icon.
 *
 * @author Ian Springer
 */
public class TitleBar extends VLayout {
    private Label label;

    public TitleBar() {
        this(null, null);
    }

    public TitleBar(String title) {
        this(title, null);
    }

    public TitleBar(String title, String icon) {
        setWidth100();
        setHeight(35);

        this.label = new Label();
        this.label.setIcon(icon);
        this.label.setIconWidth(24);
        this.label.setIconHeight(24);
        this.label.setAutoHeight();

        setVisible(false);
        addMember(this.label);
        setTitle(title);
    }

    public void setTitle(String title) {
        String normalizedTitle;
        if (title != null) {
            String trimmedTitle = title.trim();
            normalizedTitle = (!trimmedTitle.equals("")) ? trimmedTitle : null;
        } else {
            normalizedTitle = null;
        }
        refresh(normalizedTitle);
    }

    public void setIcon(String icon) {
        this.label.setIcon(icon);
    }

    private void refresh(String title) {
        String contents = "<span class='HeaderLabel'>" + title + "</span>";
        this.label.setContents(contents);
        setVisible(title != null);
        //markForRedraw();
    }
}
