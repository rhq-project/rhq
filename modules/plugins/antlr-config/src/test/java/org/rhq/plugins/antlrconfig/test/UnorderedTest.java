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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.testng.annotations.Test;

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
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.plugins.antlrconfig.ConfigurationFacade;
import org.rhq.plugins.antlrconfig.DefaultConfigurationFacade;
import org.rhq.plugins.antlrconfig.NewEntryCreator;
import org.rhq.plugins.antlrconfig.test.parsers.UnorderedLexer;
import org.rhq.plugins.antlrconfig.test.parsers.UnorderedParser;

/**
 * Test for &lt;Unordered&gt; token in grammars and its effect on
 * the mapping.
 * 
 * @author Lukas Krejci
 */
public class UnorderedTest extends AbstractTest {
    /**
     * 
     */
    private static final String TEST_CONFIGURATION_FILE_NAME = "unordered";
    private static final String FILE = "config:///file";
    private static final String ASSIGNMENT = "config://assignment";
    private static final String NAME = "config://$1";
    private static final String VALUE = "config://$2";
    

    private static class EntryCreator implements NewEntryCreator {

        public List<OpDef> getInstructions(CommonTree fullTree, CommonTree immediateParent, Property property) {
            if (ASSIGNMENT.equals(property.getName())) {
                int tokenIndex = -1;
                if (immediateParent != null) {
                    tokenIndex = immediateParent.getTokenStopIndex();
                } else {
                    tokenIndex = fullTree.getTokenStopIndex();
                }
                
                OpDef op = new OpDef();
                op.type = OpType.INSERT_AFTER;
                op.tokenIndex = tokenIndex;
                
                StringBuilder bld = new StringBuilder();
                
                PropertyMap assignment = (PropertyMap) property;
                
                bld.append(assignment.getSimpleValue(NAME, ""));
                
                bld.append(" = ");
                
                bld.append(assignment.getSimpleValue(VALUE, ""));
                
                bld.append("\n");
                
                op.text = bld.toString();
                
                return Collections.singletonList(op);
            }
            return null;
        }
    }

    protected ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("", null);
        
        PropertyDefinitionList file = new PropertyDefinitionList(FILE, null, true, null);
        def.put(file);
        
        PropertyDefinitionMap assignment = new PropertyDefinitionMap(ASSIGNMENT, null, false, (PropertyDefinition[])null);
        file.setMemberDefinition(assignment);
        
        assignment.put(new PropertyDefinitionSimple(NAME, null, true, PropertySimpleType.STRING));
        assignment.put(new PropertyDefinitionSimple(VALUE, null, true, PropertySimpleType.STRING));
        
        return def;
    }
        
    protected ConfigurationFacade getConfigurationFacade() {
        return new DefaultConfigurationFacade(getConfigurationDefinition());
    }

    protected Lexer getLexer() {
        return new UnorderedLexer();
    }

    protected NewEntryCreator getNewEntryCreator() {
        return new EntryCreator();
    }

    protected String[] getTokenNames() {
        return UnorderedParser.tokenNames;
    }

    protected CommonTree loadFile(TokenRewriteStream stream) throws IOException, RecognitionException {
        UnorderedParser parser = new UnorderedParser(stream);
        UnorderedParser.file_return ret = parser.file();
        return (CommonTree) ret.getTree();
    }
        
    @Test
    public void testRead() throws Exception {
        Configuration config = getConfigMapper().read(loadFile(getStream()));        
        basicTests(config);
        
        PropertyList file = config.getList(FILE);
        for(int i = 0; i < 5; ++i) {
            PropertyMap assignment = (PropertyMap) file.getList().get(i);
            
            String name = assignment.getSimpleValue(NAME, "");
            String value = assignment.getSimpleValue(VALUE, "");
            
            switch (i) {
            case 0:
                assertEquals(name, "a", "The first variable should be called 'a'");
                assertEquals(value, "1", "The value of the first variable should be 1.");
                break;
            case 1:
                assertEquals(name, "b", "The second variable should be called 'b'");
                assertEquals(value, "2", "The value of the second variable should be 2.");
                break;
            case 2:
                assertEquals(name, "c", "The third variable should be called 'c'");
                assertEquals(value, "3", "The value of the third variable should be 3.");
                break;
            case 3:
                assertEquals(name, "d", "The first variable should be called 'd'");
                assertEquals(value, "4", "The value of the first variable should be 4.");
                break;
            case 4:
                assertEquals(name, "e", "The first variable should be called 'e'");
                assertEquals(value, "5", "The value of the first variable should be 5.");
                break;                
            }
        }
    }
    
    @Test
    public void testUpdate() throws Exception {
        Configuration origConfig = getConfigMapper().read(loadFile(getStream()));
        basicTests(origConfig);
        
        //first a simple update
        Configuration simpleUpdateConfig = origConfig.deepCopy();
        PropertyMap firstAssignment = (PropertyMap)simpleUpdateConfig.getList(FILE).getList().get(0);
        firstAssignment.put(new PropertySimple(NAME, "a"));
        firstAssignment.put(new PropertySimple(VALUE, "42"));
        
        Configuration updatedSimpleUpdate = storeAndLoad(simpleUpdateConfig);
        
        assertEquals(updatedSimpleUpdate, simpleUpdateConfig, "Simple update failed.");
        
        //now shuffle and compare with original
        Configuration shuffleConfig = origConfig.deepCopy();
        Collections.reverse(shuffleConfig.getList(FILE).getList());
        
        Configuration updatedShuffle = storeAndLoad(shuffleConfig);
        
        assertEquals(updatedShuffle, origConfig, "Changing the order of the persisted properties shouldn't affect the file.");
    }
    
    @Test
    public void testDelete() throws Exception {
        Configuration origConfig = getConfigMapper().read(loadFile(getStream()));
        basicTests(origConfig);

        //simple delete
        Configuration simpleConfig = origConfig.deepCopy();
        simpleConfig.getList(FILE).getList().remove(0);
        
        Configuration updatedSimple = storeAndLoad(simpleConfig);
        assertEquals(updatedSimple, simpleConfig, "Simple delete failed.");
        
        //shuffle and delete
        Configuration shuffleConfig = origConfig.deepCopy();
        shuffleConfig.getList(FILE).getList().remove(0);
        Collections.reverse(shuffleConfig.getList(FILE).getList());
        
        Configuration expectedShuffled = origConfig.deepCopy();
        expectedShuffled.getList(FILE).getList().remove(0);
        
        Configuration updatedShuffle = storeAndLoad(shuffleConfig);
        
        assertEquals(updatedShuffle, expectedShuffled, "Delete with shuffle failed.");
    }
    
    @Test
    public void testCreate() throws Exception {
        Configuration config = getConfigMapper().read(loadFile(getStream()));
        basicTests(config);
        
        PropertyMap newPropertyMap = new PropertyMap(ASSIGNMENT);
        newPropertyMap.put(new PropertySimple(NAME, "test"));
        newPropertyMap.put(new PropertySimple(VALUE, "42"));
        
        config.getList(FILE).add(newPropertyMap);
        
        Configuration updated = storeAndLoad(config);
        
        assertEquals(updated, config, "Simple create failed.");
    }
    
    private Configuration storeAndLoad(Configuration config) throws RecognitionException, IOException {
        return storeAndLoad(config, getResourceStream(TEST_CONFIGURATION_FILE_NAME));
    }
    
    private TokenRewriteStream getStream() throws IOException {
        return getStreamFromResource(TEST_CONFIGURATION_FILE_NAME);
    }
    
    private void basicTests(Configuration config) throws Exception {
        assertNotNull(config, "Could not read the configuration from the file.");
        
        PropertyList file = config.getList(FILE);
        
        assertNotNull(file, "the configuration should contain a list of assignments.");
        
        assertEquals(file.getList().size(), 5, "There should be 5 assignments in the test file.");
    }
}
