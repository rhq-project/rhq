 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.gui.util;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

public class IdChunkGeneratorPhaseListener implements PhaseListener {
    private static final long serialVersionUID = -7648358897917067872L;

    public void beforePhase(PhaseEvent event) {
        // make sure this bean exists
        FacesContextUtility.setBean(new IdChunkGeneratorUIBean());
    }

    public void afterPhase(PhaseEvent event) {
        // nothing to do here, the job is done
    }

    public PhaseId getPhaseId() {
        // the earliest phase available
        return PhaseId.RESTORE_VIEW;
    }
}