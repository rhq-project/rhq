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
import java.util.List;

/**
 * A disambiguation policy determines whether two disambiguation reports are still ambiguous or not.
 * <p>
 * The policy is basically a list of {@link ResourceResolution} instances each corresponding to either
 * the resource itself of some of its parent in the hierarchy. Each of the {@link ResourceResolution} instances
 * in the list determines how the appropriate part of the report is disambiguated.
 * 
 * @author Lukas Krejci
 */
public class DisambiguationPolicy extends ArrayList<DisambiguationPolicy.Level> {
    private static final long serialVersionUID = 1L;

    private DisambiguationUpdateStrategy parentsUpdateStrategy;
    private List<String> ambiguousTypeNames;
    
    public static class Level {
        private ResourceResolution resourceResolution;
        private boolean deciding;
        
        public Level(ResourceResolution resourceResolution) {
            this(resourceResolution, false);
        }
        
        public Level(ResourceResolution resourceResolution, boolean deciding) {
            this.resourceResolution = resourceResolution;
            this.deciding = deciding;
        }

        public boolean isDeciding() {
            return deciding;
        }

        public void setDeciding(boolean deciding) {
            this.deciding = deciding;
        }

        public ResourceResolution getResourceResolution() {
            return resourceResolution;
        }
        
        public String toString() {
            return "Level[" + resourceResolution + (deciding ? ", deciding]" : ", not deciding]");
        }
    }
    
    public DisambiguationPolicy(DisambiguationUpdateStrategy parentsDisambiguationStrategy, List<String> ambiguousTypeNames) {
        this.parentsUpdateStrategy = parentsDisambiguationStrategy;
        this.ambiguousTypeNames = ambiguousTypeNames;
    }

    public DisambiguationPolicy(DisambiguationPolicy other) {
        for(Level level : other) {
            this.add(new Level(level.resourceResolution, level.deciding));
        }
        this.parentsUpdateStrategy = other.parentsUpdateStrategy;
        this.ambiguousTypeNames = other.ambiguousTypeNames;
    }

    /**
     * Creates a "starting" disambiguation policy that is used to try and disambiguate
     * the resources by just their name.
     * 
     * @see #DisambiguationPolicy(DisambiguationUpdateStrategy, List)
     * @see #getAmbiguousTypeNames()
     * @param parentsDisambiguationStrategy
     * @param ambiguousTypeNames the list of ambiguous type names
     * @return
     */
    public static DisambiguationPolicy getUniqueNamePolicy(DisambiguationUpdateStrategy parentsDisambiguationStrategy, List<String> ambiguousTypeNames) {
        DisambiguationPolicy ret = new DisambiguationPolicy(parentsDisambiguationStrategy, ambiguousTypeNames);
        ret.add(new Level(ResourceResolution.NAME));

        return ret;
    }

    public DisambiguationUpdateStrategy getParentsUpdateStrategy() {
        return parentsUpdateStrategy;
    }

    public void setParentsUpdateStrategy(DisambiguationUpdateStrategy parentsUpdateStrategy) {
        this.parentsUpdateStrategy = parentsUpdateStrategy;
    }

    /**
     * @return the list of type names that are defined in multiple plugins. Such type names
     * should be disambiguated no matter if it is needed or not.
     */
    public List<String> getAmbiguousTypeNames() {
        return ambiguousTypeNames;
    }

    public void setAmbiguousTypeNames(List<String> ambiguousTypeNames) {
        this.ambiguousTypeNames = ambiguousTypeNames;
    }

    /**
     * Tells whether the last resolution in this policy determines the reports ambiguous.
     * 
     * @param <T> 
     * @param a 
     * @param b
     * @return
     */
    public <T> boolean areAmbiguous(MutableDisambiguationReport<T> a, MutableDisambiguationReport<T> b) {
        MutableDisambiguationReport.Resource ra = getComparingResource(a);
        MutableDisambiguationReport.Resource rb = getComparingResource(b);

        if (ra == null || rb == null)
            return false;

        Level level = getCurrentLevel();

        if (level != null) {
            return level.getResourceResolution().areAmbiguous(ra, rb);
        } else {
            return false;
        }
    }

    /**
     * @return the last element in this policy
     */
    public Level getCurrentLevel() {
        return size() > 0 ? get(size() - 1) : null;
    }

    /**
     * If the user of this class determines that this policy can't disambiguate some list of reports,
     * s/he can get the next policy to try by calling this method.
     * 
     * @return the next policy in "chain" of possible policies
     */
    public DisambiguationPolicy getNext() {
        int lastIdx = size() - 1;
        ResourceResolution lastResolution = get(lastIdx).getResourceResolution();

        DisambiguationPolicy ret = new DisambiguationPolicy(this);

        switch (lastResolution) {
        case NAME:
            ret.get(lastIdx).resourceResolution = ResourceResolution.TYPE;
            break;
        case TYPE:
            ret.get(lastIdx).resourceResolution = ResourceResolution.PLUGIN;
            break;
        case PLUGIN:
            ret.add(new Level(ResourceResolution.NAME));
            break;
        }

        return ret;
    }

    /**
     * @return a policy to be used to repartition unique reports using some other criteria or null
     * if the current policy doesn't require repartitioning of unique reports.
     */
    public DisambiguationPolicy getNextRepartitioningPolicy() {
        ResourceResolution currentResolution = getCurrentLevel().getResourceResolution();

        if (parentsUpdateStrategy.alwaysRepartitionableResolutions().contains(currentResolution) || (size() == 1
            && parentsUpdateStrategy.resourceLevelRepartitionableResolutions().contains(currentResolution))) {
            
            DisambiguationPolicy newPolicy = getNext();
            
            //this policy is used for reports uniquely identified by type, but we are repartitioning them further.
            //but this doesn't mean that they need to be repartitioned by plugin name because that would
            //only supplement the information that is already evident from the type partitioning.
            //therefore, skip the plugin resolution in that case.
            if (currentResolution == ResourceResolution.TYPE) {
                newPolicy = newPolicy.getNext();
                newPolicy.get(newPolicy.size() - 2).resourceResolution = ResourceResolution.TYPE;
            }
            
            return newPolicy;
        }
        
        return null;
    }
    
    private <T> MutableDisambiguationReport.Resource getComparingResource(MutableDisambiguationReport<T> report) {
        int size = size();
        if (size == 0)
            return null;

        if (size == 1) {
            return report.resource;
        } else {
            int parentIdx = size - 2;
            if (report.parents.size() > parentIdx) {
                return report.parents.get(parentIdx);
            }
        }
        return null;
    }
}