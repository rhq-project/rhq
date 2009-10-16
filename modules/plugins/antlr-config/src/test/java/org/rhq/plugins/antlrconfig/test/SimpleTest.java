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

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.tree.CommonTree;
import org.testng.annotations.BeforeSuite;
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
import org.rhq.plugins.antlrconfig.ConfigMapper;
import org.rhq.plugins.antlrconfig.DefaultConfigurationFacade;
import org.rhq.plugins.antlrconfig.NewEntryCreator;
import org.rhq.plugins.antlrconfig.test.parsers.SimpleLexer;
import org.rhq.plugins.antlrconfig.test.parsers.SimpleParser;

/**
 * Tests with the Simple.g grammar
 * 
 * @author Lukas Krejci
 */
public class SimpleTest {

    private static final String FILE = "config:///file";
    private static final String ASSIGNMENT = "config://assignment";
    private static final String NAME = "config://$1";
    private static final String VALUE = "config://$2";
    private static final String EXPORT = "config://$3";
    
    @BeforeSuite
    public void dummy() {
        
    }
    
    private static ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("", null);
        
        PropertyDefinitionList file = new PropertyDefinitionList(FILE, null, true, null);
        def.put(file);
        
        PropertyDefinitionMap assignment = new PropertyDefinitionMap(ASSIGNMENT, null, false, (PropertyDefinition[])null);
        file.setMemberDefinition(assignment);
        
        assignment.put(new PropertyDefinitionSimple(NAME, null, true, PropertySimpleType.STRING));
        assignment.put(new PropertyDefinitionSimple(VALUE, null, true, PropertySimpleType.STRING));
        assignment.put(new PropertyDefinitionSimple(EXPORT, null, true, PropertySimpleType.BOOLEAN));
        
        return def;
    }
    
    private static class CustomFacade extends DefaultConfigurationFacade {

        public CustomFacade() {
            super(getConfigurationDefinition());
        }

        @Override
        public void applyValue(PropertySimple property, String value) {
            if (EXPORT.equals(property.getName())) {
                property.setBooleanValue("export".equalsIgnoreCase(value));
            } else {
                super.applyValue(property, value);
            }
        }

        @Override
        public String getPersistableValue(PropertySimple property) {
            if (EXPORT.equals(property.getName())) {
                return property.getBooleanValue() ? "export" : "";
            } else {
                return super.getPersistableValue(property);
            }
        }

        @Override
        public boolean isEqual(PropertySimple property, String value) {
            if (EXPORT.equals(property.getName())) {
                if (property.getBooleanValue()) {
                    return "export".equalsIgnoreCase(value);
                } else {
                    return value == null || value.length() == 0;
                }
            } else {
                return super.isEqual(property, value);
            }
        }
    }
    
    private static class EntryCreator implements NewEntryCreator {

        public List<OpDef> getInstructions(CommonTree fullTree, CommonTree immediateParent, Property property) {
            if (EXPORT.equals(property.getName()) && ((PropertySimple)property).getBooleanValue()) {
                //immediate parent should never be null
                if (immediateParent != null) {
                    OpDef op = new OpDef();
                    op.type = OpType.INSERT_BEFORE;
                    op.text = "export ";
                    op.tokenIndex = immediateParent.getTokenStartIndex();
                    return Collections.singletonList(op);
                }
            } else if (ASSIGNMENT.equals(property.getName())) {
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
                
                PropertySimple export = assignment.getSimple(EXPORT);
                if (export != null && export.getBooleanValue()) {
                    bld.append("export ");
                }
                
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
    
    private static ConfigMapper getMapper() {
        return new ConfigMapper(getConfigurationDefinition(), new CustomFacade(), new EntryCreator(), SimpleParser.tokenNames);
    }
    
    private static TokenRewriteStream getStream(InputStream stream) throws IOException {
        return new TokenRewriteStream(new SimpleLexer(new ANTLRInputStream(stream)));
    }
    
    private static TokenRewriteStream getStream() throws IOException {
        InputStream inputStream = SimpleTest.class.getClassLoader().getResourceAsStream("simple");
        return getStream(inputStream);
    }
    
    private static CommonTree loadFile(TokenRewriteStream stream) throws IOException, RecognitionException {
        SimpleParser parser = new SimpleParser(stream);
        SimpleParser.file_return ret = parser.file();
        return (CommonTree) ret.getTree();
    }
    
    @Test
    public void testRead() throws Exception {
        Configuration config = getMapper().read(loadFile(getStream()));
        
        assertNotNull(config, "Could not read the configuration from the file.");
        
        PropertyList file = config.getList(FILE);
        
        assertNotNull(file, "the configuration should contain a list of assignments.");
        
        assertEquals(file.getList().size(), 5, "There should be 5 assignments in the test file.");
        
        for(int i = 0; i < 5; ++i) {
            PropertyMap assignment = (PropertyMap) file.getList().get(i);
            
            String name = assignment.getSimpleValue(NAME, "");
            String value = assignment.getSimpleValue(VALUE, "");
            boolean export = assignment.getSimple(EXPORT).getBooleanValue();
            
            switch (i) {
            case 0:
                assertEquals(name, "a", "The first variable should be called 'a'");
                assertEquals(value, "1", "The value of the first variable should be 1.");
                assertEquals(export, false, "The first variable shouldn't be exported.");
                break;
            case 1:
                assertEquals(name, "b", "The second variable should be called 'b'");
                assertEquals(value, "2", "The value of the second variable should be 2.");
                assertEquals(export, false, "The second variable shouldn't be exported.");
                break;
            case 2:
                assertEquals(name, "c", "The third variable should be called 'c'");
                assertEquals(value, "3", "The value of the third variable should be 3.");
                assertEquals(export, true, "The third variable should be exported.");
                break;
            case 3:
                assertEquals(name, "d", "The first variable should be called 'd'");
                assertEquals(value, "4", "The value of the first variable should be 4.");
                assertEquals(export, false, "The first variable shouldn't be exported.");
                break;
            case 4:
                assertEquals(name, "e", "The first variable should be called 'e'");
                assertEquals(value, "5", "The value of the first variable should be 5.");
                assertEquals(export, false, "The first variable shouldn't be exported.");
                break;                
            }
        }
    }
    
    @Test
    public void testUpdate() throws Exception {
        ConfigMapper mapper = getMapper();
        
        Configuration config = mapper.read(loadFile(getStream()));
        assertNotNull(config, "Could not read the configuration from the file.");
        
        PropertyList file = config.getList(FILE);
        assertNotNull(file, "the configuration should contain a list of assignments.");
        
        PropertyMap firstAssignment = (PropertyMap) file.getList().get(0);
        
        firstAssignment.getSimple(VALUE).setStringValue("12");
        firstAssignment.getSimple(EXPORT).setBooleanValue(true);
        
        Configuration config2 = storeAndLoad(config);
        
        assertEquals(config, config2);
    }
    
    @Test
    public void testCreate() throws Exception {
        Configuration config = getMapper().read(loadFile(getStream()));
        assertNotNull(config, "Could not read the configuration from the file.");
        
        PropertyList file = config.getList(FILE);
        assertNotNull(file, "the configuration should contain a list of assignments.");
        
        PropertyMap newAssignment = new PropertyMap(ASSIGNMENT);
        newAssignment.put(new PropertySimple(NAME, "new"));
        newAssignment.put(new PropertySimple(VALUE, "42"));
        newAssignment.put(new PropertySimple(EXPORT, true));
        file.add(newAssignment);
        
        Configuration config2 = storeAndLoad(config);
        
        assertEquals(config, config2);
    }
    
    @Test
    public void testDelete() throws Exception {
        Configuration config = getMapper().read(loadFile(getStream()));
        assertNotNull(config, "Could not read the configuration from the file.");
        
        PropertyList file = config.getList(FILE);        
        assertNotNull(file, "the configuration should contain a list of assignments.");
        
        //remove export c = 3
        file.getList().remove(2);
        Property firstAssignment = file.getList().remove(0);
        //add it as last
        file.add(firstAssignment);
        
        Configuration config2 = storeAndLoad(config);
        
        assertEquals(config, config2);
    }
    
    private Configuration storeAndLoad(Configuration config) throws RecognitionException, IOException {
        ConfigMapper mapper = getMapper();
        
        TokenRewriteStream stream = getStream();
        
        CommonTree file = loadFile(stream);
        
        mapper.update(file, stream, config);
        
        String updatedConfig = stream.toString();
        
        ByteArrayInputStream updatedInputStream = new ByteArrayInputStream(updatedConfig.getBytes());
        
        TokenRewriteStream updatedStream = getStream(updatedInputStream);
        
        return mapper.read(loadFile(updatedStream));
    }
}
