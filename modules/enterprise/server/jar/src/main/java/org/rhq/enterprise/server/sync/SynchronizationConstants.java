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

package org.rhq.enterprise.server.sync;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.rhq.enterprise.server.xmlschema.ConfigurationInstanceDescriptorUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
public final class SynchronizationConstants {

    public static final String CONFIGURATION_NAMESPACE = "urn:xmlns:rhq-configuration";
    public static final String CONFIGURATION_NAMESPACE_PREFIX = "c";
    public static final String CONFIGURATION_INSTANCE_NAMESPACE =
        ConfigurationInstanceDescriptorUtil.NS_CONFIGURATION_INSTANCE;
    public static final String CONFIGURATION_INSTANCE_NAMESPACE_PREFIX = "ci";
    public static final String EXPORT_NAMESPACE = "urn:xmlns:rhq-configuration-export";
    public static final String EXPORT_NAMESPACE_PREFIX = XMLConstants.DEFAULT_NS_PREFIX;
    public static final String CONFIGURATION_EXPORT_ELEMENT = "configuration-export";
    public static final String ENTITIES_EXPORT_ELEMENT = "entities";
    public static final String ENTITY_EXPORT_ELEMENT = "entity";
    public static final String ERROR_MESSAGE_ELEMENT = "error-message";
    public static final String NOTES_ELEMENT = "notes";
    public static final String DATA_ELEMENT = "data";
    public static final String VALIDATOR_ELEMENT = "validator";
    public static final String DEFAULT_CONFIGURATION_ELEMENT = "default-configuration";
    public static final String ID_ATTRIBUTE = "id";
    public static final String CLASS_ATTRIBUTE = "class";

    private SynchronizationConstants() {

    }

    public static NamespaceContext createConfigurationExportNamespaceContext() {
        return new NamespaceContext() {

            //this map has to correspond to the namespaces defined in the code above
            private final Map<String, String> PREFIXES = new HashMap<String, String>();
            {
                PREFIXES.put(EXPORT_NAMESPACE_PREFIX, EXPORT_NAMESPACE);
                PREFIXES.put(CONFIGURATION_INSTANCE_NAMESPACE_PREFIX, CONFIGURATION_INSTANCE_NAMESPACE);
                PREFIXES.put(CONFIGURATION_NAMESPACE_PREFIX, CONFIGURATION_NAMESPACE);
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                String prefix = getPrefix(namespaceURI);
                if (prefix == null) {
                    return Collections.<String> emptySet().iterator();
                } else {
                    return Collections.singleton(prefix).iterator();
                }
            }

            @Override
            public String getPrefix(String namespaceURI) {
                if (namespaceURI == null) {
                    throw new IllegalArgumentException();
                } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
                    return XMLConstants.XMLNS_ATTRIBUTE;
                } else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
                    return XMLConstants.XML_NS_PREFIX;
                } else {
                    String prefix = null;
                    for (Map.Entry<String, String> e : PREFIXES.entrySet()) {
                        String p = e.getKey();
                        String namespace = e.getValue();

                        if (namespaceURI.equals(namespace)) {
                            prefix = p;
                            break;
                        }
                    }

                    return prefix;
                }
            }

            @Override
            public String getNamespaceURI(String prefix) {
                return PREFIXES.get(prefix);
            }
        };
    }
}
