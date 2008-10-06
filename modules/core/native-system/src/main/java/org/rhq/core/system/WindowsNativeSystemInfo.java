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
package org.rhq.core.system;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;
import org.rhq.core.system.windows.DWordRegistryValue;
import org.rhq.core.system.windows.MultiSzRegistryValue;
import org.rhq.core.system.windows.RegistryEntry;
import org.rhq.core.system.windows.RegistryEntry.Root;
import org.rhq.core.system.windows.RegistryValue;
import org.rhq.core.system.windows.SzRegistryValue;

/**
 * {@link SystemInfo} implementation for the Microsoft Windows operating system platform. This class is used mainly to
 * obtain information found in the Windows Registry.
 *
 * @author John Mazzitelli
 */
public class WindowsNativeSystemInfo extends NativeSystemInfo {
    // return The available ServiceInfo.  Not all existing services may be returned as Windows may protect some service information.
    @Override
    public List<ServiceInfo> getAllServices() throws SystemInfoException {
        List<ServiceInfo> services = new ArrayList<ServiceInfo>();

        try {
            @SuppressWarnings("unchecked")
            List<String> serviceNames = Service.getServiceNames();

            if (serviceNames != null) {
                for (String serviceName : serviceNames) {
                    Service s = null;

                    try {
                        s = new Service(serviceName);

                        ServiceConfig sc = s.getConfig();
                        ServiceInfo si = new ServiceInfo(sc.getName(), sc.getDisplayName(), sc.getDescription(), sc
                            .getPath(), sc.getDependencies());
                        services.add(si);
                    } catch (Win32Exception e) {
                        ; // Ignore these, it means Sigar was rejected by Windows for the particular Service.
                    } finally {
                        if (null != s) {
                            s.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new SystemInfoException("Cannot obtain Windows Services information", e);
        }

        return services;
    }

    /**
     * Gets a list of all the sub-keys that are direct children of the given registry key. You can use the key names
     * found in the returned list of strings as values to the <code>key</code> parameter for calls to
     * {@link #getRegistryEntry(Root, String, String)}.
     *
     * @param  root
     * @param  key
     *
     * @return list of child keys
     *
     * @throws SystemInfoException
     */
    public List<String> getRegistryChildKeys(Root root, String key) throws SystemInfoException {
        try {
            RegistryKey registry = null;

            switch (root) {
            case HKEY_CURRENT_USER: {
                registry = RegistryKey.CurrentUser.openSubKey(key);
                break;
            }

            case HKEY_LOCAL_MACHINE: {
                registry = RegistryKey.LocalMachine.openSubKey(key);
                break;
            }

            default: {
                throw new SystemInfoException("Invalid root: " + root);
            }
            }

            List<String> list = new ArrayList<String>();

            String[] values = registry.getSubKeyNames();
            if (values != null) {
                list.addAll(Arrays.asList(values));
            }

            return list;
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }
    }

    /**
     * Gets a list of all the names of values of the given registry key. You can use the value names found in the
     * returned list of strings as values to the <code>name</code> parameter for calls to
     * {@link #getRegistryEntry(Root, String, String)}.
     *
     * @param  root
     * @param  key
     *
     * @return list of names of values found in that registry key
     *
     * @throws SystemInfoException
     */
    public List<String> getRegistryValueNames(Root root, String key) throws SystemInfoException {
        try {
            RegistryKey registry = null;

            switch (root) {
            case HKEY_CURRENT_USER: {
                registry = RegistryKey.CurrentUser.openSubKey(key);
                break;
            }

            case HKEY_LOCAL_MACHINE: {
                registry = RegistryKey.LocalMachine.openSubKey(key);
                break;
            }

            default: {
                throw new SystemInfoException("Invalid root: " + root);
            }
            }

            List<String> list = new ArrayList<String>();

            String[] values = registry.getValueNames();
            if (values != null) {
                list.addAll(Arrays.asList(values));
            }

            return list;
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }
    }

    /**
     * Gets a list of all the value entries of the given registry key.
     *
     * @param  root
     * @param  key
     *
     * @return list of registry entries for the given registry key
     *
     * @throws SystemInfoException
     */
    public List<RegistryEntry> getRegistryEntries(Root root, String key) throws SystemInfoException {
        try {
            List<String> names = getRegistryValueNames(root, key);
            List<RegistryEntry> entries = new ArrayList<RegistryEntry>();

            for (String name : names) {
                RegistryEntry entry = getRegistryEntry(root, key, name);
                entries.add(entry);
            }

            return entries;
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }
    }

    /**
     * Returns a registry entry (which includes its value) where the key is located under either the LOCAL_MACHINE or
     * CURRENT_USER root registry node.
     *
     * @param  root the root of the registry tree where the registry key is found
     * @param  key  the name of the registry key
     * @param  name the name of the key entry value
     *
     * @return the value of the registry entry
     *
     * @throws SystemInfoException
     */
    public RegistryEntry getRegistryEntry(Root root, String key, String name) throws SystemInfoException {
        RegistryValue value;

        switch (root) {
        case HKEY_LOCAL_MACHINE: {
            value = getLocalMachineRegistryValue(key, name);
            break;
        }

        case HKEY_CURRENT_USER: {
            value = getCurrentUserRegistryValue(key, name);
            break;
        }

        default: {
            throw new SystemInfoException("Invalid root: " + root);
        }
        }

        return new RegistryEntry(root, key, name, value);
    }

    /**
     * Returns a registry value where the key is located under LOCAL_MACHINE.
     *
     * @param  key  the name of the registry key
     * @param  name the name of the key entry value
     *
     * @return the value of the registry entry
     *
     * @throws SystemInfoException
     */
    private RegistryValue getLocalMachineRegistryValue(String key, String name) throws SystemInfoException {
        RegistryKey registry;

        try {
            registry = RegistryKey.LocalMachine.openSubKey(key);
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }

        return getRegistryValue(registry, name);
    }

    /**
     * Returns a registry value where the key is located under CURRENT_USER.
     *
     * @param  key  the name of the registry key
     * @param  name the name of the key entry value
     *
     * @return the string value of the registry entry
     *
     * @throws SystemInfoException
     */
    private RegistryValue getCurrentUserRegistryValue(String key, String name) throws SystemInfoException {
        RegistryKey registry;

        try {
            registry = RegistryKey.CurrentUser.openSubKey(key);
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }

        return getRegistryValue(registry, name);
    }

    private RegistryValue getRegistryValue(RegistryKey registry, String name) throws SystemInfoException {
        RegistryValue.Type valueType = guessType(registry, name);

        try {
            switch (valueType) {
            //            case BINARY:    return new BinaryRegistryValue(registry.getByteArrayValue(name));
            //            case QWORD:     return new QWordRegistryValue(registry.getLongValue(name));
            //            case EXPAND_SZ: return new ExpandSzRegistryValue(registry.getStringValue(name));
            case DWORD: {
                return new DWordRegistryValue(registry.getIntValue(name));
            }

            case SZ: {
                return new SzRegistryValue(registry.getStringValue(name));
            }

            case MULTI_SZ: {
                List<String> values = new ArrayList<String>();
                registry.getMultiStringValue(name, values);
                return new MultiSzRegistryValue(values.toArray(new String[0]));
            }
            }
        } catch (Exception e) {
            throw new SystemInfoException(e);
        }

        throw new IllegalStateException("Bad registry value type: " + valueType);
    }

    private RegistryValue.Type guessType(RegistryKey registry, String name) {
        // TODO: SIGAR does not have a nice API to tell me what type a registry key name has - I have to try to guess.
        // For now, I can only guess at the few types SIGAR knows about.  Need to enhance SIGAR to understand
        // all the types that Windows supports (like QWORD, BINARY, etc).

        try {
            registry.getIntValue(name);
            return RegistryValue.Type.DWORD;
        } catch (Exception e) {
        }

        try {
            registry.getMultiStringValue(name, new ArrayList<String>());
            return RegistryValue.Type.MULTI_SZ;
        } catch (Exception e) {
        }

        try {
            registry.getStringValue(name);
            return RegistryValue.Type.SZ;
        } catch (Exception e) {
        }

        throw new SystemInfoException("Unknown registry value type for [" + name + "]");
    }
}