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
package org.rhq.core.domain.tagging;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * @author Greg Hinkle
 */
@Entity
@NamedQueries( {
        @NamedQuery(name = Tag.QUERY_TAG_COMPOSITE_REPORT,
                query = "SELECT new org.rhq.core.domain.tagging.compsite.TagReportComposite( \n" +
                        "   t.id, t.namespace, t.semantic, t.name,\n" +
                        "  (count(r) + count(g) + count(b) + count(bv) + count(bd) + count(bds)) AS Total,\n" +
                        "  count(r) AS Resources, count(g) AS ResourceGroups, count(b) AS Bundles, count(bv) AS BundleVersions, count(bd) AS BundleDeployments, count(bds) AS BundleDestinations )\n" +
                        "FROM Tag t LEFT JOIN t.resources r  LEFT JOIN t.resourceGroups g LEFT JOIN t.bundles b LEFT JOIN t.bundleVersions bv LEFT JOIN t.bundleDeployments bd LEFT JOIN t.bundleDestinations bds \n" +
                        "GROUP BY t.id, t.namespace, t.semantic, t.name\n" +
                        "ORDER BY (count(r) + count(g) + count(b) + count(bv) + count(bd)) desc")
})
@SequenceGenerator(name = "RHQ_TAGGING_SEQ", sequenceName = "RHQ_TAGGING_ID_SEQ", allocationSize = 10)
@Table(name = "RHQ_TAGGING")
@XmlAccessorType(XmlAccessType.FIELD)
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String QUERY_TAG_COMPOSITE_REPORT = "Tag.compositeReport";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_TAGGING_SEQ")
    @Id
    private int id;

    @Column(name = "NAMESPACE", nullable = true)
    private String namespace;

    @Column(name = "SEMANTIC", nullable = true)
    private String semantic;

    @Column(name = "NAME", nullable = false)
    private String name;

    @JoinTable(name = "RHQ_TAGGING_RESOURCE_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_ID") })
    @ManyToMany
    private Set<Resource> resources;

    @JoinTable(name = "RHQ_TAGGING_RES_GROUP_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "RESOURCE_GROUP_ID") })
    @ManyToMany
    private Set<ResourceGroup> resourceGroups;

    @JoinTable(name = "RHQ_TAGGING_BUNDLE_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "BUNDLE_ID") })
    @ManyToMany
    private Set<Bundle> bundles;

    @JoinTable(name = "RHQ_TAGGING_BUNDLE_VERSION_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "BUNDLE_VERSION_ID") })
    @ManyToMany
    private Set<BundleVersion> bundleVersions;

    @JoinTable(name = "RHQ_TAGGING_BUNDLE_DEPLOY_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "BUNDLE_DEPLOYMENT_ID") })
    @ManyToMany
    private Set<BundleDeployment> bundleDeployments;

    @JoinTable(name = "RHQ_TAGGING_BUNDLE_DEST_MAP", joinColumns = { @JoinColumn(name = "TAG_ID") }, inverseJoinColumns = { @JoinColumn(name = "BUNDLE_DESTINATION_ID") })
    @ManyToMany
    private Set<BundleDestination> bundleDestinations;

    public Tag() {
    }

    public Tag(String namespace, String semantic, String name) {
        this.namespace = namespace;
        this.semantic = semantic;
        this.name = name;
    }

    public Tag(String tag) {
        // Tag format (namespace:)(semantic=)name
        if (tag.contains(":")) {
            namespace = tag.split(":")[0];
            tag = tag.split(":")[1];
        }
        if (tag.contains("=")) {
            semantic = tag.split("=")[0];
            tag = tag.split("=")[1];
        }
        name = tag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }    

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSemantic() {
        return semantic;
    }

    public void setSemantic(String semantic) {
        this.semantic = semantic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public void setResources(Set<Resource> resources) {
        this.resources = resources;
    }

    public void addResource(Resource resource) {
        if (resources == null) {
            resources = new HashSet<Resource>();
        }
        resource.addTag(this);
        resources.add(resource);
    }

    public boolean removeResource(Resource resource) {
        if (resources != null) {
            resource.removeTag(this);
            return resources.remove(resource);
        } else {
            return false;
        }
    }

    public Set<ResourceGroup> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(Set<ResourceGroup> resourceGroups) {
        this.resourceGroups = resourceGroups;
    }

    public void addResourceGroup(ResourceGroup resourceGroup) {
        if (resourceGroups == null) {
            resourceGroups = new HashSet<ResourceGroup>();
        }
        resourceGroup.addTag(this);
        resourceGroups.add(resourceGroup);
    }

    public boolean removeResourceGroup(ResourceGroup resourceGroup) {
        if (resourceGroups != null) {
            resourceGroup.removeTag(this);
            return resourceGroups.remove(resourceGroup);
        } else {
            return false;
        }
    }

    public Set<Bundle> getBundles() {
        return bundles;
    }

    public void setBundles(Set<Bundle> bundles) {
        this.bundles = bundles;
    }

    public void addBundle(Bundle bundle) {
        if (bundles == null) {
            bundles = new HashSet<Bundle>();
        }
        bundle.addTag(this);
        bundles.add(bundle);
    }

    public boolean removeBundle(Bundle bundle) {
        if (bundles != null) {
            bundle.removeTag(this);
            return bundles.remove(bundle);
        } else {
            return false;
        }
    }

    public Set<BundleVersion> getBundleVersions() {
        return bundleVersions;
    }

    public void setBundleVersions(Set<BundleVersion> bundleVersions) {
        this.bundleVersions = bundleVersions;
    }

    public void addBundleVersion(BundleVersion bundleVersion) {
        if (bundleVersions == null) {
            bundleVersions = new HashSet<BundleVersion>();
        }
        bundleVersion.addTag(this);
        bundleVersions.add(bundleVersion);
    }

    public boolean removeBundleVersion(BundleVersion bundleVersion) {
        if (bundleVersions != null) {
            bundleVersion.removeTag(this);
            return bundleVersions.remove(bundleVersion);
        } else {
            return false;
        }
    }

    public Set<BundleDeployment> getBundleDeployments() {
        return bundleDeployments;
    }

    public void setBundleDeployments(Set<BundleDeployment> bundleDeployments) {
        this.bundleDeployments = bundleDeployments;
    }

    public void addBundleDeployment(BundleDeployment bundleDeployment) {
        if (bundleDeployments == null) {
            bundleDeployments = new HashSet<BundleDeployment>();
        }
        bundleDeployment.addTag(this);
        bundleDeployments.add(bundleDeployment);
    }

    public boolean removeBundleDeployment(BundleDeployment bundleDeployment) {
        if (bundleDeployments != null) {
            bundleDeployment.removeTag(this);
            return bundleDeployments.remove(bundleDeployment);
        } else {
            return false;
        }
    }

    public Set<BundleDestination> getBundleDestinations() {
        return bundleDestinations;
    }

    public void setBundleDestinations(Set<BundleDestination> bundleDestinations) {
        this.bundleDestinations = bundleDestinations;
    }

    public void addBundleDestination(BundleDestination bundleDestination) {
        if (bundleDestinations == null) {
            bundleDestinations = new HashSet<BundleDestination>();
        }
        bundleDestination.addTag(this);
        bundleDestinations.add(bundleDestination);
    }

    public boolean removeBundleDestination(BundleDestination bundleDestination) {
        if (bundleDestinations != null) {
            bundleDestination.removeTag(this);
            return bundleDestinations.remove(bundleDestination);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return (namespace != null ? namespace + ":" : "") + (semantic != null ? semantic + "=" : "") + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Tag tag = (Tag) o;

        if (!name.equals(tag.name))
            return false;
        if (namespace != null ? !namespace.equals(tag.namespace) : tag.namespace != null)
            return false;
        if (semantic != null ? !semantic.equals(tag.semantic) : tag.semantic != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + (semantic != null ? semantic.hashCode() : 0);
        result = 31 * result + name.hashCode();
        return result;
    }
}
