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
import org.rhq.core.domain.resource.Resource;

public class DriftConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public String getName() {
        return configuration.getSimpleValue("name", "");
    }

    public String getBasedir() {
        return configuration.getSimpleValue("basedir", "");
    }

    public Long getInterval() {
        return Long.parseLong(configuration.getSimpleValue("interval", "0"));
    }

    public boolean getEnabled() {
        return configuration.getSimpleValue("enabled", "false").equals("true");
    }

    public List<Filter> getIncludes() {
        return getFilters("includes");
    }

    public List<Filter> getExcludes() {
        return getFilters("excludes");
    }

    private List<Filter> getFilters(String type) {
        PropertyList filtersListProperty = configuration.getList(type);
        if (filtersListProperty == null) {
            return emptyList();
        }

        List<Filter> filters = new ArrayList<Filter>();
        for (Property property : filtersListProperty.getList()) {
            PropertyMap filter = (PropertyMap) property;
            filters.add(new Filter(filter.getSimpleValue("path", ""), filter.getSimpleValue("pattern", "")));
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
