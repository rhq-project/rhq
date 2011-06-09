package org.rhq.core.domain.drift;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

import static java.util.Collections.emptyList;

public class DriftConfiguration {

    public static class Filter {
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

    private Configuration configuration;

    public DriftConfiguration(Configuration c) {
        configuration = c;
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

}
