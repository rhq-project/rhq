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

/**
 *
 * @author Filip Drabek
 */
public class ApacheParserImpl implements ApacheParser {

    private final static String INCLUDE_DIRECTIVE = "Include";
    private final ApacheDirectiveTree tree;
    private ApacheDirectiveStack stack;
    private String serverRootPath;

    public ApacheParserImpl(ApacheDirectiveTree tree, String serverRootPath) {
        stack = new ApacheDirectiveStack();
        this.serverRootPath = serverRootPath;
        this.tree = tree;
        stack.addDirective(this.tree.getRootNode());
    }

    public void addDirective(ApacheDirective directive) throws Exception {
        if (directive.getName().equals(INCLUDE_DIRECTIVE)) {
            List<File> files = getIncludeFiles(directive.getValuesAsString());
            for (File fl : files) {
                if (fl.exists() && fl.isFile()) {
                    ApacheConfigReader.searchFile(fl.getAbsolutePath(), this);
                }
            }
        }
        directive.setParentNode(stack.getLastDirective());
        stack.getLastDirective().addChildDirective(directive);
    }

    public void endNestedDirective(ApacheDirective directive) {
        stack.removeLastDirective();
    }

    public void startNestedDirective(ApacheDirective directive) {
        directive.setParentNode(stack.getLastDirective());
        stack.getLastDirective().addChildDirective(directive);
        stack.addDirective(directive);
    }

    private List<File> getIncludeFiles(String foundInclude) {
        File check = new File(foundInclude);
        File root = new File(check.isAbsolute() ? Glob.rootPortion(foundInclude) : serverRootPath);

        return Glob.match(root, foundInclude, Glob.ALPHABETICAL_COMPARATOR);
    }
}
