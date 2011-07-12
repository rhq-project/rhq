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

package org.rhq.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * This map is basically an extension of the {@link Properties} class that can resolve the references
 * to values of other keys inside the values.
 * <p>
 * I.e., if the map is initialized with the following mappings:
 * <p>
 * <code>
 * name => world <br />
 * hello => Hello ${name}!
 * </code>
 * <p>
 * then the call to:
 * <p>
 * <code>
 * get("hello")
 * </code>
 * <p>
 * will return:
 * <code>
 * "Hello world!"
 * </code>
 * <p>
 * To access and modify the underlying unprocessed values, one can use the "raw" counterparts of the standard
 * map methods (e.g. instead of {@link #get(Object)}, use {@link #getRaw(Object)}, etc.).
 * 
 * @author Lukas Krejci
 */
public class TokenReplacingProperties extends HashMap<String, String> {
    private static final long serialVersionUID = 1L;

    private Map<String, String> wrapped;
    private Map<Object, String> resolved = new HashMap<Object, String>();

    private class Entry implements Map.Entry<String, String> {
        private Map.Entry<String, String> wrapped;
        private boolean process;
        
        public Entry(Map.Entry<String, String> wrapped, boolean process) {
            this.wrapped = wrapped;
            this.process = process;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            
            if (!(obj instanceof Entry)) {
                return false;
            }
               
            Entry other = (Entry) obj;
            
            String key = wrapped.getKey();
            String otherKey = other.getKey();
            String value = getValue();
            String otherValue = other.getValue();
            
            return (key == null ? otherKey == null : key.equals(otherKey)) &&
                   (value == null ? otherValue == null : value.equals(otherValue));
        }
        
        public String getKey() {
            return wrapped.getKey();
        }
        
        public String getValue() {
            if (process) {
                return get(wrapped.getKey());
            } else {
                return wrapped.getValue();
            }
        }
        
        @Override
        public int hashCode() {
            String key = wrapped.getKey();
            String value = getValue();
            return (key == null ? 0 : key.hashCode()) ^
            (value == null ? 0 : value.hashCode());
        }
        
        public String setValue(String value) {
            resolved.remove(wrapped.getKey());
            return wrapped.setValue(value);
        }
        
        @Override
        public String toString() {
            return wrapped.toString();
        }
    }
    
    public TokenReplacingProperties(Map<String, String> wrapped) {
        this.wrapped = wrapped;
    }

    @SuppressWarnings("unchecked")
    public TokenReplacingProperties(Properties properties) {
        @SuppressWarnings("rawtypes")
        Map map = properties;        
        this.wrapped = (Map<String, String>) map;
    }

    @Override
    public String get(Object key) {
        if (resolved.containsKey(key)) {
            return resolved.get(key);
        }

        String rawValue = getRaw(key);

        if (rawValue == null) {
            return null;
        }

        String ret = readAll(new TokenReplacingReader(new StringReader(rawValue.toString()), this));

        resolved.put(key, ret);

        return ret;
    }

    public String getRaw(Object key) {
        return wrapped.get(key);
    }
    
    @Override
    public String put(String key, String value) {
        resolved.remove(key);
        return wrapped.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        for(String key : m.keySet()) {
            resolved.remove(key);
        }
        wrapped.putAll(m);
    }

    public void putAll(Properties properties) {
        for(String propName : properties.stringPropertyNames()) {
            put(propName, properties.getProperty(propName));
        }
    }
    
    @Override
    public void clear() {
        wrapped.clear();
        resolved.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return wrapped.containsKey(key);
    }

    @Override
    public Set<String> keySet() {
        return wrapped.keySet();
    }

    @Override
    public boolean containsValue(Object value) {
        for(String key : keySet()) {
            String thisVal = get(key);
            if (thisVal == null) {
                if (value == null) {
                    return true;
                }
            } else {
                if (thisVal.equals(value)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Checks whether this map contains the unprocessed value.
     * 
     * @param value
     * @return
     */
    public boolean containsRawValue(Object value) {
        return wrapped.containsValue(value);
    }
    
    /**
     * The returned set <b>IS NOT</b> backed by this map
     * (unlike in the default map implementations).
     * <p>
     * The {@link java.util.Map.Entry#setValue(Object)} method
     * does modify this map though.
     */
    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        Set<Map.Entry<String, String>> ret = new HashSet<Map.Entry<String, String>>();
        for(Map.Entry<String, String> entry : wrapped.entrySet()) {
            ret.add(new Entry(entry, true));
        }
        
        return ret;
    }

    public Set<Map.Entry<String, String>> getRawEntrySet() {
        Set<Map.Entry<String, String>> ret = new HashSet<Map.Entry<String, String>>();
        for(Map.Entry<String, String> entry : wrapped.entrySet()) {
            ret.add(new Entry(entry, false));
        }
        
        return ret;
    }
    
    @Override
    public String remove(Object key) {
        resolved.remove(key);
        return wrapped.remove(key).toString();
    }

    @Override
    public int size() {
        return wrapped.size();
    }

    /**
     * Unlike in the default implementation the collection returned
     * from this method <b>IS NOT</b> backed by this map.
     */
    @Override
    public Collection<String> values() {
        List<String> ret = new ArrayList<String>();
        for(String key : keySet()) {
            ret.add(get(key));
        }
        
        return ret;
    }

    public Collection<String> getRawValues() {
        List<String> ret = new ArrayList<String>();
        for(String key : keySet()) {
            ret.add(wrapped.get(key));
        }
        
        return ret;
    }
    
    private String readAll(Reader rdr) {
        int in = -1;
        StringBuilder bld = new StringBuilder();
        try {
            while ((in = rdr.read()) != -1) {
                bld.append((char) in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Exception while reading a string.", e);
        }

        return bld.toString();
    }
}