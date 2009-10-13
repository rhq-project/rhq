/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.antlrconfig;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.antlrconfig.NewEntryCreator.OpDef;

/**
 * Maps RHQ {@link Configuration} objects to Antlr trees and back.
 * 
 * @author Lukas Krejci
 */
public class ConfigMapper {

    private ConfigurationToPathConvertor pathConvertor;
    private NewEntryCreator newEntryCreator;
    private String[] treeTypeNames;
    private ConfigurationDefinition configurationDefinition;
    
    public ConfigMapper() {
    }
    
    public ConfigMapper(ConfigurationDefinition configurationDefinition, ConfigurationToPathConvertor pathConvertor, NewEntryCreator newEntryCreator, String[] treeTypeNames) {
        this.configurationDefinition = configurationDefinition;
        this.pathConvertor = pathConvertor;
        this.newEntryCreator = newEntryCreator;
        this.treeTypeNames = treeTypeNames;
    }
    
    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public void setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    public ConfigurationToPathConvertor getPathConvertor() {
        return pathConvertor;
    }

    public void setPathConvertor(ConfigurationToPathConvertor pathConvertor) {
        this.pathConvertor = pathConvertor;
    }

    public NewEntryCreator getNewEntryCreator() {
        return newEntryCreator;
    }

    public void setNewEntryCreator(NewEntryCreator newEntryCreator) {
        this.newEntryCreator = newEntryCreator;
    }

    public String[] getTreeTypeNames() {
        return treeTypeNames;
    }

    public void setTreeTypeNames(String[] treeTypeNames) {
        this.treeTypeNames = treeTypeNames;
    }

    public Configuration read(CommonTree configurationFileAST) throws RecognitionException {
        Configuration config = new Configuration();
        
        createProperty(configurationFileAST, configurationFileAST, configurationDefinition.getPropertyDefinitions().values(), config, null, null);
        
        return config;
    }
 
    public void update(CommonTree configurationFileAST, TokenRewriteStream fileStream, Configuration configuration) throws RecognitionException {
        List<MergeRecord> mergeRecords = new ArrayList<MergeRecord>();
        mapTreeForMerge(configurationDefinition.getPropertyDefinitions().values(), configuration.getProperties(), configurationFileAST, mergeRecords);
        
        for(MergeRecord rec : mergeRecords) {
            boolean isAdd = rec.property != null && rec.tree == null;
            boolean isDelete = rec.property == null && rec.tree != null;
            
            if (isAdd) {
                List<OpDef> instructions = newEntryCreator.getInstructions(configurationFileAST, rec.property);
                applyInstructions(instructions, fileStream);
            } else if (isDelete) {
                fileStream.delete(rec.tree.getTokenStartIndex(), rec.tree.getTokenStopIndex());
            } else if (rec.property instanceof PropertySimple) {
                String value = ((PropertySimple)rec.property).getStringValue();
                fileStream.replace(rec.tree.getTokenStartIndex(), rec.tree.getTokenStopIndex(), value);
            }
        }
    }
    
    private void createProperty(CommonTree root, CommonTree tree, Collection<PropertyDefinition> childDefinitions, Configuration configuration, PropertyList parentList, PropertyMap parentMap) throws RecognitionException {
        for(PropertyDefinition pd : childDefinitions) {
            String subPath = pathConvertor.getPathRelativeToParent(pd);
            if (subPath == null) continue;
            
            TreePath subTreePath = new TreePath(tree, subPath, treeTypeNames);
            
            List<CommonTree> matches = subTreePath.matches();
            for(CommonTree match : matches) {
                PropertyDefinition propDef = pathConvertor.getPropertyDefinition(TreePath.getPath(match, treeTypeNames));
                Property prop = instantiate(propDef);
                
                if (parentList != null) {
                    parentList.add(prop);
                } else if (parentMap != null) {
                    parentMap.put(prop);
                } else {
                    configuration.put(prop);
                }
                
                if (propDef instanceof PropertyDefinitionList) {
                    createProperty(root, match, Collections.singleton(((PropertyDefinitionList) propDef).getMemberDefinition()), configuration, (PropertyList)prop, null);
                } else if (propDef instanceof PropertyDefinitionMap) {
                    createProperty(root, match, ((PropertyDefinitionMap)propDef).getPropertyDefinitions().values(), configuration, null, (PropertyMap)prop);
                } else {
                    ((PropertySimple)prop).setValue(match.getText());
                }
            }
        }
    }
        
    private void mapTreeForMerge(Collection<PropertyDefinition> definitions, Collection<Property> properties, CommonTree tree, List<MergeRecord> mergeRecords) throws RecognitionException {
        for(PropertyDefinition def : definitions) {
            //find the properties corresponding to this property definition
            List<Property> matchedProperties = new ArrayList<Property>();
            for (Property p : properties) {
                if (p.getName().equals(def.getName())) {
                    matchedProperties.add(p);
                }
            }
            
            Collection<PropertyDefinition> subDefs = null;
            if (def instanceof PropertyDefinitionList) {
                subDefs = Collections.singleton(((PropertyDefinitionList)def).getMemberDefinition());
            } else if (def instanceof PropertyDefinitionMap) {
                subDefs = ((PropertyDefinitionMap)def).getPropertyDefinitions().values();
            }
            
            //find all the trees in the AST corresponding to this property definition
            TreePath path = new TreePath(tree, pathConvertor.getPathRelativeToParent(def), treeTypeNames);
            List<CommonTree> treeMatches = path.matches();
            
            //now figure out what to delete and what to add
            for (Property prop : matchedProperties) {
                String propTreePath = pathConvertor.getPathRelativeToParent(prop);
                TreePath treePath = new TreePath(tree, propTreePath, treeTypeNames);
                CommonTree match = treePath.match();
                
                MergeRecord rec = new MergeRecord();
                rec.property = prop;
                rec.tree = match;
              
                mergeRecords.add(rec);
                
                if (match != null) {
                    int treeIdx = treeMatches.indexOf(match);
                    if (treeIdx >= 0) {
                        treeMatches.remove(treeIdx);
                    }
                    
                    //if we have a matching sub tree, let's go down a level if we can
                    Collection<Property> subProps = null;
                    if (subDefs != null && prop instanceof PropertyList) {
                        subProps = ((PropertyList)prop).getList();
                    } else if (subDefs != null && prop instanceof PropertyMap) {
                        subProps = ((PropertyMap)prop).getMap().values();
                    }
                    
                    if (subDefs != null && subProps != null) {
                        mapTreeForMerge(subDefs, subProps, match, mergeRecords);
                    }
                } 
            }
            
            //after the previous loop, we're left with the sub trees that
            //correspond to a definition (i.e. are mapped) but aren't present
            //in the supplied configuration - i.e. are to be deleted.
            for(CommonTree toDelete : treeMatches) {
                MergeRecord rec = new MergeRecord();
                rec.tree = toDelete;
                mergeRecords.add(rec);
            }
        }
    }
    
    private Property instantiate(PropertyDefinition definition) {
        if (definition instanceof PropertyDefinitionSimple) {
            return new PropertySimple(definition.getName(), null);
        } else if (definition instanceof PropertyDefinitionList) {
            return new PropertyList(definition.getName());
        } else if (definition instanceof PropertyDefinitionMap) {
            return new PropertyMap(definition.getName());
        }
        
        return null;
    }
    
    private void applyInstructions(List<NewEntryCreator.OpDef> instructions, TokenRewriteStream fileStream) {
        for (NewEntryCreator.OpDef instruction : instructions) {
            switch (instruction.type) {
            case INSERT_AFTER:
                fileStream.insertAfter(instruction.tokenIndex, instruction.text);
                break;
            case INSERT_BEFORE:
                fileStream.insertBefore(instruction.tokenIndex, instruction.text);
                break;
            case REPLACE:
                fileStream.replace(instruction.tokenIndex, instruction.text);
                break;
            case DELETE:
                fileStream.delete(instruction.tokenIndex);
                break;
            }
        }
    }
    
    private static class MergeRecord {
        public Property property;
        public CommonTree tree;
    }
}
