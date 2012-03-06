/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.arquillian.spi;

import org.jboss.arquillian.test.spi.TestEnricher;

/**
 * Implementations of this interface have similar purpose as the {@link TestEnricher}s.
 * The difference is that the {@link PostPrepareEnricher}s are executed only after
 * the plugin container has been fully prepared, that is all the {@link PluginContainerPreparator}s
 * and {@link PluginContainerOperation}s have run on the current plugin container.
 * <p>
 * This is useful if you want to inject something into the test class that is only available
 * after the discovery has run.
 * @author Lukas Krejci
 */
public interface PostPrepareEnricher {

    void enrich(Object testInstance);
}
