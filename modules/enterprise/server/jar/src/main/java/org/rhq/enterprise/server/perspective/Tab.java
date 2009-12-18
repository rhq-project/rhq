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
package org.rhq.enterprise.server.perspective;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.enterprise.server.perspective.activator.FacetActivator;
import org.rhq.enterprise.server.perspective.activator.ResourceConditionSet;
import org.rhq.enterprise.server.perspective.activator.ResourcePermissionActivator;
import org.rhq.enterprise.server.perspective.activator.ResourceTypeActivator;
import org.rhq.enterprise.server.perspective.activator.TraitActivator;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.FacetActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.InventoryActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourceActivatorsType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourcePermissionActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourceType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TabType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TraitActivatorType;

/**
 * A tab in the Resource or Group view.
 *  
 * @author Ian Springer
 */
public class Tab extends Extension implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String qualifiedName;
    private String url;
    private List<Tab> children;

    public Tab(TabType rawTab, String perspectiveName) {
        super(rawTab, perspectiveName, rawTab.getUrl());
        this.name = getSimpleName(rawTab.getName());
        this.qualifiedName = rawTab.getName();
        this.url = rawTab.getUrl();
        if (rawTab.getApplication() != null) {
            this.url += "&tab=" + this.qualifiedName;
        }
        this.children = new ArrayList<Tab>();
        initActivators(rawTab.getActivators());
    }

    @NotNull
    public List<Tab> getChildren() {
        return children;
    }

    public void setChildren(List<Tab> children) {
        this.children = (children != null) ? children : new ArrayList<Tab>();
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    private static String getSimpleName(String qualifiedName) {
        int lastDotIndex = qualifiedName.lastIndexOf(".");
        return qualifiedName.substring(lastDotIndex + 1);
    }

    private void initActivators(ResourceActivatorsType rawActivators) {
        if (rawActivators == null) {
            return;
        }

        // Let our super class init the "common" activators.
        initCommonActivators(rawActivators);

        List<FacetActivatorType> rawFacetActivators = rawActivators.getFacet();
        for (FacetActivatorType rawFacetActivator : rawFacetActivators) {
            String rawName = rawFacetActivator.getName().toString();
            FacetActivator facetActivator = new FacetActivator(ResourceTypeFacet.valueOf(rawName.toUpperCase(Locale.US)));
            getActivators().add(facetActivator);
        }

        List<ResourcePermissionActivatorType> rawResourcePermissionActivators = rawActivators.getResourcePermission();
        for (ResourcePermissionActivatorType rawResourcePermissionActivator : rawResourcePermissionActivators) {
            String rawName = rawResourcePermissionActivator.getName().toString();
            Permission permission = Permission.valueOf(rawName.toUpperCase(Locale.US));
            ResourcePermissionActivator resourcePermissionActivator = new ResourcePermissionActivator(permission);
            getActivators().add(resourcePermissionActivator);
        }

        List<TraitActivatorType> rawTraitActivators = rawActivators.getTrait();
        for (TraitActivatorType rawTraitActivator : rawTraitActivators) {
            String name = rawTraitActivator.getName();
            String value = rawTraitActivator.getValue();
            TraitActivator traitActivator = new TraitActivator(name, Pattern.compile(value));
            getActivators().add(traitActivator);
        }

        List<InventoryActivatorType> rawInventoryActivators = rawActivators.getResourceType();
        for (InventoryActivatorType rawInventoryActivator : rawInventoryActivators) {
            List<ResourceType> rawResourceConditions = rawInventoryActivator.getResource();
            List<ResourceConditionSet> resourceConditionSets =
                    new ArrayList<ResourceConditionSet>(rawResourceConditions.size());
            for (ResourceType rawResourceCondition : rawResourceConditions) {
                List<ResourcePermissionActivatorType> rawPermissions = rawResourceCondition.getPermission();
                EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
                for (ResourcePermissionActivatorType rawPermission : rawPermissions) {
                    String rawName = rawPermission.getName().toString();
                    Permission permission = Permission.valueOf(rawName.toUpperCase(Locale.US));
                    permissions.add(permission);
                }

                List<TraitActivatorType> rawTraits = rawResourceCondition.getTrait();
                Map<String, Pattern> traits = new HashMap();
                for (TraitActivatorType rawTraitActivator : rawTraits) {
                    String name = rawTraitActivator.getName();
                    String value = rawTraitActivator.getValue();
                    traits.put(name, Pattern.compile(value));
                }

                ResourceConditionSet resourceConditionSet = new ResourceConditionSet(
                        rawResourceCondition.getPlugin(), rawResourceCondition.getType(),
                        permissions, traits);
                resourceConditionSets.add(resourceConditionSet);
            }
            ResourceTypeActivator resourceTypeActivator = new ResourceTypeActivator(resourceConditionSets);
            getActivators().add(resourceTypeActivator);
        }
    }
}
