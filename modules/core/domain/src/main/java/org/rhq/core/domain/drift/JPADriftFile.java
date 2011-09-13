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

    /*
     * This should be proper JPSQL to delete drift file entities if there are no drift entities referencing them:
     * 
     * DELETE FROM JPADriftFile f
     *  WHERE f.hashId NOT IN (SELECT d1.oldDriftFile FROM JPADrift d1)
     *    AND f.hashId NOT IN (SELECT d2.newDriftFile FROM JPADrift d2)
     *
     * However, Hibernate erroneously translates this to:
     * 
     * delete from RHQ_DRIFT_FILE
     *  where (HASH_ID not in (select jpadrift1_.OLD_DRIFT_FILE from RHQ_DRIFT jpadrift1_))
     *    and (jpadriftfi0_.HASH_ID not in  (select jpadrift2_.NEW_DRIFT_FILE from RHQ_DRIFT jpadrift2_))
     *
     * Notice "jpadriftfi0_" SHOULD be the alias of RHQ_DRIFT_FILE, but it is not being defined by Hibernate.
     * Thus, that alias is unknown and a parse error occurs in the database engine.
     * 
     * Therefore, we need to define a native query that we know works. This should be periodically executed
     * in order to purge unused drift files.
     * 
     * Note that we also add the AND clause to also check for CTIME. 
     */
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
