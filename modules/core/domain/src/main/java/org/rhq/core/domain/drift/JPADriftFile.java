/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * The JPA Drift Server plugin (the RHQ default) implementation of DriftFile.  Note that JPADriftFile does not
 * include the actual bits, and therefore can be used freely client-side (gwt).  The bits are stored via the
 * JPADriftFileBits class, which is like this one but adds the Blob field. 
 *  
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 * @author John Sanda 
 */
@Entity
@Table(name = "RHQ_DRIFT_FILE")
public class JPADriftFile extends AbstractJPADriftFile implements Serializable, DriftFile {
    private static final long serialVersionUID = 1L;

    /**
     * @deprecated as of 4.13. No longer used.
     */
    @Deprecated
    public static final String NATIVE_DELETE_ORPHANED_DRIFT_FILES = "" //
        + "DELETE FROM RHQ_DRIFT_FILE " //
        + " WHERE (HASH_ID NOT IN (SELECT OLD_DRIFT_FILE FROM RHQ_DRIFT)) " //
        + "   AND (HASH_ID NOT IN (SELECT NEW_DRIFT_FILE FROM RHQ_DRIFT)) " //
        + "   AND CTIME < ?";

    // this is a hash/digest that should uniquely identify the content
    @Id
    @Column(name = "HASH_ID", nullable = false)
    private String hashId;

    protected JPADriftFile() {
        super();
    }

    public JPADriftFile(String hashId) {
        super();
        this.hashId = hashId;
    }

    @Override
    public String getHashId() {
        return hashId;
    }

    @Override
    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    @Override
    public String toString() {
        return "JPADriftFile [hashId=" + hashId + ", status=" + status + "]";
    }

}
