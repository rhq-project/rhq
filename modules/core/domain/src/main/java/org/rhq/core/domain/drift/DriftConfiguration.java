/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.resource.Resource;

/**
 * This is a convienence wrapper around a Configuration object whose schema is that
 * of {@link DriftConfigurationDefinition}.
 * 
 * Note that this is not an actual Configuration object - it's got a HAS-A relationship
 * with Configuration.
 * 
 * This object also has an optional relationship with a Resource.
 *
 * TODO: this is missing setters for includes/excludes filters. We should add those.
 *
 * @author John Sanda
 * @author John Mazzitelli
 */
public class DriftConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    public static class BaseDirectory implements Serializable {
        private static final long serialVersionUID = 1L;
        private BaseDirValueContext context;
        private String name;

        // required for jaxb/web services remoting
        protected BaseDirectory() {
        }

        public BaseDirectory(BaseDirValueContext context, String name) {
            this.context = context;
            this.name = name;
        }

        public BaseDirValueContext getValueContext() {
            return context;
        }

        public String getValueName() {
            return name;
        }
    }

    public static class Filter implements Serializable {
        private static final long serialVersionUID = 1L;

        private String path;
        private String pattern;

        public Filter(String path, String pattern) {
            setPath(path);
            setPattern(pattern);
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            if (path == null) {
                this.path = "";
            } else {
                this.path = path;
            }
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            if (pattern == null) {
                this.pattern = "";
            } else {
                this.pattern = pattern;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (obj instanceof Filter) {
                Filter that = (Filter) obj;
                return this.path.equals(that.path) && this.pattern.equals(that.pattern);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return 13 * (path.hashCode() + pattern.hashCode());
        }

        @Override
        public String toString() {
            return "Filter[path: " + path + ", pattern: " + pattern + "]";
        }
    }

    private Resource resource;

    private Configuration configuration;

    // required for jaxb/web services stuff
    protected DriftConfiguration() {
    }

    public DriftConfiguration(Configuration c) {
        this(null, c);
    }

    public DriftConfiguration(Resource resource, Configuration c) {
        this.resource = resource;
        this.configuration = c;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public int getId() {
        return configuration.getId();
    }

    public void setId(int id) {
        configuration.setId(id);
    }

    public String getName() {
        return configuration.getSimpleValue(DriftConfigurationDefinition.PROP_NAME, "");
    }

    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        configuration.put(new PropertySimple(DriftConfigurationDefinition.PROP_NAME, name));
    }

    public BaseDirectory getBasedir() {
        PropertyMap map = configuration.getMap(DriftConfigurationDefinition.PROP_BASEDIR);
        String valueContext = map.getSimpleValue(DriftConfigurationDefinition.PROP_BASEDIR_VALUECONTEXT, null);
        String valueName = map.getSimpleValue(DriftConfigurationDefinition.PROP_BASEDIR_VALUENAME, null);

        BaseDirValueContext valueContextEnum;

        if (valueContext == null) {
            throw new NullPointerException("valueContext is null");
        } else {
            try {
                valueContextEnum = BaseDirValueContext.valueOf(valueContext);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid valueContext: " + valueContext);
            }
        }

        if (valueName == null) {
            throw new NullPointerException("valueName is null");
        }

        return new BaseDirectory(valueContextEnum, valueName);
    }

    public void setBasedir(BaseDirectory basedir) {
        if (basedir == null) {
            throw new NullPointerException("basedir is null");
        }
        if (basedir.getValueContext() == null) {
            throw new NullPointerException("valueContext is null");
        }
        if (basedir.getValueName() == null) {
            throw new NullPointerException("valueName is null");
        }

        String valueContext = basedir.getValueContext().name();
        String valueName = basedir.getValueName();

        PropertyMap basedirMap = new PropertyMap(DriftConfigurationDefinition.PROP_BASEDIR);
        basedirMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_BASEDIR_VALUECONTEXT, valueContext));
        basedirMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_BASEDIR_VALUENAME, valueName));

        configuration.put(basedirMap);
    }

    public Long getInterval() {
        return Long.parseLong(configuration.getSimpleValue(DriftConfigurationDefinition.PROP_INTERVAL, String
            .valueOf(DriftConfigurationDefinition.DEFAULT_INTERVAL)));
    }

    public void setInterval(Long interval) {
        if (interval == null) {
            configuration.remove(DriftConfigurationDefinition.PROP_INTERVAL);
        } else {
            configuration.put(new PropertySimple(DriftConfigurationDefinition.PROP_INTERVAL, interval.toString()));
        }
    }

    public boolean getEnabled() {
        return configuration.getSimpleValue(DriftConfigurationDefinition.PROP_ENABLED,
            String.valueOf(DriftConfigurationDefinition.DEFAULT_ENABLED)).equals("true");
    }

    public void setEnabled(boolean enabled) {
        configuration.put(new PropertySimple(DriftConfigurationDefinition.PROP_ENABLED, String.valueOf(enabled)));
    }

    public List<Filter> getIncludes() {
        return getFilters(DriftConfigurationDefinition.PROP_INCLUDES);
    }

    public List<Filter> getExcludes() {
        return getFilters(DriftConfigurationDefinition.PROP_EXCLUDES);
    }

    private List<Filter> getFilters(String type) {
        PropertyList filtersListProperty = configuration.getList(type);
        if (filtersListProperty == null) {
            return emptyList();
        }

        List<Filter> filters = new ArrayList<Filter>();
        for (Property property : filtersListProperty.getList()) {
            PropertyMap filter = (PropertyMap) property;
            filters.add(new Filter(filter.getSimpleValue(DriftConfigurationDefinition.PROP_PATH, ""), filter
                .getSimpleValue(DriftConfigurationDefinition.PROP_PATTERN, "")));
        }

        return filters;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public static Set<DriftConfiguration> valueOf(Resource resource) {
        if (null == resource) {
            return new HashSet<DriftConfiguration>(0);
        }

        Set<Configuration> configs = resource.getDriftConfigurations();
        if (null == configs) {
            return new HashSet<DriftConfiguration>(0);
        }

        Set<DriftConfiguration> result = new HashSet<DriftConfiguration>(configs.size());

        for (Iterator<Configuration> i = configs.iterator(); i.hasNext();) {
            result.add(new DriftConfiguration(resource, i.next()));
        }

        return result;
    }

    public static <T extends Collection<Configuration>> Set<DriftConfiguration> valueOf(T configs) {
        if (null == configs) {
            return new HashSet<DriftConfiguration>(0);
        }

        Set<DriftConfiguration> result = new HashSet<DriftConfiguration>(configs.size());

        for (Iterator<Configuration> i = configs.iterator(); i.hasNext();) {
            result.add(new DriftConfiguration(i.next()));
        }

        return result;
    }

}
