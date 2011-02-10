/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.SystemGWTService;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Provides access to the system manager that allows you to obtain information about
 * the server as well as allowing you to reconfigure parts of the server.
 *
 * @author John Mazzitelli
 * @author Ian Springer
 */
public class SystemGWTServiceImpl extends AbstractGWTServiceImpl implements SystemGWTService {

    private static final long serialVersionUID = 1L;

    private SystemManagerLocal systemManager = LookupUtil.getSystemManager();

    @Override
    public ProductInfo getProductInfo() throws RuntimeException {
        try {
            return this.systemManager.getServerDetails(getSessionSubject()).getProductInfo();
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    @Override
    public ServerDetails getServerDetails() throws RuntimeException {
        try {
            return this.systemManager.getServerDetails(getSessionSubject());
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    @Override
    public HashMap<String, String> getSystemConfiguration() throws RuntimeException {
        try {
            Properties props = this.systemManager.getSystemConfiguration(getSessionSubject());
            return convertFromProperties(props);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    @Override
    public void setSystemConfiguration(HashMap<String, String> map, boolean skipValidation) throws RuntimeException {
        try {
            Properties props = convertToProperties(map);
            this.systemManager.setSystemConfiguration(getSessionSubject(), props, skipValidation);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    // GWT does not support java.util.Properties - we have to convert to/from Properties <-> HashMap
    private Properties convertToProperties(HashMap<String, String> map) {
        Properties props = new Properties();
        if (map != null) {
            props.putAll(map);
        }
        return props;
    }

    private HashMap<String, String> convertFromProperties(Properties props) {
        if (props == null) {
            return new HashMap<String, String>(0);
        }
        HashMap<String, String> map = new HashMap<String, String>(props.size());
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return map;
    }
}