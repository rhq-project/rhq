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
package org.rhq.enterprise.communications.command.server;

import org.rhq.enterprise.communications.command.CommandExecutor;
import org.rhq.enterprise.communications.command.CommandType;

/**
 * The interface that all command services implement. Command services are responsible for actually executing commands
 * and returning their results.
 *
 * <p>This should be the MBean interface for all command services - there is no need for each service to have its own
 * <code>xxxMBean</code> interface so long as the command service implementation extends {@link CommandService}.</p>
 *
 * @author John Mazzitelli
 * @see    CommandService
 */
public interface CommandServiceMBean extends CommandExecutor {
    /**
     * Returns a set of {@link CommandType} identifiers that this service supports. This will identify which commands
     * this command service provides (i.e. it indicates the commands this service can execute).
     *
     * <p>The returned array should be fixed during the lifetime of this service (or at least during its registration in
     * an MBeanServer). Changes to the supported command types during runtime will not be detected once the
     * {@link CommandServiceDirectory} has discovered this service. As a corollary to this rule, this method must be
     * ready to provide the array of support command types at the time it is registered on an MBeanServer (in other
     * words, this method will be called, specifically by the {@link CommandServiceDirectory directory}, as soon as this
     * service is registered).</p>
     *
     * @return array of supported command types
     */
    CommandType[] getSupportedCommandTypes();
}