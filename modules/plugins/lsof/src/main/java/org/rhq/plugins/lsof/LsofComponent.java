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
package org.rhq.plugins.lsof;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.script.ScriptServerComponent;

/**
 * Component that represents the lsof tool.
 * 
 * @author John Mazzitelli
 */
public class LsofComponent extends ScriptServerComponent {
    private final Log log = LogFactory.getLog(LsofComponent.class);

    @Override
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {
        OperationResult result;

        if ("getNetworkConnections".equals(name)) {
            // compile the regex that will be used to parse the output
            String regex = params.getSimpleValue("regex", "");
            if (regex.length() == 0) {
                throw new Exception("missing regex parameter");
            }
            Pattern pattern = Pattern.compile(regex);

            // first run the executable
            OperationResult intermediaryResult = super.invokeOperation(name, params);
            Configuration intermediaryConfig = intermediaryResult.getComplexResults();

            // now build our results object
            result = new OperationResult();
            Configuration config = result.getComplexResults();
            config.put(intermediaryConfig.getSimple(OPERATION_RESULT_EXITCODE));
            if (intermediaryResult.getErrorMessage() != null) {
                result.setErrorMessage(intermediaryResult.getErrorMessage());
            } else {
                String output = intermediaryConfig.getSimpleValue(OPERATION_RESULT_OUTPUT, "");
                if (output.length() > 0) {
                    PropertyList list = new PropertyList("networkConnections");
                    config.put(list);

                    String[] lines = output.split("\n");
                    for (String line : lines) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.matches()) {
                            PropertyMap map = new PropertyMap("networkConnection");
                            list.add(map);
                            map.put(new PropertySimple("host", matcher.group(1)));
                            map.put(new PropertySimple("port", matcher.group(2)));
                            map.put(new PropertySimple("remoteHost", matcher.group(3)));
                            map.put(new PropertySimple("remotePort", matcher.group(4)));
                        }
                    }
                }
            }
        } else {
            result = super.invokeOperation(name, params);
        }
        return result;
    }
}
