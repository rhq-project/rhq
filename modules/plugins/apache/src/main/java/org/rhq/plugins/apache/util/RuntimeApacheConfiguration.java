/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.plugins.apache.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.apache.parser.ApacheDirective;
import org.rhq.plugins.apache.parser.ApacheDirectiveTree;

/**
 * @author Lukas Krejci
 */
public class RuntimeApacheConfiguration {

    private static final Log LOG = LogFactory.getLog(RuntimeApacheConfiguration.class);
    
    private RuntimeApacheConfiguration() {

    }

    /**
     * Given the apache configuration and information about the parameters httpd was executed
     * with this method provides the directive tree that corresponds to the actual
     * runtime configuration as used by httpd.
     * <p>
     * This enables us to see which directives are actually in effect as opposed to just
     * declared.
     *
     * @param tree
     * @param httpdProcessInfo
     * @param httpdBinaryInfo
     * @param moduleNames the mapping from the module filename to the module name
     * (i.e. mapping from the name used in IfModule to the name used in LoadModule)
     * @return a new directive tree that represents the runtime configuration 
     */
    public static ApacheDirectiveTree extract(ApacheDirectiveTree tree, ProcessInfo httpdProcessInfo, ApacheBinaryInfo httpdBinaryInfo, Map<String, String> moduleNames) {
        ApacheDirectiveTree ret = tree.clone();
        
        List<String> defines = new ArrayList<String>(httpdBinaryInfo.getCompiledInDefines());
        
        String[] args = httpdProcessInfo.getCommandLine();
        for(int i = 1; i < args.length; ++i) {
            String define = null;
            if (args[i] != null && args[i].startsWith("-D")) {
                define = args[i].substring(2).trim();
            }
            
            if (define != null && define.isEmpty()) {
                //this means we saw an empty -D arg. This can happen if there is a space between -D and the value.
                //That is legal though, so we have to accomodate for that.
                if (i < args.length - 1) {
                    define = args[i + 1].trim();
                    if (define.startsWith("-")) {
                        //this would be another option
                        define = null;
                    } else {
                        ++i; //we can skip the next arg
                    }
                } else {
                    define = null; //well -D is the last argument
                }
            }
            
            if (define != null) {
                defines.add(define);
            }
        }
        
        HashSet<String> loadedModules = new HashSet<String>();
        loadedModules.addAll(httpdBinaryInfo.getCompiledInModules());
        
        //build a map for reverse lookup we might need in the transform method
        HashMap<String, String> moduleFiles = new HashMap<String, String>(moduleNames.size());
        for(Map.Entry<String, String> e : moduleNames.entrySet()) {
            moduleFiles.put(e.getValue(), e.getKey());
        }
        
        transform(ret.getRootNode(), loadedModules, defines, moduleNames, moduleFiles);

        return ret;
    }
    
    private static void transform(ApacheDirective parentNode, Set<String> currentlyLoadedModules, List<String> defines, Map<String, String> moduleNames, Map<String, String> moduleFiles) {
        if (parentNode.getChildDirectives().isEmpty()) {
            return;
        }
        
        ArrayList<ApacheDirective> nodesToRemove = new ArrayList<ApacheDirective>();
        ArrayList<ApacheDirective> nodesToPromote = new ArrayList<ApacheDirective>();
        
        for (ApacheDirective node : parentNode.getChildDirectives()) {
            if (node.getName().equalsIgnoreCase("LoadModule")) {
                currentlyLoadedModules.add(node.getValues().get(0));
            } else if (node.getName().equalsIgnoreCase("<IfModule")) {
                String moduleFile = node.getValuesAsString();
                boolean negate = false;
                if (moduleFile.startsWith("!")) {
                    negate = true;
                    moduleFile = moduleFile.substring(1);
                }
                
                String moduleName = moduleNames.get(moduleFile);
                if (moduleName == null) {
                    //as of apache 2.1 module files and module names can both be used in IfModule
                    moduleName = moduleFile;
                    moduleFile = moduleFiles.get(moduleName);
 
                    //reverse lookup failed - there is really no such module, then
                    if (moduleFile == null) {
                        LOG.warn("Encountered unknown module name in an IfModule directive: " + moduleFile);
                        continue;
                    }
                }
                
                boolean result = currentlyLoadedModules.contains(moduleFile);
                
                if (result != negate) {
                    nodesToPromote.add(node);
                } else {
                    nodesToRemove.add(node);
                }
            } else if (node.getName().equalsIgnoreCase("<IfDefine")) {
                String define = node.getValuesAsString();
                boolean negate = false;
                if (define.startsWith("!")) {
                    negate = true;
                    define = define.substring(1);
                }
                
                boolean result = defines.contains(define);
                
                if (negate != result) {
                    nodesToPromote.add(node);
                } else {
                    nodesToRemove.add(node);
                }
            }
            
            transform(node, currentlyLoadedModules, defines, moduleNames, moduleFiles);
        }
        
        for(ApacheDirective node : nodesToRemove) {
            parentNode.getChildDirectives().remove(node);
        }
        
        //add the children of node as children of parent node at the place node
        //was declared and remove node ... i.e. make it so as if the child nodes
        //of node were directly in the parentNode in the place of node
        for(ApacheDirective node : nodesToPromote) {
            int nodeIdx = parentNode.getChildDirectives().indexOf(node);
            
            List<ApacheDirective> childNodes = node.getChildDirectives();
            for(int i = childNodes.size() - 1; i >= 0; --i) {
                ApacheDirective childNode = childNodes.get(i);                
                parentNode.getChildDirectives().add(nodeIdx, childNode);
                childNode.setParentNode(parentNode);
            }

            parentNode.getChildDirectives().remove(nodeIdx + childNodes.size());
        }        
    }
}
