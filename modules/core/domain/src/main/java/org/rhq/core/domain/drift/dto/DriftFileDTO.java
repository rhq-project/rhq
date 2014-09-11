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

package org.rhq.core.domain.drift.dto;

import java.io.Serializable;

import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.DriftFileStatus;

public class DriftFileDTO implements Serializable, DriftFile {

    private static final long serialVersionUID = 1L;

    private String hash;

    private Long ctime;

    private Long size;

    private DriftFileStatus status;

    @Override
    public String getHashId() {
        return hash;
    }

    @Override
    public void setHashId(String hashId) {
        hash = hashId;
    }

    @Override
    public Long getCtime() {
        return ctime;
    }

    @Override
    public Long getDataSize() {
        return size;
    }

    @Override
    public void setDataSize(Long size) {
        this.size = size;
    }

    @Override
    public DriftFileStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(DriftFileStatus status) {
        this.status = status;
    }
}
