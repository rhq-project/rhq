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

package org.rhq.core.util.obfuscation;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * @author Stefan Negrea
 *
 */
public class ObfuscatedPreferences extends Preferences {

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Restricted {
    }

    private Preferences actualPreferences;

    private Set<String> restrictedPreferences = new HashSet<String>();
    private Set<String> userRestrictedPreferences = new HashSet<String>();

    @SuppressWarnings("rawtypes")
    public ObfuscatedPreferences(Preferences actualPreferences, Class classz) {
        this.actualPreferences = actualPreferences;

        for (Field field : classz.getFields()) {
            Restricted restricted = field.getAnnotation(Restricted.class);
            if (restricted != null) {
                try {
                    String restrictedProperty = field.get(classz).toString();
                    restrictedPreferences.add(restrictedProperty);
                    this.get(restrictedProperty, null);
                } catch (Exception e) {
                    //nothing to do, the field is just not accesible
                }
            }
        }

        try{
            for (String key : actualPreferences.keys()) {
                if (!restrictedPreferences.contains(key)) {
                    String storedValue = actualPreferences.get(key, null);
                    if (storedValue != null && RestrictedFormat.isRestrictedFormat(storedValue)) {
                        userRestrictedPreferences.add(key);
                    }
                }
            }
        } catch (Exception e) {
            //nothing to do since this was just an exploration to see if the
            //user requested any other properties to be restricted
        }
    }

    @Override
    public void put(String key, String value) {
        if (restrictedPreferences.contains(key) || userRestrictedPreferences.contains(key)) {
            try {
                if (RestrictedFormat.isRestrictedFormat(value)) {
                    value = RestrictedFormat.retrieveValue(value);
                    value = PicketBoxObfuscator.decode(value);
                } else {
                    throw new Exception("Value not in a retricted format");
                }
            } catch (Exception e) {
                try {
                    value = PicketBoxObfuscator.decode(value);
                } catch (Exception e2) {
                    //nothing to do, the value was not obfuscated
                }
            }

            try{
                value = PicketBoxObfuscator.encode(value);
            } catch (Exception ex2) {
                //an error occurred during encoding so just store
                //the value as is
            }

            actualPreferences.put(key, RestrictedFormat.formatValue(value));
        } else {
            if (RestrictedFormat.isRestrictedFormat(value)) {
                userRestrictedPreferences.add(key);

                value = RestrictedFormat.retrieveValue(value);

                try {
                    PicketBoxObfuscator.decode(value);
                } catch (Exception e) {
                    value = PicketBoxObfuscator.encode(value);
                }

                actualPreferences.put(key, RestrictedFormat.formatValue(value));
            } else {
                actualPreferences.put(key, value);
            }
        }
    }

    @Override
    public String get(String key, String def) {
        String value = actualPreferences.get(key, null);

        if (value == null) {
            return def;
        }

        if (restrictedPreferences.contains(key) || userRestrictedPreferences.contains(key)) {
            try {
                if (RestrictedFormat.isRestrictedFormat(value)) {
                    value = RestrictedFormat.retrieveValue(value);
                    return PicketBoxObfuscator.decode(value);
                } else {
                    throw new Exception("Value not in a restricted format");
                }
            } catch (Exception ex) {
                this.put(key, value);

                return value;
            }
        } else {
            return value;
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

    public static class RestrictedFormat {

        private static final Pattern RESTRICTED_PATTERN = Pattern.compile("RESTRICTED::.*", Pattern.CASE_INSENSITIVE);
        private static final String RESTRICTED_FORMAT = "RESTRICTED::%s";

        /**
         * Checks if a property value is in a restricted format.
         *
         * @param str string to check
         * @return
         */
        public static boolean isRestrictedFormat(String str) {
            return str != null && RESTRICTED_PATTERN.matcher(str).matches();
        }

        /**
         * Retrieves the actual value from a restricted format string.
         *
         * @param value
         * @return
         */
        public static String retrieveValue(String value) {
            if (!isRestrictedFormat(value)) {
                return null;
            }

            StringTokenizer tokenizer = new StringTokenizer(value, "::");

            if (!tokenizer.hasMoreTokens()) {
                return null;
            }

            tokenizer.nextToken();

            if (tokenizer.hasMoreTokens()) {
                return tokenizer.nextToken();
            }

            return null;
        }

        /**
         * Formats a value in the proper restricted format.
         *
         * @param value
         * @return
         */
        public static String formatValue(String value) {
            return String.format(RESTRICTED_FORMAT, value);
        }
    }

}
