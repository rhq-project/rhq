/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc;

/**
 * Validates that a given plugin descriptor is correct.
 *
 * This is usually used during a build of a server plugin, but can be used at runtime
 * to confirm a descriptor is valid. This does more than just validating the XML
 * descriptor parses (in fact, that is confirmed before this validator object is ever
 * called). Instead, it detects if things such as classnames and configurations match
 * was is to be expected.
 * 
 * Implementors of this class must have a no-arg constructor so it can be
 * found and created by {@link ServerPluginValidatorUtil}. The names of the
 * implementor classes (if they are to be used by that utility) must match the
 * name of the plugin type followed by "Validator" (e.g. "GenericPluginValidator"
 * or "AlertPluginValidator"). They must be in a subpackage under the
 * package where this interface is, where that subpackage is named
 * for that plugin type (minus the word "Plugin" and all lowercase) such as
 * ".generic" or ".alert".
 *
 * Note that when this validator is invoked, the plugin descriptor has already had
 * its basics validated (that is, it is assured the descriptor XML is well-formed
 * and validates against the XML schema, the plugin has been given a version, its
 * plugin component class is valid and can be instantiated (if one was specified)
 * among other things). Implementors of this validator class need only validate
 * specific things that are unique to the specific plugin type for which this
 * validator is used.
 *
 * @author John Mazzitelli
 */
public interface ServerPluginValidator {

    /**
     * Given a server plugin environment, this method should validate the descriptor
     * to ensure it is logically correct.
     * 
     * @param env the environment of the plugin to test
     * @return <code>true</code> if the plugin descriptor is valid, <code>false</code> otherwise
     */
    boolean validate(ServerPluginEnvironment env);
}
