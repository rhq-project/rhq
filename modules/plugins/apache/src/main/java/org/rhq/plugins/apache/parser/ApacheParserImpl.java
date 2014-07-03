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
package org.rhq.plugins.apache.parser;

import java.io.File;
import java.util.List;

import org.rhq.augeas.util.Glob;
import org.rhq.plugins.apache.util.AugeasNodeValueUtil;
import org.rhq.plugins.apache.util.RuntimeApacheConfiguration;

/**
 *
 * @author Filip Drabek
 * @author Lukas Krejci
 */
public class ApacheParserImpl implements ApacheParser {

    private final static String INCLUDE_DIRECTIVE = "Include";
    private final static String INCLUDEOPTIONAL_DIRECTIVE = "IncludeOptional";
    private static final String SERVER_ROOT_DIRECTIVE = "ServerRoot";
    private final ApacheDirectiveTree tree;
    private ApacheDirectiveStack stack;
    private String serverRootPath;
    private RuntimeApacheConfiguration.NodeInspector nodeInspector;

    /**
     * 
     * @param tree the tree that this parser will fill in
     * @param initialServerRootPath the initial server root path as detected by other means
     * @param nodeInspector the node inspector to determine the runtime configuration or null, if full configuration tree is needed
     */
    public ApacheParserImpl(ApacheDirectiveTree tree, String initialServerRootPath,
        RuntimeApacheConfiguration.NodeInspector nodeInspector) {
        stack = new ApacheDirectiveStack();
        this.serverRootPath = initialServerRootPath;
        this.tree = tree;
        stack.addDirective(this.tree.getRootNode());
        this.nodeInspector = nodeInspector;
    }

    public void addDirective(ApacheDirective directive) throws Exception {
        if (stack.getLastDirective() == null) {
            //we're ignoring until the end of an ignored nested directive
            return;
        }

        String directiveName = directive.getName();

        if (directiveName.equals(INCLUDE_DIRECTIVE) || directiveName.equals(INCLUDEOPTIONAL_DIRECTIVE)) {
            List<File> files = getIncludeFiles(directive.getValuesAsString());
            for (File fl : files) {
                if (fl.exists() && fl.isFile()) {
                    ApacheConfigReader.searchFile(fl.getAbsolutePath(), this);
                }
            }
        } else if (directiveName.equals(SERVER_ROOT_DIRECTIVE)) {
            this.serverRootPath = AugeasNodeValueUtil.unescape(directive.getValuesAsString());
        }

        if (nodeInspector != null) {
            //let the inspector process this directive in case it sees something of interest
            nodeInspector.inspect(directiveName, directive.getValues(), directive.getValuesAsString());
        }

        directive.setParentNode(stack.getLastDirective());
        stack.getLastDirective().addChildDirective(directive);
    }

    public void endNestedDirective(ApacheDirective directive) {
        stack.removeLastDirective();
    }

    public void startNestedDirective(ApacheDirective directive) {
        if (nodeInspector != null) {
            //now we have a node inspector so the tree construction is driven by it - we actually leave out the conditional
            //directives and replace them with their contents (if they are to be applied of course)...

            RuntimeApacheConfiguration.NodeInspectionResult res =
                nodeInspector.inspect(directive.getName(), directive.getValues(), directive.getValuesAsString());
            if (res == null || (res.nodeIsConditional && !res.shouldRecurseIntoConditionalNode)) {
                //ok, this node should be ignored, mark that fact with a null parent
                stack.addDirective(null);
            } else if (res.nodeIsConditional && res.shouldRecurseIntoConditionalNode) {
                //this is a little tricky..
                //we need to leave out this directive, because it is conditional and we should be replacing it with its contents
                //but also we need to keep the stack balanced (i.e. we're going down a nested directive, so we ought to put something
                //on the stack). By duplicating the last known directive on the stack we actually achieve both of the goals - the 
                //stack remains balanced and the child nodes get added to the parent of this directive (i.e. in the end it is going to
                //look like we've replaced the directive with its child directives).
                stack.addDirective(stack.getLastDirective());
            } else {
                //ok, a "normal" (non-conditional) nested node
                //we might be inside an ignored nested directive, so leave all the weaving stuff
                //out in that case...
                if (stack.getLastDirective() != null) {
                    directive.setParentNode(stack.getLastDirective());
                    stack.getLastDirective().addChildDirective(directive);
                }
                stack.addDirective(directive);
            }
        } else {
            //there is no node inspector, so we have no "guidance" on the tree construction.
            //Just include all the nested structures so that a full configuration tree is built
            //with all the directives.

            //we might be inside an ignored nested directive, so leave all the weaving stuff
            //out in that case...
            if (stack.getLastDirective() != null) {
                directive.setParentNode(stack.getLastDirective());
                stack.getLastDirective().addChildDirective(directive);
            }
            stack.addDirective(directive);
        }
    }

    private List<File> getIncludeFiles(String foundInclude) {
        File check = new File(foundInclude);
        File root = new File(check.isAbsolute() ? Glob.rootPortion(foundInclude) : serverRootPath);

        return Glob.match(root, foundInclude, Glob.ALPHABETICAL_COMPARATOR);
    }
}
