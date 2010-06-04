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
import java.util.List;

/**
 * This class partitions the reports inserted into it by chunking them up 
 * into sublists containing mutually ambiguous reports. The ambiguity is determined using 
 * a {@link DisambiguationPolicy}.
 * <p>
 * The ReportPartition instances can form a tree using the {@link #getPreviousPartitioning()}
 * and the parameters provided to the constructors. This is handy to keep track of what
 * partitions have been disambiguated at what level and how.
 * 
 * @param <T> the type of the original instances the are being disambiguated.
 * 
 * @author Lukas Krejci
 */
public class ReportPartitions<T> {

    private static final long serialVersionUID = 1L;
    private DisambiguationPolicy disambiguationPolicy;
    private List<List<MutableDisambiguationReport<T>>> uniquePartitions;
    private List<List<MutableDisambiguationReport<T>>> ambiguousPartitions;
    
    /**
     * Constructs a new instance with no partitions in it.
     * 
     * @param disambiguationPolicy the policy that decides if reports are ambiguous.
     */
    public ReportPartitions(DisambiguationPolicy disambiguationPolicy) {
        this.disambiguationPolicy = disambiguationPolicy;
        uniquePartitions = new ArrayList<List<MutableDisambiguationReport<T>>>();
        ambiguousPartitions = new ArrayList<List<MutableDisambiguationReport<T>>>();
    }

    public ReportPartitions(DisambiguationPolicy disambiguationPolicy, ReportPartitions<T> other) {
        this(disambiguationPolicy);
        putAll(other);
    }
    
    public ReportPartitions(DisambiguationPolicy disambiguationPolicy, List<List<MutableDisambiguationReport<T>>> partitions) {
        this(disambiguationPolicy);
        for(List<MutableDisambiguationReport<T>> partition : partitions) {
            putAll(partition);
        }
    }
    
    public DisambiguationPolicy getDisambiguationPolicy() {
        return disambiguationPolicy;
    }

    public List<List<MutableDisambiguationReport<T>>> getAmbiguousPartitions() {
        return Collections.unmodifiableList(ambiguousPartitions);
    }

    public List<List<MutableDisambiguationReport<T>>> getUniquePartitions() {
        return Collections.unmodifiableList(uniquePartitions);
    }

    public List<List<MutableDisambiguationReport<T>>> getAllPartitions() {
        List<List<MutableDisambiguationReport<T>>> ret = new ArrayList<List<MutableDisambiguationReport<T>>>(ambiguousPartitions);
        ret.addAll(uniquePartitions);
        return Collections.unmodifiableList(ret);
    }
    
    public void put(MutableDisambiguationReport<T> value) {
        put(value, true);
    }

    public void putAll(List<MutableDisambiguationReport<T>> values) {
        for (MutableDisambiguationReport<T> v : values) {
            put(v, false);
        }
        updatePolicy();
    }

    public void putAll(ReportPartitions<T> other) {
        for (List<MutableDisambiguationReport<T>> partition : other.getAllPartitions()) {
            for (MutableDisambiguationReport<T> v : partition) {
                put(v, false);
            }
        }
        updatePolicy();
    }
    
    public boolean isPartitionsUnique() {
        return ambiguousPartitions.size() == 0;
    }

    public String toString() {
        return "ReportPartitions[policy=" + disambiguationPolicy + ", uniquePartitions=" + uniquePartitions + ", ambiguousPartitions=" + ambiguousPartitions + "]";
    }
    
    private void put(MutableDisambiguationReport<T> value, boolean updatePolicy) {
        if (insertIntoExisting(value, ambiguousPartitions) >= 0) {
            return;
        } else {    
            int idx = insertIntoExisting(value, uniquePartitions);
            if (idx >= 0) {
                ambiguousPartitions.add(uniquePartitions.remove(idx));
            } else {
                List<MutableDisambiguationReport<T>> newPartition = new ArrayList<MutableDisambiguationReport<T>>();
                newPartition.add(value);
                uniquePartitions.add(newPartition);
            }
        }
        
        if (updatePolicy) {
            updatePolicy();
        }
    }
    
    private void updatePolicy() {
        this.disambiguationPolicy.getCurrentLevel().setDeciding(uniquePartitions.size() > 0);
    }
    
    private int insertIntoExisting(MutableDisambiguationReport<T> value, List<List<MutableDisambiguationReport<T>>> partitions) {
        int idx = -1;
        boolean found = false;
        for (List<MutableDisambiguationReport<T>> partition : partitions) {
            for (MutableDisambiguationReport<T> partitionPrototype : partition) {
                if (disambiguationPolicy.areAmbiguous(partitionPrototype, value)) {
                    found = true;
                    break;
                }
            }

            idx++;
            
            if (found) {
                partition.add(value);
                break;
            }
        }
        
        return found ? idx : -1;
    }
}