/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.authz.Role;

/**
 * Defines a grouping of bundles, typically for role-based authz reasons.
 *
 * @author Jay Shaughnessy
 */
@Entity
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_BUNDLE_GROUP_ID_SEQ", sequenceName = "RHQ_BUNDLE_GROUP_ID_SEQ")
@Table(name = "RHQ_BUNDLE_GROUP")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_BUNDLE_GROUP_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "CTIME")
    private Long ctime;

    @Column(name = "MTIME")
    private Long mtime;

    @JoinTable(name = "RHQ_BUNDLE_GROUP_BUNDLE_MAP", joinColumns = { @JoinColumn(name = "BUNDLE_GROUP_ID") }, inverseJoinColumns = { @JoinColumn(name = "BUNDLE_ID") })
    @ManyToMany
    private Set<Bundle> bundles;

    @ManyToMany(mappedBy = "bundleGroups", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<Role> roles;

    public BundleGroup() {
        // for JPA use
    }

    public BundleGroup(String name) {
        setName(name);
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

    public Set<Bundle> getBundles() {
        if (null == bundles) {
            bundles = new HashSet<Bundle>();
        }
        return bundles;
    }

    /**
     * This also updates the inverse relation (add this bundle group to bundle)
     * @param bundle
     */
    public void addBundle(Bundle bundle) {
        getBundles().add(bundle);
        bundle.addBundleGroup(this);
    }

    /**
     * This also updates the inverse relation (remove this bundle group from bundle)
     * @param bundle
     * @return true if bundle was removed, otherwise false
     */
    public boolean removeBundle(Bundle bundle) {
        boolean result = getBundles().remove(bundle);
        bundle.removeBundleGroup(this);
        return result;
    }

    /**
     * This also updates the inverse relations
     * @param bundles
     */
    public void setBundles(Set<Bundle> bundles) {
        for (Bundle bundle : getBundles()) {
            bundle.removeBundleGroup(this);
        }

        this.bundles.clear();

        if (null != bundles) {
            for (Bundle bundle : bundles) {
                addBundle(bundle);
            }
        }
    }

    public Set<Role> getRoles() {
        if (null == roles) {
            roles = new HashSet<Role>();
        }
        return roles;
    }

    /**
     * This also updates the inverse relation (add this bundle group to role)
     * @param role
     */
    public void addRole(Role role) {
        getRoles().add(role);
        role.addBundleGroup(this);
    }

    /**
     * This also updates the inverse relation (remove this bundle group from role)
     * @param role
     * @return true if role was removed, otherwise false
     */
    public boolean removeRole(Role role) {
        role.removeBundleGroup(this);
        return getRoles().remove(role);
    }

    public Long getCtime() {
        return ctime;
    }

    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    public Long getMtime() {
        return mtime;
    }

    public void setMtime(Long mtime) {
        this.mtime = mtime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = System.currentTimeMillis();
        this.ctime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "BundleGroup[id=" + id + ",name=" + name + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof BundleGroup)) {
            return false;
        }

        final BundleGroup other = (BundleGroup) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }
}