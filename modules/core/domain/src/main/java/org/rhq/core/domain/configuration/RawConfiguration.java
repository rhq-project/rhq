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

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Resources support structured configuration as well as raw configuration which is represented by this class. A raw
 * configuration is typically in the form of a file on the file system. This could be httpd.conf in the case of apache.
 * <p/>
 * A raw configuration is stored as a CLOB and has a SHA-256 hash with which it can be uniquely identified.
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

    @Lob
    @Column(name = "CONTENTS", nullable = false)
    private String contents;

    @Column(name = "SHA256", nullable = false)
    private String sha256;

    @Column(name = "CTIME", nullable = false)
    private long ctime = System.currentTimeMillis();

    @Column(name = "MTIME", nullable = false)
    private long mtime = System.currentTimeMillis();

    @ManyToOne(optional = false)
    @JoinColumn(name = "CONFIG_ID", nullable = false)
    private Configuration configuration;

    /**
     * THis value is not persisted to the database, but is 
     * set when validation indicates that  there is a problem 
     * with the file structure.
     */
    private String errorMessage;
    
    /** @return The database identifier or primary key */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /** @return The file system path of the configuration that this object represents */
    public String getPath() {
        return path;
    }

    /** @param path The new file system path of the configuration file represented by this object */
    public void setPath(String path) {
        this.path = path;
    }

    /** @return The contents of the raw configuration which typically will be a file */
    public String getContents() {
        return contents;
    }

    /**
     * Replaces the contents of the raw configuration. The SHA-256 hash returned from {@link #getSha256()} will be
     * updated as well.
     *
     * @param contents The new contents
     */
    public void setContents(String contents) {
        this.contents = contents;
        updateSha256();
    }

    private void updateSha256() {
        MessageDigestGenerator sha256Generator = new MessageDigestGenerator("SHA-256");
        sha256Generator.add(contents.getBytes());
        sha256 = sha256Generator.getDigestString();
    }

    /**
     * @return A SHA-256 hash of the contents for this raw configuration, which can be accessed via
     * {@link #getContents()}
     */
    public String getSha256() {
        return sha256;
    }

    /** @return A timestamp of when this object was created */
    public long getCtime() {
        return ctime;
    }

    /** @return A timestamp of when this object was last modified */
    public long getMtime() {
        return mtime;
    }

    /** @return The owning {@link org.rhq.core.domain.configuration.Configuration} object */
    public Configuration getConfiguration() {
        return configuration;
    }

    /** @param configuration The parent configuration object */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
    
    /** @param validation error message */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    /**
     * Returns a string representation of this object that is in the following format,
     *
     * <code>
     * RawConfiguration[id=1, path=/foo/bar/raw.txt, sha256=13xcx9sd82e, configuration=<configuration.toString()>]
     * </code>
     *
     * @return A string representation of this object
     */
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

    /**
     * Creates a deep copy of this object that includes all of the properties except for the parent configuration. If
     * the <code>keepId</code> flag is <code>false</code>, then the id property is not copied.
     *
     * @param keepId A flag indicating whether or not the id should be copied
     * @return A copy of this object
     */
    public RawConfiguration deepCopy(boolean keepId) {
        RawConfiguration copy = new RawConfiguration();
        if (keepId) {
            copy.id = this.id;
        }

        copy.path = this.path;
        
        if (this.contents != null) {
            copy.setContents(this.getContents());
        }

        return copy;
    }


}

