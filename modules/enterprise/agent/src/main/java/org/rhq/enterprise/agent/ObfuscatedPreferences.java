/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.enterprise.agent;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import org.rhq.core.util.obfuscation.PicketBoxObfuscator;
import org.rhq.enterprise.agent.AgentConfigurationConstants.Restricted;

/**
 * @author Stefan Negrea
 *
 */
public class ObfuscatedPreferences extends Preferences {

    private Preferences actualPreferences;

    private Set<String> restrictedProperties = new HashSet<String>();

    public ObfuscatedPreferences(Preferences actualPreferences) {
        this.actualPreferences = actualPreferences;

        for (Field field : AgentConfigurationConstants.class.getFields()) {
            Restricted restricted = field.getAnnotation(Restricted.class);
            if (restricted != null) {
                try {
                    String restrictedProperty = field.get(AgentConfigurationConstants.class).toString();
                    restrictedProperties.add(restrictedProperty);
                    String retrieveToObfuscate = this.get(restrictedProperty, null);
                } catch (Exception e) {
                    //nothing to do, the field is just not accesible
                }
            }
        }
    }

    @Override
    public void put(String key, String value) {
        if (restrictedProperties.contains(key)) {
            try {
                //if the user passes an obfuscated value then store it as is
                //first try to decode, if it succeeds then the value is already
                //obfuscated
                String unobfuscatedValue = PicketBoxObfuscator.decode(value);
                actualPreferences.put(key, value);
            } catch (Exception ex) {
                try {
                    actualPreferences.put(key, PicketBoxObfuscator.encode(value));
                } catch (Exception ex2) {
                    actualPreferences.put(key, value);
                }
            }
        } else {
            actualPreferences.put(key, value);
        }
    }

    @Override
    public String get(String key, String def) {
        if (restrictedProperties.contains(key)) {
            try {
                String value = actualPreferences.get(key, null);

                if (value == null) {
                    return def;
                }

                return PicketBoxObfuscator.decode(value);
            } catch (Exception ex) {
                //if an exception is thrown that means the value
                //is not encoded restore the
                String value = actualPreferences.get(key, null);

                if (value == null) {
                    return def;
                }

                try {
                    actualPreferences.put(key, PicketBoxObfuscator.encode(value));
                } catch (Exception ex2) {
                    //do nothing, decoding failed, encoding failed too ...
                    //just move on
                }

                return value;
            }
        } else {
            return actualPreferences.get(key, def);
        }
    }

    @Override
    public void remove(String key) {
        actualPreferences.remove(key);
    }

    @Override
    public void clear() throws BackingStoreException {
        actualPreferences.clear();
    }

    @Override
    public void putInt(String key, int value) {
        actualPreferences.putInt(key, value);
    }

    @Override
    public int getInt(String key, int def) {
        return actualPreferences.getInt(key, def);
    }

    @Override
    public void putLong(String key, long value) {
        actualPreferences.putLong(key, value);
    }

    @Override
    public long getLong(String key, long def) {
        return actualPreferences.getLong(key, def);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        actualPreferences.putBoolean(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return actualPreferences.getBoolean(key, def);
    }

    @Override
    public void putFloat(String key, float value) {
        actualPreferences.putFloat(key, value);
    }

    @Override
    public float getFloat(String key, float def) {
        return actualPreferences.getFloat(key, def);
    }

    @Override
    public void putDouble(String key, double value) {
        actualPreferences.putDouble(key, value);
    }

    @Override
    public double getDouble(String key, double def) {
        return actualPreferences.getDouble(key, def);
    }

    @Override
    public void putByteArray(String key, byte[] value) {
        actualPreferences.putByteArray(key, value);
    }

    @Override
    public byte[] getByteArray(String key, byte[] def) {
        return actualPreferences.getByteArray(key, def);
    }

    @Override
    public String[] keys() throws BackingStoreException {
        return actualPreferences.keys();
    }

    @Override
    public String[] childrenNames() throws BackingStoreException {
        return actualPreferences.childrenNames();
    }

    @Override
    public Preferences parent() {
        return actualPreferences.parent();
    }

    @Override
    public Preferences node(String pathName) {
        return actualPreferences.node(pathName);
    }

    @Override
    public boolean nodeExists(String pathName) throws BackingStoreException {
        return actualPreferences.nodeExists(pathName);
    }

    @Override
    public void removeNode() throws BackingStoreException {
        actualPreferences.removeNode();
    }

    @Override
    public String name() {
        return actualPreferences.name();
    }

    @Override
    public String absolutePath() {
        return actualPreferences.absolutePath();
    }

    @Override
    public boolean isUserNode() {
        return actualPreferences.isUserNode();
    }

    @Override
    public String toString() {
        return actualPreferences.toString();
    }

    @Override
    public void flush() throws BackingStoreException {
        actualPreferences.flush();
    }

    @Override
    public void sync() throws BackingStoreException {
        actualPreferences.sync();
    }

    @Override
    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        actualPreferences.addPreferenceChangeListener(pcl);
    }

    @Override
    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        actualPreferences.removePreferenceChangeListener(pcl);
    }

    @Override
    public void addNodeChangeListener(NodeChangeListener ncl) {
        actualPreferences.addNodeChangeListener(ncl);
    }

    @Override
    public void removeNodeChangeListener(NodeChangeListener ncl) {
        actualPreferences.removeNodeChangeListener(ncl);
    }

    @Override
    public void exportNode(OutputStream os) throws IOException, BackingStoreException {
        actualPreferences.exportNode(os);
    }

    @Override
    public void exportSubtree(OutputStream os) throws IOException, BackingStoreException {
        actualPreferences.exportSubtree(os);
    }

}
