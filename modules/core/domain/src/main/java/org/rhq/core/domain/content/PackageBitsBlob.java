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
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

/**
 * The actual Lob contents for a PackageBits entity.  Note that PackageBits and PackageBitsBlob are two 
 * entities that *share* the same db row.  This is done to allow for Lazy load semantics on the Lob.  Hibernate
 * does not honor field-level Lazy load on a Lob (or any field) unless the entity class is instrumented. 
 * We can't use that approach because it introduces Hibernate imports into the domain class, and that violates our
 * restriction of exposing hibernate classes to the Agent and Remote clients. As a workaround we pull the Lob into
 * its own entity and access it through a relational mapping. Note that the entities share the same Id since they
 * share the same physical row.  The row is persisted via this class and as such the sequence is declared here.  Creating
 * this entity first allows us to satisfy the required 1-1 mapping in {@link PackageBits}.
 * <br>
 * Related Links:
 * <br>http://docs.jboss.org/hibernate/stable/core/reference/en/html/performance.html#performance-fetching-lazyproperties
 * <br>http://community.jboss.org/wiki/Someexplanationsonlazyloadingone-to-one
 * <br>http://docs.jboss.org/hibernate/stable/annotations/reference/en/html_single/#entity-hibspec-singleassoc
 * <br>http://community.jboss.org/wiki/AShortPrimerOnFetchingStrategies
 * <br>http://docs.codehaus.org/display/MAVENUSER/Howto+instrument+domain+model+classes+using+hibernate
 * 
 * @author Jay Shaughnessy
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PACKAGE_BITS_ID_SEQ")
@Table(name = PackageBits.TABLE_NAME)
public class PackageBitsBlob implements Serializable {
    public static final String TABLE_NAME = "RHQ_PACKAGE_BITS";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Lob
    @Column(name = "BITS")
    @XmlTransient
    private byte[] bits;

    public PackageBitsBlob() {
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
     * 
     * For large file contents, you should use ContentManager.updateBlobStream() to write and
     * ContentManager.writeBlobOutToStream() to read/direct file contents into as no byte[] is used.
     */
    @XmlTransient
    public byte[] getBits() {
        return bits;
    }

    /**
     * For large file contents, you should use ContentManager.updateBlobStream() to write and 
     * ContentManager.writeBlobOutToStream() to stream the binary bits and avoid a byte[].
     */
    public void setBits(byte[] bits) {
        this.bits = bits;
    }
}