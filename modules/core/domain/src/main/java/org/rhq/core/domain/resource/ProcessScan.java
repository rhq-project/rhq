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
package org.rhq.core.domain.resource;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Describes a process scan query that can be used to auto-discover resources of a particular {@link ResourceType}.
 *
 * @author John Mazzitelli
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PROCESS_SCAN_ID_SEQ")
@Table(name = "RHQ_PROCESS_SCAN")
public class ProcessScan implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "QUERY", nullable = false)
    private String query;

    @Column(name = "NAME", nullable = true)
    private String name;

    protected ProcessScan() {
    }

    public ProcessScan(String query, String name) {
        if (query == null) {
            throw new NullPointerException("query==null");
        }

        this.query = query;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Two process scans are equal if they have the same query
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof ProcessScan)) {
            return false;
        }

        return query.equals(((ProcessScan) obj).query);
    }

    @Override
    public int hashCode() {
        return query.hashCode();
    }

    @Override
    public String toString() {
        return "ProcessScan: query=[" + this.query + "], name=[" + this.name + "]";
    }
}