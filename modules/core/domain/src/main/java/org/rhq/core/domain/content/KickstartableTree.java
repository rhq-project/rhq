/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;


/**
 *
 * @author Pradeep Kilambi
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = KickstartableTree.QUERY_FIND_PATH_BY_KSTREE_LABEL,
            query = "SELECT ks.base_path FROM KickstartableTree AS ks WHERE ks.label = :label")})
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_KICKSTARTABLE_TREE_ID_SEQ")
@Table(name = "RHQ_KICKSTARTABLE_TREE")
public class KickstartableTree implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_PATH_BY_KSTREE_LABEL = "Package.findPathByKSTreeLabel";

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    /**
     *Kickstart tree label
     */
    @Column(name = "LABEL", nullable = false)
    private String label;
    
    /**
     * Base path where the kickstart tree is located
     */
    @Column(name = "BASE_PATH", nullable = false)
    private String base_path;


    // Constructor ----------------------------------------

    public KickstartableTree() {
    }

    public KickstartableTree(String label, String basepathIn) {
        setLabel(label);
        setBasePath(basepathIn);
    }

    public String getLabel() {
        return this.label;
    }

    public String getBasePath() {
        return this.base_path;
    }

    public void setLabel(String labelIn) {
        this.label = labelIn;
    }

    public void setBasePath(String basepathIn) {
        this.base_path = basepathIn;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return "Kickstart Tree[label=" + label + ",basePath=" + base_path + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof KickstartableTree)) {
            return false;
        }

        KickstartableTree kstree = (KickstartableTree) o;

        if ((label != null) ? (!label.equals(kstree.label)) : (kstree.label != null)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((label == null) ? 0 : label.hashCode());
        return result;
    }
}
