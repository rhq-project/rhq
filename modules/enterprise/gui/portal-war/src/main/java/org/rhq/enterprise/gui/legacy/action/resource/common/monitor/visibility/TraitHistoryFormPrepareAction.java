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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Obtain the historical data of a trait and put it into the session.
 *
 * @author Heiko W. Rupp
 */
public class TraitHistoryFormPrepareAction extends TilesAction {
    /**
     * Obtain the historical data of a trait and put it into the session. Input: resource_id: Id of a Resource<br/>
     * Input: m : Id of a MeasurementDefinition<br/>
     * Output: traits: List of MeasurementDataTrait<br/>
     * Output: traitName: Name of the Trait
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        MeasurementDataManagerLocal mgr = LookupUtil.getMeasurementDataManager();

        int resourceId = WebUtility.getResourceId(request);
        int definitionId = WebUtility.getRequiredIntRequestParameter(request, "m");

        List<MeasurementDataTrait> traits = mgr.getAllTraitDataForResourceAndDefinition(resourceId, definitionId);

        if (traits.size() > 0) {
            MeasurementDataTrait mdt = traits.get(0);
            request.setAttribute("traitName", mdt.getName());
        }

        request.setAttribute("traits", traits);
        request.setAttribute("rid", resourceId);

        return super.execute(mapping, form, request, response);
    }
}