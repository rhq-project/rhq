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

/**
 * A disambiguation policy determines whether two disambiguation reports are still ambiguous or not.
 * <p>
 * The policy is basically a list of {@link ResourceResolution} instances each corresponding to either
 * the resource itself of some of its parent in the hierarchy. Each of the {@link ResourceResolution} instances
 * in the list determines how the appropriate part of the report is disambiguated.
 * <p>
 * Only last element in this list actually decides whether the two reports are still ambiguous. The thinking
 * behind this is that the previous resolutions in the chain are "decided" and the last one is the one
 * we are currently trying to determine the correct resolution for.
 * 
 * 
 * @author Lukas Krejci
 */
public class DisambiguationPolicy extends ArrayList<ResourceResolution> {
    private static final long serialVersionUID = 1L;

    private DisambiguationUpdateStrategy parentsUpdateStrategy;
    
    public DisambiguationPolicy(DisambiguationUpdateStrategy parentsDisambiguationStrategy) {
        this.parentsUpdateStrategy = parentsDisambiguationStrategy;
    }

    public DisambiguationPolicy(DisambiguationPolicy other) {
        super(other);
        this.parentsUpdateStrategy = other.parentsUpdateStrategy;
    }

    public static DisambiguationPolicy getUniqueNamePolicy(DisambiguationUpdateStrategy parentsDisambiguationStrategy) {
        DisambiguationPolicy ret = new DisambiguationPolicy(parentsDisambiguationStrategy);
        ret.add(ResourceResolution.NAME);

        return ret;
    }

    public DisambiguationUpdateStrategy getParentsUpdateStrategy() {
        return parentsUpdateStrategy;
    }

    public void setParentsUpdateStrategy(DisambiguationUpdateStrategy parentsUpdateStrategy) {
        this.parentsUpdateStrategy = parentsUpdateStrategy;
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

        ResourceResolution resolution = get(size() - 1);

        return resolution.areAmbiguous(ra, rb);
    }

    /**
     * @return the currently deciding {@link ResourceResolution} (i.e. the last element in this
     * policy)
     */
    public ResourceResolution getCurrentResourceResolution() {
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
        ResourceResolution lastResolution = get(lastIdx);

        DisambiguationPolicy ret = new DisambiguationPolicy(this);

        switch (lastResolution) {
        case NAME:
            ret.set(lastIdx, ResourceResolution.TYPE);
            break;
        case TYPE:
            ret.set(lastIdx, ResourceResolution.PLUGIN);
            break;
        case PLUGIN:
            ret.add(ResourceResolution.NAME);
            break;
        }

        return ret;
    }

    /**
     * @return a policy to be used to repartition unique reports using some other criteria or null
     * if the current policy doesn't require repartitioning of unique reports.
     */
    public DisambiguationPolicy getNextRepartitioningPolicy() {
        ResourceResolution currentResolution = get(size() - 1);

        if (parentsUpdateStrategy.alwaysRepartitionableResolutions().contains(currentResolution) || (size() == 1)
            && parentsUpdateStrategy.resourceLevelRepartitionableResolutions().contains(currentResolution)) {
            
            DisambiguationPolicy newPolicy = getNext();
            
            //this policy is used for reports uniquely identified by type, but we are repartitioning them further.
            //but this doesn't mean that they need to be repartitioned by plugin name because that would
            //only supplement the information that is already evident from the type partitioning.
            //therefore, skip the plugin resolution in that case.
            if (currentResolution == ResourceResolution.TYPE) {
                newPolicy = newPolicy.getNext();
                newPolicy.set(newPolicy.size() - 2, ResourceResolution.TYPE);
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