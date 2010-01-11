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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

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

}
