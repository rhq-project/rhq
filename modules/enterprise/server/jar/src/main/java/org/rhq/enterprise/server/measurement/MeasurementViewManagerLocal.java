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
package org.rhq.enterprise.server.measurement;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.common.EntityContext;

/**
 * @author Joseph Marques
 */

@Local
public interface MeasurementViewManagerLocal {

    List<String> getViewNames(Subject user, EntityContext context);

    void createView(Subject user, EntityContext context, String viewName) throws MeasurementViewException;

    void deleteView(Subject user, EntityContext context, String viewName);

    String getSelectedView(Subject user, EntityContext context);

    void setSelectedView(Subject user, EntityContext context, String viewName);

    List<String> getCharts(Subject user, EntityContext context, String viewName) throws MeasurementViewException;

    void saveCharts(Subject user, EntityContext context, String viewName, List<String> charts);

    void moveChartUp(Subject user, EntityContext context, String viewName, String viewKey);

    void moveChartDown(Subject user, EntityContext context, String viewName, String viewKey);

    void addChart(Subject user, EntityContext context, String viewName, String viewKey);

    void removeChart(Subject user, EntityContext context, String viewName, String viewKey);
}
