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

import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.testng.annotations.Test;

import org.rhq.plugins.antlrconfig.test.parsers.TransparentLexer;
import org.rhq.plugins.antlrconfig.test.parsers.TransparentParser;
import org.rhq.plugins.antlrconfig.test.parsers.UnorderedParser;

/**
 * The test for the transparent token. This can be the same as the {@link UnorderedTest} because
 * the "segment"s in the grammar are trasparent and therefore invisible to the mapper.
 * Besides from those, the grammar looks the same as for the unordered test.
 * 
 * @author Lukas Krejci
 */
public class TransparentTest extends UnorderedTest {

    @Override
    protected String getConfigurationFileResourceName() {
        return "transparent";
    }

    @Override
    protected Lexer getLexer() {
        return new TransparentLexer();
    }

    @Override
    protected String[] getTokenNames() {
        return TransparentParser.tokenNames;
    }

    @Override
    protected CommonTree loadFile(TokenRewriteStream stream) throws IOException, RecognitionException {
        TransparentParser parser = new TransparentParser(stream);
        TransparentParser.file_return ret = parser.file();
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
