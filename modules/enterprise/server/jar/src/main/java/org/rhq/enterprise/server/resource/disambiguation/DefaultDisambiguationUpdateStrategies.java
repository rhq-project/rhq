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

import java.util.EnumSet;

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
            updateResources(policy, report);
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
    },

    /**
     * Even if the disambiguation policy determined that parents are not needed to disambiguate the 
     * results, at least one of them is kept in the report.
     */
    KEEP_AT_LEAST_ONE_PARENT {
        public <T> void update(DisambiguationPolicy policy, MutableDisambiguationReport<T> report) {
            updateResources(policy, report);
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
    },

    /**
     * The parentage of the report is retained at least up to the server/service directly under platform.
     * If the policy needs the platform to stay, it is of course preserved.
     */
    KEEP_PARENTS_TO_TOPMOST_SERVERS {
        public <T> void update(DisambiguationPolicy policy, MutableDisambiguationReport<T> report) {
            updateResources(policy, report);
            //only remove the platform, if the policy doesn't dictate its presence...
            if (policy.size() > 1 && report.parents.size() > policy.size() - 1) {
                report.parents.remove(report.parents.size() - 1);
            }
        }

        @Override
        public EnumSet<ResourceResolution> resourceLevelRepartitionableResolutions() {
            return EnumSet.allOf(ResourceResolution.class);
        }
    },

    /**
     * All parents are preserved no matter what the policy says.
     */
    KEEP_ALL_PARENTS {
        public <T> void update(DisambiguationPolicy policy, MutableDisambiguationReport<T> report) {
            updateResources(policy, report);
            //do nothing to the parents, keep them as they are...
        }
        
        public EnumSet<ResourceResolution> resourceLevelRepartitionableResolutions() {
            return EnumSet.of(ResourceResolution.NAME);
        }

        public EnumSet<ResourceResolution> alwaysRepartitionableResolutions() {
            return EnumSet.of(ResourceResolution.NAME);
        }
    };

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
     */
    public static <T> void updateResources(DisambiguationPolicy policy, MutableDisambiguationReport<T> report) {
        updateResource(policy.get(0), report.resource);
        
        int disambiguationPolicyIndex = 1;
        while (disambiguationPolicyIndex < policy.size() && disambiguationPolicyIndex - 1 < report.parents.size()) {
            ResourceResolution parentResolution = policy.get(disambiguationPolicyIndex);
            MutableDisambiguationReport.Resource parent = report.parents.get(disambiguationPolicyIndex - 1);
            updateResource(parentResolution, parent);

            disambiguationPolicyIndex++;
        }

        disambiguationPolicyIndex--;

        //because the parents update strategy might leave more parents than this policy requires for disambiguation
        //we need to treat those parents as well. Because they are not needed for disambiguation, treat them as
        //if only the name and type was needed for them.
        for (; disambiguationPolicyIndex < report.parents.size(); ++disambiguationPolicyIndex) {
            updateResource(ResourceResolution.TYPE, report.parents.get(disambiguationPolicyIndex));
        }

        //include the plugin information only if it was actually the deciding resolution of the policy.
        if (policy.get(policy.size() - 1) == ResourceResolution.PLUGIN) {
            String decidingPlugin = policy.size() > 1 ? report.parents.get(policy.size() - 2).resourceType.plugin : report.resource.resourceType.plugin;
            
            for (int i = policy.size() - 3; i >= 0; i--) {
                if (decidingPlugin.equals(report.parents.get(i).resourceType.plugin)) {
                    updateResource(ResourceResolution.TYPE, report.parents.get(i));
                }
            }
            
            if (policy.size() > 1 && decidingPlugin.equals(report.resource.resourceType.plugin)) {
                updateResource(ResourceResolution.TYPE, report.resource);
            }
        } else {
            //ok, this report was in the end resolved by some name or type at the resource level
            //or higher up in the parents. This alone uniquely identifies the resource, so there's
            //no need to show the (ambigous anyway) plugin info.
            updateResource(ResourceResolution.TYPE, report.resource);
            
            for (MutableDisambiguationReport.Resource parent : report.parents) {
                updateResource(ResourceResolution.TYPE, parent);
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


    private static void updateResource(ResourceResolution resolution, Resource resource) {
        switch (resolution) {
        case NAME: case TYPE:
            resource.resourceType.plugin = null;
        }
    }    
    
    public static DefaultDisambiguationUpdateStrategies getDefault() {
        return DefaultDisambiguationUpdateStrategies.KEEP_ALL_PARENTS;
    }
}