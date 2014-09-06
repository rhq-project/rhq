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
package org.rhq.core.domain.bundle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.tagging.Tag;

/**
 * Defines a set of configuration values that can be used to deploy a bundle version somewhere. Once set the
 * configuration should not be changed.  Also stores any other deployment settings to be applied to deployments
 * using this def.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries( { //
@NamedQuery(name = BundleDeployment.QUERY_FIND_ALL, query = "" //
    + "SELECT bd FROM BundleDeployment bd "),
    @NamedQuery(name = BundleDeployment.QUERY_UPDATE_FOR_DESTINATION_REMOVE, query = "" //
        + "UPDATE BundleDeployment bd " //
        + "   SET bd.replacedBundleDeploymentId = NULL " //
        + " WHERE bd.replacedBundleDeploymentId IN " //
        + "     ( SELECT innerbd.id FROM BundleDeployment innerbd " //
        + "        WHERE innerbd.destination.id  = :destinationId ) "),
    @NamedQuery(name = BundleDeployment.QUERY_UPDATE_FOR_VERSION_REMOVE, query = "" //
        + "UPDATE BundleDeployment bd " //
        + "   SET bd.replacedBundleDeploymentId = NULL " //        
        + " WHERE bd.replacedBundleDeploymentId IN " //        
        + "     ( SELECT innerbd.id FROM BundleDeployment innerbd " //
        + "        WHERE innerbd.bundleVersion.id  = :bundleVersionId ) "),
    @NamedQuery(name = BundleDeployment.QUERY_UPDATE_FOR_DEPLOYMENT_REMOVE, query = "" //
        + "UPDATE BundleDeployment bd " //
        + "   SET bd.replacedBundleDeploymentId = ( SELECT innerbd.replacedBundleDeploymentId "
        + "                                         FROM BundleDeployment innerbd "
        + "                                         WHERE innerbd.id = :bundleId ) " //        
        + " WHERE bd.replacedBundleDeploymentId = :bundleId ") })
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_BUNDLE_DEPLOYMENT_ID_SEQ", sequenceName = "RHQ_BUNDLE_DEPLOYMENT_ID_SEQ")
@Table(name = "RHQ_BUNDLE_DEPLOYMENT")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleDeployment implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "BundleDeployment.findAll";
    public static final String QUERY_UPDATE_FOR_DESTINATION_REMOVE = "BundleDeployment.updateForDestinationRemove";
    public static final String QUERY_UPDATE_FOR_VERSION_REMOVE = "BundleDeployment.updateForVersionRemove";
    public static final String QUERY_UPDATE_FOR_DEPLOYMENT_REMOVE = "BundleDeployment.updateForDeploymentRemove";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_BUNDLE_DEPLOYMENT_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    protected BundleDeploymentStatus status;

    @Column(name = "ERROR_MESSAGE")
    protected String errorMessage;

    @Column(name = "SUBJECT_NAME")
    protected String subjectName;

    @Column(name = "IS_LIVE")
    private boolean isLive = false;

    @Column(name = "CTIME")
    private Long ctime = System.currentTimeMillis();

    @Column(name = "MTIME")
    private Long mtime = System.currentTimeMillis();

    // This is intentionally not annotated as a OneToOne association for a BundleDeployment field. If done that way
    // then a fetch could result in a very deep recursive fetch of all replaced deployments (for many deployments
    // to a single destination), which is typically not what we want.  And, it can cause fits in HibernateDetach
    // which does not like extreme depth in its recursive scrubbing [BZ 702390].
    @Column(name = "REPLACED_BUNDLE_DEPLOYMENT_ID", nullable = true)
    private Integer replacedBundleDeploymentId;

    @JoinColumn(name = "CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Configuration configuration;

    @JoinColumn(name = "BUNDLE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BundleVersion bundleVersion;

    @JoinColumn(name = "BUNDLE_DESTINATION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BundleDestination destination;

    @OneToMany(mappedBy = "bundleDeployment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BundleResourceDeployment> resourceDeployments;

    @ManyToMany(mappedBy = "bundleDeployments", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<Tag> tags;

    @Column(name="DISCOVERY_DELAY")
    private Integer discoveryDelay;

    public BundleDeployment() {
        // for JPA use
    }

    public BundleDeployment(BundleVersion bundleVersion, BundleDestination destination, String name) {
        this.bundleVersion = bundleVersion;
        this.destination = destination;
        this.name = name;
        this.status = BundleDeploymentStatus.PENDING;
        this.isLive = false;
        this.discoveryDelay = Integer.valueOf(30);
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

    public long getCtime() {
        return this.ctime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    /**
     * The time that any part of this entity was updated in the database.
     *
     * @return entity modified time
     */
    public long getMtime() {
        return this.mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
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

    /**
     * If not <code>null</code>, this is an error message (possibly a full stack trace) to indicate the overall error
     * that occurred when the configuration update failed. This will normally be <code>null</code> unless the
     * {@link #getStatus() status} indicates a {@link BundleDeploymentStatus#FAILURE}.
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
            setStatus(BundleDeploymentStatus.FAILURE);
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

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean isLive) {
        this.isLive = isLive;
    }

    /**
     * The duration of the configuration update request which simply is the difference between the
     * {@link #getCtime()} and the {@link #getMtime()}. If the request hasn't completed yet, this will be
     * the difference between the current time and the created time.
     *
     * @return the duration of time that the request took or is taking to complete
     */
    public long getDuration() {
        long start = this.ctime;
        long end;
        if ((status == null) || (status == BundleDeploymentStatus.IN_PROGRESS)) {
            end = System.currentTimeMillis();
        } else {
            end = this.mtime;
        }

        return end - start;
    }

    /** 
     * @return The previously "live" BundleDeployment.
     */
    public Integer getReplacedBundleDeploymentId() {
        return replacedBundleDeploymentId;
    }

    public void setReplacedBundleDeploymentId(Integer replacedBundleDeploymentId) {
        this.replacedBundleDeploymentId = replacedBundleDeploymentId;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration config) {
        this.configuration = config;
    }

    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(BundleVersion bundleVersion) {
        this.bundleVersion = bundleVersion;
    }

    public BundleDestination getDestination() {
        return destination;
    }

    public void setDestination(BundleDestination destination) {
        this.destination = destination;
    }

    public List<BundleResourceDeployment> getResourceDeployments() {
        return resourceDeployments;
    }

    public void setResourceDeployments(List<BundleResourceDeployment> resourceDeployments) {
        this.resourceDeployments = resourceDeployments;
    }

    public void addResourceDeployment(BundleResourceDeployment resourceDeployment) {
        if (null == this.resourceDeployments) {
            resourceDeployments = new ArrayList<BundleResourceDeployment>();
        }
        this.resourceDeployments.add(resourceDeployment);
        resourceDeployment.setBundleDeployment(this);
    }

    public Integer getDiscoveryDelay() {
        return discoveryDelay;
    }

    public void setDiscoveryDelay(Integer discoveryDelay) {
        this.discoveryDelay = discoveryDelay;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public void addTag(Tag tag) {
        if (this.tags == null) {
            tags = new HashSet<Tag>();
        }
        tags.add(tag);
    }

    public boolean removeTag(Tag tag) {
        if (tags != null) {
            return tags.remove(tag);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "BundleDeployment[id=" + id + ", name=" + name + "]";
    }

    /*
     * These fields make up the natural key but note that some fields are lazy loaded. As such care should
     * be taken to have properly loaded instances when required.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.bundleVersion == null) ? 0 : this.bundleVersion.hashCode());
        result = prime * result + ((this.destination == null) ? 0 : this.destination.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
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
        if (!(obj instanceof BundleDeployment)) {
            return false;
        }

        BundleDeployment other = (BundleDeployment) obj;

        if (this.bundleVersion == null) {
            if (other.bundleVersion != null) {
                return false;
            }
        } else if (!this.bundleVersion.equals(other.bundleVersion)) {
            return false;
        }

        if (this.destination == null) {
            if (other.destination != null) {
                return false;
            }
        } else if (!this.destination.equals(other.destination)) {
            return false;
        }

        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }

        return true;
    }
}