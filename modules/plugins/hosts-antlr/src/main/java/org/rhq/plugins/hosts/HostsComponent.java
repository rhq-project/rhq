/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.hosts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.plugins.antlrconfig.ConfigMapper;
import org.rhq.plugins.antlrconfig.DefaultConfigurationFacade;
import org.rhq.plugins.hosts.helper.HostDefCreator;
import org.rhq.plugins.hosts.helper.HostsComponentHelper;
import org.rhq.plugins.hosts.parser.HostsLexer;
import org.rhq.plugins.hosts.parser.HostsParser;

/**
 * The ResourceComponent for the "Hosts File" ResourceType.
 *
 * @author Ian Springer
 */
public class HostsComponent implements ResourceComponent, ConfigurationFacet {

    private static final String CANONICAL_PROPERTY_NAME = "config://$2";
    private static final String IP_ADDRESS_PROPERTY_NAME = "config://$1";
    private static final String HOST_DEF_PROPERTY_NAME = "config://host_def";
    private static final String ALIAS_PROPERTY_NAME = "config://hostname";
    private static final String ALIASES_PROPERTY_NAME = "config://$3";
    private static final String FILE_PROPERTY_NAME = "config:///file";

    public static final String PATH_PROP = "path";

    private final Log log = LogFactory.getLog(this.getClass());

    private ResourceContext resourceContext;
    private File hostsFile;

    private ConfigMapper configMapper;
    
    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        this.hostsFile = HostsComponentHelper.getHostsFile(this.resourceContext.getPluginConfiguration());
        HostsComponentHelper.validateHostFileExists(this.hostsFile);

        configMapper = new ConfigMapper(getResourceConfigurationDefinition(), new DefaultConfigurationFacade(getResourceConfigurationDefinition()), 
            new HostDefCreator("file/host_def", HostsParser.tokenNames), HostsParser.tokenNames);
        
        return;
    }

    public void stop() {
        return;
    }

    public AvailabilityType getAvailability() {
        try {
            HostsComponentHelper.validateHostFileExists(this.hostsFile);
            return AvailabilityType.UP;
        }
        catch (InvalidPluginConfigurationException e) {
            log.debug("Hosts file Resource is down: " + e.getLocalizedMessage());
            return AvailabilityType.DOWN;
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = configMapper.read(loadFile(getStream()));
        
        convertToPluginFormat(config);
        
        return convertToPluginFormat(config);
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            TokenRewriteStream stream = getStream();
            
            Configuration config = report.getConfiguration();

            config = convertToASTFormat(config);
            
            configMapper.update(loadFile(stream), stream, config);
            FileWriter wrt = new FileWriter(this.hostsFile);
            wrt.write(stream.toString());
            wrt.close();
            report.setStatus(ConfigurationUpdateStatus.SUCCESS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write hosts file [" + this.hostsFile + "].", e);
        }
    }
    
    /**
     * TODO this should be provided by the resource context ideally
     * @return
     */
    private ConfigurationDefinition getResourceConfigurationDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("", null);
        
        PropertyDefinitionList aliases = new PropertyDefinitionList(ALIASES_PROPERTY_NAME, null, false, 
            new PropertyDefinitionSimple(ALIAS_PROPERTY_NAME, null, false, PropertySimpleType.STRING));
        
        PropertyDefinitionMap map = new PropertyDefinitionMap(HOST_DEF_PROPERTY_NAME, null, false, 
            new PropertyDefinitionSimple(IP_ADDRESS_PROPERTY_NAME, null, true, PropertySimpleType.STRING),
            new PropertyDefinitionSimple(CANONICAL_PROPERTY_NAME, null, true, PropertySimpleType.STRING),
            aliases);
        
        PropertyDefinitionList list = new PropertyDefinitionList(FILE_PROPERTY_NAME, null, true, map);
        
        def.put(list);
        
        return def;
    }

    private CommonTree loadFile(TokenRewriteStream stream) throws IOException, RecognitionException {
        HostsParser parser = new HostsParser(stream);
        HostsParser.file_return result = parser.file();
        return (CommonTree) result.getTree();
    }
    
    private TokenRewriteStream getStream() throws IOException {
        return new TokenRewriteStream(new HostsLexer(new ANTLRFileStream(this.hostsFile.getAbsolutePath())));
    }
    
    private Configuration convertToASTFormat(Configuration config) {
        Configuration copy = config.deepCopy();
        PropertyList file = copy.getList(FILE_PROPERTY_NAME);
        
        for(Property p : file.getList()) {
            PropertyMap hostDef = (PropertyMap) p;
            
            PropertySimple aliasesProperty = hostDef.getSimple(ALIASES_PROPERTY_NAME);
            if (aliasesProperty != null) {
                String[] aliasesValue = aliasesProperty.getStringValue().split("\\s+");
                PropertyList newAliases = new PropertyList(ALIASES_PROPERTY_NAME);
                hostDef.put(newAliases);
                for(String alias : aliasesValue) {
                    newAliases.add(new PropertySimple(ALIAS_PROPERTY_NAME, alias));
                }
            }
        }
        
        return copy;
    }
    
    private Configuration convertToPluginFormat(Configuration config) {
        Configuration copy = config.deepCopy();
        
        PropertyList file = copy.getList(FILE_PROPERTY_NAME);
        
        for(Property p : file.getList()) {
            PropertyMap hostDef = (PropertyMap) p;
            
            PropertyList aliasesProperty = hostDef.getList(ALIASES_PROPERTY_NAME);
            if (aliasesProperty != null) {
                StringBuilder aliasesString = new StringBuilder();
                
                for(Property p2 : aliasesProperty.getList()) {
                    PropertySimple alias = (PropertySimple) p2;
                    aliasesString.append(alias.getStringValue()).append(" ");
                }
                
                if (aliasesString.length() > 0) {
                    aliasesString.deleteCharAt(aliasesString.length() - 1);
                }
                
                PropertySimple newAliases = new PropertySimple(ALIASES_PROPERTY_NAME, aliasesString.toString());
                
                hostDef.put(newAliases);
            }
        }
        
        return copy;
    }
}
