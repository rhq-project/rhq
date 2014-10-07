/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.content;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Defines a type of (@link Distribution)
 * e.g. Kickstart trees for a linux distribution.
 * e.g  JumpStarts for a solaris distribution
 * @author Pradeep Kilambi
 */

@Entity
@NamedQueries({
    @NamedQuery(name = DistributionType.QUERY_FIND_BY_NAME, query = "SELECT dt FROM DistributionType dt WHERE dt.name = :name"),
    @NamedQuery(name = DistributionType.QUERY_DELETE_BY_NAME, query = "DELETE FROM DistributionType dt where dt.name = :name") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_DISTRIBUTION_TYPE_ID_SEQ", sequenceName = "RHQ_DISTRIBUTION_TYPE_ID_SEQ")
@Table(name = "RHQ_DISTRIBUTION_TYPE")
public class DistributionType implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_NAME = "DistributionType.findByName";
    public static final String QUERY_DELETE_BY_NAME = "DistributionType.deleteByName";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_DISTRIBUTION_TYPE_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    public DistributionType() {
        // for JPA use
    }

    public DistributionType(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "DistributionType: name=[" + this.name + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof DistributionType))) {
            return false;
        }

        final DistributionType other = (DistributionType) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }
}