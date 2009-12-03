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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This is the entity mapping that correlates an advisory with associated bugid. 
 *
 * @author Pradeep Kilambi
 */

@Entity
@NamedQueries( { @NamedQuery(name = AdvisoryBuglist.FIND_BUGS_BY_ADV_ID, query = "SELECT ab FROM AdvisoryBuglist AS ab "
    + "WHERE ab.advisory.id = :advId") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_ADVISORY_BUG_SEQ")
@Table(name = "RHQ_ADVISORY_BUGLIST")
public class AdvisoryBuglist implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String FIND_BUGS_BY_ADV_ID = "AdvisoryCVE.findBugsByAveId";

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    //@ManyToOne
    @JoinColumn(name = "ADVISORY_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Advisory advisory;

    @Column(name = "BUG_ID", nullable = false)
    private String bugid;

    @Column(name = "CTIME", nullable = false)
    private long createdTime;

    @Column(name = "LAST_MODIFIED", nullable = false)
    private long lastModifiedDate;

    protected AdvisoryBuglist() {
    }

    public AdvisoryBuglist(Advisory adv, String bugid) {
        this.advisory = adv;
        this.bugid = bugid;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public Advisory getAdvisory() {
        return advisory;
    }

    public void setAdvisory(Advisory advisory) {
        this.advisory = advisory;
    }

    public String getBugid() {
        return bugid;
    }

    public void setBugid(String bugid) {
        this.bugid = bugid;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @PrePersist
    void onPersist() {
        this.createdTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("AdvisoryCVE: ");
        str.append("ctime=[").append(new Date(this.createdTime)).append("]");
        str.append(", Advisory=[").append(this.advisory).append("]");
        str.append(", BUGID=[").append(this.bugid).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((advisory == null) ? 0 : advisory.hashCode());
        result = (31 * result) + ((bugid == null) ? 0 : bugid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof AdvisoryBuglist))) {
            return false;
        }

        final AdvisoryBuglist other = (AdvisoryBuglist) obj;

        if (advisory == null) {
            if (advisory != null) {
                return false;
            }
        } else if (!advisory.equals(other.advisory)) {
            return false;
        }

        return true;
    }
}