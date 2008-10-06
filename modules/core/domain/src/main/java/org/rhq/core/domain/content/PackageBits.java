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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Contains the actual package contents ("the bits") for a particular {@link PackageVersion}.
 *
 * @author John Mazzitelli
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = PackageBits.QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID, query = "SELECT new org.rhq.core.domain.content.composite.LoadedPackageBitsComposite( "
        + "          pv.id, "
        + "          pv.fileName, "
        + "          pv.packageBits.id, "
        + "          (SELECT count(pb.id) FROM pv.packageBits pb WHERE pb.bits IS NOT NULL) "
        + "       ) "
        + "  FROM PackageVersion pv " + " WHERE pv.id = :id "),

    // deletes orphaned package bits - that is, if they have no associated package version
    @NamedQuery(name = PackageBits.DELETE_IF_NO_PACKAGE_VERSION, query = "DELETE PackageBits AS pb "
        + " WHERE pb.id NOT IN ( SELECT pv.packageBits.id " + "                        FROM PackageVersion pv "
        + "                       WHERE pv.packageBits IS NOT NULL ) ") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PACKAGE_BITS_ID_SEQ")
@Table(name = PackageBits.TABLE_NAME)
public class PackageBits implements Serializable {
    public static final String TABLE_NAME = "RHQ_PACKAGE_BITS";

    public static final String QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID = "PackageBits.isLoaded";
    public static final String DELETE_IF_NO_PACKAGE_VERSION = "PackageBits.deleteIfNoPackageVersion";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "BITS", nullable = true)
    private byte[] bits;

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
     * The actual content of the package ("the bits"). If the package content is not stored in the database, this will
     * return <code>null</code>. In this case, the content is probably stored somewhere else on a local file system.
     * When <code>null</code> is returned, it is assumed that who ever needs the content can know where to find it based
     * on the {@link PackageVersion} details.
     */
    public byte[] getBits() {
        return bits;
    }

    public void setBits(byte[] bits) {
        this.bits = bits;
    }
}