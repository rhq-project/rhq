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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.server.perspective.activator.InventoryActivator;
import org.rhq.enterprise.server.perspective.activator.ResourceConditionSet;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.GlobalActivatorsType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.InventoryActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.PageLinkType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourcePermissionActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourceType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TraitActivatorType;

/**
 * An item in the RHQ GUI's menu.
 */
public class PageLink extends Extension implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String pageName;

    public PageLink(PageLinkType rawPageLink, String perspectiveName, String pageName, String url) {
        super(rawPageLink, perspectiveName, url);

        this.pageName = pageName;

        initActivators(rawPageLink.getActivators());
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
            List<ResourceConditionSet> resourceConditionSets = new ArrayList<ResourceConditionSet>(
                rawResourceConditions.size());
            for (ResourceType rawResourceCondition : rawResourceConditions) {
                List<ResourcePermissionActivatorType> rawPermissions = rawResourceCondition.getResourcePermission();
                EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
                for (ResourcePermissionActivatorType rawPermission : rawPermissions) {
                    String rawName = rawPermission.getName().toString();
                    Permission permission = Permission.valueOf(rawName.toUpperCase(Locale.US));
                    permissions.add(permission);
                }

                List<TraitActivatorType> rawTraits = rawResourceCondition.getTrait();
                Map<String, Pattern> traits = new HashMap<String, Pattern>();
                for (TraitActivatorType rawTraitActivator : rawTraits) {
                    String name = rawTraitActivator.getName();
                    String value = rawTraitActivator.getValue();
                    traits.put(name, Pattern.compile(value));
                }

                ResourceConditionSet resourceConditionSet = new ResourceConditionSet(rawResourceCondition.getPlugin(),
                    rawResourceCondition.getType(), permissions, traits);
                resourceConditionSets.add(resourceConditionSet);
            }
            InventoryActivator resourceTypeActivator = new InventoryActivator(resourceConditionSets);
            getActivators().add(resourceTypeActivator);
        }
    }

    /**
     * @return the pageName
     */
    public String getPageName() {
        return pageName;
    }

}
