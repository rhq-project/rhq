/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.core.util.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Ian Springer
 */
public class MavenArtifactProperties extends HashMap<String, String> {
    private static final Map<String, MavenArtifactProperties> INSTANCE_CACHE =
            new HashMap<String, MavenArtifactProperties>();

    private MavenArtifactProperties(Properties props) {
        super();
        for (Object key : props.keySet()) {
            Object value = get(key);
            if (key instanceof String && value instanceof String) {
	            put((String)key, (String)value);
            }
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("This object is immutable.");
    }

    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException("This object is immutable.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This object is immutable.");
    }

    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException("This object is immutable.");
    }

	private transient Map<String,String> unmodifiableMap;

    @Override
	public Set<String> keySet() {
	    if (this.unmodifiableMap == null) {
		    this.unmodifiableMap = Collections.unmodifiableMap(this);
        }
	    return this.unmodifiableMap.keySet();
	}

    @Override
	public Set<Map.Entry<String,String>> entrySet() {
        if (this.unmodifiableMap == null) {
		    this.unmodifiableMap = Collections.unmodifiableMap(this);
        }
	    return this.unmodifiableMap.entrySet();
	}

    @Override
	public Collection<String> values() {
	    if (this.unmodifiableMap == null) {
		    this.unmodifiableMap = Collections.unmodifiableMap(this);
        }
	    return this.unmodifiableMap.values();
	}
    /**
     * TODO
     * @param groupId
     * @param artifactId
     * @return
     */
    public static MavenArtifactProperties getInstance(String groupId, String artifactId) {
        String cacheKey = groupId + ":" + artifactId;
        MavenArtifactProperties instance = INSTANCE_CACHE.get(cacheKey);
        if (instance != null) {
            return instance;
        }
        ClassLoader classLoader = MavenArtifactProperties.class.getClassLoader();
        String resourcePath = "META-INF/maven/" + groupId + "/" + artifactId + "pom.properties";
        InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            return null;
        }
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            //("Failed to load resource " + resourcePath + " into Properties object.", e);
            return null;
        }
        return new MavenArtifactProperties(props);
    }
}
