/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.storage.installer;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.rhq.core.util.PropertiesFileUpdate;

public enum StorageProperty {
    HOSTNAME("rhq.storage.hostname"), //
    RPC_ADDRESS("rhq.storage.rpc-address"), //
    LISTEN_ADDRESS("rhq.storage.listen-address"), //
    SEEDS("rhq.storage.seeds"), //
    CQL_PORT("rhq.storage.cql-port"), //
    JMX_PORT("rhq.storage.jmx-port"), //
    GOSSIP_PORT("rhq.storage.gossip-port"), //
    COMMITLOG("rhq.storage.commitlog"), //
    DATA("rhq.storage.data"), //
    SAVED_CACHES("rhq.storage.saved-caches"), //
    HEAP_SIZE("rhq.storage.heap-size"), //
    HEAP_NEW_SIZE("rhq.storage.heap-new-size"), //
    STACK_SIZE("rhq.storage.stack-size"), //
    VERIFY_DATA_DIRS_EMPTY("rhq.storage.verify-data-dirs-empty");

    private static final HashSet<StorageProperty> STRINGS = new HashSet<StorageProperty>();
    static {
        // currently no required string properties
    }

    private static final HashSet<StorageProperty> INTEGERS = new HashSet<StorageProperty>();
    static {
        INTEGERS.add(CQL_PORT);
        INTEGERS.add(JMX_PORT);
        INTEGERS.add(GOSSIP_PORT);
    }

    private static final HashSet<StorageProperty> BOOLEANS = new HashSet<StorageProperty>();
    static {
        BOOLEANS.add(VERIFY_DATA_DIRS_EMPTY);
    }

    // validate optional non-string properties, if set
    private static final HashSet<StorageProperty> OPTIONAL = new HashSet<StorageProperty>();
    static {
        OPTIONAL.add(CQL_PORT);
        OPTIONAL.add(JMX_PORT);
        OPTIONAL.add(GOSSIP_PORT);
        OPTIONAL.add(VERIFY_DATA_DIRS_EMPTY);
    }

    private String property;

    StorageProperty(String property) {
        this.property = property;
    }

    public String property() {
        return this.property;
    }

    @Override
    public String toString() {
        return this.property;
    }

    public static void validate(File storagePropertiesFile) throws Exception {
        validate(storagePropertiesFile, null);
    }

    /**
     * @param storagePropertiesFile
     * @param additionalProperties additional properties that should be set (present and not empty). can be null.
     * @throws Exception
     */
    public static void validate(File storagePropertiesFile, Set<StorageProperty> additionalProperties) throws Exception {
        if (!storagePropertiesFile.isFile()) {
            throw new Exception("Properties file not found: [" + storagePropertiesFile.getAbsolutePath() + "]");
        }

        PropertiesFileUpdate pfu = new PropertiesFileUpdate(storagePropertiesFile.getAbsolutePath());
        Properties props = pfu.loadExistingProperties();
        final HashMap<String, String> map = new HashMap<String, String>(props.size());
        for (Object property : props.keySet()) {
            map.put(property.toString(), props.getProperty(property.toString()));
        }

        validate(map, additionalProperties);
    }

    public static void validate(Map<String, String> storageProperties) throws Exception {
        validate(storageProperties, null);
    }

    /**
     * @param storageProperties
     * @param additionalProperties additional properties that should be set (present and not empty). can be null.
     * @throws Exception
     */
    public static void validate(Map<String, String> storageProperties, Set<StorageProperty> additionalProperties)
        throws Exception {
        final StringBuilder dataErrors = new StringBuilder();

        for (StorageProperty storageProperty : BOOLEANS) {
            String val = storageProperties.get(storageProperty.property());
            if (isEmpty(val) && OPTIONAL.contains(storageProperty)) {
                continue;
            }
            if (!("true".equals(val) || "false".equals(val))) {
                dataErrors
                    .append("[" + storageProperty + "] must exist and be set 'true' or 'false' : [" + val + "]\n");
            }
        }

        for (StorageProperty storageProperty : StorageProperty.INTEGERS) {
            String val = storageProperties.get(storageProperty.property);
            if (isEmpty(val) && OPTIONAL.contains(storageProperty)) {
                continue;
            }
            try {
                Integer.parseInt(val);
            } catch (NumberFormatException e) {
                dataErrors.append("[" + storageProperty + "] must exist and be set to a number : [" + val + "]\n");
            }
        }

        Set<StorageProperty> requiredStringProperties = new HashSet<StorageProperty>();
        requiredStringProperties.addAll(STRINGS);
        if (null != additionalProperties) {
            requiredStringProperties.addAll(additionalProperties);
        }
        for (StorageProperty storageProperty : requiredStringProperties) {
            String val = storageProperties.get(storageProperty.property);
            if (isEmpty(val)) {
                dataErrors.append("[" + storageProperty + "] must exist and be set to a valid string value\n");
            }
        }

        for (String property : storageProperties.keySet()) {
            boolean unknown = true;
            for (StorageProperty storageProperty : EnumSet.allOf(StorageProperty.class)) {
                if (storageProperty.property.equals(property)) {
                    unknown = false;
                    break;
                }
            }
            if (unknown) {
                dataErrors.append("[" + property
                    + "] property found in file but not recognized. Please fix or remove.\n");
            }
        }

        if (dataErrors.length() > 0) {
            throw new Exception("Validation errors:\n" + dataErrors.toString());
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

}
