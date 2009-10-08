/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.core.domain.configuration;

import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.io.Serializable;

@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_RAW_CONFIG_ID_SEQ")
@Table(name = "RAW_CONFIG")
public class RawConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    private int id;

    @Column(name = "PATH")
    private String path;

    @Column(name = "CONTENTS")
    private byte[] contents;

    @Column(name = "VERSION")
    private long version;

    @Column(name = "SHA256")
    private String sha256;

    @Column(name = "CTIME")
    private long ctime = System.currentTimeMillis();

    @Column(name = "MTIME")
    private long mtime = System.currentTimeMillis();
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "CONFIG_ID", nullable = false)
    private Configuration configuration;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public byte[] getContents() {
        return contents;
    }

    public void setContents(byte[] contents) {
        this.contents = contents;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @PrePersist
    void onPersist() {
        mtime = System.currentTimeMillis();
        ctime = System.currentTimeMillis();
    }

    @PreUpdate
    void onUpdate() {
        mtime = System.currentTimeMillis();
    }

}
