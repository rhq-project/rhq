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
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * @author Jay Shaughnessy
 * @author John Sanda 
 */
@Entity
@Table(name = "RHQ_DRIFT_FILE")
public class DriftFile implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "SHA256", nullable = false)
    private String sha256;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    @Lob
    @Column(name = "DATA", nullable = true)
    private Blob data;

    @Column(name = "DATA_SIZE", nullable = true)
    private Long dataSize;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftFileStatus status = DriftFileStatus.EMPTY;

    protected DriftFile() {
    }

    public DriftFile(String sha256) {
        this.sha256 = sha256;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
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

    public Blob getBlob() {
        return data;
    }

    public InputStream getData() throws SQLException {
        return data.getBinaryStream();
    }

    public void setData(Blob blob) {
        this.data = blob;
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

}
