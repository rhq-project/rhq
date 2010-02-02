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

package org.rhq.enterprise.server.perspective.activator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.CommonActivatorsType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.DebugModeActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.FacetActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.GlobalActivatorsType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.GlobalPermissionActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.InventoryActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.LicenseFeatureActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourceActivatorsType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourcePermissionActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.ResourceType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.SuperuserActivatorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.perspective.TraitActivatorType;

public class ActivatorHelper {

    private static final Log LOG = LogFactory.getLog(ActivatorHelper.class);

    /**
      * Test trait conditions against resources.  Optionally, one or all resources must much all of the
      * trait conditions.
      * 
      * @param subject The current user
      * @param traitMatchers The trait activator pattern matchers that must all be satisfied
      * @param resources The resources whose trait values will be tested
      * @param matchAll If true then all resources must pass, if false only one must pass
      * @return true if, optionally, all or any resources satisfy the trait conditions
      */
    public static boolean areTraitsSatisfied(Subject subject, Map<String, Matcher> traitMatchers,
        Collection<Resource> resources, boolean matchAll) {

        // return true if there are no trait activators to satisfy
        if (traitMatchers.isEmpty()) {
            return true;
        }

        MeasurementDataManagerLocal measurementDataManager = LookupUtil.getMeasurementDataManager();

        for (Resource resource : resources) {
            boolean traitsSatisfied = true;
            List<MeasurementDataTrait> traits = measurementDataManager.findCurrentTraitsForResource(subject, resource
                .getId(), null);

            int numTraitsTested = 0;
            for (MeasurementDataTrait trait : traits) {
                Matcher traitMatcher = traitMatchers.get(trait.getName());
                if (null != traitMatcher) {
                    ++numTraitsTested;

                    traitMatcher.reset(trait.getValue());
                    if (!traitMatcher.find()) {
                        traitsSatisfied = false;
                        break;
                    }
                }
            }

            if (traitsSatisfied) {
                if (numTraitsTested != traitMatchers.size()) {
                    if (LOG.isDebugEnabled()) {
                        String error = "" //
                            + "Potential error in perspective descriptor." //
                            + " Not all trait activators matched trait for resource type: " + traitMatchers.keySet() //
                            + " Or, Trait value may not yet have been collected for resource.";
                        LOG.debug(error);
                    }

                    return false;
                }

                if (!matchAll) {
                    return true;
                }
            } else {
                if (matchAll) {
                    return false;
                }
            }
        }

        // if we've run through all the resources then either every resource matched (for matchAll) or
        // every resource failed (for !matchAll)
        return matchAll;
    }

    public static boolean initCommonActivators(CommonActivatorsType rawActivators, List<Activator<?>> activators) {
        boolean debugMode = false;

        if (rawActivators == null) {
            return debugMode;
        }

        DebugModeActivatorType rawDebugModeActivator = rawActivators.getDebugMode();
        if (rawDebugModeActivator != null) {
            debugMode = true;
        }

        List<LicenseFeatureActivatorType> rawLicenseFeatures = rawActivators.getLicenseFeature();
        for (LicenseFeatureActivatorType rawLicenseFeature : rawLicenseFeatures) {
            String rawName = rawLicenseFeature.getName().value();
            LicenseFeature licenseFeature = LicenseFeature.valueOf(rawName.toUpperCase(Locale.US));
            LicenseFeatureActivator licenseFeatureActivator = new LicenseFeatureActivator(licenseFeature);
            activators.add(licenseFeatureActivator);
        }

        SuperuserActivatorType rawSuperuserActivator = rawActivators.getSuperuser();
        if (rawSuperuserActivator != null) {
            SuperuserActivator superuserActivator = new SuperuserActivator();
            activators.add(superuserActivator);
        }

        List<GlobalPermissionActivatorType> rawGlobalPermissionActivators = rawActivators.getGlobalPermission();
        for (GlobalPermissionActivatorType rawGlobalPermissionActivator : rawGlobalPermissionActivators) {
            String rawName = rawGlobalPermissionActivator.getName().value();
            Permission permission = Permission.valueOf(rawName.toUpperCase(Locale.US));
            GlobalPermissionActivator globalPermissionActivator = new GlobalPermissionActivator(permission);
            activators.add(globalPermissionActivator);
        }

        return debugMode;
    }

    public static boolean initGlobalActivators(GlobalActivatorsType rawActivators, List<Activator<?>> activators) {
        if (rawActivators == null) {
            return false;
        }

        // Let our super class init the "common" activators.
        boolean debugMode = initCommonActivators(rawActivators, activators);

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
            activators.add(resourceTypeActivator);
        }

        return debugMode;
    }

    public static boolean initResourceActivators(ResourceActivatorsType rawActivators, List<Activator<?>> activators) {
        if (rawActivators == null) {
            return false;
        }

        // Let our super class init the "common" activators.
        boolean debugMode = initCommonActivators(rawActivators, activators);

        List<FacetActivatorType> rawFacetActivators = rawActivators.getFacet();
        for (FacetActivatorType rawFacetActivator : rawFacetActivators) {
            String rawName = rawFacetActivator.getName().toString();
            FacetActivator facetActivator = new FacetActivator(ResourceTypeFacet
                .valueOf(rawName.toUpperCase(Locale.US)));
            activators.add(facetActivator);
        }

        List<ResourcePermissionActivatorType> rawResourcePermissionActivators = rawActivators.getResourcePermission();
        for (ResourcePermissionActivatorType rawResourcePermissionActivator : rawResourcePermissionActivators) {
            String rawName = rawResourcePermissionActivator.getName().toString();
            Permission permission = Permission.valueOf(rawName.toUpperCase(Locale.US));
            ResourcePermissionActivator resourcePermissionActivator = new ResourcePermissionActivator(permission);
            activators.add(resourcePermissionActivator);
        }

        List<TraitActivatorType> rawTraitActivators = rawActivators.getTrait();
        for (TraitActivatorType rawTraitActivator : rawTraitActivators) {
            String name = rawTraitActivator.getName();
            String value = rawTraitActivator.getValue();
            TraitActivator traitActivator = new TraitActivator(name, Pattern.compile(value));
            activators.add(traitActivator);
        }

        List<InventoryActivatorType> rawInventoryActivators = rawActivators.getResourceType();
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
            ResourceTypeActivator resourceTypeActivator = new ResourceTypeActivator(resourceConditionSets);
            activators.add(resourceTypeActivator);
        }

        return debugMode;
    }

}
