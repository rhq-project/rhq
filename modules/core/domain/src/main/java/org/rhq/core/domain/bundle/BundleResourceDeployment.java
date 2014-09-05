/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
 * This is the many-to-many entity that correlates a bundle deployment with a (platform) resource.  It keeps
 * information about the currently installed bundle and assists with enforcing the deployment's policy on the
 * deployed bundle. It also provides the anchor for audit history related to the deployment.
 * 
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries({
    @NamedQuery(name = BundleResourceDeployment.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM BundleResourceDeployment brd "
        + " WHERE brd.resource.id IN ( :resourceIds ) )"),
    @NamedQuery(name = BundleResourceDeployment.QUERY_FIND_BY_DEPLOYMENT_ID_NO_FETCH, query = "SELECT brd FROM BundleResourceDeployment brd WHERE brd.bundleDeployment.id = :id "),
    @NamedQuery(name = BundleResourceDeployment.QUERY_FIND_BY_RESOURCE_ID_NO_FETCH, query = "SELECT brd FROM BundleResourceDeployment brd WHERE brd.resource.id = :id ") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_BUNDLE_RES_DEPLOY_ID_SEQ", sequenceName = "RHQ_BUNDLE_RES_DEPLOY_ID_SEQ")
@Table(name = "RHQ_BUNDLE_RES_DEPLOY")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleResourceDeployment implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "BundleResourceDeployment.deleteByResources";
    public static final String QUERY_FIND_BY_DEPLOYMENT_ID_NO_FETCH = "BundleResourceDeployment.findByDeploymentIdNoFetch";
    public static final String QUERY_FIND_BY_RESOURCE_ID_NO_FETCH = "BundleResourceDeployment.findByResourceIdNoFetch";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_BUNDLE_RES_DEPLOY_ID_SEQ")
    @Id
    private int id;

    @JoinColumn(name = "BUNDLE_DEPLOYMENT_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BundleDeployment bundleDeployment;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Resource resource;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    protected BundleDeploymentStatus status;

    @Column(name = "CTIME")
    private Long ctime = -1L;

    @OneToMany(mappedBy = "resourceDeployment", fetch = FetchType.LAZY, cascade = { CascadeType.DETACH,
        CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    private List<BundleResourceDeploymentHistory> histories = new ArrayList<BundleResourceDeploymentHistory>();

    protected BundleResourceDeployment() {
    }

    public BundleResourceDeployment(BundleDeployment bundleDeployment, Resource resource) {
        this.bundleDeployment = bundleDeployment;
        this.resource = resource;
        this.status = BundleDeploymentStatus.IN_PROGRESS;
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

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Long getCtime() {
        return ctime;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    public BundleDeployment getBundleDeployment() {
        return bundleDeployment;
    }

    public void setBundleDeployment(BundleDeployment bundleDeployment) {
        this.bundleDeployment = bundleDeployment;
    }

    public List<BundleResourceDeploymentHistory> getBundleResourceDeploymentHistories() {
        return histories;
    }

    public void setBundleResourceDeploymentHistories(List<BundleResourceDeploymentHistory> histories) {
        this.histories = histories;
    }

    public void addBundleResourceDeploymentHistory(BundleResourceDeploymentHistory history) {
        history.setResourceDeployment(this);
        this.histories.add(history);
    }

    /**
     * The status of the request which indicates that the request is either still in progress, or it has completed and
     * either succeeded or failed.
     *
     * @return the request status
     */
    public BundleDeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(BundleDeploymentStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "BundleResourceDeployment: " + "bdd=[" + this.bundleDeployment + "]" + ", resource=[" + this.resource
            + "]";
    }

    /*
     * These fields make up the natural key but note that some fields are lazy loaded. As such care should
     * be taken to have properly loaded instances when required.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((bundleDeployment == null) ? 0 : bundleDeployment.hashCode());
        result = (31 * result) + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    /*
     * These fields make up the natural key but note that some fields are lazy loaded. As such care should
     * be taken to have properly loaded instances when required.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof BundleResourceDeployment))) {
            return false;
        }

        final BundleResourceDeployment other = (BundleResourceDeployment) obj;

        if (bundleDeployment == null) {
            if (other.bundleDeployment != null) {
                return false;
            }
        } else if (!bundleDeployment.equals(other.bundleDeployment)) {
            return false;
        }

        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }

        return true;
    }
}
