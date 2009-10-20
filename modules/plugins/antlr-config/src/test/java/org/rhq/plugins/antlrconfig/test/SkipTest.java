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

package org.rhq.plugins.antlrconfig.test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.plugins.antlrconfig.NewEntryCreator;
import org.rhq.plugins.antlrconfig.test.parsers.SkipLexer;
import org.rhq.plugins.antlrconfig.test.parsers.SkipParser;

/**
 * Test for the &lt;Skip&gt; token type. The config parsing should behave the same as in {@link UnorderedTest}
 * test because the grammar only defines a new "skipped" level in the AST under which it looks the same
 * as the one for the unordered test.
 * 
 * @author Lukas Krejci
 */
public class SkipTest extends UnorderedTest {

    //the grammar prescribes, that the assignments can only exist in a "segment".
    //We therefore mustn't use the super class' entry creator that doesn't know about this
    //restriction. Note that it is impossible from the ConfigMapper side to "guess" which
    //segment would you like to insert the new entry into.
    private static class EntryCreator implements NewEntryCreator {

        public List<OpDef> getInstructions(CommonTree fullTree, CommonTree immediateParent, Property property) {
            if (ASSIGNMENT.equals(property.getName())) {
                int tokenIndex = -1;
                if (immediateParent != null) {
                    tokenIndex = immediateParent.getTokenStopIndex();
                } else {
                    tokenIndex = fullTree.getTokenStopIndex();
                }
                
                OpDef assignmentOp = new OpDef();
                assignmentOp.type = OpType.INSERT_AFTER;
                assignmentOp.tokenIndex = tokenIndex;
                
                StringBuilder bld = new StringBuilder("\nsegment [\n");
                
                PropertyMap assignment = (PropertyMap) property;
                
                bld.append(assignment.getSimpleValue(NAME, ""));
                
                bld.append(" = ");
                
                bld.append(assignment.getSimpleValue(VALUE, ""));
                
                bld.append("\n");
                
                bld.append("]\n");
                
                assignmentOp.text = bld.toString();
                
                return Collections.singletonList(assignmentOp);
            }
            return null;
        }
    }

    @Override
    protected NewEntryCreator getNewEntryCreator() {
        return new EntryCreator();
    }

    @Override
    protected String getConfigurationFileResourceName() {
        return "skip";
    }

    @Override
    protected Lexer getLexer() {
        return new SkipLexer();
    }

    @Override
    protected String[] getTokenNames() {
        return SkipParser.tokenNames;
    }

    @Override
    protected CommonTree loadFile(TokenRewriteStream stream) throws IOException, RecognitionException {
        SkipParser parser = new SkipParser(stream);
        SkipParser.file_return ret = parser.file();
        return (CommonTree) ret.getTree();
    }

    @Test
    @Override
    public void testCreate() throws Exception {
        super.testCreate();
    }

    @Test
    @Override
    public void testDelete() throws Exception {
        super.testDelete();
    }

    @Test
    @Override
    public void testRead() throws Exception {
        super.testRead();
    }

    @Test
    @Override
    public void testUpdate() throws Exception {
        super.testUpdate();
    }
    
    
}
