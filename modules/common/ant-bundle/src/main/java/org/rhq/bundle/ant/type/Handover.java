/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.bundle.ant.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An ANT data type representing the rhq:handover tag.
 *
 * @author Thomas Segismont
 */
public class Handover extends AbstractBundleType {
    private String action;
    private boolean failonerror;
    private List<HandoverParam> handoverParams;

    public Handover() {
        handoverParams = new ArrayList<HandoverParam>();
        failonerror = true;
    }

    public String getAction() {
        return action;
    }

    @SuppressWarnings("unused")
    public void setAction(String action) {
        this.action = action;
    }

    public boolean isFailonerror() {
        return failonerror;
    }

    @SuppressWarnings("unused")
    public void setFailonerror(boolean failonerror) {
        this.failonerror = failonerror;
    }

    @SuppressWarnings("unused")
    public void addConfigured(HandoverParam handoverParam) {
        handoverParams.add(handoverParam);
    }

    public Map<String, String> getHandoverParams() {
        if (handoverParams == null) {
            return Collections.emptyMap();
        }
        Map<String, String> params = new LinkedHashMap<String, String>();
        for (HandoverParam handoverParam : handoverParams) {
            params.put(handoverParam.getName(), handoverParam.getValue());
        }
        return params;
    }

    @Override
    public String toString() {
        return "Handover[" + "action='" + action + '\'' + ", handoverParams=" + handoverParams + ", failOnError="
            + failonerror + ']';
    }
}
