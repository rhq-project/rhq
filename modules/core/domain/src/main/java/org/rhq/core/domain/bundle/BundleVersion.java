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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.tagging.Tag;

/**
 * Defines a versioned bundle of content that can be provisioned somewhere.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = BundleVersion.UPDATE_VERSION_ORDER_BY_BUNDLE_ID_AFTER_DELETE, query = "" //
        + "UPDATE BundleVersion bv "//
        + "   SET bv.versionOrder = (bv.versionOrder-1) " //
        + " WHERE bv.bundle.id = :bundleId " //
        + "   AND bv.versionOrder > :versionOrder"), //
    @NamedQuery(name = BundleVersion.UPDATE_VERSION_ORDER_BY_BUNDLE_ID, query = "" // 
        + "UPDATE BundleVersion bv "//
        + "   SET bv.versionOrder = (bv.versionOrder+1) " //
        + " WHERE bv.bundle.id = :bundleId " //
        + "   AND bv.versionOrder >= :versionOrder"), //
    @NamedQuery(name = BundleVersion.QUERY_FIND_LATEST_BY_BUNDLE_ID, query = "" //
        + "SELECT bv " //
        + "  FROM BundleVersion bv " // 
        + " WHERE bv.bundle.id = :bundleId " //
        + "   AND bv.versionOrder = (SELECT MAX(bv2.versionOrder) FROM BundleVersion bv2 WHERE bv2.bundle.id = :bundleId) "), //
    // this returns a desc ordered list of a 2-D array - first element in array is the version string, second is its associated version order
    @NamedQuery(name = BundleVersion.QUERY_FIND_VERSION_INFO_BY_BUNDLE_ID, query = "" //
        + "SELECT bv.version, bv.versionOrder " //
        + "  FROM BundleVersion bv " // 
        + " WHERE bv.bundle.id = :bundleId " //
        + " ORDER BY bv.versionOrder DESC "), //
    @NamedQuery(name = BundleVersion.QUERY_FIND_ALL, query = "SELECT bv FROM BundleVersion bv "), //
    @NamedQuery(name = BundleVersion.QUERY_FIND_BY_NAME, query = "SELECT bv FROM BundleVersion bv WHERE bv.name = :name "), //
    @NamedQuery(name = BundleVersion.QUERY_FIND_BY_BUNDLE_ID, query = "SELECT bv FROM BundleVersion bv WHERE bv.bundle.id = :bundleId ") //
})
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_BUNDLE_VERSION_ID_SEQ", sequenceName = "RHQ_BUNDLE_VERSION_ID_SEQ")
@Table(name = "RHQ_BUNDLE_VERSION")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String UPDATE_VERSION_ORDER_BY_BUNDLE_ID_AFTER_DELETE = "BundleVersion.updateVersionOrderByBundleIdAfterDelete";
    public static final String UPDATE_VERSION_ORDER_BY_BUNDLE_ID = "BundleVersion.updateVersionOrderByBundleId";
    public static final String QUERY_FIND_LATEST_BY_BUNDLE_ID = "BundleVersion.findLatestByBundleId";
    public static final String QUERY_FIND_VERSION_INFO_BY_BUNDLE_ID = "BundleVersion.findVersionsByBundleId";
    public static final String QUERY_FIND_ALL = "BundleVersion.findAll";
    public static final String QUERY_FIND_BY_NAME = "BundleVersion.findByName";
    public static final String QUERY_FIND_BY_BUNDLE_ID = "BundleVersion.findByBundleId";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_BUNDLE_VERSION_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "VERSION", nullable = false)
    private String version;

    @Column(name = "VERSION_ORDER", nullable = false)
    private int versionOrder;

    @Column(name = "ACTION", nullable = false)
    private String recipe;

    @JoinColumn(name = "BUNDLE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Bundle bundle;

    @JoinColumn(name = "CONFIG_DEF_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private ConfigurationDefinition configurationDefinition;

    @OneToMany(mappedBy = "bundleVersion", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<BundleDeployment> bundleDeployments = new ArrayList<BundleDeployment>();

    @OneToMany(mappedBy = "bundleVersion", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BundleFile> bundleFiles = new ArrayList<BundleFile>();

    @ManyToMany(mappedBy = "bundleVersions", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<Tag> tags;

    public BundleVersion() {
        // for JPA use
    }

    public BundleVersion(String name, String version, Bundle bundle, String recipe) {
        setName(name);
        setVersion(version);
        setBundle(bundle);
        setRecipe(recipe);
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getVersionOrder() {
        return versionOrder;
    }

    public void setVersionOrder(int versionOrder) {
        this.versionOrder = versionOrder;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Returns the metadata that describes the configuration that must be set in order for this
     * bundle to be properly deployed. Think of this as "the questions that the user must answer"
     * in order to provide values that are needed to deploy the content. This definition
     * describes the {@link BundleDeployment#getConfiguration() bundle config data}.
     *
     * @return defines the values that must be set in order for this bundle to be deployed properly
     */
    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public void setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    public List<BundleDeployment> getBundleDeployments() {
        return bundleDeployments;
    }

    public void setBundleDeployments(List<BundleDeployment> bundleDeployments) {
        this.bundleDeployments = bundleDeployments;
    }

    public void addBundleDeployment(BundleDeployment bundleDeployment) {
        this.bundleDeployments.add(bundleDeployment);
        bundleDeployment.setBundleVersion(this);
    }

    public List<BundleFile> getBundleFiles() {
        return bundleFiles;
    }

    public void addBundleFile(BundleFile bundleFile) {
        this.bundleFiles.add(bundleFile);
        bundleFile.setBundleVersion(this);
    }

    public void setBundleFiles(List<BundleFile> bundleFiles) {
        this.bundleFiles = bundleFiles;
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
        return "BundleVersion[id=" + id + ",name=" + name + ",version=" + version + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BundleVersion)) {
            return false;
        }

        BundleVersion other = (BundleVersion) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }

        return true;
    }
}