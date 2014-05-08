/*
  * RHQ Management Platform
  * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.enterprise.server.core.plugin;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.group.expressions.CannedGroupExpressions;
import org.rhq.core.domain.plugin.CannedGroupAddition;
import org.rhq.core.domain.plugin.CannedGroupExpression;
import org.rhq.enterprise.server.resource.group.definition.framework.ExpressionEvaluator;
/**
 * Provides static methods which read, parse and validate additional (optional) plugin descriptor files living in plugins (basically all other files except rhq-plugin.xml)
 * @author lzoubek
 *
 */
public class PluginAdditionsReader {
    private static final Log log = LogFactory.getLog(PluginAdditionsReader.class.getName());
    
    /**
     * reads canned group additions from given plugin JAR
     * @param url pluginJAR url
     * @param pluginName 
     * @return canned dyna-group addition instance or null on error or non-existing descriptor
     */
    public static CannedGroupAddition getCannedGroupsAddition(URL url, String pluginName) {
        try {
            CannedGroupExpressions descriptor = AgentPluginDescriptorUtil.loadCannedExpressionsFromUrl(url);
            return parseCGAdditions(descriptor, pluginName);
        } catch (Exception e) {
            log.error("Failed to parse plugin addition found in plugin [" + url + "]", e);
            return null;
        }
    }

    private static CannedGroupAddition parseCGAdditions(CannedGroupExpressions descriptor, String plugin) {
        if (descriptor == null) {
            return null;
        }
        CannedGroupAddition addition = new CannedGroupAddition();
        
        for (CannedGroupExpressions.Definition def : descriptor.getGroupDefinitions()) {
            CannedGroupExpression cge = new CannedGroupExpression();
            cge.setId(def.getId());
            cge.setName(def.getName());
            cge.setPlugin(plugin);
            cge.setCreateByDefault(def.isCreateByDefault());
            cge.setDescription(def.getDescription());
            cge.setExpression(def.getExpression());
            cge.setRecalcInMinutes(def.getRecalcInMinutes().intValue());
            if (validateCGE(cge)) {
                addition.getExpressions().add(cge);
            }
        }
        return addition;
    }

    private static boolean validateCGE(CannedGroupExpression cge) {
        boolean valid = true;
        try {
            ExpressionEvaluator evaluator = new ExpressionEvaluator();
            evaluator.setTestMode(true); // to prevent actual query from happening
            for (String expr : cge.getExpression()) {
                evaluator.addExpression(expr);
            }
            evaluator.execute();
            
        } catch (Exception ex) {
            log.error("Failed to evaluate [expression], evaluator result : "+ex.getMessage());
            valid = false;
        }
        return valid;
    }
}
