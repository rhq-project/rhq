/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * An address in AS7
 * @author Heiko W. Rupp
 */
public class Address {
    private static final Pattern PATH_PATTERN = Pattern.compile("[,]+");
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("=");

    /** List of individual path components */
    List<PROPERTY_VALUE> path;

    /** Create an empty address */
    public Address() {
        path = new ArrayList<PROPERTY_VALUE>();
    }

    /**
     * Create an Address with an initial path element
     * @param key key part of the path
     * @param value value part of the path
     */
    public Address(String key, String value) {
        this();
        add(key, value);
    }

    /**
     * Construct an Address by cloning another
     * @param other Address to clone
     */
    public Address(Address other) {
        this();
        if (other != null && other.path != null)
            path.addAll(other.path);
    }

    /**
     * Construct an address from the passed list of path segments of property values. Should not be used
     * outside the json package
     * @param property_values list of values, each describing a key-value pair of a path
     */
    Address(List<PROPERTY_VALUE> property_values) {
        this();
        if (property_values != null)
            path.addAll(property_values);
    }

    /**
     * Construct an Address from a path in the form (key=value)?(,key=value)*
     * @param path Path string to parse
     */
    public Address(String path) {
        this();
        if (path == null || path.isEmpty())
            return;
        // strips / from the start or end of the key if it happens to be there
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            // We need to check that this "/" isn't the only value of the last key.
            if (!path.endsWith("=/")) {
                path = path.substring(0, path.length() - 1);
            }
        }
        // Now split on comma boundaries
        String[] components = PATH_PATTERN.split(path);
        for (String component : components) {
            String tmp = component.trim();
            // Split each segment on equals sign into key and value
            if (tmp.contains("=")) {
                PROPERTY_VALUE valuePair = pathFromSegment(tmp);
                this.path.add(valuePair);
            }
        }

    }

    /**
     * Generates a path from a segment in the form of key=value.
     * @param segment A segment in the form key=value
     * @return A path
     */
    private PROPERTY_VALUE pathFromSegment(String segment) {
        String[] pair = SEGMENT_PATTERN.split(segment);
        return new PROPERTY_VALUE(pair[0], pair[1]);
    }

    /**
     * Add a key value pair to the path
     * @param key Key part of this path element
     * @param value Value part of this path element
     */
    public void add(String key, String value) {
        path.add(new PROPERTY_VALUE(key, value));
    }

    public void addSegment(String segment) {
        if (!segment.contains("="))
            throw new IllegalArgumentException("Segment [" + segment + "] contains no '='");

        PROPERTY_VALUE pv = pathFromSegment(segment);
        path.add(pv);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Address{" + "path: ");
        if (path != null) {
            Iterator<PROPERTY_VALUE> iterator = path.iterator();
            while (iterator.hasNext()) {
                PROPERTY_VALUE pv = iterator.next();
                builder.append(pv.getKey()).append('=').append(pv.getValue());
                if (iterator.hasNext())
                    builder.append(',');
            }
        } else {
            builder.append("-empty-");
        }

        builder.append('}');
        return builder.toString();
    }

    /**
     * Returns the Address as a string
     * @return list of key value pairs for this path
     */
    @JsonIgnore
    public String getPath() {
        StringBuilder builder = new StringBuilder();
        Iterator<PROPERTY_VALUE> iter = path.iterator();
        while (iter.hasNext()) {
            PROPERTY_VALUE val = iter.next();
            builder.append(val.getKey()).append('=').append(val.getValue());
            if (iter.hasNext())
                builder.append(',');
        }
        return builder.toString();

    }

    /**
     * Add a whole address to the given path
     * @param address Other address
     * @see #Address(Address)
     */
    public void add(Address address) {
        if (address != null && address.path != null)
            this.path.addAll(address.path);
    }

    /**
     * Indicates if this Address has path elements
     * @return true if the address has no path elements
     */
    @JsonIgnore
    public boolean isEmpty() {
        return path.isEmpty();
    }

    /**
     * Returns the number of path elements of this address
     * @return the number of path elements of this address
     */
    public int size() {
        return path.size();
    }

    /**
     * Returns the n'th path element of the address
     * @param n Number of the wanted path element
     * @return A string representation of the wanted element
     */
    public String get(int n) {
        PROPERTY_VALUE property_value = path.get(n);
        return property_value.getKey() + "=" + property_value.getValue();
    }

    /**
     * Return the parent Address of the current one. That is the address with one
     * path segment less.
     * If the current address is empty (the root), an empty address is returned.
     * @return parent Address
     */
    public Address getParent() {
        Address tmp = new Address();
        int l = path.size();
        if (l < 1)
            return tmp;

        for (int i = 0; i < l - 1; i++) {
            tmp.path.add(path.get(i));
        }

        return tmp;
    }
}
