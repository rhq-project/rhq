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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A DriftFile represents one unique piece of content used for drift tracking.  Note that DriftFile does not
 * include the actual bits, and therefore can be used freely client-side (gwt).  The bits are stored via the
 * DriftFileBits class, which is like this one but adds the Blob field. 
 *  
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 * @author John Sanda 
 */
@Entity
@Table(name = "RHQ_DRIFT_FILE")
public class DriftFile extends AbstractDriftFile implements Serializable {
    private static final long serialVersionUID = 1L;

    // this is a hash/digest that should uniquely identify the content
    @Id
    @Column(name = "HASH_ID", nullable = false)
    private String hashId;

    protected DriftFile() {
        super();
    }

    public DriftFile(String hashId) {
        super();
        this.hashId = hashId;
    }

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    @Override
    public String toString() {
        return "DriftFile [hashId=" + hashId + ", status=" + status + "]";
    }

}
