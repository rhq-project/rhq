/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.domain.content;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Domain representation of the steps used to install a package.
 *
 * @author Jason Dobies
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PACKAGE_INST_STEP_ID_SEQ")
@Table(name = "RHQ_PACKAGE_INST_STEP")
public class PackageInstallationStep implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    /**
     * Database assigned ID.
     */
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    /**
     * Relative order of the step in the overall list of steps.
     */
    @Column(name = "STEP_ORDER")
    private int order;

    /**
     * Description of what the step will do.
     */
    @Column(name = "DESCRIPTION")
    private String description;

    /**
     * Package version against which this step applies.
     */
    @JoinColumn(name = "INSTALLED_PACKAGE_ID", referencedColumnName = "ID")
    @ManyToOne
    private InstalledPackage installedPackage;

    // Constructor ----------------------------------------

    public PackageInstallationStep() {
        // for JPA use
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InstalledPackage getInstalledPackage() {
        return installedPackage;
    }

    public void setPackageVersion(InstalledPackage installedPackage) {
        this.installedPackage = installedPackage;
    }
}