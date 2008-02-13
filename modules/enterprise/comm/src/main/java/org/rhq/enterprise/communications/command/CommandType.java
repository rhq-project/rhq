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
package org.rhq.enterprise.communications.command;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Encapsulates the identification of a command type. This class is immutable.
 *
 * @author John Mazzitelli
 */
public class CommandType implements Serializable, Comparable {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(CommandType.class);

    /**
     * the command name
     */
    private final String m_name;

    /**
     * the version of the command
     */
    private final int m_version;

    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link CommandType}.
     *
     * @param  name    the name of the command type
     * @param  version the version of the command type
     *
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code> or empty
     */
    public CommandType(String name, int version) throws IllegalArgumentException {
        if ((name == null) || (name.trim().length() == 0)) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.EMPTY_NAME));
        }

        m_name = name.trim();
        m_version = version;

        return;
    }

    /**
     * Copy constructor for {@link CommandType}.
     *
     * @param original the original object to copy
     */
    public CommandType(CommandType original) {
        this(original.m_name, original.m_version);
    }

    /**
     * Constructor for {@link CommandType} given a string string that encodes both the command type name and version.
     * The string must separate the name and version with a space. The version may simply be a number or (as with the
     * format returned by {@link #toString()}) may be prefixed with 'v'. Examples: "cmd 5", "cmd v5". If just the name
     * was specified with no version, the version will default to 1.
     *
     * @param  nameVersion command type name and version separated with a space
     *
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code> or <code>nameVersion</code> was
     *                                  invalid
     */
    public CommandType(String nameVersion) throws IllegalArgumentException {
        if (nameVersion == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NULL_NAME_VERSION));
        }

        StringTokenizer strtok = new StringTokenizer(nameVersion, " ");

        try {
            m_name = strtok.nextToken();

            if (strtok.hasMoreTokens()) {
                String versionString = strtok.nextToken();

                if (Character.toLowerCase(versionString.charAt(0)) == 'v') {
                    versionString = versionString.substring(1);
                }

                m_version = Integer.parseInt(versionString);
            } else {
                // no version string was specified, we'll default to version 1
                m_version = 1;
            }

            // there should be no more tokens - if there are, the nameVersion string was invalid
            if (strtok.hasMoreTokens()) {
                throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.INVALID_NAME_VERSION,
                    nameVersion));
            }
        } catch (NoSuchElementException nsee) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NAME_MISSING, nameVersion));
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.VERSION_INVALID, nameVersion));
        }

        return;
    }

    /**
     * Returns the name of this command type.
     *
     * @return name command type name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the version number associated with this command type.
     *
     * @return version version number
     */
    public int getVersion() {
        return m_version;
    }

    /**
     * This will return the name of the command following by the version (prefixed with a 'v') unless the version number
     * is 1, in which case just the name is returned. For example, "mycommand v2" or "mycommand".
     *
     * @see Object#toString()
     */
    public String toString() {
        String retString = m_name;

        if (m_version != 1) {
            retString += " v" + m_version;
        }

        return retString;
    }

    /**
     * CommandType objects are equal if they have the same {@link #getName() name} and same
     * {@link #getVersion() version}.
     *
     * @see Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof CommandType)) {
            return false;
        }

        CommandType compareThis = (CommandType) obj;

        if (compareThis == this) {
            return true;
        }

        return (this.m_name.equals(compareThis.m_name)) && (this.m_version == compareThis.m_version);
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode() {
        return (m_name + m_version).hashCode();
    }

    /**
     * Used strictly to determine if one command type has a higher or lower version number than another with the same
     * {@link #getName() command type name}. If the given object's command type name does not equal this object's
     * command type name, an <code>IllegalArgumentException</code> is thrown - you cannot compare two command types with
     * different names.
     *
     * @see Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object obj) throws IllegalArgumentException {
        if (obj == null) {
            throw new ClassCastException("obj=null");
        }

        // will throw ClassCastException, as per Comparable contract, if obj is not a CommandType
        CommandType compareThis = (CommandType) obj;

        if (this.equals(compareThis)) {
            return 0;
        }

        if (!this.m_name.equals(compareThis.m_name)) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.CANNOT_COMPARE_DIFF_NAMES,
                this.m_name, compareThis.m_name));
        }

        return (this.m_version > compareThis.m_version) ? 1 : (-1);
    }
}