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

package org.rhq.rhqtransform;

import org.rhq.augeas.AugeasProxy;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

/**
 * An overarching interface above {@link AugeasToConfiguration} and {@link ConfigurationToAugeas}
 * to provide an easy default access to the main functionality to the users.
 * The main raison d'etre for this interface is to provide the ability to completely abstract out
 * the dealing with Augeas from the classes that should only deal with RHQ configuration.
 * 
 * @author Filip Drabek
 *
 */
public interface RhqAugeasMapping {

    /**
     * Updates the Augeas data from the RHQ configuration instance.
     * 
     * @param augeasProxy the acces to Augeas
     * @param config the configuration to persist
     * @param configDef the definition of the configuration
     * @throws AugeasRhqException
     */
    public void updateAugeas(AugeasProxy augeasProxy, Configuration config, ConfigurationDefinition configDef)
        throws AugeasRhqException;

    /**
     * Reads in the RHQ configuration from Augeas.
     * 
     * @param augeasProxy the access to Augeas
     * @param configDef the configuration definition 
     * @return the configuration created from the definition and the data in Augeas
     * @throws AugeasRhqException
     */
    public Configuration updateConfiguration(AugeasProxy augeasProxy, ConfigurationDefinition configDef)
        throws AugeasRhqException;

}
