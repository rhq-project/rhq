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

package org.rhq.gui.webdav;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The measurement traits resource that provides information on all traits of a managed resource.
 * 
 * @author John Mazzitelli
 */
public class TraitsResource extends GetableBasicResource {

    private List<MeasurementDataTrait> traits;
    private String content;

    public TraitsResource(Subject subject, Resource managedResource) {
        super(subject, managedResource);
    }

    public String getUniqueId() {
        return "traits_" + getManagedResource().getId();
    }

    public String getName() {
        return "measurement_traits.xml";
    }

    /**
     * The modified date is that of the last time a trait changed.
     */
    public Date getModifiedDate() {
        long latestTimestamp = 0L;
        for (MeasurementDataTrait trait : getTraits()) {
            if (latestTimestamp < trait.getTimestamp()) {
                latestTimestamp = trait.getTimestamp();
            }
        }
        return new Date(latestTimestamp);
    }

    /**
     * The created date is the date of the resource itself was created.
     */
    public Date getCreateDate() {
        return new Date(getManagedResource().getCtime());
    }

    protected String loadContent() {
        if (this.content == null) {
            StringBuilder str = new StringBuilder();
            str.append("<?xml version=\"1.0\"?>\n");
            str.append("<traits>\n");
            for (MeasurementDataTrait trait : getTraits()) {
                str.append("   <trait>\n");
                str.append("      <name>").append(trait.getName()).append("</name>\n");
                str.append("      <value>").append(trait.getValue()).append("</value>\n");
                str.append("      <last-changed>").append(new Date(trait.getTimestamp())).append("</last-changed>\n");
                str.append("      <schedule-id>").append(trait.getScheduleId()).append("</schedule-id>\n");
                str.append("   </trait>\n");
            }
            str.append("</traits>\n");
            this.content = str.toString();
        }
        return this.content;
    }

    private List<MeasurementDataTrait> getTraits() {
        if (this.traits == null) {
            MeasurementDataManagerLocal mdm = LookupUtil.getMeasurementDataManager();
            List<MeasurementDataTrait> traits = mdm.findCurrentTraitsForResource(getSubject(), getManagedResource()
                .getId(), null);
            if (traits != null) {
                this.traits = traits;
            } else {
                this.traits = new ArrayList<MeasurementDataTrait>();
            }
        }
        return this.traits;
    }
}
