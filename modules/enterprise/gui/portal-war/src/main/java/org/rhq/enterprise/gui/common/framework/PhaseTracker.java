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
package org.rhq.enterprise.gui.common.framework;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A phase listener that trackS which phase of the JSF lifecycle the current request is in.  
 * It also provides a public static getter for other components that want to know what phase 
 * they are in, which could be useful for condition processing.
 *
 * @author Joseph Marques
 */
public class PhaseTracker implements PhaseListener {

    private static final long serialVersionUID = -138417172988283133L;

    private final Log log = LogFactory.getLog(this.getClass());

    private static PhaseId currentPhase;

    public static PhaseId getCurrentPhase() {
        return currentPhase;
    }

    public PhaseId getPhaseId() {
        return PhaseId.ANY_PHASE;
    }

    public void beforePhase(PhaseEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Just BEFORE the JSF Phase " + event.getPhaseId());
        }
        currentPhase = event.getPhaseId();
    }

    public void afterPhase(PhaseEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Just AFTER the JSF Phase " + event.getPhaseId());
        }
    }
}