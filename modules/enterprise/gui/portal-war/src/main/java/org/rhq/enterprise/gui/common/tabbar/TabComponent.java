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
package org.rhq.enterprise.gui.common.tabbar;

import java.util.ArrayList;
import java.util.List;
import javax.faces.component.UIComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A component that represents a tab on a tab bar.
 *
 * @author Ian Springer
 */
public class TabComponent extends AbstractTabComponent {
    public static final String COMPONENT_TYPE = "org.jboss.on.Tab";
    public static final String COMPONENT_FAMILY = "org.jboss.on.Tab";

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @NotNull
    public List<SubtabComponent> getSubtabs() {
        List<SubtabComponent> subtabs = new ArrayList<SubtabComponent>();
        if (getChildCount() == 0) {
            return subtabs;
        }

        List<UIComponent> children = getChildren();
        for (UIComponent child : children) {
            if (child instanceof SubtabComponent) {
                subtabs.add((SubtabComponent) child);
            }
        }

        return subtabs;
    }

    @Nullable
    public SubtabComponent getSubtabByName(String subtabName) {
        SubtabComponent selectedSubtab = null;
        for (SubtabComponent subtab : getSubtabs()) {
            if (subtab.getName().equals(subtabName)) {
                selectedSubtab = subtab;
                break;
            }
        }

        return selectedSubtab;
    }

    @Nullable
    public SubtabComponent getSelectedSubtab() {
        if (getSubtabs().isEmpty()) {
            return null;
        }

        List<SubtabComponent> subtabs = getSubtabs();
        for (SubtabComponent subtab : subtabs) {
            if (subtab.isSelected()) {
                return subtab;
            }
        }

        if (isSelected()) {
            throw new IllegalStateException("'" + getName()
                + "' tab is selected and has subtabs, but no subtab is selected.");
        }

        return null;
    }

    @Nullable
    public SubtabComponent getDefaultSubtab() {
        if (getSubtabs().isEmpty()) {
            return null;
        }

        SubtabComponent subtab = (isSelected()) ? getSelectedSubtab() : getSubtabs().get(0);
        return subtab;
    }
}