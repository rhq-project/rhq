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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * @author Greg Hinkle
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = Principal.QUERY_FIND_BY_USERNAME, query = "SELECT m FROM Principal AS m WHERE m.principal = :principal"),
    @NamedQuery(name = Principal.QUERY_FIND_ALL_USERS, query = "SELECT m FROM Principal AS m") })
@SequenceGenerator(name = "RHQ_PRINCIPAL_ID_SEQ", sequenceName = "RHQ_PRINCIPAL_ID_SEQ", allocationSize = 10)
@Table(name = "RHQ_PRINCIPAL")
public class Principal implements Serializable {
    public static final String QUERY_FIND_BY_USERNAME = "Principal.findByUsername";
    public static final String QUERY_FIND_ALL_USERS = "Principal.findAllUsers";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_PRINCIPAL_ID_SEQ")
    @Id
    private int id;

    @Column(name = "PRINCIPAL", nullable = false)
    private String principal;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    /* A no-arg constructor is required by the EJB spec, as well as Seam. Note, the EJB spec permits the constructor to
     *be public or protected, whereas Seam requires it to be public. */
    public Principal() {
    }

    /**
     * Constructor for {@link Principal}. Note that the <code>password_md5</code> is not the actual password string; it
     * must be the MD5 hash of the password. See {@link #setPassword(String)}.
     *
     * @param principal
     * @param password_md5
     */
    public Principal(String principal, String password_md5) {
        this.principal = principal;
        this.password = password_md5;
    }

    public int getId() {
        return this.id;
    }

    private void setId(int id) {
        this.id = id;
    }

    public String getPrincipal() {
        return this.principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    /**
     * Returns the principal password's MD5 hash. The password itself is never persisted.
     *
     * @return the password's MD5 hashcode
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the principal password's MD5 hash. Do not pass in the actual password itself, the caller must pass in an MD5
     * hash that was generated from the actual password string using something like: <code>
     * org.jboss.security.Util.createPasswordHash("MD5", "base64", null, null, password);</code>
     *
     * @param password_md5 the MD5 hash representing the actual password
     */
    public void setPassword(String password_md5) {
        this.password = password_md5;
    }

    @Override
    public String toString() {
        return "Principal[id=" + this.id + ", username=" + this.principal + "]";
    }

    @Override
    public int hashCode() {
        return (principal == null) ? 0 : principal.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Principal)) {
            return false;
        }

        final Principal other = (Principal) obj;
        if (principal == null) {
            if (other.principal != null) {
                return false;
            }
        } else if (!principal.equals(other.principal)) {
            return false;
        }

        return true;
    }
}