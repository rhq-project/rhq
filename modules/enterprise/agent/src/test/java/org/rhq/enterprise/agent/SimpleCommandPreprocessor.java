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
package org.rhq.enterprise.agent;

import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.CommandPreprocessor;

/**
 * A preprocessor our tests can use that simply add a constant string to each command's configuration.
 * {@link SimpleCommandAuthenticator} will authenticate commands that have the string this preprocessor puts in the
 * command.
 *
 * @author John Mazzitelli
 */
public class SimpleCommandPreprocessor implements CommandPreprocessor {
    /**
     * @see CommandPreprocessor#preprocess(Command, ClientCommandSender)
     */
    public void preprocess(Command command, ClientCommandSender sender) {
        String prop = SimpleCommandAuthenticator.AUTHENTICATION_PROP;
        String value = SimpleCommandAuthenticator.AUTHENTICATION_PROP_VALUE;
        command.getConfiguration().setProperty(prop, value);
    }
}