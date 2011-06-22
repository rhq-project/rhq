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
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * A DriftFile represents one unique piece of content used for drift tracking.  Note that DriftFile does not
 * include the actual bits, and therefore can be used freely client-side (gwt).  The bits are stored via the
 * DriftFileContent sub-class, which adds only a Blob field. 
 *  
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@DiscriminatorColumn(name = "DTYPE")
@DiscriminatorValue("no-content")
@Entity
@Table(name = "RHQ_DRIFT_FILE")
public class DriftFile implements Serializable {
    private static final long serialVersionUID = 1L;

    // this is a hash/digest that should uniquely identify the content
    @Id
    @Column(name = "HASH_ID", nullable = false)
    private String hashId;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    @Column(name = "DATA_SIZE", nullable = true)
    private Long dataSize;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftFileStatus status = DriftFileStatus.EMPTY;

    protected DriftFile() {
    }

    public DriftFile(String hashId) {
        this.hashId = hashId;
    }

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public Long getCtime() {
        return ctime;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    public void setDataSize(Long dataSize) {
        this.dataSize = dataSize;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long size) {
        dataSize = size;
    }

    public DriftFileStatus getStatus() {
        return status;
    }

    public void setStatus(DriftFileStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DriftFile [hashId=" + hashId + ", status=" + status + "]";
    }

}
