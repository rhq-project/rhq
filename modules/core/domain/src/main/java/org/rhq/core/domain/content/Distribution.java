/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

/**
 *
 * @author Pradeep Kilambi
 */
@Entity
@NamedQueries({
    @NamedQuery(name = Distribution.QUERY_FIND_ALL, query = "SELECT dt FROM Distribution dt"),
    @NamedQuery(name = Distribution.QUERY_FIND_PATH_BY_DIST_TYPE, query = "SELECT dt " + "  FROM Distribution dt "
        + " WHERE dt.label = :label AND dt.distributionType.name = :typeName "),
    @NamedQuery(name = Distribution.QUERY_FIND_BY_DIST_LABEL, query = "SELECT dt FROM Distribution dt WHERE dt.label = :label"),
    @NamedQuery(name = Distribution.QUERY_FIND_BY_DIST_PATH, query = "SELECT dt FROM Distribution dt WHERE dt.basePath = :path"),
    @NamedQuery(name = Distribution.QUERY_DELETE_BY_DIST_ID, query = "DELETE Distribution dt WHERE dt.id = :distid") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DISTRIBUTION_ID_SEQ")
@Table(name = "RHQ_DISTRIBUTION")
public class Distribution implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String QUERY_FIND_ALL = "Distribution.findAll";
    public static final String QUERY_FIND_BY_DIST_LABEL = "Distribution.findByDistLabel";
    public static final String QUERY_FIND_BY_DIST_PATH = "Distribution.findByDistPath";
    public static final String QUERY_FIND_PATH_BY_DIST_TYPE = "Distribution.findPathByDistType";
    public static final String QUERY_DELETE_BY_DIST_ID = "Distribution.deleteByDistId";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "DISTRIBUTION_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(cascade = CascadeType.PERSIST)
    private DistributionType distributionType;

    /**
     *Distribution label
     */
    @Column(name = "LABEL", nullable = false)
    private String label;

    /**
     * Base path where the kickstart tree is located
     */
    @Column(name = "BASE_PATH", nullable = false)
    private String basePath;

    @Column(name = "LAST_MODIFIED", nullable = false)
    private long lastModifiedDate;

    @OneToMany(mappedBy = "distribution", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<DistributionFile> distributionFiles;

    public Set<DistributionFile> getDistributionFiles() {
        return distributionFiles;
    }

    public void setDistributionFiles(Set<DistributionFile> distributionFiles) {
        this.distributionFiles = distributionFiles;
    }

    public Distribution() {
    }

    public Distribution(String label, String basepathIn, DistributionType distributionType) {
        setLabel(label);
        setBasePath(basepathIn);
        setDistributionType(distributionType);
    }

    public String getLabel() {
        return this.label;
    }

    public String getBasePath() {
        return this.basePath;
    }

    public void setLabel(String labelIn) {
        this.label = labelIn;
    }

    public void setBasePath(String basepathIn) {
        this.basePath = basepathIn;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Describes the capabilities of this distribution.
     */
    public DistributionType getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(DistributionType distributionType) {
        this.distributionType = distributionType;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return "Distribution [label=" + label + ", Type=" + distributionType + ", basePath=" + basePath + "]";
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Distribution)) {
            return false;
        }

        Distribution kstree = (Distribution) o;

        if ((getLabel() != null) ? (!getLabel().equals(kstree.getLabel())) : (kstree.getLabel() != null)) {
            return false;
        }

        return true;
    }

    @PrePersist
    void onPersist() {
        this.setLastModifiedDate(System.currentTimeMillis());
    }

    @PreUpdate
    void onUpdate() {
        this.setLastModifiedDate(System.currentTimeMillis());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((getLabel() == null) ? 0 : getLabel().hashCode());
        return result;
    }

}
