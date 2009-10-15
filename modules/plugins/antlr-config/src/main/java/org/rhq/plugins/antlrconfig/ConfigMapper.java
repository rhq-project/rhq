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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.plugins.antlrconfig.NewEntryCreator.OpDef;
import org.rhq.plugins.antlrconfig.tokens.Id;
import org.rhq.plugins.antlrconfig.tokens.Unordered;

/**
 * Maps RHQ {@link Configuration} objects to Antlr trees and back.
 * 
 * @author Lukas Krejci
 */
public class ConfigMapper {

    private ConfigurationFacade configFacade;
    private NewEntryCreator newEntryCreator;
    private String[] treeTypeNames;
    private ConfigurationDefinition configurationDefinition;
    
    AntlrTreeStructure astStructure = new AntlrTreeStructure();
    
    public ConfigMapper() {
    }
    
    public ConfigMapper(ConfigurationDefinition configurationDefinition, ConfigurationFacade configFacade, NewEntryCreator newEntryCreator, String[] treeTypeNames) {
        this.configurationDefinition = configurationDefinition;
        this.configFacade = configFacade;
        this.newEntryCreator = newEntryCreator;
        this.treeTypeNames = treeTypeNames;
    }
    
    public ConfigurationDefinition getConfigurationDefinition() {
        return configurationDefinition;
    }

    public void setConfigurationDefinition(ConfigurationDefinition configurationDefinition) {
        this.configurationDefinition = configurationDefinition;
    }

    public ConfigurationFacade getConfigFacade() {
        return configFacade;
    }

    public void setConfigFacade(ConfigurationFacade pathConvertor) {
        this.configFacade = pathConvertor;
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
                List<OpDef> instructions = newEntryCreator.getInstructions(configurationFileAST, rec.parent, rec.property);
                if (instructions != null && instructions.size() > 0) {
                    applyInstructions(instructions, fileStream);
                }
            } else if (isDelete) {
                fileStream.delete(rec.tree.getTokenStartIndex(), rec.tree.getTokenStopIndex());
            } else if (rec.property instanceof PropertySimple) {
                String value = configFacade.getPersistableValue((PropertySimple)rec.property);
                fileStream.replace(rec.tree.getTokenStartIndex(), rec.tree.getTokenStopIndex(), value);
            }
        }
    }
    
    private void createProperty(CommonTree root, CommonTree tree, Collection<PropertyDefinition> childDefinitions, Configuration configuration, PropertyList parentList, PropertyMap parentMap) throws RecognitionException {
        for(PropertyDefinition pd : childDefinitions) {
            String subPath = configFacade.getPathRelativeToParent(pd);
            if (subPath == null) continue;
            
            TreePath subTreePath = new TreePath(tree, subPath, treeTypeNames);
            
            List<CommonTree> matches = subTreePath.matches();
            for(CommonTree match : matches) {
                //TODO why am i doing this? shouldn't this always be the same as pd? 
                PropertyDefinition propDef = configFacade.getPropertyDefinition(TreePath.getPath(match, treeTypeNames));
                Property prop = instantiate(propDef, configuration, parentList, parentMap);
                
                if (propDef instanceof PropertyDefinitionList) {
                    createProperty(root, match, Collections.singleton(((PropertyDefinitionList) propDef).getMemberDefinition()), configuration, (PropertyList)prop, null);
                } else if (propDef instanceof PropertyDefinitionMap) {
                    createProperty(root, match, ((PropertyDefinitionMap)propDef).getPropertyDefinitions().values(), configuration, null, (PropertyMap)prop);
                } else {
                    configFacade.applyValue((PropertySimple)prop, match.getText());
                }
            }
            
            if (matches.size() == 0 && pd.isRequired()) {
                Property prop = instantiate(pd, configuration, parentList, parentMap);
                if (pd instanceof PropertyDefinitionSimple) {
                    ConfigurationTemplate defaultTemplate = configurationDefinition.getDefaultTemplate();
                    
                    if (defaultTemplate != null) {
                        //TODO find the default value for the property we're creating.
                    } else {
                        //a poor man's default
                        ((PropertySimple)prop).setStringValue("");
                    }
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
            TreePath path = new TreePath(tree, configFacade.getPathRelativeToParent(def), treeTypeNames);
            List<CommonTree> treeMatches = path.matches();
            
            boolean lookingForChildren = path.getPath().size() > 1;
            
            //now figure out what to delete and what to add
            for (Property prop : matchedProperties) {
                CommonTree match = getMatch(prop, tree, treeMatches, lookingForChildren);
                
                MergeRecord rec = new MergeRecord();
                rec.property = prop;
                rec.tree = match;
                rec.parent = lookingForChildren ? tree : astStructure.getParent(tree);
                
                if (match == null) {
                    mergeRecords.add(rec);
                } else if (prop instanceof PropertySimple) {
                    //only add in the updates
                    if (!configFacade.isEqual((PropertySimple)prop, match.getText())) {
                        mergeRecords.add(rec);
                    }
                }
                
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
    
    private Property instantiate(PropertyDefinition definition, Configuration config, PropertyList parentList, PropertyMap parentMap) {
        Property prop = null;
        
        if (definition instanceof PropertyDefinitionSimple) {
            prop = new PropertySimple(definition.getName(), null);
        } else if (definition instanceof PropertyDefinitionList) {
            prop = new PropertyList(definition.getName());
        } else if (definition instanceof PropertyDefinitionMap) {
            prop = new PropertyMap(definition.getName());
        }
        
        if (prop != null) {
            if (parentList != null) {
                parentList.add(prop);
            } else if (parentMap != null) {
                parentMap.put(prop);
            } else {
                config.put(prop);
            }
        }
        
        return prop;
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
    
    private CommonTree getMatch(Property prop, CommonTree tree, List<CommonTree> possibleMatches, boolean lookingForChildren) throws RecognitionException, MatchException {
        if (lookingForChildren && tree.getToken() instanceof Unordered) {
            List<CommonTree> idTokensInTree = new ArrayList<CommonTree>();
            for(CommonTree child : astStructure.getChildren(tree)) {
                getIdTokensInTree(child, idTokensInTree);
            }
            
            if (idTokensInTree.size() == 0) return null;
            
            //generally we cannot assume that all the Id tokens will be in the same
            //position in the tree. We need to recompute the position for each such id.
            for(CommonTree idToken : idTokensInTree) {
                PropertyDefinition idPropDef = configFacade.getPropertyDefinition(TreePath.getPath(idToken, treeTypeNames));
                String idValue = idToken.getText();
                
                //now find out the corresponding property for the id
                Collection<Property> idProps = configFacade.findCorrespondingProperties(prop, idPropDef);
                if (idProps.size() != 1) {
                    if (idProps.size() == 0) {
                        throw new MatchException("Found 0 Id properties in the subtree of property " + prop + " but was expecting 1.");
                    } else {
                        throw new MatchException("Found more than 1 Id properties in the subtree of property " + prop);
                    }
                }
                
                Property idProp = idProps.iterator().next();
                if (!(idProp instanceof PropertySimple)) {
                    throw new MatchException("Id property must be a simple property but is instead " + idProp);
                }
                
                if (configFacade.isEqual((PropertySimple)idProp, idValue)) {
                    //right off to finding the right tree
                    return getParentOutOf(idToken, possibleMatches);
                }
            }
            
            return null;
        } else {
            String propTreePath = configFacade.getPathRelativeToParent(prop);
            TreePath treePath = new TreePath(tree, propTreePath, treeTypeNames);
            return treePath.match();
        }
    }
    
    private void getIdTokensInTree(CommonTree tree, List<CommonTree> list) {
        if (tree.getToken() instanceof Id) {
            list.add(tree);
            return;
        }
        
        //do not traverse into another Unordered dictionary
        if (tree.getToken() instanceof Unordered) {
            return;
        }
        
        for(CommonTree child : astStructure.getChildren(tree)) {
            getIdTokensInTree(child, list);
        }
    }
    
    private CommonTree getParentOutOf(CommonTree child, List<CommonTree> possibleParents) {
        CommonTree parent = child;
        while (parent != null) {
            if (possibleParents.contains(parent)) {
                break;
            }
            
            parent = astStructure.getParent(parent);
        }
        
        return parent;
    }
    
    private static class MergeRecord {
        public Property property;
        public CommonTree tree;
        public CommonTree parent;
    }
}
