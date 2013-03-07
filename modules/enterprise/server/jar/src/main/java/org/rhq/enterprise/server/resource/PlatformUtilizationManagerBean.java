/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.resource;

import static java.util.Arrays.asList;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static org.rhq.core.domain.resource.InventoryStatus.COMMITTED;
import static org.rhq.core.domain.resource.ResourceCategory.PLATFORM;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary.CPUMetric;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary.MemoryMetric;
import org.rhq.core.domain.resource.composite.PlatformMetricsSummary.SwapMetric;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

/**
 * @author jsanda
 */
@Stateless
public class PlatformUtilizationManagerBean implements PlatformUtilizationManagerLocal {

    @EJB
    private ResourceTypeManagerLocal resourceTypeMgr;

    @EJB
    private ResourceManagerLocal resourceMgr;

    @EJB
    private MeasurementDataManagerLocal measurementDataMgr;

    @EJB
    private PlatformUtilizationManagerLocal platformUtilizationMgr;

    @Override
    public PageList<PlatformMetricsSummary> loadPlatformMetrics(final Subject subject) {
        final ResourceTypeCriteria typeCriteria = new ResourceTypeCriteria();
        typeCriteria.addFilterCategory(PLATFORM);
        typeCriteria.fetchMetricDefinitions(true);

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<ResourceType, ResourceTypeCriteria> queryExecutor = new CriteriaQueryExecutor<ResourceType, ResourceTypeCriteria>() {
            @Override
            public PageList<ResourceType> execute(ResourceTypeCriteria criteria) {
                return resourceTypeMgr.findResourceTypesByCriteria(subject, typeCriteria);
            }
        };

        CriteriaQuery<ResourceType, ResourceTypeCriteria> resourceTypes = new CriteriaQuery<ResourceType, ResourceTypeCriteria>(
            typeCriteria, queryExecutor);

        Map<Integer, Set<Integer>> platformMetricDefs = new HashMap<Integer, Set<Integer>>();
        for (ResourceType resourceType : resourceTypes) {
            platformMetricDefs.put(resourceType.getId(), getPlatformMetricDefIds(resourceType));
        }

        final ResourceCriteria resourceCriteria = new ResourceCriteria();
        resourceCriteria.addFilterResourceCategories(PLATFORM);
        resourceCriteria.addFilterInventoryStatus(COMMITTED);

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<Resource, ResourceCriteria> resourceQueryExecutor = new CriteriaQueryExecutor<Resource, ResourceCriteria>() {
            @Override
            public PageList<Resource> execute(ResourceCriteria criteria) {
                return resourceMgr.findResourcesByCriteria(subject, resourceCriteria);
            }
        };

        CriteriaQuery<Resource, ResourceCriteria> platforms = new CriteriaQuery<Resource, ResourceCriteria>(
            resourceCriteria, resourceQueryExecutor);

        PageList<PlatformMetricsSummary> summaries = new PageList<PlatformMetricsSummary>();

        for (Resource platform : platforms) {
            Set<Integer> metricDefIds = platformMetricDefs.get(platform.getResourceType().getId());
            try {
                Set<MeasurementData> measurementDataSet = platformUtilizationMgr.loadLiveMetricsForPlatform(subject,
                    platform, metricDefIds);
                summaries.add(createSummary(platform, measurementDataSet));
            } catch (RuntimeException e) {
                PlatformMetricsSummary summary = new PlatformMetricsSummary();
                summary.setResource(platform);
                summary.setMetricsAvailable(false);
                summaries.add(summary);
            }
        }

        return summaries;
    }

    @Override
    @TransactionAttribute(REQUIRES_NEW)
    public Set<MeasurementData> loadLiveMetricsForPlatform(Subject subject, Resource platform,
        Set<Integer> metricDefinitionIds) {
        return measurementDataMgr.findLiveData(subject, platform.getId(), ArrayUtils.unwrapArray(
            metricDefinitionIds.toArray(new Integer[metricDefinitionIds.size()])));
    }

    private Set<Integer> getPlatformMetricDefIds(ResourceType resourceType) {
        Set<Integer> metricDefIds = new TreeSet<Integer>();
        List<String> metricDefNames = asList(MemoryMetric.Used.getProperty(), MemoryMetric.ActualUsed.getProperty(),
            MemoryMetric.Free.getProperty(), MemoryMetric.ActualFree.getProperty(), MemoryMetric.Total.getProperty(),
            CPUMetric.Idle.getProperty(), CPUMetric.System.getProperty(), CPUMetric.User.getProperty(),
            SwapMetric.Free.getProperty(), SwapMetric.Used.getProperty(), SwapMetric.Total.getProperty());

        for (String metricDefName : metricDefNames) {
            Integer metricDefId = findMetricDefId(resourceType.getMetricDefinitions(), metricDefName);
            if (metricDefId != null) {
                metricDefIds.add(metricDefId);
            }
        }

        return metricDefIds;
    }

    private Integer findMetricDefId(Set<MeasurementDefinition> measurementDefs, String name) {
        for (MeasurementDefinition definition : measurementDefs) {
            if (name.equals(definition.getName())) {
                return definition.getId();
            }
        }
        return null;
    }

    private PlatformMetricsSummary createSummary(Resource resource, Set<MeasurementData> measurementDataSet) {
        PlatformMetricsSummary summary = new PlatformMetricsSummary();
        summary.setResource(resource);

        summary.setIdleCPU(findMeasurementData(measurementDataSet, CPUMetric.Idle.getProperty()));
        summary.setSystemCPU(findMeasurementData(measurementDataSet, CPUMetric.System.getProperty()));
        summary.setUserCPU(findMeasurementData(measurementDataSet, CPUMetric.User.getProperty()));

        summary.setFreeMemory(findMeasurementData(measurementDataSet, MemoryMetric.Free.getProperty()));
        summary.setActualFreeMemory(findMeasurementData(measurementDataSet, MemoryMetric.ActualFree.getProperty()));
        summary.setUsedMemory(findMeasurementData(measurementDataSet, MemoryMetric.Used.getProperty()));
        summary.setActualUsedMemory(findMeasurementData(measurementDataSet, MemoryMetric.ActualUsed.getProperty()));
        summary.setTotalMemory(findMeasurementData(measurementDataSet, MemoryMetric.Total.getProperty()));

        summary.setFreeSwap(findMeasurementData(measurementDataSet, SwapMetric.Free.getProperty()));
        summary.setTotalSwap(findMeasurementData(measurementDataSet, SwapMetric.Total.getProperty()));
        summary.setUsedSwap(findMeasurementData(measurementDataSet, SwapMetric.Used.getProperty()));

        return summary;
    }

    private MeasurementData findMeasurementData(Set<MeasurementData> measurementDataSet, String name) {
        for (MeasurementData measurementData : measurementDataSet) {
            if (name.equals(measurementData.getName())) {
                return measurementData;
            }
        }
        return null;
    }
}
