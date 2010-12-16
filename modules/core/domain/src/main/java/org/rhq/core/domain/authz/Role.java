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
package org.rhq.core.domain.authz;

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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CollectionOfElements;
import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * A role has zero or more {@link org.rhq.core.domain.resource.group.ResourceGroup}s assigned to it. You can assign a
 * role to zero or more {@link Subject}s. A role defines a set of {@link Permission}s that the assigned {@link Subject}s
 * are authorized for in order to operate on the Resources in the assigned {@link
 * org.rhq.core.domain.resource.group.ResourceGroup}s.
 *
 * @author Greg Hinkle
 */
@Entity
@NamedQueries( { //
@NamedQuery(name = Role.QUERY_FIND_BY_IDS, query = "SELECT r FROM Role AS r WHERE r.id IN ( :ids )"), //
    @NamedQuery(name = Role.QUERY_FIND_ALL, query = "SELECT r FROM Role AS r"), //
    @NamedQuery(name = Role.QUERY_FIND_AVAILABLE_ROLES_WITH_EXCLUDES, query = "" //
        + "   SELECT DISTINCT r " //
        + "     FROM Role AS r " //
        + "LEFT JOIN r.subjects AS s " //
        + "    WHERE r.id NOT IN ( SELECT rr.id " //
        + "                          FROM Subject ss " //
        + "                          JOIN ss.roles AS rr " //
        + "                         WHERE ss.id = :subjectId ) " //
        + "      AND r.id NOT IN ( :excludes )"), //
    @NamedQuery(name = Role.QUERY_FIND_AVAILABLE_ROLES, query = "" //
        + "   SELECT DISTINCT r " //
        + "     FROM Role AS r " //
        + "LEFT JOIN r.subjects AS s " //
        + "    WHERE r.id NOT IN ( SELECT rr.id " //
        + "                          FROM Subject ss " //
        + "                          JOIN ss.roles AS rr " //
        + "                         WHERE ss.id = :subjectId )"), //
    @NamedQuery(name = Role.QUERY_DYNAMIC_CONFIG_VALUES, query = "" //
        + "SELECT r.name, r.name FROM Role AS r") })
@SequenceGenerator(name = "RHQ_ROLE_ID_SEQ", sequenceName = "RHQ_ROLE_ID_SEQ")
@Table(name = "RHQ_ROLE")
public class Role implements Serializable {
    public static final String QUERY_FIND_ALL = "Role.findAll";
    public static final String QUERY_FIND_BY_IDS = "Role.findByIds";
    public static final String QUERY_FIND_AVAILABLE_ROLES_WITH_EXCLUDES = "Role.findAvailableRolesWithExcludes";
    public static final String QUERY_FIND_AVAILABLE_ROLES = "Role.findAvailableRoles";

    public static final String QUERY_DYNAMIC_CONFIG_VALUES = "Role.dynamicConfigValues";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_ROLE_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "FSYSTEM")
    private Boolean fsystem;

    @ManyToMany(mappedBy = "roles")
    private Set<Subject> subjects = new HashSet<Subject>();

    @ManyToMany(mappedBy = "ldapRoles")
    private Set<Subject> ldapSubjects = new HashSet<Subject>();

    @OneToMany(mappedBy = "role", cascade = { CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.REFRESH })
    private Set<LdapGroup> ldapGroups = new HashSet<LdapGroup>();

    @ManyToMany(mappedBy = "roles")
    private Set<ResourceGroup> resourceGroups = new HashSet<org.rhq.core.domain.resource.group.ResourceGroup>();

    @Cascade( { org.hibernate.annotations.CascadeType.ALL })
    @CollectionOfElements(fetch = FetchType.EAGER)
    @Column(name = "operation")
    @JoinTable(name = "RHQ_PERMISSION", joinColumns = @JoinColumn(name = "ROLE_ID"))
    private Set<Permission> permissions = new HashSet<Permission>();

    public Role() {
        fsystem = Boolean.FALSE;
    }

    public Role(@NotNull String name) {
        this();
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getFsystem() {
        return this.fsystem;
    }

    public void setFsystem(Boolean fsystem) {
        this.fsystem = fsystem;
    }

    public Set<Permission> getPermissions() {
        return this.permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions.clear();
        if (permissions != null) {
            this.permissions.addAll(permissions);
        }
    }

    public void addPermission(Permission permission) {
        // permission.setRole(this);
        this.permissions.add(permission);
    }

    public boolean removePermission(Permission permission) {
        return this.permissions.remove(permission);
    }

    public Set<LdapGroup> getLdapGroups() {
        if (ldapGroups == null) {
            ldapGroups = new HashSet<LdapGroup>();
        }
        return this.ldapGroups;
    }

    public void setLdapGroups(Set<LdapGroup> groups) {
        this.ldapGroups = groups;
    }

    public void addLdapGroup(LdapGroup ldapGroup) {
        ldapGroup.setRole(this);
        this.ldapGroups.add(ldapGroup);
    }

    public boolean removeLdapGroup(LdapGroup ldapGroup) {        
        return this.ldapGroups.remove(ldapGroup);
    }

    public java.util.Set<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(Set<Subject> subjects) {
        if (subjects == null) {
            this.subjects = new HashSet<Subject>();
        } else {
            this.subjects = subjects;
            for (Subject subject :subjects) {
                subject.addRole(this);
                this.subjects.add(subject);
            }
        }
    }

    public void addSubject(Subject subject) {
        if (this.subjects == null) {
            this.subjects = new HashSet<Subject>();
        }

        subject.addRole(this);
        this.subjects.add(subject);
    }

    public void removeSubject(Subject subject) {
        if (this.subjects == null) {
            this.subjects = new HashSet<Subject>();
        }

        subject.removeRole(this);
        this.subjects.remove(subject);
    }

    public Set<Subject> getLdapSubjects() {
        return ldapSubjects;
    }

    public void setLdapSubjects(Set<Subject> subjects) {
        this.ldapSubjects = subjects;
    }

    public void addLdapSubject(Subject subject) {
        if (this.ldapSubjects == null) {
            this.ldapSubjects = new HashSet<Subject>();
        }

        subject.addLdapRole(this);
        this.ldapSubjects.add(subject);
    }

    public void removeLdapSubject(Subject subject) {
        if (this.ldapSubjects == null) {
            this.ldapSubjects = new HashSet<Subject>();
        }

        subject.removeLdapRole(this);
        this.ldapSubjects.remove(subject);
    }

    public Set<ResourceGroup> getResourceGroups() {
        return resourceGroups;
    }

    public void setResourceGroups(Set<ResourceGroup> resourceGroups) {
        if (resourceGroups == null) {
            this.resourceGroups = new HashSet<ResourceGroup>();
        } else {
            this.resourceGroups = resourceGroups;
            for (ResourceGroup resourceGroup : this.resourceGroups) {
                resourceGroup.addRole(this);
                this.resourceGroups.add(resourceGroup);
            }
        }
    }

    public void addResourceGroup(ResourceGroup resourceGroup) {
        if (this.resourceGroups == null) {
            this.resourceGroups = new HashSet<ResourceGroup>();
        }

        resourceGroup.addRole(this);
        this.resourceGroups.add(resourceGroup);
    }

    public void removeResourceGroup(ResourceGroup resourceGroup) {
        if (this.resourceGroups == null) {
            this.resourceGroups = new HashSet<ResourceGroup>();
        }

        resourceGroup.removeRole(this);
        this.resourceGroups.remove(resourceGroup);
    }

    public int getMemberCount() {
        int count = 0;
        for (Subject member : getSubjects()) {
            if (member.getFsystem() == false && member.getFactive() == true) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        return "Role[id=" + this.id + ", name=" + this.name + ", permissions=" + this.permissions + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if ((o == null) || !(o instanceof Role)) {
            return false;
        }

        Role role = (Role) o;

        return name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
