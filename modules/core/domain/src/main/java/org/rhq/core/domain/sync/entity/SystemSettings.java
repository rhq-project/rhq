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

package org.rhq.core.domain.sync.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * A domain class representing the system settings of an RHQ installation.
 *
 * @author Lukas Krejci
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SystemSettings extends AbstractExportedEntity {

    private static final long serialVersionUID = 2L;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Entry implements Serializable {

        private static final long serialVersionUID = 1L;

        @XmlAttribute
        private String key;

        @XmlValue
        private String value;

        public Entry() {
            
        }
        
        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }
        
        /**
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * @param key the key to set
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            
            if (obj == null) {
                return false;
            }
            
            if (getClass() != obj.getClass()) {
                return false;
            }
            
            Entry other = (Entry) obj;
            
            return key.equals(other.getKey());
        }

        @Override
        public String toString() {
            return key + " = '" + value + "'";
        }
    }

    @XmlElement(name = "entry")
    private Set<Entry> entries;

    public SystemSettings() {
        setReferencedEntityId(0);
        entries = new HashSet<Entry>();
    }

    public SystemSettings(Map<String, String> settings) {
        this();
        initFrom(settings);
    }

    public SystemSettings(Properties settings) {
        this();
        HashMap<String, String> map = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : settings.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            map.put(key, value);
        }

        initFrom(map);
    }

    public Map<String, String> toMap() {
        HashMap<String, String> settings = new HashMap<String, String>();

        for(Entry e : entries) {
            settings.put(e.getKey(), e.getValue());
        }
        
        return settings;
    }

    public Properties toProperties() {
        Properties ret = new Properties();
        ret.putAll(toMap());
        return ret;
    }

    public void initFrom(Map<String, String> settings) {
        entries.clear();
        for(Map.Entry<String, String> e : settings.entrySet()) {
            entries.add(new Entry(e.getKey(), e.getValue()));
        }
    }

    @Override
    public String toString() {
        return "SystemSettings" + entries;
    }
}
