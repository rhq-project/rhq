/*
 * RHQ Management Platform
 * Copyright (C) 2014 Red Hat, Inc.
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

package org.rhq.core.clientapi.agent.lifecycle;

/**
 * This is an interface of a remote POJO that can be used to manipulate the running state of agent's plugin container.
 * <p/>
 * In another words, on the server, using the {@code ClientCommandSender.getRemotePojoFactory} one can obtain an
 * instance of this interface that will be executed on the agent (the other direction is NOT possible and wouldn't make
 * sense anyway, because there is no agent plugin container on the server).
 *
 * @author Lukas Krejci
 * @since 4.11
 */
public interface PluginContainerLifecycle {

    void start();

    void stop();

    void updatePlugins();
}
