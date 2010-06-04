/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.server.resource.disambiguation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.rhq.enterprise.server.resource.disambiguation.MutableDisambiguationReport.Resource;

/**
 * This enumerates different strategies that can be used to update the results to produce disambiguated list.
 * 
 * @author Lukas Krejci
 */
public enum DefaultDisambiguationUpdateStrategies implements DisambiguationUpdateStrategy {

    /**
     * The disambiguation policy is followed precisely.
     */
    EXACT {
        public <T> void update(DisambiguationPolicy policy, MutableDisambiguationReport<T> report) {
            updateResources(policy, report, true, true);
            int nofParents = policy.size() - 1;
            if (nofParents < 0)
                nofParents = 0;

            if (nofParents == 0) {
                report.parents.clear();
            } else {
                while (report.parents.size() > nofParents) {
                    report.parents.remove(report.parents.size() - 1);
                }
            }
        }
        
        public <T> boolean partitionFurther(ReportPartitions<T> partitions) {
            return true;
        }
    },

    /**
     * Even if the disambiguation policy determined that parents are not needed to disambiguate the 
     * results, at least one of them is kept in the report.
     */
    KEEP_AT_LEAST_ONE_PARENT {
        public <T> void update(DisambiguationPolicy policy, MutableDisambiguationReport<T> report) {
            updateResources(policy, report, true, true);
            int nofParents = policy.size() - 1;
            if (nofParents < 1)
                nofParents = 1;

            while (report.parents.size() > nofParents) {
                report.parents.remove(report.parents.size() - 1);
            }
        }

        @Override
        public EnumSet<ResourceResolution> resourceLevelRepartitionableResolutions() {
            return EnumSet.allOf(ResourceResolution.class);
        }
        
        public <T> boolean partitionFurther(ReportPartitions<T> partitions) {
            return true;
        }
    },

    /**
     * The parentage of the report is retained at least up to the server/service directly under platform.
     * If the policy needs the platform to stay, it is of course preserved.
     */
    KEEP_PARENTS_TO_TOPMOST_SERVERS {
        public <T> void update(DisambiguationPolicy policy, MutableDisambiguationReport<T> report) {
            updateResources(policy, report, true, true);
            //only remove the platform, if the policy doesn't dictate its presence...
            if (policy.size() > 1 && report.parents.size() > policy.size() - 1) {
                report.parents.remove(report.parents.size() - 1);
            }
        }

        @Override
        public EnumSet<ResourceResolution> resourceLevelRepartitionableResolutions() {
            return EnumSet.allOf(ResourceResolution.class);
        }
        
        public <T> boolean partitionFurther(ReportPartitions<T> partitions) {
            return partitions.getDisambiguationPolicy().size() < Disambiguator.MAXIMUM_DISAMBIGUATED_TREE_DEPTH - 1;
        }
    },

    /**
     * All parents are preserved no matter what the policy says.
     */
    KEEP_ALL_PARENTS {
        public <T> void update(DisambiguationPolicy policy, MutableDisambiguationReport<T> report) {
            updateResources(policy, report, true, true);
            //do nothing to the parents, keep them as they are...
        }
        
        public EnumSet<ResourceResolution> resourceLevelRepartitionableResolutions() {
            return EnumSet.of(ResourceResolution.NAME);
        }

        public EnumSet<ResourceResolution> alwaysRepartitionableResolutions() {
            return EnumSet.of(ResourceResolution.NAME);
        }
        
        public <T> boolean partitionFurther(ReportPartitions<T> partitions) {
            //we always keep all the info about the resources in this strategy
            //so there's no real need to disambiguate anything.
            return false;
        }
    };

    private static final DisambiguationPolicy.Level overridingResolution = new DisambiguationPolicy.Level(ResourceResolution.TYPE);
    
    /**
     * This updates the resources in the report according to the resolutions contained in the policy.
     * This method is called as part of the {@link DisambiguationUpdateStrategy#update(DisambiguationPolicy, MutableDisambiguationReport)}
     * implementations in this enum before the individual enums modify the parent list as they see fit.
     * This method is left public because it is generic enough to be reused by other potential implementations of the
     * {@link DisambiguationUpdateStrategy} interface.
     * 
     * @param <T>
     * @param policy
     * @param report
     * @param honorAmbiguousTypeNamesList whether to honor the list of ambiguous type names as listed in the policy when updating the resources.
     * @param pushDownPluginInfo if true, the plugin information is pushed down as low in the resource hierarchy as possible. This means that if
     * some parent needs plugin disambiguation or is of an ambiguous type and the resource comes from the same plugin, the plugin info is preserved
     * on the resource rather than on the parent. This is mainly useful for the display purposes, because it just
     * looks nicer to have that info at a resource than somewhere in the location string.
     */
    public static <T> void updateResources(DisambiguationPolicy policy, MutableDisambiguationReport<T> report, boolean honorAmbiguousTypeNamesList, boolean pushDownPluginInfo) {
        List<String> ambiguousTypeNames = honorAmbiguousTypeNamesList ? policy.getAmbiguousTypeNames() : Collections.<String>emptyList();
        
        String resourcePlugin = report.resource.resourceType.plugin;
        List<String> parentPlugins = new ArrayList<String>(report.parents.size());
        updateResource(policy.get(0), report.resource, ambiguousTypeNames);
        
        int disambiguationPolicyIndex = 1;
        while (disambiguationPolicyIndex < policy.size() && disambiguationPolicyIndex - 1 < report.parents.size()) {
            DisambiguationPolicy.Level resolutionLevel = policy.get(disambiguationPolicyIndex);
            MutableDisambiguationReport.Resource parent = report.parents.get(disambiguationPolicyIndex - 1);
            
            if (pushDownPluginInfo) {
                parentPlugins.add(parent.resourceType.plugin);
            }
            
            updateResource(resolutionLevel, parent, ambiguousTypeNames);

            disambiguationPolicyIndex++;
        }

        disambiguationPolicyIndex--;

        //because the parents update strategy might leave more parents than this policy requires for disambiguation
        //we need to treat those parents as well. Because they are not needed for disambiguation, treat them as
        //if only the name and type was needed for them.
        for (; disambiguationPolicyIndex < report.parents.size(); ++disambiguationPolicyIndex) {
            if (pushDownPluginInfo) {
                parentPlugins.add(report.parents.get(disambiguationPolicyIndex).resourceType.plugin);
            }
            updateResource(overridingResolution, report.parents.get(disambiguationPolicyIndex), ambiguousTypeNames);
        }

        if (pushDownPluginInfo) {
            for (int i = report.parents.size() - 1; i >= 0; --i) {
                String plugin = report.parents.get(i).resourceType.plugin;
                if (plugin != null && i > 0 && plugin.equals(parentPlugins.get(i - 1))) {
                    report.parents.get(i - 1).resourceType.plugin = plugin;
                    report.parents.get(i).resourceType.plugin = null;
                }
            }
            
            if (report.parents.size() > 0) {
                String plugin = report.parents.get(0).resourceType.plugin;
                if (plugin != null && plugin.equals(resourcePlugin)) {
                    report.resource.resourceType.plugin = resourcePlugin;
                    report.parents.get(0).resourceType.plugin = null;
                }
            }
        }
    }
    
    /**
     * @return a set of resolutions for which the unique reports need to be repartitioned at the resource level.
     * In another words this forces the disambiguation to continue on up the disambiguation chain even if the 
     * it disambiguates the resuts successfully at the resource level.
     */
    public EnumSet<ResourceResolution> resourceLevelRepartitionableResolutions() {
        return EnumSet.noneOf(ResourceResolution.class);
    }

    /**
     * @return a set of resolutions for which uniquely disambiguated reports are to be repartitioned further.
     * The resolutions from this set apply on the parents (on any level), unlike the resolutions from {@link #resourceLevelRepartitionableResolutions()}.
     */
    public EnumSet<ResourceResolution> alwaysRepartitionableResolutions() {
        return EnumSet.noneOf(ResourceResolution.class);
    }


    private static void updateResource(DisambiguationPolicy.Level resolutionLevel, Resource resource, List<String> ambiguousTypeNames) {
        switch (resolutionLevel.getResourceResolution()) {
        case NAME: case TYPE:
            if (!ambiguousTypeNames.contains(resource.resourceType.name)) {
                resource.resourceType.plugin = null;
            }
            break;
        case PLUGIN:
            if (!(resolutionLevel.isDeciding() || ambiguousTypeNames.contains(resource.resourceType.name))) {
                resource.resourceType.plugin = null;
            }
        }
    }    
    
    public static DefaultDisambiguationUpdateStrategies getDefault() {
        return DefaultDisambiguationUpdateStrategies.KEEP_ALL_PARENTS;
    }
}