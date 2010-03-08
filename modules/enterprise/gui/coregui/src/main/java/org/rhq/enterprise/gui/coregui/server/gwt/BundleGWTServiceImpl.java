/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class BundleGWTServiceImpl extends AbstractGWTServiceImpl implements BundleGWTService {
    private static final long serialVersionUID = 1L;

    private BundleManagerLocal bundleManager = LookupUtil.getBundleManager();

    // TODO: this just illustrates how we are to prepare the results for the GWT client on the other end
    public Object foo() {
        return SerialUtility.prepare(null, null);
    }
}