/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

import java.io.InputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

/**
 * A DriftFile represents onee unique piece of content used for drift tracking.  Note that DriftFileContent provides
 * access to the bits through java.sql.Blob. This entity can not be used client-side (gwt) whereas the super-class,
 * DriftFile can. See RHQDomain.gwt.xml for how to exclude unwanted domain classes from the gwt compile.
 *
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@DiscriminatorValue("content")
@Entity
public class DriftFileContent extends DriftFile implements Serializable {
    private static final long serialVersionUID = 1L;

    @Lob
    @Column(name = "DATA", nullable = true)
    private Blob data;

    protected DriftFileContent() {
    }

    public Blob getBlob() {
        return data;
    }

    public InputStream getData() throws SQLException {
        return data.getBinaryStream();
    }

    public void setData(Blob blob) {
        this.data = blob;
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
