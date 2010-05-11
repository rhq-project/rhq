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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourceNamesDisambiguationResult;
import org.rhq.core.util.IntExtractor;

/**
 * This is basically a helper class that provides the disambiguation method.
 * It is intended to be used in an SLSB context.
 * 
 * @author Lukas Krejci
 */
public class Disambiguator {

    public static final int MAXIMUM_DISAMBIGUATED_TREE_DEPTH = 7;

    private Disambiguator() {

    }

    /**
     * Given a list of results, this method produces an object decorates the provided original results
     * with data needed to disambiguate the results with respect to resource names, their types and ancestory.
     * <p>
     * The disambiguation result contains information on what types of information are needed to make the resources
     * in the original result unambiguous and contains the decorated original data in the same order as the 
     * supplied result list.
     * <p>
     * The objects in results do not necessarily need to correspond to a resource. In case of such objects,
     * the resourceIdExtractor should return 0. In the resulting report such objects will still be wrapped
     * in a {@link DisambiguationReport} but the parent list will be empty and resource type and plugin name will
     * be null.
     *  
     * @see ResourceNamesDisambiguationResult
     * 
     * @param <T> the type of the result elements
     * @param results the results to disambiguate
     * @param disambiguationUpdateStrategy how is the disambiguation info going to be applied to the results.
     * @param resourceIdExtractor an object able to extract resource id from an instance of type parameter.
     * @param entityManager an entityManager to be used to access the database
     * @return the disambiguation result or null on error
     */
    public static <T> ResourceNamesDisambiguationResult<T> disambiguate(List<T> results,
        DisambiguationUpdateStrategy disambiguationUpdateStrategy, IntExtractor<? super T> extractor,
        EntityManager entityManager) {

        if (results.isEmpty()) {
            return new ResourceNamesDisambiguationResult<T>(new ArrayList<DisambiguationReport<T>>(), false, false,
                false);
        }

        //this is obsolete
        boolean typeResolutionNeeded = true;
        boolean pluginResolutionNeeded = true;
        boolean parentResolutionNeeded = true;

        //we can't assume the ordering of the provided results and the disambiguation query results
        //will be the same.

        //this list contains the resulting reports in the same order as the original results
        List<MutableDisambiguationReport<T>> reports = new ArrayList<MutableDisambiguationReport<T>>(results.size());

        //this maps the reports to resourceIds. More than one report can correspond to a single
        //resource id. The reports in this map are the same instances as in the reports list.
        Map<Integer, List<MutableDisambiguationReport<T>>> reportsByResourceId = new HashMap<Integer, List<MutableDisambiguationReport<T>>>();
        for (T r : results) {
            int resourceId = extractor.extract(r);
            MutableDisambiguationReport<T> value = new MutableDisambiguationReport<T>();
            value.original = r;
            if (resourceId > 0) {
                List<MutableDisambiguationReport<T>> correspondingResults = reportsByResourceId.get(resourceId);
                if (correspondingResults == null) {
                    correspondingResults = new ArrayList<MutableDisambiguationReport<T>>();
                    reportsByResourceId.put(resourceId, correspondingResults);
                }
                correspondingResults.add(value);
            }
            reports.add(value);
        }

        //check that we still have something to disambiguate
        if (reportsByResourceId.size() > 0) {
            //k, now let's construct the JPQL query to get the parents and type infos...
            StringBuilder selectBuilder = new StringBuilder(
                "SELECT r0.id, r0.name, r0.resourceType.id, r0.resourceType.name, r0.resourceType.plugin, r0.resourceType.singleton");
            StringBuilder fromBuilder = new StringBuilder("FROM Resource r0");

            for (int i = 1; i <= MAXIMUM_DISAMBIGUATED_TREE_DEPTH; ++i) {
                int pi = i - 1;
                selectBuilder.append(", r").append(i).append(".id");
                selectBuilder.append(", r").append(i).append(".name");
                selectBuilder.append(", rt").append(i).append(".id");
                selectBuilder.append(", rt").append(i).append(".name");
                selectBuilder.append(", rt").append(i).append(".plugin");
                selectBuilder.append(", rt").append(i).append(".singleton");

                fromBuilder.append(" left join r").append(pi).append(".parentResource r").append(i);
                fromBuilder.append(" left join r").append(i).append(".resourceType rt").append(i);
            }

            fromBuilder.append(" WHERE r0.id IN (:resourceIds)");

            Query parentsQuery = entityManager.createQuery(selectBuilder.append(" ").append(fromBuilder).toString());

            parentsQuery.setParameter("resourceIds", reportsByResourceId.keySet());

            //ok, now I will obtain all the information about the parents and types
            //using the above defined JPQL query.
            //I will partition the resulting reports by resource name.to create groups of
            //resources that are "mutually ambiguous". Because each such group potenitally
            //requires different level of disambiguation, I will then process them individually.

            ReportPartitions<T> partitionedReports = new ReportPartitions<T>(DisambiguationPolicy
                .getUniqueNamePolicy(disambiguationUpdateStrategy));

            @SuppressWarnings("unchecked")
            List<Object[]> parentsResults = (List<Object[]>) parentsQuery.getResultList();
            for (Object[] parentsResult : parentsResults) {
                List<MutableDisambiguationReport.Resource> parents = new ArrayList<MutableDisambiguationReport.Resource>(
                    MAXIMUM_DISAMBIGUATED_TREE_DEPTH);
                Integer resourceId = (Integer) parentsResult[0];
                String resourceName = (String) parentsResult[1];
                Integer typeId = (Integer) parentsResult[2];
                String typeName = (String) parentsResult[3];
                String pluginName = (String) parentsResult[4];
                Boolean singleton = (Boolean) parentsResult[5];

                MutableDisambiguationReport.ResourceType resourceType = new MutableDisambiguationReport.ResourceType();
                resourceType.id = typeId;
                resourceType.name = typeName;
                resourceType.plugin = pluginName;
                resourceType.singleton = singleton;

                MutableDisambiguationReport.Resource resource = new MutableDisambiguationReport.Resource();
                resource.id = resourceId;
                resource.name = resourceName;
                resource.resourceType = resourceType;

                for (int i = 0; i < MAXIMUM_DISAMBIGUATED_TREE_DEPTH; ++i) {
                    Integer parentId = (Integer) parentsResult[6 + 6 * i];
                    if (parentId == null)
                        break;
                    String parentName = (String) parentsResult[6 + 6 * i + 1];
                    Integer parentTypeId = (Integer) parentsResult[6 + 6 * i + 2];
                    String parentType = (String) parentsResult[6 + 6 * i + 3];
                    String parentPlugin = (String) parentsResult[6 + 6 * i + 4];
                    Boolean parentSingleton = (Boolean) parentsResult[6 + 6 * i + 5];

                    MutableDisambiguationReport.ResourceType type = new MutableDisambiguationReport.ResourceType();
                    type.id = parentTypeId;
                    type.name = parentType;
                    type.plugin = parentPlugin;
                    type.singleton = parentSingleton;

                    MutableDisambiguationReport.Resource parent = new MutableDisambiguationReport.Resource();
                    parent.id = parentId;
                    parent.name = parentName;
                    parent.resourceType = type;

                    parents.add(parent);
                }

                //update all the reports that correspond to this resourceId
                for (MutableDisambiguationReport<T> report : reportsByResourceId.get(resourceId)) {
                    report.resource = resource;
                    report.parents = parents;

                    partitionedReports.put(report);
                }
            }

            //ok, now I have the reports partitioned by resource name. let's go through each partition
            //and figure out the disambiguation needed for it.

            List<ReportPartitions<T>> ambiguousSubPartitions = new ArrayList<ReportPartitions<T>>();

            if (!partitionedReports.isPartitionsUnique()) {
                ambiguousSubPartitions.add(partitionedReports);
            } else {
                repartitionUnique(partitionedReports, disambiguationUpdateStrategy, ambiguousSubPartitions);                
            }

            while (ambiguousSubPartitions.size() > 0) {
                Iterator<ReportPartitions<T>> subPartitionIterator = ambiguousSubPartitions.iterator();
                List<ReportPartitions<T>> newAmbiguousPartitions = new ArrayList<ReportPartitions<T>>();

                while (subPartitionIterator.hasNext()) {
                    ReportPartitions<T> subPartition = subPartitionIterator.next();

                    repartitionUnique(subPartition, disambiguationUpdateStrategy, newAmbiguousPartitions);                

                    for (List<MutableDisambiguationReport<T>> partitionReports : subPartition.getAmbiguousPartitions()) {
                        ReportPartitions<T> replacementSubpartition = new ReportPartitions<T>(subPartition
                            .getDisambiguationPolicy().getNext());
                        replacementSubpartition.putAll(partitionReports);
                        if (!replacementSubpartition.isPartitionsUnique()) {
                            newAmbiguousPartitions.add(replacementSubpartition);
                        } else {
                            repartitionUnique(replacementSubpartition, disambiguationUpdateStrategy, newAmbiguousPartitions);            
                        }
                    }
                    subPartitionIterator.remove();
                }

                for (ReportPartitions<T> newPartition : newAmbiguousPartitions) {
                    ambiguousSubPartitions.add(newPartition);
                }
            }
        }

        List<DisambiguationReport<T>> resolution = new ArrayList<DisambiguationReport<T>>(results.size());

        for (MutableDisambiguationReport<T> report : reports) {
            resolution.add(report.getReport());
        }

        return new ResourceNamesDisambiguationResult<T>(resolution, typeResolutionNeeded, parentResolutionNeeded,
            pluginResolutionNeeded);
    }

    private static <T> void repartitionUnique(ReportPartitions<T> partitions, DisambiguationUpdateStrategy updateStrategy, List<ReportPartitions<T>> ambigousPartitions) {

        while (true) {
            //try to repartition
            DisambiguationPolicy repartitionPolicy = partitions.getDisambiguationPolicy().getNextRepartitioningPolicy();
            if (repartitionPolicy != null) {
                //ok, we have a new policy to try... let's see if it makes any difference.
                partitions = new ReportPartitions<T>(repartitionPolicy, partitions.getUniquePartitions());

                //bail out if we have partitions that are not unique
                if (!partitions.isPartitionsUnique()) {
                    ambigousPartitions.add(partitions);
                    return;
                }
            } else {
                //ok, there is no other repartitioning policy that we can try. 
                //Let's update the reports in the unique partitions...
                for (List<MutableDisambiguationReport<T>> partition : partitions.getUniquePartitions()) {
                    for (MutableDisambiguationReport<T> report : partition) {
                        updateStrategy.update(partitions.getDisambiguationPolicy(), report);
                    }
                }
                
                return;
            }
        }
    }
}
