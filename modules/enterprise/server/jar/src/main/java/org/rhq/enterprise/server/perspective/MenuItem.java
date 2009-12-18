/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.perspective;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.GlobalActivatorsType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.InventoryActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.MenuItemFeatureType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.MenuItemType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourceType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TraitActivatorType;

/**
 * An item in the RHQ GUI's menu.
 */
public class MenuItem extends Extension implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private MenuItemFeatureType feature;
    private boolean newWindow;
    private boolean addBreak;
    private List<MenuItem> children;

    public MenuItem(MenuItemType rawMenuItem, String perspectiveName) {
        super(rawMenuItem, perspectiveName, rawMenuItem.getUrl());
        this.feature = rawMenuItem.getFeature();
        this.newWindow = rawMenuItem.isNewWindow();
        this.addBreak = rawMenuItem.isAddBreak();
        this.children = new ArrayList<MenuItem>();

        initActivators(rawMenuItem.getActivators());
    }

    public MenuItemFeatureType getFeature() {
        return feature;
    }

    public boolean isNewWindow() {
        return newWindow;
    }

    public boolean isAddBreak() {
        return addBreak;
    }

    @NotNull
    public List<MenuItem> getChildren() {
        return children;
    }

    public void setChildren(List<MenuItem> children) {
        this.children = (children != null) ? children : new ArrayList<MenuItem>();
    }

    public boolean isMenuGroup() {
        return (null != this.children && this.children.size() > 0);
    }

    public boolean isGraphic() {
        String displayName = getDisplayName();
        return (null == displayName || "".equals(displayName.trim()));
    }

    private void initActivators(GlobalActivatorsType rawActivators) {
        if (rawActivators == null) {
            return;
        }

        // Let our super class init the "common" activators.
        initCommonActivators(rawActivators);

        List<InventoryActivatorType> rawInventoryActivators = rawActivators.getInventory();
        for (InventoryActivatorType rawInventoryActivator : rawInventoryActivators) {
            List<ResourceType> rawResourceConditions = rawInventoryActivator.getResource();
            for (ResourceType rawResourceCondition : rawResourceConditions) {
                List<TraitActivatorType> rawTraitActivators = rawResourceCondition.getTrait();
                Map<String, Pattern> traits = new HashMap<String, Pattern>();
                for (TraitActivatorType rawTraitActivator : rawTraitActivators) {
                    String name = rawTraitActivator.getName();
                    String value = rawTraitActivator.getValue();
                    traits.put(name, Pattern.compile(value));
                }

                // TODO: finish this.
            }
        }
    }

    /**
     * Note that this will clone the children list but not the child MenuItem objects themselves.
     * @see java.lang.Object#clone()
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
