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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author Ian Springer
 */
public class MavenArtifactProperties {
    private static final Log LOG = LogFactory.getLog(MavenArtifactProperties.class);

    private static final Map<String, MavenArtifactProperties> INSTANCE_CACHE =
            new HashMap<String, MavenArtifactProperties>();

    private Properties props;
    private Set<String> propNames;

    private MavenArtifactProperties(Properties props) {
        this.props = props;
    }

    public String getGroupId() {
        return getProperty("groupId");
    }

    public String getArtifactId() {
        return getProperty("artifactId");
    }

    public String getVersion() {
        return getProperty("version");
    }

    public String getProperty(String propName) {
        return this.props.getProperty(propName);
    }

    public Set<String> getPropertyNames() {
        if (this.propNames == null) {
            this.propNames = new LinkedHashSet<String>(this.props.size());
            Set<Object> keys = this.props.keySet();
            for (Object key : keys) {
                this.propNames.add((String) key);
            }
        }
        return this.propNames;
    }

    /**
     * Returns a MavenArtifactProperties for the Maven artifact with the specified group id and artifact id.
     * First tries to load the pom.properties file for the artifact using the context class loader. If that
     * fails, then tries to load it using our class loader.
     *
     * @param groupId the group id of the Maven artifact
     * @param artifactId the artifact id of the Maven artifact
     *
     * @return a MavenArtifactProperties for the Maven artifact with the specified group id and artifact id
     *
     * @throws MavenArtifactNotFoundException if a pom.properties file for the artifact could not be found in the context
     *         class loader or in our class loader
     */
    @Nullable
    public static MavenArtifactProperties getInstance(String groupId, String artifactId)
            throws MavenArtifactNotFoundException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        MavenArtifactProperties instance;
        try {
            instance = getInstance(groupId, artifactId, contextClassLoader);
        } catch (MavenArtifactNotFoundException e) {
            ClassLoader ourClassLoader = MavenArtifactProperties.class.getClassLoader();
            instance = getInstance(groupId, artifactId, ourClassLoader);
        }
        return instance;
    }

    /**
     * Returns a MavenArtifactProperties for the Maven artifact with the specified group id and artifact id.
     * Tries to load the pom.properties file for the artifact using the specified class loader.
     *
     * @param groupId the group id of the Maven artifact
     * @param artifactId the artifact id of the Maven artifact
     * @param classLoader the class loader in which to look for the artifact
     *
     * @return a MavenArtifactProperties for the Maven artifact with the specified group id and artifact id
     *
     * @throws MavenArtifactNotFoundException if a pom.properties file for the artifact could not be found in the
     *         specified class loader
     */
    @Nullable
    public static MavenArtifactProperties getInstance(String groupId, String artifactId, ClassLoader classLoader)
            throws MavenArtifactNotFoundException {
        String cacheKey = groupId + ":" + artifactId;
        MavenArtifactProperties instance = INSTANCE_CACHE.get(cacheKey);
        if (instance != null) {
            return instance;
        }

        String resourcePath = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        InputStream inputStream = classLoader.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new MavenArtifactNotFoundException(cacheKey);
        }
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            LOG.error("Failed to load resource " + resourcePath + " into Properties object.", e);
            return null;
        }
        instance = new MavenArtifactProperties(props);

        INSTANCE_CACHE.put(cacheKey, instance);
        return instance;
    }
}
