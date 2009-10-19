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

import org.rhq.core.util.MessageDigestGenerator;

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
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * Resources support structured configuration as well as raw configuration which is represented by this class. A raw
 * configuration is typically in the form of a file on the file system. This could be httpd.conf in the case of apache.
 * Note however that while a raw configuration generally refers to some configuration file, this class does not limit
 * the actual configuration source to files.
 * <p/>
 * A raw configuration is stored as an array of bytes and has a SHA-256 hash with which it can be uniquely identified.
 * <p/>
 * A RawConfiguration is always associated with its parent {@link org.rhq.core.domain.configuration.Configuration} which
 * can be structured, raw, or both. A Configuration can have multiple RawConfigurations associated with it. Suppose for
 * apache that each virtual host configuration is stored in a separate file. We might have a single Configuration object
 * that represents all of the apache configuration, and that object may contain multiple RawConfigurations for each of
 * the virtual host config files.
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_RAW_CONFIG_ID_SEQ")
@Table(name = "RHQ_RAW_CONFIG")
public class RawConfiguration implements Serializable, DeepCopyable<RawConfiguration> {

    private static final long serialVersionUID = 1L;

    @GeneratedValue(generator = "SEQ", strategy = GenerationType.AUTO)
    @Id
    @Column(name = "ID")
    private int id;

    @Column(name = "PATH", nullable = true)
    private String path;

    @Column(name = "CONTENTS", nullable = false)
    private byte[] contents;

    @Column(name = "SHA256", nullable = false)
    private String sha256;

    @Column(name = "CTIME", nullable = false)
    private long ctime = System.currentTimeMillis();

    @Column(name = "MTIME", nullable = false)
    private long mtime = System.currentTimeMillis();
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "CONFIG_ID", nullable = false)
    private Configuration configuration;

    @Transient
    private MessageDigestGenerator sha256Generator = new MessageDigestGenerator("SHA-256");

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

    /**
     * Returns a copy of the contents as an array of bytes. Modifications to the underlying array have to happen through
     * {@link #setContents(byte[])}; otherwise, we could wind up with an incorrect SHA-256 hash. This behavior is
     * enforced by returning a copy instead of a reference to the underlying array. By returning a copy, callers can only
     * modify the underlying array by calling the setter.
     *
     * @return A copy of the file contents as an array of bytes
     */
    public byte[] getContents() {
        return copy(contents);
    }

    /**
     * Replaces the contents of this raw config with a copy of the specified bytes. The SHA-256 hash returned from
     * {@link #getSha256()} will be changed as well, provided the contents actually changed in some way.
     *
     * @param newContents The new bytes
     */
    public void setContents(byte[] newContents) {
        this.contents = copy(newContents);
        updateSha256();
    }

    private byte[] copy(byte[] original) {
        byte[] copy = new byte[original.length];
        for (int i = 0; i < original.length; ++i) {
            copy[i] = original[i];
        }
        return copy;
    }

    private void updateSha256() {
        sha256Generator.add(contents);
        sha256 = sha256Generator.getDigestString();
    }

    /** @return A SHA-256 hash of the bytes for this raw configuration, which can be accessed via {@link #getContents()} */
    public String getSha256() {
        return sha256;
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

    /** @return The owning {@link org.rhq.core.domain.configuration.Configuration} object */
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

    /**
     * Two RawConfiguration objects are considered equal when the following conditions hold:
     * <ul>
     *   <li>Both have the same sha256 and path property is null for both or</li>
     *   <li>Both have the same sha256, path property is non-null and equal for both</li>
     * </ul>
     *
     * <strong>Note:</strong> This definition of equality holdsonly when comparing RawConfigurations belonging to
     * the same resource.
     *
     * @param obj The object to compare for equality
     * @return true if obj is a RawConfiguration and has the same values for the sha256 and path properties.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof RawConfiguration) {
            RawConfiguration that = (RawConfiguration) obj;
            if (this.sha256 != null && this.sha256.equals(that.sha256)) {
                if (this.path == null && that.path == null) {
                    return true;
                }
                if ((this.path !=null && that.path != null) && this.path.equals(that.path)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**@return A hash which is calculated from the sha256 and path properties. */
    @Override
    public int hashCode() {
        if (sha256 == null) {
            return 0;
        }

        if (path == null) {
            return sha256.hashCode() * 37;
        }

        return sha256.hashCode() * path.hashCode() * 37;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append(getClass().getSimpleName())
            .append("[id=").append(id)
            .append(", path=").append(path)
            .append(", sha256=").append(sha256)
            .append(", configuration=").append(configuration)
            .append("]")
            .toString();
    }

    public RawConfiguration deepCopy() {
        RawConfiguration copy = new RawConfiguration();
        copy.path = this.path;
        if (this.contents != null) {
            copy.contents = this.getContents();
        }

        return copy;
    }
}
