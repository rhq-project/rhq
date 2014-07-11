/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.core.util.obfuscation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.security.vault.SecurityVault;
import org.jboss.security.vault.SecurityVaultException;

import org.rhq.core.util.obfuscation.ObfuscatedPreferences.RestrictedFormat;

/**
 * @author Stefan Negrea
 *
 */
public class PropertyObfuscationVault implements SecurityVault {

    private static final Log LOG = LogFactory.getLog(SecurityVault.class);

    private static final String RESTRICTED = "restricted";

    private volatile Map<String, Object> options;

    @Override
    public void init(Map<String, Object> options) throws SecurityVaultException {
        this.options = Collections.synchronizedMap(new HashMap<String, Object>());
        this.options.putAll(options);
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public byte[] handshake(Map<String, Object> handshakeOptions) throws SecurityVaultException {
        return null;
    }

    @Override
    public Set<String> keyList() throws SecurityVaultException {
        return options.keySet();
    }

    @Override
    public boolean exists(String vaultBlock, String attributeName) throws SecurityVaultException {
        return true;
    }

    @Override
    public void store(String vaultBlock, String attributeName, char[] attributeValue, byte[] sharedKey)
        throws SecurityVaultException {
        //nothing to do because this vault does not have any backing store
    }

    /**
     * Documentation provided since slightly changing the meaning of method parameters
     * from the overriden method.
     *
     * @param vaultBlock  if 'restricted' then the value is obfuscated; if 'open' then value is not obfuscated
     * @param attributeName name of system property where that contains the value
     * @param sharedKey default value if no system property found or empty
     * @return value
     * @throws SecurityVaultException
     */
    @Override
    public char[] retrieve(String vaultBlock, String attributeName, byte[] sharedKey) throws SecurityVaultException {

        LOG.info("Deobfuscations result [" + vaultBlock + "-" + attributeName + "-" + new String(sharedKey) + "] ");

        try {
            boolean isRestricted = false;
            if (RESTRICTED.equals(vaultBlock)) {
                isRestricted = true;
            }

            char[] result = null;

            String systemPropertyValue = System.getProperty(attributeName);
            if (systemPropertyValue != null && !systemPropertyValue.trim().isEmpty()) {
                if (isRestricted) {
                    String actualSystemPropertyValue = systemPropertyValue;
                    if (RestrictedFormat.isRestrictedFormat(actualSystemPropertyValue)) {
                        actualSystemPropertyValue = RestrictedFormat.retrieveValue(actualSystemPropertyValue);
                    }

                    try {
                        result = PicketBoxObfuscator.decode(actualSystemPropertyValue).toCharArray();
                    } catch (Exception e) {
                        //have a fallback in case the password not obfuscated
                        result = actualSystemPropertyValue.toCharArray();
                    }
                } else {
                    result = systemPropertyValue.toCharArray();
                }

            } else if (sharedKey != null && sharedKey.length != 0) {
                if (isRestricted) {
                    try {
                        result = PicketBoxObfuscator.decode(new String(sharedKey)).toCharArray();
                    } catch (Exception e) {
                        //have a fallback in case the password not obfuscated
                        result = new String(sharedKey).toCharArray();
                    }
                } else {
                    result = new String(sharedKey).toCharArray();
                }
            } else {
                //ran out of options to de-obfuscate so throw an exception
                throw new IllegalArgumentException();
            }

            return result;
        } catch (Exception e) {
            throw new SecurityVaultException(e);
        }
    }

    @Override
    public boolean remove(String vaultBlock, String attributeName, byte[] sharedKey) throws SecurityVaultException {
        return true;
    }
}

