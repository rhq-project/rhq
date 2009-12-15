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
package org.rhq.enterprise.server.plugin.pc.content;

/**
 * 
 * @author mmccune
 *
 */
public class SyncProgressWeight {

    public static final SyncProgressWeight DEFAULT_WEIGHTS = new SyncProgressWeight(100, 1, 0, 0, 0);

    /**
     * @param packageMetadataWeight
     * @param packageBitsWeight
     * @param distribtutionMetadataWeight
     * @param distribtutionBitsWeight
     * @param advisoryWeight
     */
    public SyncProgressWeight(int packageMetadataWeight, int packageBitsWeight, int distribtutionMetadataWeight,
        int distribtutionBitsWeight, int advisoryWeight) {
        super();
        this.packageMetadataWeight = packageMetadataWeight;
        this.packageBitsWeight = packageBitsWeight;
        this.distribtutionMetadataWeight = distribtutionMetadataWeight;
        this.distribtutionBitsWeight = distribtutionBitsWeight;
        this.advisoryWeight = advisoryWeight;
    }

    private int packageMetadataWeight;
    private int packageBitsWeight;
    private int distribtutionMetadataWeight;
    private int distribtutionBitsWeight;
    private int advisoryWeight;

    /**
     * @return the packageMetadataWeight
     */
    public int getPackageMetadataWeight() {
        return packageMetadataWeight;
    }

    /**
     * @param packageMetadataWeight the packageMetadataWeight to set
     */
    public void setPackageMetadataWeight(int packageMetadataWeight) {
        this.packageMetadataWeight = packageMetadataWeight;
    }

    /**
     * @return the packageBitsWeight
     */
    public int getPackageBitsWeight() {
        return packageBitsWeight;
    }

    /**
     * @param packageBitsWeight the packageBitsWeight to set
     */
    public void setPackageBitsWeight(int packageBitsWeight) {
        this.packageBitsWeight = packageBitsWeight;
    }

    /**
     * @return the distribtutionMetadataWeight
     */
    public int getDistribtutionMetadataWeight() {
        return distribtutionMetadataWeight;
    }

    /**
     * @param distribtutionMetadataWeight the distribtutionMetadataWeight to set
     */
    public void setDistribtutionMetadataWeight(int distribtutionMetadataWeight) {
        this.distribtutionMetadataWeight = distribtutionMetadataWeight;
    }

    /**
     * @return the distribtutionBitsWeight
     */
    public int getDistribtutionBitsWeight() {
        return distribtutionBitsWeight;
    }

    /**
     * @param distribtutionBitsWeight the distribtutionBitsWeight to set
     */
    public void setDistribtutionBitsWeight(int distribtutionBitsWeight) {
        this.distribtutionBitsWeight = distribtutionBitsWeight;
    }

    /**
     * @return the advisoryWeight
     */
    public int getAdvisoryWeight() {
        return advisoryWeight;
    }

    /**
     * @param advisoryWeight the advisoryWeight to set
     */
    public void setAdvisoryWeight(int advisoryWeight) {
        this.advisoryWeight = advisoryWeight;
    }
}
