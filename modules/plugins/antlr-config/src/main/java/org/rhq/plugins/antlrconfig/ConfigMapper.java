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

/**
 * Maps RHQ {@link Configuration} objects to Antlr trees and back.
 * 
 * @author Lukas Krejci
 */
public class ConfigMapper {

    private ConfigurationToPathConvertor pathConvertor;
    private NewEntryCreator newEntryCreator;
    private String[] treeTypeNames;
    
    private static class ConfigurationDefinitionStructure implements TreeStructure<PropertyDefinition> {
        public Collection<PropertyDefinition> getChildren(PropertyDefinition parent) {
            if (parent instanceof PropertyDefinitionList) {
                return Collections.singletonList(((PropertyDefinitionList)parent).getMemberDefinition());
            } else if (parent instanceof PropertyDefinitionMap) {
                return ((PropertyDefinitionMap)parent).getPropertyDefinitions().values();
            }
            return null;
        }    
    }
    private static final ConfigurationDefinitionStructure configurationDefinitionStructure = new ConfigurationDefinitionStructure();
    
    private static class ConfigurationStructure implements TreeStructure<Property> {
        public Collection<Property> getChildren(Property parent) {
            if (parent instanceof PropertyList) {
                return ((PropertyList)parent).getList();
            } else if (parent instanceof PropertyMap) {
                return ((PropertyMap)parent).getMap().values();
            }
            return null;
        }
    }
    private static final ConfigurationStructure configurationStructure = new ConfigurationStructure();
    
    private static final AntlrTreeStructure astStructure = new AntlrTreeStructure();
    
    public ConfigMapper() {
    }
    
    public ConfigMapper(ConfigurationToPathConvertor pathConvertor, NewEntryCreator newEntryCreator, String[] treeTypeNames) {
        this.pathConvertor = pathConvertor;
        this.newEntryCreator = newEntryCreator;
        this.treeTypeNames = treeTypeNames;
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

    public Configuration read(ConfigurationDefinition configurationDefinition, Tree configurationFileAST) throws RecognitionException {
        Configuration config = new Configuration();
        
        createProperty(configurationFileAST, configurationFileAST, configurationDefinition.getPropertyDefinitions().values(), config, null, null);
        
        return config;
    }
 
    private void mapPropertyToTree(Tree configurationFileAST, Property property, PropertyList parentList, PropertyMap parentMap, Map<Property, MergeRecord> mapping, MergeRecord rootMergeRecord) throws RecognitionException {
        String propertyTreePath = pathConvertor.getTreePath(property);
        TreePath treePath = new TreePath(configurationFileAST, propertyTreePath, treeTypeNames);
        Tree propertyTree = treePath.match();
        
        if (propertyTree == null) {
            MergeRecord parentRecord;
            if (parentList != null) {
                parentRecord = mapping.get(parentList);
            } else if (parentMap != null) {
                parentRecord = mapping.get(parentMap);
            } else {
                parentRecord = rootMergeRecord;
            }
            
            parentRecord.additions.add(property);
        }
        
        MergeRecord rec = new MergeRecord();
        rec.property = property;
        rec.tree = propertyTree;
        mapping.put(property, rec);
        
        if (property instanceof PropertyList) {
            PropertyList propertyList = (PropertyList) property;
            for(Property child : propertyList.getList()) {
                mapPropertyToTree(configurationFileAST, child, propertyList, null, mapping, rootMergeRecord);
            }
        } else if (property instanceof PropertyMap) {
            PropertyMap propertyMap = (PropertyMap) property;
            for(Property child : propertyMap.getMap().values()) {
                mapPropertyToTree(configurationFileAST, child, null, parentMap, mapping, rootMergeRecord);
            }
        }
    }
   
    private void detectDeletions(Property root, Map<Property, MergeRecord> mapping, Tree astRoot) {
        // TODO Auto-generated method stub
        MergeRecord rec = mapping.get(root);
        if (rec.tree != null) {
            for (int i  = 0; i < rec.tree.getChildCount(); ++i) {
                Tree child = rec.tree.getChild(i);
                
                //check if the child is "creatable"
                List<PathElement> path = TreePath.getPath(child, astRoot, treeTypeNames);
                if (pathConvertor.getPropertyDefinition(path) != null) {
                    
                }
            }
        }
    }
    
    public void update(Tree configurationFileAST, TokenRewriteStream fileStream, Configuration configuration) throws RecognitionException {
        Map<Property, MergeRecord> mapping = new HashMap<Property, MergeRecord>();
        MergeRecord rootMergeRecord = new MergeRecord();
        rootMergeRecord.tree = configurationFileAST;
        
        for(Property prop : configuration.getProperties()) {
            mapPropertyToTree(configurationFileAST, prop, null, null, mapping, rootMergeRecord);
        }
        
        for(Property prop : configuration.getProperties()) {
            detectDeletions(prop, mapping, configurationFileAST);            
        }
        
//        //TODO implement <Unordered>
//        DfsWalker<Tree> treeWalker = new DfsWalker<Tree>(astStructure, configurationFileAST);
//        HashMap<Tree, Boolean> visitedMap = new HashMap<Tree, Boolean>();
//        while(treeWalker.hasNext()) {
//            Tree node = treeWalker.next();
//            visitedMap.put(node, false);
//        }
//        
//        DfsWalker<Property> walker = new DfsWalker<Property>(configurationStructure, configuration.getProperties());
//        
//        //this was a stab at <Unordered>
//        //Deque<Map.Entry<List<PathElement>,Tree>> lastKnownParents = new ArrayDeque<Map.Entry<List<PathElement>,Tree>>();
//        //int lastDepth = 0;
//        
//        Deque<Property> newPropertiesInCreation = new ArrayDeque<Property>();
//        
//        while(walker.hasNext()) {
//            Property property = walker.next();
//            
//            while (walker.getCurrentDepth() < newPropertiesInCreation.size()) {
//                createProperty(configurationFileAST, fileStream, newPropertiesInCreation.pop());
//            }
////this was a stab at implementing <Unordered>
////            if ((propertyPath.size() - 1) > lastDepth) {
////                //we stepped a level deeper...
////                //there's a complication with the unordered properties.
////                //their location in the config file does not necessarily correspond
////                //to the order in the configuration structures and therefore we need
////                //to do a little bit of voodoo here.
////                //we keep a track of last known parents - stuff that has been found before
////                //and try to match the new property to that hierarchy (using DFS helps here).
////
////                //get the concrete path of the property (this includes all the positional and indexed
////                //path elements).
////                String treePath = pathConvertor.getConcretePath(propertyPath);
////                List<PathElement> path = TreePath.parsePath(treePath);
////                
////                //now get the parent. The path stored is what we actually found in the parse tree.
////                Map.Entry<List<PathElement>, Tree> parentEntry = lastKnownParents.peek();
////                if (parentEntry == null) {
////                    //TODO throw a better exception type?
////                    throw new RuntimeException("Could not find a place to put " + last.getName() + " property in its configuration file.");
////                }
////                
////                //now we have the immediate parent and can append the concrete path ending to it.
////                Tree immediateParentTree = parentEntry.getValue();
////                PathElement lastElement = path.get(path.size() - 1);
////
////                ArrayList<PathElement> actualPath = new ArrayList<PathElement>(parentEntry.getKey());
////                
////                if (lastElement.getType() == PathElement.Type.INDEX_REFERENCE) {
////                    //this is simple... the indexed references are by definition anonymous.
////                    //we don't know anything else about them but their index in the parent.
////                    //so there's not much we can do here even if the parent is <Unordered>.
////                    actualPath.add(lastElement);
////                } else if (lastElement.getType() == PathElement.Type.POSITION_REFERENCE) {
////                    //ok... we have the name and the position... this means that we might have a problem
////                    //if the parent is unordered.
////                    if (immediateParentTree instanceof Unordered) {
////                        //TODO .... this is where I gave up...
////                    } else {
////                        //well.. parent is ordered, so we have to find the child on the position
////                        actualPath.add(lastElement);
////                    }
////                } else {
////                    throw new RuntimeException("Concrete path contains non-concrete path elements: " + path.toString());
////                }
////            }
//            
//            
//            String propertyTreePath = pathConvertor.getTreePath(property);
//            TreePath treePath = new TreePath(configurationFileAST, propertyTreePath, treeTypeNames);
//            Tree propertyTree = treePath.match();
//            
//            if (propertyTree == null) {
//                //scenarios possible here:
//                //1) this is a new property added to the configuration
//                //2) the property has moved within the AST - we need to find it if its parent is <Unordered>
//                if (newPropertiesInCreation.size() == walker.getCurrentDepth()) {
//                    createProperty(configurationFileAST, fileStream, newPropertiesInCreation.pop());
//                }
//                newEntryCreator.prepareFor(property);
//                newPropertiesInCreation.push(property);
//            } else {
//                if (property instanceof PropertySimple) {
//                    String value = ((PropertySimple)property).getStringValue();
//                    fileStream.replace(propertyTree.getTokenStartIndex(), propertyTree.getTokenStopIndex(), value);
//                }
//                setVisited(propertyTree, visitedMap);
//            }
//        }
//        
//        while (newPropertiesInCreation.size() > 0) {
//            createProperty(configurationFileAST, fileStream, newPropertiesInCreation.pop());
//        }
//        
//        //ok, now we've gone through the configuration object and applied it to the tree.
//        //all there's left is to clear out what should be deleted from the tree.
//        List<Tree> toBeDeleted = new ArrayList<Tree>();
//        treeWalker = new DfsWalker<Tree>(astStructure, configurationFileAST);
//        while(treeWalker.hasNext()) {
//            Tree node = treeWalker.next();
//            Boolean isVisited = visitedMap.get(node);
//            if (!isVisited) toBeDeleted.add(node);
//        }
//        //now remove children of all things we found to be deleted from the visited map
//        //this will leave us only with the topmost tree nodes marked as to be deleted
//        for(Tree node : toBeDeleted) {
//            removeChildren(node, visitedMap);
//        }
//        
//        //and now move on to the deletion itself
//        for(Tree node : toBeDeleted) {
//            if (visitedMap.containsKey(node)) {
//                Token nodeToken = fileStream.get(node.getTokenStartIndex());
//                if (!(nodeToken instanceof RhqConfigToken) || !((RhqConfigToken)nodeToken).isTransparent()) {
//                    fileStream.delete(nodeToken);
//                }
//            }
//        }
    }
    
    private void createProperty(Tree root, Tree tree, Collection<PropertyDefinition> childDefinitions, Configuration configuration, PropertyList parentList, PropertyMap parentMap) throws RecognitionException {
        for(PropertyDefinition pd : childDefinitions) {
            String subPath = pathConvertor.getPathRelativeToParent(pd);
            if (subPath == null) continue;
            
            TreePath subTreePath = new TreePath(tree, subPath, treeTypeNames);
            
            List<Tree> matches = subTreePath.matches();
            for(Tree match : matches) {
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
    
    private void setVisited(Tree tree, Map<Tree, Boolean> visitedMap) {
        //if a tree is visited, all its children are visited as well
        DfsWalker<Tree> walker = new DfsWalker<Tree>(astStructure, tree);
        while(walker.hasNext()) {
            Tree child = walker.next();
            visitedMap.put(child, true);
        }
    }
    
    private void removeChildren(Tree tree, Map<Tree, Boolean> visitedMap) {
        Collection<Tree> children = astStructure.getChildren(tree);
        DfsWalker<Tree> walker = new DfsWalker<Tree>(astStructure, children);
        while(walker.hasNext()) {
            Tree child = walker.next();
            visitedMap.remove(child);
        }
    }
    
    private void createProperty(Tree tree, TokenRewriteStream fileStream, Property prop) {
        List<NewEntryCreator.OpDef> instructions = newEntryCreator.getInstructions(tree, prop);
        if (instructions != null) {
            applyInstructions(instructions, fileStream);
        }
    }
    
    private static class MergeRecord {
        public Property property;
        public Tree tree;
        public List<Property> additions = new ArrayList<Property>();
        public Set<Tree> deletions = new HashSet<Tree>();
    }
}
