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
package org.rhq.core.domain.content;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Contains the actual package contents ("the bits") for a particular {@link PackageVersion}.
 *
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID, query = "" //        
        + "   SELECT new org.rhq.core.domain.content.composite.LoadedPackageBitsComposite( " //
        + "          pv.id, " //
        + "          pv.fileName, " //
        + "          pv.packageBits.id, "
        + "          (SELECT count(pb.id) FROM pv.packageBits pb WHERE pb.blob.bits IS NOT NULL) " //
        + "          ) " //
        + "   FROM PackageVersion pv " + " WHERE pv.id = :id "),

    // deletes orphaned package bits - that is, if they have no associated package version
    @NamedQuery(name = PackageBits.DELETE_IF_NO_PACKAGE_VERSION, query = "" //
        + " DELETE PackageBits AS pb " //
        + " WHERE pb.id NOT IN ( SELECT pv.packageBits.id " //
        + "                        FROM PackageVersion pv " //
        + "                       WHERE pv.packageBits IS NOT NULL ) ") })
@Table(name = PackageBits.TABLE_NAME)
public class PackageBits implements Serializable {
    public static final String TABLE_NAME = "RHQ_PACKAGE_BITS";

    public static final String QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID = "PackageBits.isLoaded";
    public static final String DELETE_IF_NO_PACKAGE_VERSION = "PackageBits.deleteIfNoPackageVersion";
    /**
     *  Can be used as initial contents for a PackageVersion's PackageBits whenever a predictable non-null
     *  value is required. Use as an initial value for the PackageBits.blob.bits. The value will
     *  typically be replaced with the actual streamed content bits...<br>
     *  Note: This is a String and not a byte[] because gwt can't handle String.getBytes(). 
     */
    public static final String EMPTY_BLOB = " ";

    private static final long serialVersionUID = 1L;

    // Note that the persistance for this table is done through the PackageBitsBlob entity to avoid
    // constraint violations.
    @Column(name = "ID", nullable = false)
    @Id
    private int id;

    // To get lazy load semantics for the Lob field we would need to instrument the class. We can't do that
    // because it introduces hibernate class dependencies into the domain jar. As a workaround, we access
    // the Lob through a required relational mapping *to ourself*.
    // Note: To get Lazy load on xxxToOne mappings "optional=false" must be declared!
    @JoinColumn(name = "ID", referencedColumnName = "ID", nullable = false)
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @XmlTransient
    private PackageBitsBlob blob;

    public PackageBits() {
        // for JPA use
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the blob wrapper. This is never null although the actual bits (PackageBitsBlob.getBits()) can be null.
     * 
     * For large file contents, you should use ContentManager.updateBlobStream() to write and
     * ContentManager.writeBlobOutToStream() to read/direct file contents into as no byte[] is used.
     */
    @XmlTransient
    public PackageBitsBlob getBlob() {
        return blob;
    }

    /**
     * For large file contents, you should use ContentManager.updateBlobStream() to write and 
     * ContentManager.writeBlobOutToStream() to stream the binary bits and avoid a byte[]. 
     */
    public void setBlob(PackageBitsBlob blob) {
        this.blob = blob;
    }
}