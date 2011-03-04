/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.client.proxy;

import org.rhq.enterprise.client.ClientMain;
import org.rhq.bindings.output.TabularWriter;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.configuration.definition.PropertyGroupDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionEnumeration;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;

import jline.ConsoleReader;
import jline.ConsoleOperations;

/**
 * @author Greg Hinkle
 */
public class ConfigurationEditor {

    private ClientMain client;
    private PrintWriter writer;
    private ConsoleReader console;


    public ConfigurationEditor(ClientMain client) {
        this.client = client;
        this.writer = client.getPrintWriter();
        this.console = client.getConsoleReader();
    }


    /**
     * Edit a configuration interactively. Return the altered config if the user chooses
     * to save or null otherwise.
     *
     * @param def
     * @param config
     * @return
     * @throws IOException
     */
    public Configuration editConfiguration(ConfigurationDefinition def, Configuration config) {

        try {
            Configuration newConfig = config.deepCopy();
            newConfig = editExistingConfiguration(def, newConfig);

            while (true) {
                String input = console.readLine("[R]eview, [E]dit, Re[V]ert [S]ave or [C]ancel: ");

                char inputChar = input.charAt(0);
                switch (inputChar) {
                    case 'r':
                    case 'R':
                        printConfiguration(newConfig);
                        continue;
                    case 'e':
                    case 'E':
                        editExistingConfiguration(def, newConfig);
                        continue;
                    case 'v':
                    case 'V':
                        newConfig = config;
                        continue;
                    case 's':
                    case 'S':
                        return newConfig;
                    case 'c':
                    case 'C':
                        return null;
                    default:
                        writer.println("unknown option");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (QuitException e) {
            writer.println("Cancelling edit");
            return null;
        }
    }


    public Configuration createConfiguration(ConfigurationDefinition def) throws IOException, QuitException {
        if (client.isInteractiveMode()) {
            throw new UnsupportedOperationException("Configuration wizard is only available in interactive model.");
        }

        Map<String, ConfigurationTemplate> templates = def.getTemplates();

        ConfigurationTemplate template = (ConfigurationTemplate) question(templates, null, "template", "Select from the available templates");

        return editConfiguration(def, template == null ? null : template.createConfiguration());
    }

    public Configuration editExistingConfiguration(ConfigurationDefinition def, Configuration config) throws IOException, QuitException {
        List<PropertyGroupDefinition> groups = new ArrayList<PropertyGroupDefinition>(def.getGroupDefinitions());

        if (config == null) {
            config = new Configuration();
        }

        writer.println("Non-Grouped Properties: ");
        List<PropertyDefinition> properties = new ArrayList<PropertyDefinition>(def.getNonGroupedProperties());
        Collections.sort(properties, new Comparator<PropertyDefinition>() {
            public int compare(PropertyDefinition o1, PropertyDefinition o2) {
                return new Integer(o1.getOrder()).compareTo(new Integer(o2.getOrder()));
            }
        });

        for (PropertyDefinition propDef : properties) {
            question(propDef, config);
        }


        Collections.sort(groups, new Comparator<PropertyGroupDefinition>() {
            public int compare(PropertyGroupDefinition o1, PropertyGroupDefinition o2) {
                return new Integer(o1.getOrder()).compareTo(new Integer(o2.getOrder()));
            }
        });


        for (PropertyGroupDefinition groupDef : groups) {
            writer.println("Group: " + groupDef.getDisplayName());

            properties = new ArrayList<PropertyDefinition>(def.getPropertiesInGroup(groupDef.getName()));
            Collections.sort(properties, new Comparator<PropertyDefinition>() {
                public int compare(PropertyDefinition o1, PropertyDefinition o2) {
                    return new Integer(o1.getOrder()).compareTo(new Integer(o2.getOrder()));
                }
            });

            for (PropertyDefinition propDef : properties) {
                question(propDef, config);
            }
        }


        return config;
    }


    private void question(PropertyDefinition definition, Configuration template) throws IOException, QuitException {
        if (definition instanceof PropertyDefinitionSimple) {
            PropertyDefinitionSimple simpleDef = (PropertyDefinitionSimple) definition;
            String base = null;
            if (template != null && template.getSimple(definition.getName()) != null) {
                PropertySimple simple = template.getSimple(definition.getName());

                if (simple == null) {
                    simple = new PropertySimple(definition.getName(), null);
                    template.put(simple);
                }

                List<PropertyDefinitionEnumeration> optionList = simpleDef.getEnumeratedValues();
                if (optionList != null && optionList.size() > 0) {
                    // Select from a set of enumerate options

                    Map options = new LinkedHashMap();
                    for (PropertyDefinitionEnumeration enumValue : simpleDef.getEnumeratedValues()) {
                        options.put(enumValue.getName(), enumValue.getValue());
                    }

                    Object result = question(options, simple.getStringValue(), definition.getName(), definition.getDescription());
                    if (result == null) {
                        template.remove(simple.getName());
                    } else {
                        simple.setValue(result);
                    }
                } else {

                    while (true) {
                        String currentValue = simple.getStringValue();
                        String prompt = definition.getName() + (currentValue != null ? "[" + currentValue + "]" : "") + ": ";

                        String input = getInput(prompt, definition.getDescription());
                        if (input == null) {
                            template.remove(simple.getName());
                            break;
                        } else {

                            try {
                                String newval = validate(simpleDef, input);
                                simple.setStringValue(newval);
                                break;
                            } catch (Exception e) {
                                writer.println("Invalid value for " + simpleDef.getType().name() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }


    public String validate(PropertyDefinitionSimple def, String value) {

        switch (def.getType()) {
            case BOOLEAN:
                if ("y".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "t".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) {
                    return Boolean.TRUE.toString();
                } else if ("n".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "f".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    return Boolean.FALSE.toString();
                }
                break;
            case INTEGER:
                return String.valueOf(Integer.parseInt(value));
            case LONG:
                return String.valueOf(Long.parseLong(value));
            case FLOAT:
                return String.valueOf(Float.parseFloat(value));
            case DOUBLE:
                return String.valueOf(Double.parseDouble(value));
        }
        return null;
    }


    private Object question(Map map, String currentValue, String prompt, String help) throws IOException, QuitException {
        PrintWriter writer = client.getPrintWriter();

        if (map != null && map.size() > 0) {
            writer.println("Select a " + prompt + ":");

            int i = 1;
            int currentSelection = -1;
            for (Object key : map.keySet()) {
                if (currentValue != null && currentValue.equals(map.get(key)))
                    currentSelection = i;
                writer.println("\t" + i++ + ") " + key);

            }

            writer.flush();

            Object answer = null;
            while (answer == null) {
                String input = getInput(prompt + "[" + currentSelection + "]:", help);

                int index = -1;
                if (input == null) {
                    return null;
                } else if (input.length() == 0) {
                    index = currentSelection;
                } else {
                    index = Integer.parseInt(input);
                }

                if (index == 0) {
                    return null;
                }

                i = 1;
                for (Object templateName : map.keySet()) {
                    if (i++ == index) {
                        answer = map.get(templateName);
                        writer.println("\t" + templateName + " selected.");
                    }
                }
            }
            return answer;
        } else {
            return null;
        }
    }


    private void printConfiguration(Configuration configuration) {
        TabularWriter tw = new TabularWriter(writer);
        tw.print(configuration);
    }


    private String getInput(String prompt, String extraHelp) throws IOException, QuitException {
        StringBuilder buf = new StringBuilder();
        writer.print(prompt);
        writer.flush();
        try {
            while (true) {
                int ch = console.readVirtualKey();

                switch ((char) ch) {
                    case ConsoleOperations.CTRL_K:
                        // quit editing
                        throw new QuitException();
                    case ConsoleOperations.CTRL_D:
                        // unset
                        return null;
                    case ConsoleOperations.CTRL_E:
                        writer.println();
                        writer.println(extraHelp);
                        writer.print(prompt);
                        writer.print(buf.toString());
                        writer.print(prompt);
                        writer.flush();                        
                        break;
                    case 10:
                        return buf.toString();
                    default:
                        buf.append(ch);
                        break;

                }
            }
        } finally {
            writer.println();
            writer.flush();
        }
    }

    static class QuitException extends Exception {
    }

}
