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
package org.rhq.modules.plugins.jbossas7.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * An address in AS7
 * @author Heiko W. Rupp
 */
public class Address {

    List<PROPERTY_VALUE> path;
    public Address() {
        path = new ArrayList<PROPERTY_VALUE>();
    }

    public Address(String key, String value) {
        this();
        add(key,value);
    }

    public Address(Address other) {
        this();
        path.addAll(other.path);
    }

    public Address(List<PROPERTY_VALUE> other) {
        this();
        if (other!=null)
            path.addAll(other);
    }

    public void add(String key, String value) {
        path.add(new PROPERTY_VALUE(key,value));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder( "Address{" +
            "path: " );
        if (path!=null) {
            Iterator<PROPERTY_VALUE> iterator = path.iterator();
            while (iterator.hasNext()) {
                PROPERTY_VALUE pv = iterator.next();
                builder.append(pv.getKey()).append('=').append(pv.getValue());
                if (iterator.hasNext())
                    builder.append(',');
            }
        }
        else {
            builder.append("-empty-");
        }

        builder.append('}');
        return builder.toString();
    }
}
