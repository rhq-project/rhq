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
package org.rhq.core.domain.bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.resource.Resource;

/**
 * This is the many-to-many entity that correlates a bundle deployment def with a (platform) resource.  It keeps
 * information about the currently installed bundle and assists with enforcing the def's policy on the deployed bundle.
 * It also provides the anchor for audit history related to the deployment.
 * 
 * @author John Mazzitelli
 */
@Entity
//@IdClass(BundleDeploymentPK.class)
@NamedQueries( {
    @NamedQuery(name = BundleDeployment.QUERY_FIND_BY_DEFINITION_ID_NO_FETCH, query = "SELECT bd FROM BundleDeployment bd WHERE bd.bundleDeployDefinition.id = :id "),
    @NamedQuery(name = BundleDeployment.QUERY_FIND_BY_RESOURCE_ID_NO_FETCH, query = "SELECT bd FROM BundleDeployment bd WHERE bd.resource.id = :id ") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_BUNDLE_DEPLOY_ID_SEQ")
@Table(name = "RHQ_BUNDLE_DEPLOY")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleDeployment implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_DEFINITION_ID_NO_FETCH = "BundleDeployment.findByDefinitionIdNoFetch";
    public static final String QUERY_FIND_BY_RESOURCE_ID_NO_FETCH = "BundleDeployment.findByResourceIdNoFetch";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "BUNDLE_DEPLOY_DEF_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private BundleDeployDefinition bundleDeployDefinition;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Resource resource;

    @Column(name = "CTIME")
    private Long ctime = -1L;

    @OneToMany(mappedBy = "bundleDeployment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BundleDeploymentHistory> histories = new ArrayList<BundleDeploymentHistory>();

    protected BundleDeployment() {
    }

    public BundleDeployment(BundleDeployDefinition bundleDeploymentDef, Resource resource) {
        this.bundleDeployDefinition = bundleDeploymentDef;
        this.resource = resource;
    }

    public BundleDeployDefinition getBundleDeployDefinition() {
        return bundleDeployDefinition;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Resource getResource() {
        return resource;
    }

    public Long getCtime() {
        return ctime;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    public void setBundleDeployDefinition(BundleDeployDefinition bundleDeployDefinition) {
        this.bundleDeployDefinition = bundleDeployDefinition;
    }

    public List<BundleDeploymentHistory> getBundleDeploymentHistories() {
        return histories;
    }

    public void setBundleDeploymentHistories(List<BundleDeploymentHistory> histories) {
        this.histories = histories;
    }

    public void addBundleDeploymentHistory(BundleDeploymentHistory history) {
        history.setBundleDeployment(this);
        this.histories.add(history);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("BundleDeployment: ");
        str.append(", bdd=[").append(this.bundleDeployDefinition).append("]");
        str.append(", resource=[").append(this.resource).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((bundleDeployDefinition == null) ? 0 : bundleDeployDefinition.hashCode());
        result = (31 * result) + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof BundleDeployment))) {
            return false;
        }

        final BundleDeployment other = (BundleDeployment) obj;

        if (bundleDeployDefinition == null) {
            if (bundleDeployDefinition != null) {
                return false;
            }
        } else if (!bundleDeployDefinition.equals(other.bundleDeployDefinition)) {
            return false;
        }

        if (resource == null) {
            if (resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }

        return true;
    }
}