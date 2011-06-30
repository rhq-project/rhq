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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * A DriftFile represents one unique piece of content used for drift tracking.  Because the bits are supported
 * by java.sql.Blob and java.io streaming, we must hide the bits from entities used on the gwt client. This base
 * class is used as the superclass for both DriftFile (no blob) and DriftFileBits (blob).
 *  
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 * @author John Sanda 
 */
@MappedSuperclass
@XmlAccessorType(XmlAccessType.FIELD)
public class AbstractDriftFile implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "CTIME", nullable = false)
    protected Long ctime = -1L;

    @Column(name = "DATA_SIZE", nullable = true)
    protected Long dataSize;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    protected DriftFileStatus status = DriftFileStatus.EMPTY;

    protected AbstractDriftFile() {
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

}
