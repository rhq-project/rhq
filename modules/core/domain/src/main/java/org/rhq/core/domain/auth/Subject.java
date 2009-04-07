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
package org.rhq.core.domain.auth;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.QueryHint;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.alert.notification.SubjectNotification;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Greg Hinkle
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = Subject.QUERY_FIND_BY_IDS, query = "" //
        + "SELECT s " //
        + "  FROM Subject s " //
        + " WHERE s.id IN ( :ids ) " //
        + "   AND s.fsystem = FALSE " //
        + "   AND s.factive = TRUE"),
    @NamedQuery(name = Subject.QUERY_FIND_ALL, query = "" //
        + "SELECT s " //
        + "  FROM Subject s " //
        + " WHERE s.fsystem = false"),
    @NamedQuery(name = Subject.QUERY_FIND_BY_NAME, query = "" //
        + "SELECT s " //
        + "  FROM Subject s " //
        + " WHERE s.name = :name"),
    @NamedQuery(name = Subject.QUERY_GET_SUBJECTS_ASSIGNED_TO_ROLE, query = "" //
        + "SELECT s " //
        + "  FROM Subject s " //
        + "  JOIN s.roles r " //
        + " WHERE r.id = :id " //
        + "   AND s.fsystem = FALSE " //
        + "   AND s.factive = TRUE"),

    /* AuthorizationManager queries */
    @NamedQuery(name = Subject.QUERY_GET_GLOBAL_PERMISSIONS, hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "security") }, query = "SELECT distinct p "
        + "FROM Subject AS s, IN (s.roles) r, IN (r.permissions) p " + "WHERE s = :subject"),

    @NamedQuery(name = Subject.QUERY_HAS_GLOBAL_PERMISSION, hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true"),
        @QueryHint(name = "org.hibernate.cacheRegion", value = "security") }, query = "SELECT COUNT(p) "
        + "FROM Subject AS s, IN (s.roles) r, IN (r.permissions) p " + "WHERE s = :subject AND p = :permission"),

    @NamedQuery(name = Subject.QUERY_GET_PERMISSIONS_BY_GROUP_ID, query =
            "SELECT DISTINCT p " +
            "FROM Role r JOIN r.subjects s JOIN r.permissions p " +
            "WHERE " +
            "  (" +
            "    r in (SELECT r2 from ResourceGroup g JOIN g.roles r2 WHERE g.id = :groupId) " +
            "    OR r in (SELECT r3 from ResourceGroup g JOIN g.clusterResourceGroup crg JOIN crg.roles r3 WHERE g.id = :groupId AND crg.recursive = true) " +
            "  ) " +
            "  AND s = :subject"),

    @NamedQuery(name = Subject.QUERY_HAS_GROUP_PERMISSION, query =
            "SELECT count(r) " +
            "FROM Role r JOIN r.subjects s JOIN r.permissions p " +
            "WHERE " +
            "  (" +
            "    r in (SELECT r2 from ResourceGroup g JOIN g.roles r2 WHERE g.id = :groupId) " +
            "    OR r in (SELECT r3 from ResourceGroup g JOIN g.clusterResourceGroup crg JOIN crg.roles r3 WHERE g.id = :groupId AND crg.recursive = true) " +
            "  ) " +
            "  AND s = :subject " +
            "  AND p = :permission"),

    @NamedQuery(name = Subject.QUERY_GET_PERMISSIONS_BY_RESOURCE_ID, query = "SELECT distinct p "
        + "FROM Resource res, IN (res.implicitGroups) g, IN (g.roles) r, IN (r.subjects) s, IN (r.permissions) p "
        + "WHERE s = :subject AND res.id = :resourceId"),

    @NamedQuery(name = Subject.QUERY_HAS_RESOURCE_PERMISSION, query = "SELECT COUNT(res) "
        + "FROM Resource res, IN (res.implicitGroups) g, IN (g.roles) r, IN (r.subjects) s, IN (r.permissions) p "
        + "WHERE s = :subject AND res.id = :resourceId AND p = :permission"),

    @NamedQuery(name = Subject.QUERY_CAN_VIEW_RESOURCE, query = "SELECT COUNT(res) "
        + "FROM Resource res, IN (res.implicitGroups) g, IN (g.roles) r, IN (r.subjects) s "
        + "WHERE s = :subject AND res.id = :resourceId"),

    @NamedQuery(name = Subject.QUERY_CAN_VIEW_GROUP, query =
            "SELECT count(g) " +
            "FROM ResourceGroup g " +
            "WHERE (g.id IN (SELECT rg.id " +
            "                  FROM ResourceGroup rg " +
            "                  JOIN rg.roles r " +
            "                  JOIN r.subjects s " +
            "                 WHERE s = :subject) " +
            "    OR g.id IN (SELECT rg.id " +
            "                  FROM ResourceGroup rg " +
            "                  JOIN rg.clusterResourceGroup crg " +
            "                  JOIN crg.roles r " +
            "                  JOIN r.subjects s " +
            "                 WHERE crg.recursive = true AND s = :subject)) " +
            "    AND g.id = :groupId"),

    /*
     * No easy way to test whether ALL resources are      in some group     in some role     in some subject     where
     * subject.id = <id> & role.permission = <perm>
     *
     * Instead, we must use this potentially VERY costly resource query (costly because the result list could be huge in large
     * environments).  However, we can return res.id only, to save a lot of traffic across the line and speed it up.
     */
    @NamedQuery(name = Subject.QUERY_GET_RESOURCES_BY_PERMISSION, query = "SELECT distinct res.id "
        + "FROM Subject s, IN (s.roles) r, IN (r.permissions) p, IN (r.resourceGroups) g, IN (g.implicitResources) res "
        + "WHERE s = :subject AND p = :permission"),

    @NamedQuery(name = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ALERT_DEFINITION_WITH_EXCLUDES, query = "" //
        + "SELECT s" + "  FROM Subject s" //
        + " WHERE s.id NOT IN" //
        + "       ( " //
        + "         SELECT sn.subject.id" //
        + "           FROM SubjectNotification sn" //
        + "          WHERE sn.alertDefinition.id = :alertDefinitionId " //
        + "       ) " //
        + "   AND s.id NOT IN ( :excludes ) " //
        + "   AND s.fsystem = FALSE " //
        + "   AND s.factive = TRUE"),
    @NamedQuery(name = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ALERT_DEFINITION, query = "" //
        + "SELECT s" //
        + "  FROM Subject s" //
        + " WHERE s.id NOT IN" //
        + "       ( " //
        + "         SELECT sn.subject.id" //
        + "           FROM SubjectNotification sn" //
        + "          WHERE sn.alertDefinition.id = :alertDefinitionId " //
        + "       ) " //
        + "   AND s.fsystem = FALSE" //
        + "   AND s.factive = TRUE"), //
    @NamedQuery(name = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ROLE_WITH_EXCLUDES, query = "" //
        + "SELECT DISTINCT s " + "  FROM Subject AS s LEFT JOIN s.roles AS r " //
        + " WHERE s.id NOT IN " //
        + "      ( " //
        + "        SELECT ss.id " //
        + "          FROM Role rr JOIN rr.subjects AS ss " //
        + "          WHERE rr.id = :roleId" //
        + "      ) " //
        + "  AND s.id NOT IN ( :excludes ) " //
        + "  AND s.fsystem = FALSE " //
        + "  AND s.factive = TRUE"), //
    @NamedQuery(name = Subject.QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ROLE, query = "" //
        + "SELECT DISTINCT s " //
        + "  FROM Subject AS s LEFT JOIN s.roles AS r " //
        + " WHERE s.id NOT IN " //
        + "       ( " //
        + "         SELECT ss.id " //
        + "         FROM Role rr JOIN rr.subjects AS ss " //
        + "         WHERE rr.id = :roleId" + "       ) " //
        + "   AND s.fsystem = FALSE " //
        + "   AND s.factive = TRUE") })
@SequenceGenerator(name = "RHQ_SUBJECT_ID_SEQ", sequenceName = "RHQ_SUBJECT_ID_SEQ")
@Table(name = "RHQ_SUBJECT")
/*@Cache(usage= CacheConcurrencyStrategy.TRANSACTIONAL)*/
@XmlSeeAlso( { Property.class, PropertySimple.class, PropertyList.class, PropertyMap.class })
public class Subject implements Externalizable {
    public static final String QUERY_FIND_ALL = "Subject.findAll";
    public static final String QUERY_FIND_BY_IDS = "Subject.findByIds";
    public static final String QUERY_FIND_BY_NAME = "Subject.findByName";

    public static final String QUERY_GET_SUBJECTS_ASSIGNED_TO_ROLE = "Subject.getSubjectsAssignedToRole";

    public static final String QUERY_GET_GLOBAL_PERMISSIONS = "Subject.getGlobalPermissions";
    public static final String QUERY_GET_PERMISSIONS_BY_GROUP_ID = "Subject.getPermissionsByGroup";
    public static final String QUERY_GET_PERMISSIONS_BY_RESOURCE_ID = "Subject.getPermissionsByResource";

    public static final String QUERY_HAS_GLOBAL_PERMISSION = "Subject.hasGlobalPermission";
    public static final String QUERY_HAS_GROUP_PERMISSION = "Subject.hasGroupPermission";
    public static final String QUERY_HAS_RESOURCE_PERMISSION = "Subject.hasResourcePermission";

    public static final String QUERY_CAN_VIEW_RESOURCE = "Subject.canViewResource";
    public static final String QUERY_CAN_VIEW_GROUP = "Subject.canViewGroup";

    public static final String QUERY_GET_RESOURCES_BY_PERMISSION = "Subject.getResourcesByPermission";

    public static final String QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ROLE_WITH_EXCLUDES = "Subject.findAvailableSubjectsForRoleWithExcludes";
    public static final String QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ROLE = "Subject.findAvailableSubjectsForRole";
    public static final String QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ALERT_DEFINITION_WITH_EXCLUDES = "Subject.findAvailableSubjectsForAlertDefinitionWithExcludes";
    public static final String QUERY_FIND_AVAILABLE_SUBJECTS_FOR_ALERT_DEFINITION = "Subject.findAvailableSubjectForAlertDefinition";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_SUBJECT_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "EMAIL_ADDRESS")
    private String emailAddress;

    @Column(name = "SMS_ADDRESS")
    private String smsAddress;

    @Column(name = "PHONE_NUMBER")
    private String phoneNumber;

    @Column(name = "DEPARTMENT")
    private String department;

    @Column(name = "FACTIVE", nullable = false)
    private boolean factive;

    @Column(name = "FSYSTEM", nullable = false)
    private boolean fsystem;

    @JoinColumn(name = "CONFIGURATION_ID")
    @OneToOne(cascade = { CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE })
    private Configuration configuration;

    @JoinTable(name = "RHQ_SUBJECT_ROLE_MAP", joinColumns = { @JoinColumn(name = "SUBJECT_ID") }, inverseJoinColumns = { @JoinColumn(name = "ROLE_ID") })
    @ManyToMany
    private java.util.Set<Role> roles;

    @OneToMany(mappedBy = "subject", cascade = javax.persistence.CascadeType.ALL)
    private Set<SubjectNotification> subjectNotifications = new HashSet<SubjectNotification>();

    @Transient
    private Integer sessionId = null;

    private void init() {
        roles = new HashSet<Role>();
    }

    /**
     * Creates a new instance of Subject
     */
    public Subject() {
        init();
    }

    public Subject(@NotNull String name, boolean factive, boolean fsystem) {
        init();
        this.name = name;
        this.factive = factive;
        this.fsystem = fsystem;
    }

    public int getId() {
        return this.id;
    }

    /**
     * When a user successfully logs in, the user will be assigned a session ID. This is that session ID - when not
     * <code>null</code>, you can assume this user has been authenticated and is currently logged into the system.
     *
     * @param sessionId
     */
    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * If not <code>null</code>, you can assume the user associated with this Subject has been authenticated.
     *
     * @return the logged in user's session ID
     */
    public Integer getSessionId() {
        return this.sessionId;
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

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmailAddress() {
        return this.emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getSmsAddress() {
        return this.smsAddress;
    }

    public void setSmsAddress(String smsAddress) {
        this.smsAddress = smsAddress;
    }

    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDepartment() {
        return this.department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean getFactive() {
        return this.factive;
    }

    public void setFactive(boolean factive) {
        this.factive = factive;
    }

    public boolean getFsystem() {
        return this.fsystem;
    }

    public void setFsystem(boolean fsystem) {
        this.fsystem = fsystem;
    }

    public Configuration getUserConfiguration() {
        return this.configuration;
    }

    public void setUserConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public java.util.Set<Role> getRoles() {
        if (this.roles == null) {
            this.roles = new HashSet<Role>();
        }

        return this.roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void addRole(Role role) {
        getRoles().add(role);
    }

    public void removeRole(Role role) {
        getRoles().remove(role);
    }

    public Set<SubjectNotification> getSubjectNotifications() {
        return subjectNotifications;
    }

    public void setSubjectNotifications(Set<SubjectNotification> subjectNotifications) {
        this.subjectNotifications = subjectNotifications;
    }

    public void addSubjectNotification(SubjectNotification subjectNotification) {
        this.subjectNotifications.add(subjectNotification);
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.auth.Subject[id=" + id + ",name=" + name + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = (PRIME * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof Subject)) {
            return false;
        }

        final Subject other = (Subject) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(this.id);
        out.writeUTF(this.name);
        out.writeInt(this.sessionId);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readInt();
        this.name = in.readUTF();
        this.sessionId = in.readInt();
    }
}