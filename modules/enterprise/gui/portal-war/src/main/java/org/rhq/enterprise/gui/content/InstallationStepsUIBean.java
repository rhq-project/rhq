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
package org.rhq.enterprise.gui.content;

import java.util.ArrayList;
import java.util.List;
import javax.faces.component.UIData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.content.transfer.DeployPackageStep;

/**
 * @author Jason Dobies
 */
public class InstallationStepsUIBean {

    private List<DeployPackageStep> deploySteps;
    private UIData stepsData;

    private final Log log = LogFactory.getLog(this.getClass());

    public List<DeployPackageStep> getDeploySteps() {
        if (deploySteps == null) {

            // TEST DATA
            deploySteps = new ArrayList<DeployPackageStep>();

            deploySteps.add(new DeployPackageStep("0", "Backup some file somewhere"));
            deploySteps.add(new DeployPackageStep("1", "Do some more stuff"));
            deploySteps.add(new DeployPackageStep("2", "Restart something"));
            deploySteps.add(new DeployPackageStep("3", "Drink beer"));
        }

        return deploySteps;
    }
    public UIData getStepsData() {
        return stepsData;
    }

    public void setStepsData(UIData stepsData) {
        this.stepsData = stepsData;
    }
}
