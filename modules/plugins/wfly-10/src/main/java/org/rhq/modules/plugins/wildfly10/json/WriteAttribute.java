/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.modules.plugins.wildfly10.json;

/**
 * Operation to write one attribute
 * @author Heiko W. Rupp
 */
public class WriteAttribute extends Operation {

    private static final String WRITE_ATTRIBUTE = "write-attribute";

    public WriteAttribute(Address address) {
        super(WRITE_ATTRIBUTE,address);
    }

    public WriteAttribute(Address address,String name, Object value) {
        super(WRITE_ATTRIBUTE,address);
        addAdditionalProperty("name",name);
        addAdditionalProperty("value",value);
    }

}
