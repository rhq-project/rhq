/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.domain.common;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Provides simple strings to describe certain server details, useful for end users that have a need to see
 * this data. This is meant to be used as a read-only pojo to be shared to clients.
 * 
 * Some of the fields in this pojo may be empty if the user didn't have permissions to see those fields.
 * 
 * @author John Mazzitelli
 */
public class ServerDetails implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Detail {
        DATABASE_CONNECTION_URL, //
        DATABASE_PRODUCT_NAME, //
        DATABASE_PRODUCT_VERSION, //
        DATABASE_DRIVER_NAME, //
        DATABASE_DRIVER_VERSION, //
        SERVER_TIMEZONE, //
        SERVER_LOCAL_TIME, //
        CURRENT_MEASUREMENT_TABLE, //
        NEXT_MEASUREMENT_TABLE_ROTATION;
    };

    private HashMap<Detail, String> details = new HashMap<Detail, String>(Detail.values().length);
    private ProductInfo productInfo;

    /**
     * @return information about the product itself (like its version).
     */
    public ProductInfo getProductInfo() {
        return this.productInfo;
    }

    public void setProductInfo(ProductInfo info) {
        this.productInfo = info;
    }

    /**
     * Returns details about the server that returned this object. The keys are one of the
     * {@link Detail} enums. You are not guaranteed to have all details in the returned map;
     * if the user doesn't have the permissions necessary, some details will not be available.
     */
    public HashMap<Detail, String> getDetails() {
        return this.details;
    }

    public void setDetails(HashMap<Detail, String> details) {
        this.details = details;
    }
}
