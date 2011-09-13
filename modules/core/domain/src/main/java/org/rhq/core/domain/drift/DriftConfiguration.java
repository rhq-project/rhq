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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext;
import org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode;
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
 * @author John Sanda
 * @author John Mazzitelli
 * @author Jay Shaughnessy
 */
@Entity
@Table(name = "RHQ_DRIFT_CONFIG")
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_DRIFT_CONFIG_ID_SEQ")
public class DriftConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "CTIME", nullable = false)
    private Long ctime = -1L;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "IS_ENABLED", nullable = false)
    private boolean isEnabled;

    @Column(name = "MODE", nullable = false)
    @Enumerated(EnumType.STRING)
    private DriftHandlingMode driftHandlingMode;

    // unit = millis
    @Column(name = "INTERVAL", nullable = false)
    private long interval;

    @JoinColumn(name = "CONFIG_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private Configuration configuration;

    @JoinColumn(name = "RESOURCE_ID", referencedColumnName = "ID", nullable = true)
    @ManyToOne(optional = true)
    private Resource resource = null;

    // required for jaxb/web services stuff
    protected DriftConfiguration() {
    }

    public DriftConfiguration(Configuration c) {
        this.setConfiguration(c);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getCtime() {
        return ctime;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (null == name) {
            throw new IllegalArgumentException("Drift congig name can not be null");
        }

        this.name = name;
        this.setNameProperty(name);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * If null set to default
     * @param isEnabled
     */
    public void setEnabled(Boolean isEnabled) {
        if (isEnabled == null) {
            isEnabled = DriftConfigurationDefinition.DEFAULT_ENABLED;
        }

        this.isEnabled = isEnabled;
        this.setEnabledProperty(isEnabled);
    }

    public DriftHandlingMode getDriftHandlingMode() {
        return driftHandlingMode;
    }

    public void setDriftHandlingMode(DriftHandlingMode driftHandlingMode) {
        if (null == driftHandlingMode) {
            driftHandlingMode = DriftConfigurationDefinition.DEFAULT_DRIFT_HANDLING_MODE;
        }

        this.driftHandlingMode = driftHandlingMode;
        this.setDriftHandlingModeProperty(driftHandlingMode);
    }

    public long getInterval() {
        return interval;
    }

    /**
     * If null, set to default.
     * @param interval
     */
    public void setInterval(Long interval) {
        if (interval == null) {
            interval = DriftConfigurationDefinition.DEFAULT_INTERVAL;
        }

        this.interval = interval;
        this.setIntervalProperty(interval);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        this.name = this.getNameProperty();
        this.isEnabled = this.getIsEnabledProperty();
        this.interval = this.getIntervalProperty();
        this.driftHandlingMode = this.getDriftHandlingModeProperty();
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
        if (this.resource != null) {
            this.resource.getDriftConfigurations().add(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DriftConfiguration [id=").append(id).append(", name=").append(name).append(", enabled=")
            .append(isEnabled).append(", interval=").append(interval).append(", resource=").append(resource).append(
                ", basedir=").append(getBasedir()).append(", includes=").append(getIncludes()).append(", excludes=")
            .append(getExcludes()).append("]");
        return builder.toString();
    }

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

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("BaseDirectory [context=").append(context).append(", name=").append(name).append("]");
            return builder.toString();
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + ((context == null) ? 0 : context.hashCode());
            result = 31 * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BaseDirectory)) {
                return false;
            }
            BaseDirectory other = (BaseDirectory) obj;
            if (context == null) {
                if (other.context != null) {
                    return false;
                }
            } else if (!context.equals(other.context)) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }
    }

    private String getNameProperty() {
        return configuration.getSimpleValue(DriftConfigurationDefinition.PROP_NAME, null);
    }

    private void setNameProperty(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        configuration.put(new PropertySimple(DriftConfigurationDefinition.PROP_NAME, name));
    }

    public BaseDirectory getBasedir() {
        PropertyMap map = configuration.getMap(DriftConfigurationDefinition.PROP_BASEDIR);
        if (map == null) {
            return null;
        }

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

    private Long getIntervalProperty() {
        return Long.parseLong(configuration.getSimpleValue(DriftConfigurationDefinition.PROP_INTERVAL, String
            .valueOf(DriftConfigurationDefinition.DEFAULT_INTERVAL)));
    }

    private void setIntervalProperty(Long interval) {
        configuration.put(new PropertySimple(DriftConfigurationDefinition.PROP_INTERVAL, interval.toString()));
    }

    private DriftHandlingMode getDriftHandlingModeProperty() {
        return DriftHandlingMode.valueOf(configuration.getSimpleValue(
            DriftConfigurationDefinition.PROP_DRIFT_HANDLING_MODE,
            DriftConfigurationDefinition.DEFAULT_DRIFT_HANDLING_MODE.name()));
    }

    private void setDriftHandlingModeProperty(DriftHandlingMode mode) {
        configuration.put(new PropertySimple(DriftConfigurationDefinition.PROP_DRIFT_HANDLING_MODE, mode.name()));
    }

    private boolean getIsEnabledProperty() {
        return configuration.getSimpleValue(DriftConfigurationDefinition.PROP_ENABLED,
            String.valueOf(DriftConfigurationDefinition.DEFAULT_ENABLED)).equals("true");
    }

    private void setEnabledProperty(boolean enabled) {
        configuration.put(new PropertySimple(DriftConfigurationDefinition.PROP_ENABLED, String.valueOf(enabled)));
    }

    public List<Filter> getIncludes() {
        return getFilters(DriftConfigurationDefinition.PROP_INCLUDES);
    }

    public List<Filter> getExcludes() {
        return getFilters(DriftConfigurationDefinition.PROP_EXCLUDES);
    }

    public void addInclude(Filter filter) {
        PropertyList filtersList = configuration.getList(DriftConfigurationDefinition.PROP_INCLUDES);
        if (filtersList == null) {
            // this is going to be our first include filter - make sure we create an initial list and put it in the config
            filtersList = new PropertyList(DriftConfigurationDefinition.PROP_INCLUDES);
            configuration.put(filtersList);
        }

        PropertyMap filterMap = new PropertyMap(DriftConfigurationDefinition.PROP_INCLUDES_INCLUDE);
        filterMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_PATH, filter.getPath()));
        filterMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_PATTERN, filter.getPattern()));
        filtersList.add(filterMap);
    }

    public void addExclude(Filter filter) {
        PropertyList filtersList = configuration.getList(DriftConfigurationDefinition.PROP_EXCLUDES);
        if (filtersList == null) {
            // this is going to be our first include filter - make sure we create an initial list and put it in the config
            filtersList = new PropertyList(DriftConfigurationDefinition.PROP_EXCLUDES);
            configuration.put(filtersList);
        }

        PropertyMap filterMap = new PropertyMap(DriftConfigurationDefinition.PROP_EXCLUDES_EXCLUDE);
        filterMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_PATH, filter.getPath()));
        filterMap.put(new PropertySimple(DriftConfigurationDefinition.PROP_PATTERN, filter.getPattern()));
        filtersList.add(filterMap);
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
}
