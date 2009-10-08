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
package org.rhq.enterprise.gui.measurement;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * JSF managed bean for the display of summary traits on the top of a resource page.
 *
 * <p/>The Constructor fetches the trait data from the server and fills them into a list of trait pairs, each pair to be
 * represented as a row in a two-column table display.
 *
 * @author Heiko W. Rupp
 * @author Ian Springer
 */
public class TraitListBean {

    private List<List<MeasurementDataTrait>> traitPairs;
    private int totalTraits;

    private MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();

    public TraitListBean() {

    }

    private void load() {
        int resourceId = EnterpriseFacesContextUtility.getResource().getId();
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        List<MeasurementDataTrait> traits = this.dataManager.findCurrentTraitsForResource(subject, resourceId,
            DisplayType.SUMMARY);
        this.totalTraits = traits.size();
        int middleIndex = (this.totalTraits + 1) / 2;
        this.traitPairs = new ArrayList<List<MeasurementDataTrait>>(middleIndex);
        if (this.totalTraits > 0) {
            List<MeasurementDataTrait> leftColumnTraits = traits.subList(0, middleIndex);
            List<MeasurementDataTrait> rightColumnTraits = traits.subList(middleIndex, this.totalTraits);
            for (int i = 0; i < leftColumnTraits.size(); i++) {
                List<MeasurementDataTrait> traitPair = new ArrayList<MeasurementDataTrait>(2);
                traitPair.add(leftColumnTraits.get(i));
                if (i < rightColumnTraits.size()) {
                    traitPair.add(rightColumnTraits.get(i));
                } else {
                    traitPair.add(null);
                }

                this.traitPairs.add(traitPair);
            }
        }
    }

    public List<List<MeasurementDataTrait>> getTraitPairs() {
        if (this.traitPairs == null) {
            try {
                load();
            } catch (Exception e) {
                // This happens sometimes when the summary areas is closed, but we won't worry about it
            }
        }
        return this.traitPairs;
    }

    public int getTotalTraits() {
        if (this.traitPairs == null)
            load();
        return this.totalTraits;
    }
}