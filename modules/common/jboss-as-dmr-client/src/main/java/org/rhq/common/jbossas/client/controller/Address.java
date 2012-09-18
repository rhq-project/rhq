/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import java.util.Arrays;

import org.jboss.dmr.ModelNode;

/**
 * Identifies a managed resource.
 *
 * @author John Mazzitelli
 */
public class Address implements Cloneable {
    private ModelNode addressNode;

    public static Address root() {
        return new Address();
    }

    public Address() {
        addressNode = new ModelNode();
    }

    public Address(String... addressParts) {
        this();
        add(addressParts);
    }

    public ModelNode getAddressNode() {
        return addressNode;
    }

    public Address add(String... addressParts) {
        if (addressParts != null) {
            if ((addressParts.length % 2) != 0) {
                throw new IllegalArgumentException("address is incomplete: " + Arrays.toString(addressParts));
            }

            if (addressParts.length > 0) {
                for (int i = 0; i < addressParts.length; i += 2) {
                    addressNode.add(addressParts[i], addressParts[i + 1]);
                }
            }
        }

        return this;
    }

    public Address add(String type, String name) {
        addressNode.add(type, name);
        return this;
    }

    @Override
    public Address clone() throws CloneNotSupportedException {
        Address clone = new Address();
        clone.addressNode = addressNode.clone();
        return clone;
    }

    @Override
    public String toString() {
        return addressNode.asString();
    }

    @Override
    public int hashCode() {
        return addressNode.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Address)) {
            return false;
        }

        return this.addressNode.equals(((Address) obj).addressNode);
    }
}