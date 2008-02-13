/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.configuration.definition;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;

/**
 * This object represents a single template of configuration values for a given definition. Definitions may have many
 * templates. E.g. the Datasource resource definition could have templates for an oracle server and a postgres server.
 * The templates are one-to-one linked to a Configuration which holds the values for the properties of that template.
 *
 * @author Greg Hinkle
 */
@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONFIG_TEMPLATE_ID_SEQ")
@Table(name = "RHQ_CONFIG_TEMPLATE")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigurationTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_TEMPLATE_NAME = "default";

    @GeneratedValue(generator = "SEQ", strategy = GenerationType.SEQUENCE)
    @Id
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @JoinColumn(name = "config_id")
    @OneToOne(optional = false, cascade = CascadeType.ALL)
    private Configuration configuration;

    @JoinColumn(name = "config_def_id")
    @ManyToOne
    @XmlTransient
    private ConfigurationDefinition configurationDefinition;

    @Column(name = "is_default")
    private boolean isDefault;

    /* no-arg protected or public constructor required by EJB spec */
    protected ConfigurationTemplate() {
    }

    public ConfigurationTemplate(@NotNull
    String name, @Nullable
    String description) {
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public void setName(@NotNull
    String name) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable
    String description) {
        this.description = description;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public void setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Configuration createConfiguration() {
        // Create a clone of this template's Configuration with all id's zeroed out.
        return getConfiguration().deepCopy(false);
    }

    @Override
    public String toString() {
        return "ConfigurationTemplate[id=" + this.id + ", name=" + this.name
            + ((this.configurationDefinition != null) ? (", config=" + this.configurationDefinition.getName()) : "")
            + "]";
    }
}