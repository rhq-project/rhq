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
package org.rhq.bindings.util;

import java.util.LinkedHashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;

/**
 * @author Greg Hinkle
 */
public class ConfigurationClassBuilder {

    /**
     * @param def
     * @return Map of propertyName to types for the config def
     * @throws NotFoundException
     */
    public static LinkedHashMap<String, CtClass> translateParameters(ClassPool cp, ConfigurationDefinition def)
        throws NotFoundException {
        LinkedHashMap<String, CtClass> result = new LinkedHashMap<String, CtClass>();
        if (def == null || def.getPropertyDefinitions() == null) {
            return result;
        }

        for (PropertyDefinition pd : def.getPropertyDefinitions().values()) {
            if (pd instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple simple = (PropertyDefinitionSimple) pd;
                String name = pd.getName();
                CtClass paramType = getSimpleTypeClass(cp, simple);
                result.put(name, paramType);
            }
        }
        return result;
    }

    private static CtClass getSimpleTypeClass(ClassPool cp, PropertyDefinitionSimple simple) throws NotFoundException {
        Class<?> paramType = null;
        switch (simple.getType()) {
        case STRING:
        case LONG_STRING:
        case PASSWORD:
        case FILE:
        case DIRECTORY:
            paramType = String.class;
            break;
        case BOOLEAN:
            paramType = Boolean.TYPE;
            break;
        case INTEGER:
            paramType = Integer.TYPE;
            break;
        case LONG:
            paramType = Long.TYPE;
            break;
        case FLOAT:
            paramType = Float.TYPE;
            break;
        case DOUBLE:
            paramType = Double.TYPE;
            break;
        }
        return cp.get(paramType.getName());
    }

    public static CtClass translateConfiguration(ClassPool cp, ConfigurationDefinition def) throws NotFoundException {
        final String OPERATION_RESULT = "operationResult";
        if (def == null) {
            return CtClass.voidType;
        } else if (def.get(OPERATION_RESULT) != null && def.get(OPERATION_RESULT) instanceof PropertyDefinitionSimple) {
            // Its a simple type
            return getSimpleTypeClass(cp, def.getPropertyDefinitionSimple(OPERATION_RESULT));
        } else {

            // TODO GH: Build a custom type?
            return cp.get(Configuration.class.getName());
        }
    }

    private static String simpleName(String name) {
        return decapitalize(name.replaceAll("\\W", ""));
    }

    private static String decapitalize(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1, name.length());
    }

    public static Configuration translateParametersToConfig(ClassPool cp, ConfigurationDefinition parametersConfigurationDefinition,
        Object[] args) throws NotFoundException {
        LinkedHashMap<String, CtClass> translateParameters = translateParameters(cp, parametersConfigurationDefinition);
        Configuration config = new Configuration();

        int index = 0;
        for (String key : translateParameters.keySet()) {
            config.put(new PropertySimple(key, args[index++]));

        }
        return config;

    }

    public static Object translateResults(ClassPool cp, ConfigurationDefinition resultsConfigurationDefinition, Configuration result)
        throws NotFoundException {

        CtClass expectedReturn = translateConfiguration(cp, resultsConfigurationDefinition);

        if (expectedReturn.equals(cp.get(Configuration.class.getName()))) {
            return result;
        } else {
            //bail on translation if Configuration passed in is null
            if (result == null)
                return result;
            PropertySimple simple = result.getSimple("operationResult");
            if (simple != null) {
                if (expectedReturn.getName().equals(String.class.getName())) {
                    return simple.getStringValue();
                } else if (expectedReturn.getName().equals(Boolean.class.getName())
                    || expectedReturn.getName().equals(Boolean.TYPE.getName())) {
                    return simple.getBooleanValue();
                } else if (expectedReturn.getName().equals(Integer.class.getName())
                    || expectedReturn.getName().equals(Integer.TYPE.getName())) {
                    return simple.getIntegerValue();
                } else if (expectedReturn.getName().equals(Long.class.getName())
                    || expectedReturn.getName().equals(Long.TYPE.getName())) {
                    return simple.getLongValue();
                } else if (expectedReturn.getName().equals(Float.class.getName())
                    || expectedReturn.getName().equals(Float.TYPE.getName())) {
                    return simple.getFloatValue();
                } else if (expectedReturn.getName().equals(Double.class.getName())
                    || expectedReturn.getName().equals(Double.TYPE.getName())) {
                    return simple.getDoubleValue();
                } else if (expectedReturn.getName().equals(Boolean.class.getName())
                    || expectedReturn.getName().equals(Boolean.TYPE.getName())) {
                    return simple.getBooleanValue();
                }
            }
        }
        return null;
    }
}
