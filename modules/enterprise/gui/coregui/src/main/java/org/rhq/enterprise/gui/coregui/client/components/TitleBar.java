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

import com.google.gwt.user.client.Window;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Label;

import org.rhq.enterprise.gui.coregui.client.util.selenium.Locatable;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableToolStrip;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A title bar to be displayed at the top of a content view - contains a label and/or an icon.
 *
 * @author Ian Springer
 */
public class TitleBar extends LocatableToolStrip {
    private Label label;

    public TitleBar(Locatable parent) {
        this(parent, null, null);
    }

    public TitleBar(Locatable parent, String title) {
        this(parent, title, null);
    }

    public TitleBar(Locatable parent, String title, String icon) {
        super(parent.extendLocatorId("TitleBar"));

        setWidth100();
        setHeight(30);

        LocatableVLayout vLayout = new LocatableVLayout(extendLocatorId("VLayout"));
        vLayout.setAlign(VerticalAlignment.CENTER);
        vLayout.setLayoutMargin(6);

        LocatableHLayout hLayout = new LocatableHLayout(vLayout.extendLocatorId("HLayout"));
        vLayout.addMember(hLayout);

        this.label = new Label();
        this.label.setWidth("*");
        this.label.setIcon(icon);
        this.label.setIconWidth(24);
        this.label.setIconHeight(24);
        this.label.setAutoHeight();
        hLayout.addMember(this.label);

        setVisible(false);
        addMember(vLayout);

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
        setVisible(title != null);

        String contents;
        String windowTitle;
        if (title == null) {
            contents = null;
            windowTitle = "RHQ";
        } else {
            contents = "<span class='HeaderLabel'>" + title + "</span>";
            windowTitle = "RHQ: " + title;
        }
        this.label.setContents(contents);
        Window.setTitle(windowTitle);
    }

}
