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
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Defines a set of configuration values that can be used to deploy a bundle version somewhere. Once set the
 * configuration should not be changed.  Also stores any other deployment settings to be applied to deployments
 * using this def.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries( { @NamedQuery(name = BundleDeployment.QUERY_FIND_ALL, query = "SELECT bd FROM BundleDeployment bd") //
})
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_BUNDLE_DEPLOY_ID_SEQ")
@Table(name = "RHQ_BUNDLE_DEPLOY")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleDeployment implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "BundleDeployment.findAll";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "CTIME")
    private Long ctime = System.currentTimeMillis();

    @Column(name = "MTIME")
    private Long mtime = System.currentTimeMillis();

    @Column(name = "INSTALL_DIR", nullable = false)
    private String installDir;

    @JoinColumn(name = "CONFIG_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    private Configuration configuration;

    @JoinColumn(name = "BUNDLE_VERSION_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private BundleVersion bundleVersion;

    @OneToMany(mappedBy = "bundleDeployment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BundleResourceDeployment> resourceDeployments = new ArrayList<BundleResourceDeployment>();

    @OneToMany(mappedBy = "bundleDeployment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BundleGroupDeployment> groupDeployments = new ArrayList<BundleGroupDeployment>();

    // The following are reserved for future policy work 
    @JoinColumn(name = "BUNDLE_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    private Bundle bundle;

    @Column(name = "ENFORCE_POLICY", nullable = false)
    private boolean enforcePolicy;

    @Column(name = "ENFORCEMENT_INTERVAL", nullable = true)
    private int enforcementInterval;

    public BundleDeployment() {
        // for JPA use
    }

    public BundleDeployment(BundleVersion bundleVersion, String name, String installDir) {
        this.bundleVersion = bundleVersion;
        this.name = name;
        this.installDir = installDir;
        this.enforcePolicy = false;
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

    public String getInstallDir() {
        return installDir;
    }

    public void setInstallDir(String installDir) {
        this.installDir = installDir;
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

    public List<BundleResourceDeployment> getResourceDeployments() {
        return resourceDeployments;
    }

    public void setResourceDeployments(List<BundleResourceDeployment> resourceDeployments) {
        this.resourceDeployments = resourceDeployments;
    }

    public void addResourceDeployment(BundleResourceDeployment resourceDeployment) {
        this.resourceDeployments.add(resourceDeployment);
        resourceDeployment.setBundleDeployment(this);
    }

    public List<BundleGroupDeployment> getGroupDeployments() {
        return groupDeployments;
    }

    public void addGroupDeployment(BundleGroupDeployment groupDeployment) {
        this.groupDeployments.add(groupDeployment);
        groupDeployment.setBundleDeployment(this);
    }

    public void setGroupDeployments(List<BundleGroupDeployment> groupDeployments) {
        this.groupDeployments = groupDeployments;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public boolean isEnforcePolicy() {
        return enforcePolicy;
    }

    public void setEnforcePolicy(boolean enforcePolicy) {
        this.enforcePolicy = enforcePolicy;
    }

    public int getEnforcementInterval() {
        return enforcementInterval;
    }

    public void setEnforcementInterval(int enforcementInterval) {
        this.enforcementInterval = enforcementInterval;
    }

    @Override
    public String toString() {
        return "BundleDeployment[id=" + id + ", name=" + name + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bundleVersion == null) ? 0 : bundleVersion.hashCode());
        // result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((installDir == null) ? 0 : installDir.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BundleDeployment)) {
            return false;
        }

        BundleDeployment other = (BundleDeployment) obj;

        if (bundleVersion == null) {
            if (other.bundleVersion != null) {
                return false;
            }
        } else if (!bundleVersion.equals(other.bundleVersion)) {
            return false;
        }

        //        if (bundle == null) {
        //            if (other.bundle != null) {
        //                return false;
        //            }
        //        } else if (!bundle.equals(other.bundle)) {
        //            return false;
        //        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (installDir == null) {
            if (other.installDir != null) {
                return false;
            }
        } else if (!installDir.equals(other.installDir)) {
            return false;
        }

        return true;
    }
}