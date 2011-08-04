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
import java.util.HashSet;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Defines a type of {@link Package} that can exist in the system. Package types are used to provide information about
 * the content of a particular package. Plugins can then use this information to provide clues on how to deal with a
 * package.
 *
 * <p>A package type is defined by a resource type and is only supported by its parent resource type. A resource type
 * can support multiple package types.</p>
 *
 * @author Jason Dobies
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = PackageType.QUERY_FIND_ALL, query = "SELECT pt FROM PackageType pt"),
    @NamedQuery(name = PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID, query = "SELECT pt FROM PackageType pt WHERE pt.resourceType.id = :typeId"),
    @NamedQuery(name = PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_NAME, query = "SELECT pt FROM PackageType pt WHERE pt.resourceType.id = :typeId AND pt.name = :name"),
    @NamedQuery(name = PackageType.QUERY_FIND_BY_NAME_AND_NULL_RESOURCE_TYPE, query = "SELECT pt FROM PackageType pt WHERE pt.resourceType = null AND pt.name = :name"),
    @NamedQuery(name = PackageType.QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_CREATION_FLAG, query = "SELECT pt FROM PackageType pt "
        + "JOIN pt.resourceType rt "
        + "LEFT JOIN FETCH pt.deploymentConfigurationDefinition cd "
        + "LEFT JOIN FETCH cd.templates cts " + "WHERE rt.id = :typeId AND pt.isCreationData = true"),
    @NamedQuery(name = PackageType.QUERY_DYNAMIC_CONFIG_VALUES, query = "SELECT pt.resourceType.plugin || ' - ' || pt.resourceType.name || ' - ' || pt.displayName, pt.name FROM PackageType AS pt") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PACKAGE_TYPE_ID_SEQ")
@Table(name = "RHQ_PACKAGE_TYPE")
@XmlAccessorType(XmlAccessType.FIELD)
public class PackageType implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "PackageType.findAll";

    public static final String QUERY_FIND_BY_RESOURCE_TYPE_ID = "PackageType.findByResourceTypeId";
    public static final String QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_NAME = "PackageType.findByResourceTypeIdAndName";
    public static final String QUERY_FIND_BY_NAME_AND_NULL_RESOURCE_TYPE = "PackageType.findByNameAndNullResourceType";
    public static final String QUERY_FIND_BY_RESOURCE_TYPE_ID_AND_CREATION_FLAG = "PackageType.findByResourceTypeIdAndCreationFlag";

    public static final String QUERY_DYNAMIC_CONFIG_VALUES = "PackageType.dynamicConfigValues";

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DISPLAY_NAME", nullable = true)
    private String displayName;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "CATEGORY", nullable = true)
    @Enumerated(EnumType.STRING)
    private PackageCategory category;

    @Column(name = "DISCOVERY_INTERVAL", nullable = true)
    private long discoveryInterval;

    @Column(name = "IS_CREATION_DATA", nullable = false)
    private boolean isCreationData;

    @Column(name = "SUPPORTS_ARCHITECTURE", nullable = false)
    private boolean supportsArchitecture;

    @JoinColumn(name = "DEPLOYMENT_CONFIG_DEF_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, optional = true)
    private ConfigurationDefinition deploymentConfigurationDefinition;

    @JoinColumn(name = "PACKAGE_EXTRA_CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, optional = true)
    private ConfigurationDefinition packageExtraPropertiesDefinition;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "packageType", cascade = { CascadeType.REMOVE })
    private Set<Package> packages;

    @JoinColumn(name = "RESOURCE_TYPE_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(optional = true)
    @XmlTransient
    private ResourceType resourceType;

    public void afterUnmarshal(Object u, Object resourceType) {
        this.resourceType = (ResourceType) resourceType;
    }

    // Constructors  --------------------------------------------

    public PackageType() {
        // for JPA use
    }

    public PackageType(String name, ResourceType resourceType) {
        setName(name);
        setResourceType(resourceType);
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Programmatic name of the package type.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Name of this package type that is suitable for display to the user in the UI.
     */
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Free text description of this package type.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Describes the type of content in this package.
     */
    public PackageCategory getCategory() {
        return category;
    }

    public void setCategory(PackageCategory category) {
        this.category = category;
    }

    /**
     * Time, in milliseconds, between discovery scans for packages of this type.
     */
    public long getDiscoveryInterval() {
        return discoveryInterval;
    }

    public void setDiscoveryInterval(long discoveryInterval) {
        this.discoveryInterval = discoveryInterval;
    }

    /**
     * Indicates if this type of package must be specified when a new instance of the parent resource type is created.
     */
    public boolean isCreationData() {
        return isCreationData;
    }

    public void setCreationData(boolean creationData) {
        isCreationData = creationData;
    }

    /**
     * Indicates if this package type will be of any specific architecture. If this is <code>true</code>, each package
     * may be of a different architecture. If this is <code>false</code>, all packages will be of architecture
     * "noarch".
     */
    public boolean isSupportsArchitecture() {
        return supportsArchitecture;
    }

    public void setSupportsArchitecture(boolean supportsArchitecture) {
        this.supportsArchitecture = supportsArchitecture;
    }

    /**
     * Defines the properties the user should be prompted to enter when deploying a package of this type. These are
     * settings that tell the plugin information that it will need in order to successfully install the package. For
     * example, a typical deployment property would be a directory location to indicate where the package should be
     * installed.
     */
    public ConfigurationDefinition getDeploymentConfigurationDefinition() {
        return deploymentConfigurationDefinition;
    }

    public void setDeploymentConfigurationDefinition(ConfigurationDefinition deploymentConfigurationDefinition) {
        this.deploymentConfigurationDefinition = deploymentConfigurationDefinition;
    }

    /**
     * A package type may have the need to indicate more data about a package than the package entity allows. Packages
     * of this type can provide values for each property defined here to further describe the package. These extra
     * properties are not for deployment-time configuration settings. They are merely used to further describe the
     * package in a way custom to the package type. For example, a typical extra property would be the name of the
     * vendor that created the package.
     */
    public ConfigurationDefinition getPackageExtraPropertiesDefinition() {
        return packageExtraPropertiesDefinition;
    }

    public void setPackageExtraPropertiesDefinition(ConfigurationDefinition packageExtraPropertiesDefinition) {
        this.packageExtraPropertiesDefinition = packageExtraPropertiesDefinition;
    }

    /**
     * The packages of this type.
     */
    public Set<Package> getPackages() {
        if (packages == null) {
            packages = new HashSet<Package>();
        }

        return packages;
    }

    public void addPackage(Package pkg) {
        getPackages().add(pkg);
        pkg.setPackageType(this);
    }

    public void setPackages(Set<Package> packages) {
        this.packages = packages;
    }

    /**
     * The resource type that defined this package type. Resources of this resource type can have packages of this
     * package type installed on them. This can be null if this package type only exists in support of some serverside-only
     * functionality.
     */
    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Updates the contents of this definition with values from the specified new defintion. The intention is for this
     * to be used as a merge between this attached instance and a detached instance. The name and resourceTypes will NOT
     * be updated as part of this call; they are used as identifiers and should already be the same if this merge is
     * being performed.
     *
     * @param newType contains new data to merge into this definition; cannot be <code>null</code>
     */
    public void update(PackageType newType) {
        this.displayName = newType.getDisplayName();
        this.description = newType.getDescription();
        this.category = newType.getCategory();
        this.discoveryInterval = newType.getDiscoveryInterval();
        // Don't update references... these have to be linked to persistent objects
        //        this.deploymentConfigurationDefinition = newType.getDeploymentConfigurationDefinition();
        //        this.packageExtraPropertiesDefinition = newType.getPackageExtraPropertiesDefinition();
        this.isCreationData = newType.isCreationData();
        this.packages = newType.getPackages();
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return "PackageType[id=" + id + ",name=" + name + ",resourceType="
            + ((resourceType != null) ? resourceType.getName() : "?") + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + ((resourceType == null) ? 0 : resourceType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof PackageType))) {
            return false;
        }

        final PackageType other = (PackageType) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (resourceType == null) {
            if (other.resourceType != null) {
                return false;
            }
        } else if (!resourceType.equals(other.resourceType)) {
            return false;
        }

        return true;
    }
}