/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.common.layout;

import javax.faces.component.UIComponentBase;
import javax.faces.component.html.HtmlPanelGroup;
import org.rhq.core.gui.util.FacesComponentUtility;

public class DynamicPanelGroup extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.jboss.on.DynamicPanelGroup";
    public static final String COMPONENT_FAMILY = "org.jboss.on.DynamicPanelGroup";

    private static final String PANEL_GROUP_ATTRIBUTE_NAME = "panelGroup";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    private HtmlPanelGroup panelGroup;

    public HtmlPanelGroup getPanelGroup() {
        if (this.panelGroup == null) {
            this.panelGroup = FacesComponentUtility.getExpressionAttribute(this, PANEL_GROUP_ATTRIBUTE_NAME,
                HtmlPanelGroup.class);
            getChildren().add(this.panelGroup);
        }

        return this.panelGroup;
    }

    public void setPanelGroup(HtmlPanelGroup panelGroup) {
        this.panelGroup = panelGroup;
    }
}