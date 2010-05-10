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
import java.util.List;
import java.util.Map;

/**
 * This class partitions the reports inserted into it by chunking them up 
 * into sublists containing mutually ambiguous reports. The ambiguity is determined using 
 * a {@link DisambiguationPolicy}.
 * 
 * @param <T> the type of the original instances the are being disambiguated.
 * 
 * @author Lukas Krejci
 */
public class ReportPartitions<T> {

    private static final long serialVersionUID = 1L;
    private DisambiguationPolicy disambiguationPolicy;
    private Map<Integer, List<MutableDisambiguationReport<T>>> partitions;

    /**
     * @see MutableDisambiguationReport#getComparisonForLevel(int)
     * 
     * @param comparisonLevel
     */
    public ReportPartitions(DisambiguationPolicy disambiguationPolicy) {
        this.disambiguationPolicy = disambiguationPolicy;
        partitions = new HashMap<Integer, List<MutableDisambiguationReport<T>>>();
    }

    public DisambiguationPolicy getDisambiguationPolicy() {
        return disambiguationPolicy;
    }

    public List<List<MutableDisambiguationReport<T>>> getAmbiguousPartitions() {
        List<List<MutableDisambiguationReport<T>>> ret = new ArrayList<List<MutableDisambiguationReport<T>>>();

        for (List<MutableDisambiguationReport<T>> partition : partitions.values()) {
            if (partition.size() > 1) {
                ret.add(partition);
            }
        }

        return ret;
    }

    public List<List<MutableDisambiguationReport<T>>> getUniquePartitions() {
        List<List<MutableDisambiguationReport<T>>> ret = new ArrayList<List<MutableDisambiguationReport<T>>>();

        for (List<MutableDisambiguationReport<T>> partition : partitions.values()) {
            if (partition.size() == 1) {
                ret.add(partition);
            }
        }

        return ret;
    }

    public void put(MutableDisambiguationReport<T> value) {
        boolean found = false;
        for (Map.Entry<Integer, List<MutableDisambiguationReport<T>>> entry : partitions.entrySet()) {
            for (MutableDisambiguationReport<T> partitionPrototype : entry.getValue()) {
                if (disambiguationPolicy.areAmbiguous(partitionPrototype, value)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                entry.getValue().add(value);
                break;
            }
        }

        if (!found) {
            List<MutableDisambiguationReport<T>> newPartition = new ArrayList<MutableDisambiguationReport<T>>();
            newPartition.add(value);
            partitions.put(partitions.size(), newPartition);
        }
    }

    public void putAll(List<MutableDisambiguationReport<T>> values) {
        for (MutableDisambiguationReport<T> v : values) {
            put(v);
        }
    }

    /**
     * Updates the reports in the unique partitions.
     * 
     * @return if the current policy is determined "repartitionable", the returned partitions instance splits the current
     * unique reports by some other policy. If the current policy is not repartitionable, a null value is returned.
     */
    public ReportPartitions<T> updateUniqueReports() {
        List<MutableDisambiguationReport<T>> uniqueReports = new ArrayList<MutableDisambiguationReport<T>>();
        for (List<MutableDisambiguationReport<T>> val : partitions.values()) {
            if (val.size() == 1) {
                uniqueReports.addAll(val);
            }
        }

        DisambiguationPolicy repartitioningPolicy = disambiguationPolicy.getNextRepartitioningPolicy();
        if (repartitioningPolicy != null) {
            ReportPartitions<T> ret = new ReportPartitions<T>(repartitioningPolicy);
            ret.putAll(uniqueReports);

            return ret;
        }

        for (MutableDisambiguationReport<T> report : uniqueReports) {
            disambiguationPolicy.update(report);            
        }

        return null;
    }

    public boolean isPartitionsUnique() {
        for (List<MutableDisambiguationReport<T>> partition : partitions.values()) {
            if (partition.size() > 1) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "ReportPartitions[policy=" + disambiguationPolicy + ", partitions=" + partitions + "]";
    }
}