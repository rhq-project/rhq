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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7;

import org.rhq.core.pluginapi.measurement.MeasurementFacet;

/**
 * Component class for the WebConnector (subsystem=web,connector=* )
 * @author Heiko W. Rupp
 *
 * @deprecated This class no longer provides any specialized semantics for web connectors. It may be removed in the
 *             future. You can use its base class, {@link org.rhq.modules.plugins.jbossas7.BaseComponent} instead.
 */
@Deprecated
public class WebConnectorComponent extends BaseComponent<WebConnectorComponent> implements MeasurementFacet {
}
