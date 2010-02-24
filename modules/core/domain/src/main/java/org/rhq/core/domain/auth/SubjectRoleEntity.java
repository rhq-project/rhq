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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import org.rhq.core.domain.authz.Role;

@Entity
@IdClass(SubjectRolePK.class)
@Table(name = "RHQ_SUBJECT_ROLE_MAP")
public class SubjectRoleEntity implements Serializable {

    @Id
    private Subject subject;

    @Id
    private Role role;

    @Column(name = "IS_LDAP")
    private boolean isLdap;

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isLdap() {
        return isLdap;
    }

    public void setLdap(boolean isLdap) {
        this.isLdap = isLdap;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = (PRIME * result) + ((subject == null) ? 0 : subject.hashCode());
        result = (PRIME * result) + ((role == null) ? 0 : role.hashCode());
        result = (PRIME * result) + ((isLdap) ? 0 : PRIME);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof SubjectRoleEntity)) {
            return false;
        }

        final SubjectRoleEntity other = (SubjectRoleEntity) obj;
        if (subject == null) {
            if (other.subject != null) {
                return false;
            }
        } else if (!subject.equals(other.subject)) {
            return false;
        }

        if (role == null) {
            if (other.role != null) {
                return false;
            }
        } else if (!role.equals(other.role)) {
            return false;
        }

        return isLdap == other.isLdap;
    }
}
