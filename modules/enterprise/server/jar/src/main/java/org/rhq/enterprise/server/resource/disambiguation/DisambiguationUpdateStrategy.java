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

/**
 * Defines a strategy for updating a results list with the disambiguation information.
 * <p>
 * See {@link DefaultDisambiguationUpdateStrategies} for a couple of implemented strategies.
 * 
 * @see DefaultDisambiguationUpdateStrategies
 * 
 * @author Lukas Krejci
 */
public interface DisambiguationUpdateStrategy {

    /**
     * Updates the report using the policy. It is guaranteed that the resource and its parents
     * in the report are already processed using the 
     * {@link ResourceResolution#update(MutableDisambiguationReport.Resource)}
     * method. This method is then called to ensure that the report as a whole conforms to the policy *and* this
     * strategy. This might entail removing some elements from the parent list for example.
     * 
     * @param <T>
     * @param policy
     * @param report
     */
    <T> void update(DisambiguationPolicy policy, MutableDisambiguationReport<T> report);

    /**
     * @return a set of resolutions for which the unique reports need to be repartitioned at the resource level.
     * In another words this forces the disambiguation to continue on up the disambiguation chain even if the 
     * it disambiguates the resuts successfully at the resource level.
     */
    EnumSet<ResourceResolution> resourceLevelRepartitionableResolutions();

    /**
     * @return a set of resolutions for which uniquely disambiguated reports are to be repartitioned further.
     * The resolutions from this set apply on the parents (on any level), unlike the resolutions from {@link #resourceLevelRepartitionableResolutions()}.
     */
    EnumSet<ResourceResolution> alwaysRepartitionableResolutions();

    /**
     * The disambiguation procedure calls this method for every not yet fully unique
     * partitions set when it determines that further disambiguation should be performed.
     * <p>
     * But this is not always necessary, depending on the update strategy implementation.
     * <p>
     * This method is therefore the means for the update strategy to short-circuit the disambiguation
     * procedure when it determines that further disambiguation would not make sense for this strategy.
     * 
     * @param <T>
     * @param partitions the partitions object holding a subset of the results being disambiguated along
     * with the policy that is used to disambiguate them
     * @return true if further disambiguation is useful, false otherwise
     */
    <T> boolean partitionFurther(ReportPartitions<T> partitions);
}
