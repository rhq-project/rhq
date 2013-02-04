/*
 *
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 *
 */

package org.rhq.server.metrics.domain;

/**
 * @author John Sanda
 */
public class ColumnMetadata {

    private Integer ttl;

    private Long writeTime;

    public ColumnMetadata(Integer ttl, Long writeTime) {
        this.ttl = ttl;
        this.writeTime = writeTime;
    }

    public Integer getTtl() {
        return ttl;
    }

    public Long getWriteTime() {
        return writeTime;
    }

    @Override
    public String toString() {
        return "ColumnMetadata[ttl=" + ttl + ", writeTime=" + writeTime + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColumnMetadata that = (ColumnMetadata) o;

        if (ttl != null ? !ttl.equals(that.ttl) : that.ttl != null) return false;
        if (!writeTime.equals(that.writeTime)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ttl != null ? ttl.hashCode() : 0;
        result = 31 * result + writeTime.hashCode();
        return result;
    }
}
