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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.resource.group.ResourceGroup;

@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_BUNDLE_GROUP_DEPLOY_ID_SEQ")
@Table(name = "RHQ_BUNDLE_GROUP_DEPLOY")
public class BundleGroupDeployment implements Serializable {

    private static final long serialVersionUID = 1L;

    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    @JoinColumn(name = "GROUP_ID", referencedColumnName = "ID")
    @ManyToOne(cascade = { CascadeType.REMOVE })
    private ResourceGroup group;

    @JoinColumn(name = "BUNDLE_DEPLOY_DEF_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(cascade = { CascadeType.REMOVE }, fetch = FetchType.LAZY)
    protected BundleDeployDefinition bundleDeployDefinition;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    protected BundleGroupDeploymentStatus status;

    @Column(name = "ERROR_MESSAGE")
    protected String errorMessage;

    @Column(name = "SUBJECT_NAME")
    protected String subjectName;

    @Column(name = "CTIME", nullable = false)
    protected long createdTime = System.currentTimeMillis();

    @Column(name = "MTIME", nullable = false)
    protected long modifiedTime = System.currentTimeMillis();

    @OneToMany(mappedBy = "bundleGroupDeployment", fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private List<BundleDeployment> bundleDeployments = new ArrayList<BundleDeployment>();

    // For JPA
    public BundleGroupDeployment() {
    }

    public BundleGroupDeployment(String subjectName, BundleDeployDefinition bundleDeployDefinition, ResourceGroup group) {
        this.subjectName = subjectName;
        this.bundleDeployDefinition = bundleDeployDefinition;
        this.group = group;
        this.status = BundleGroupDeploymentStatus.INPROGRESS;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * The status of the request which indicates that the request is either still in progress, or it has completed and
     * either succeeded or failed.
     *
     * @return the request status
     */
    public BundleGroupDeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(BundleGroupDeploymentStatus status) {
        this.status = status;
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
    }

    public BundleDeployDefinition getBundleDeployDefinition() {
        return bundleDeployDefinition;
    }

    public void setBundleDeployDefinition(BundleDeployDefinition bundleDeployDefinition) {
        this.bundleDeployDefinition = bundleDeployDefinition;
    }

    /**
     * If not <code>null</code>, this is an error message (possibly a full stack trace) to indicate the overall error
     * that occurred when the configuration update failed. This will normally be <code>null</code> unless the
     * {@link #getStatus() status} indicates a {@link BundleGroupDeploymentStatus#FAILURE}.
     *
     * @return overall error that occurred
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Calling this method with a non-<code>null</code> error message implies that the request's status is
     * {@link ConfigurationUpdateStatus#FAILURE}. The inverse is <i>not</i> true - that is, if you set the error message
     * to <code>null</code>, the status is left as-is; it will not assume that a <code>null</code> error message means
     * the status is successful.
     *
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;

        if (this.errorMessage != null) {
            setStatus(BundleGroupDeploymentStatus.FAILURE);
        }
    }

    /**
     * For auditing purposes, this method tells you the username of the person that created the request. This is not a
     * relationship to an actual Subject because we want to maintain the audit trail, even if a Subject has been deleted
     * from the database.
     *
     * @return the actual name string of the submitter of the request
     */
    public String getSubjectName() {
        return subjectName;
    }

    /**
     * The time this entity was originally created; in other words, when the request was originally made.
     *
     * @return creation time
     */
    public long getCreatedTime() {
        return this.createdTime;
    }

    /**
     * The time this entity was last modified. This is the last time the status was updated. If the status has never
     * been updated, this will be the {@link #getCreatedTime() created time}.
     *
     * @return last modified time
     */
    public long getModifiedTime() {
        return this.modifiedTime;
    }

    public List<BundleDeployment> getBundleDeployments() {
        return bundleDeployments;
    }

    public void setBundleDeployments(List<BundleDeployment> bundleDeployments) {
        this.bundleDeployments = bundleDeployments;
    }

    public void addBundleDeployment(BundleDeployment bundleDeployment) {
        bundleDeployment.setBundleGroupDeployment(this);
        this.bundleDeployments.add(bundleDeployment);
    }

    /**
     * The duration of the configuration update request which simply is the difference between the
     * {@link #getCreatedTime()} and the {@link #getModifiedTime()}. If the request hasn't completed yet, this will be
     * the difference between the current time and the created time.
     *
     * @return the duration of time that the request took or is taking to complete
     */
    public long getDuration() {
        long start = this.createdTime;
        long end;
        if ((status == null) || (status == BundleGroupDeploymentStatus.INPROGRESS)) {
            end = System.currentTimeMillis();
        } else {
            end = this.modifiedTime;
        }

        return end - start;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = (PRIME * result) + (int) (createdTime ^ (createdTime >>> 32));
        result = (PRIME * result) + ((subjectName == null) ? 0 : subjectName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof BundleGroupDeployment)) {
            return false;
        }

        final BundleGroupDeployment other = (BundleGroupDeployment) obj;

        if (this.createdTime != other.createdTime) {
            return false;
        }

        if (this.subjectName == null) {
            if (other.subjectName != null) {
                return false;
            }
        } else if (!this.subjectName.equals(other.subjectName)) {
            return false;
        }

        return true;
    }

    @PrePersist
    void onPersist() {
        // don't set createdTime - we use it in equals/hashCode - it is already set at instantiation time
        this.modifiedTime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.modifiedTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("BundleGroupDeployment [");
        str.append("id=").append(this.id);
        str.append(", status=").append(this.status);
        str.append(", resourceGroup=").append(this.group);
        str.append(", deployDef=").append(this.getBundleDeployDefinition());
        str.append(", subjectName=").append(this.subjectName);
        str.append(", createdTime=").append(this.createdTime);
        str.append(", modifiedTime=").append(this.modifiedTime);

        String err = this.errorMessage;
        if ((err != null) && (err.indexOf('\n') > -1)) {
            err = err.substring(0, err.indexOf('\n')) + "...";
        }
        str.append(", errorMessage=").append(err);
        str.append(']');

        return str.toString();
    }
}
