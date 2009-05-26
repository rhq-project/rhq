/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5.util;

import org.jboss.on.common.jbossas.AbstractJBossASContentFacetDelegate;
import org.jboss.on.common.jbossas.JBPMWorkflowManager;

/**
 * Specialization of the content facet delegate for JBoss AS 5.
 * 
 * @author Lukas Krejci
 */
public class JBossASContentFacetDelegate extends AbstractJBossASContentFacetDelegate {

    public JBossASContentFacetDelegate(JBPMWorkflowManager workflowManager) {
        super(workflowManager);
    }

    //TODO what exactly do we need reimplemented here? Come back here once we have
    //support for operations in ApplicationServerComponent so that we can finish
    //the content support.
}
