/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.helpers.rtfilter.subsystem.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.spec.FilterMappingMetaData;
import org.jboss.metadata.web.spec.FilterMetaData;
import org.jboss.metadata.web.spec.FiltersMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

import org.rhq.helpers.rtfilter.filter.RtFilter;
import org.rhq.helpers.rtfilter.subsystem.RtFilterExtension;

/**
 * A deployment unit processor that configures the RHQ RT filter on each WAR that is deployed.
 *
 * @author Ian Springer
 */
public class RtFilterDeploymentUnitProcessor implements DeploymentUnitProcessor {

    private static final String RT_FILTER_NAME = "RHQ Response-Time Filter";
    private final Logger log = Logger.getLogger(RtFilterDeploymentUnitProcessor.class);

    /**
     * See {@link Phase} for descriptions of the different phases.
     */
    public static final Phase PHASE = Phase.DEPENDENCIES;

    /**
     * The relative order of this processor within the {@link #PHASE}.
     * The current number is large enough for it to happen after all
     * the standard deployment unit processors that come with JBoss AS.
     */
    public static final int PRIORITY = 0x4000;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isWar(deploymentUnit)) {
            log.debug("Configuring RHQ response-time servlet filter for WAR " + deploymentUnit.getName() + "...");

            final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
            WebMetaData webMetaData = warMetaData.getSharedWebMetaData();

            FilterMetaData rtFilter = null;
            FiltersMetaData filters = webMetaData.getFilters();
            if (filters != null) {
                for (FilterMetaData filter : filters) {
                    if (RtFilter.class.getName().equals(filter.getFilterClass())) {
                        // the filter's already configured in this webapp.
                        rtFilter = filter;
                        break;
                    }
                }
            } else {
                filters = new FiltersMetaData();
                webMetaData.setFilters(filters);
            }
            if (rtFilter == null) {
                rtFilter = new FilterMetaData();
                rtFilter.setFilterName(RT_FILTER_NAME);
                rtFilter.setFilterClass(RtFilter.class.getName());
                filters.add(rtFilter);
            }

            List<ParamValueMetaData> initParams = rtFilter.getInitParam();
            if (initParams == null) {
                initParams = new ArrayList<ParamValueMetaData>();
                rtFilter.setInitParam(initParams);
            }
            for (String paramName : RtFilterExtension.INIT_PARAMS.keySet()) {
                ParamValueMetaData initParam = new ParamValueMetaData();
                initParam.setParamName(paramName);
                String paramValue = RtFilterExtension.INIT_PARAMS.get(paramName);
                initParam.setParamValue(paramValue);
                initParams.add(initParam);
            }

            boolean filterMappingAlreadyConfigured = false;
            List<FilterMappingMetaData> filterMappings = webMetaData.getFilterMappings();
            if (filterMappings != null) {
                for (FilterMappingMetaData filterMapping : filterMappings) {
                    if (filterMapping.getFilterName().equals(rtFilter.getFilterName())) {
                        // a mapping for the filter's already configured in this webapp.
                        filterMappingAlreadyConfigured = true;
                        break;
                    }
                }
            } else {
                filterMappings = new ArrayList<FilterMappingMetaData>();
                webMetaData.setFilterMappings(filterMappings);
            }

            if (!filterMappingAlreadyConfigured) {
                FilterMappingMetaData filterMapping = new FilterMappingMetaData();
                filterMapping.setFilterName(rtFilter.getFilterName());
                filterMapping.setUrlPatterns(Arrays.asList("/*"));
                filterMappings.add(filterMapping);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        return;
    }

    private static boolean isWar(final DeploymentUnit deploymentUnit) {
        return (DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit));
    }

}
