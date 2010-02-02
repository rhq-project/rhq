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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Defines a type of bundle that can exist in the system. Bundle types are used to determine
 * how a bundle is to be processed.
 *
 * @author John Mazzitelli
 */
@Entity
@NamedQueries( { @NamedQuery(name = BundleType.QUERY_FIND_ALL, query = "SELECT bt FROM BundleType bt"), //
    @NamedQuery(name = BundleType.QUERY_FIND_BY_NAME, query = "SELECT bt FROM BundleType bt WHERE bt.name = :name") //
})
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_BUNDLE_TYPE_ID_SEQ")
@Table(name = "RHQ_BUNDLE_TYPE")
@XmlAccessorType(XmlAccessType.FIELD)
public class BundleType implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_ALL = "BundleType.findAll";
    public static final String QUERY_FIND_BY_NAME = "BundleType.findByName";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    public BundleType() {
        // for JPA use
    }

    public BundleType(String name) {
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

    @Override
    public String toString() {
        return "BundleType[id=" + id + ",name=" + name + "]";
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

        if (!(obj instanceof BundleType)) {
            return false;
        }

        final BundleType other = (BundleType) obj;

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