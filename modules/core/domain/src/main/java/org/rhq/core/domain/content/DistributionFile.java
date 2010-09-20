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

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

/**
 * This is the many-to-many entity that correlates a distribution with its associated files.
 *
 * @author Pradeep Kilambi
 */
@Entity
//@IdClass(DistributionFilePK.class)
@NamedQueries({
    @NamedQuery(name = DistributionFile.SELECT_BY_DIST_ID, query = "SELECT df from DistributionFile df WHERE df.distribution.id = :distId"),
    @NamedQuery(name = DistributionFile.DELETE_BY_DIST_ID, query = "DELETE DistributionFile df WHERE df.distribution.id = :distId") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DISTRIBUTION_FILE_ID_SEQ")
@Table(name = "RHQ_DISTRIBUTION_FILE")
public class DistributionFile {
    public static final String SELECT_BY_DIST_ID = "DistributionFile.selectByDistId";
    public static final String DELETE_BY_DIST_ID = "DistributionFile.deleteByDistId";

    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @XmlTransient
    @JoinColumn(name = "DISTRIBUTION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    private Distribution distribution;

    @Column(name = "RELATIVE_FILENAME", nullable = false)
    private String relative_filename;

    @Column(name = "LAST_MODIFIED", nullable = false)
    private long last_modified;

    @Column(name = "MD5SUM", nullable = false)
    private String md5sum;

    protected DistributionFile() {
    }

    public DistributionFile(Distribution dist, String filenameIn, String md5sum) {
        this.distribution = dist;
        this.relative_filename = filenameIn;
        this.md5sum = md5sum;
    }

    public DistributionFilePK getDistributionFilePK() {
        return new DistributionFilePK(distribution);
    }

    public void setDistributionFilePK(DistributionFilePK pk) {
        this.distribution = pk.getDistribution();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getLastModified() {
        return last_modified;
    }

    public void setLastModified(long lastModifiedIn) {
        this.last_modified = lastModifiedIn;
    }

    public Distribution getDistribution() {
        return distribution;
    }

    public void setDistribution(Distribution dist) {
        this.distribution = dist;
    }

    public String getRelativeFilename() {
        return relative_filename;
    }

    public void setRelativeFilename(String relative_filename) {
        this.relative_filename = relative_filename;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    @PrePersist
    void onPersist() {
        this.last_modified = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("DistributionFile: ");
        str.append("ctime=[").append(new Date(this.last_modified)).append("]");
        str.append(", ch=[").append(this.relative_filename).append("]");
        str.append(", ch=[").append(this.md5sum).append("]");
        str.append(", cs=[").append(this.distribution).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((getDistribution() == null) ? 0 : getDistribution().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if ((obj == null) || (!(obj instanceof DistributionFile))) {
            return false;
        }

        DistributionFile other = (DistributionFile) obj;

        if (getDistribution() == null) {
            if (other.getDistribution() != null) {
                return false;
            }
        } else if (!getDistribution().equals(other.getDistribution())) {
            return false;
        }

        return true;
    }

}
