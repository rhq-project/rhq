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
package org.rhq.core.system.windows;

/**
 * A single registry entry, including its key and value.
 *
 * @author John Mazzitelli
 */
public class RegistryEntry {
    public enum Root {
        HKEY_LOCAL_MACHINE, HKEY_CURRENT_USER
    }

    private final Root root;
    private final String key;
    private final String name;
    private final RegistryValue value;

    public RegistryEntry(Root root, String key, String name, RegistryValue value) {
        this.root = root;
        this.key = key;
        this.name = name;
        this.value = value;
    }

    public Root getRoot() {
        return root;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public RegistryValue getValue() {
        return value;
    }
}