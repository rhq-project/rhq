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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.plugins.antlrconfig.ConfigMapper;
import org.rhq.plugins.antlrconfig.ConfigurationFacade;
import org.rhq.plugins.antlrconfig.NewEntryCreator;

/**
 * A base class for tests.
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractTest {

    protected abstract ConfigurationDefinition getConfigurationDefinition();
    
    protected abstract Lexer getLexer();
    
    protected abstract ConfigurationFacade getConfigurationFacade();
    
    protected abstract NewEntryCreator getNewEntryCreator();
    
    protected abstract String[] getTokenNames();
    
    protected abstract String getConfigurationFileResourceName();
    
    protected abstract CommonTree loadFile(TokenRewriteStream stream) throws IOException, RecognitionException;
    
    protected ConfigMapper getConfigMapper() {
        return new ConfigMapper(getConfigurationDefinition(), getConfigurationFacade(), getNewEntryCreator(), getTokenNames());
    }
    
    protected TokenRewriteStream getStream(InputStream stream) throws IOException {
        Lexer lexer = getLexer();
        lexer.setCharStream(new ANTLRInputStream(stream));
        return new TokenRewriteStream(lexer);
    }
    
    protected InputStream getResourceStream(String resourceName) throws IOException {
        return this.getClass().getClassLoader().getResourceAsStream(resourceName);
    }
    
    protected TokenRewriteStream getStreamFromResource(String resourceName) throws IOException {
        return getStream(getResourceStream(resourceName));
    }

    protected TokenRewriteStream getStream() throws IOException {
        return getStreamFromResource(getConfigurationFileResourceName());
    }
    
    protected Configuration storeAndLoad(Configuration config) throws RecognitionException, IOException {
        ConfigMapper mapper = getConfigMapper();
        
        TokenRewriteStream stream = getStream();
        
        CommonTree file = loadFile(stream);
        
        mapper.update(file, stream, config);
        
        String updatedConfig = stream.toString();
        
        ByteArrayInputStream updatedInputStream = new ByteArrayInputStream(updatedConfig.getBytes());
        
        TokenRewriteStream updatedStream = getStream(updatedInputStream);
        
        return mapper.read(loadFile(updatedStream));
    }
}
