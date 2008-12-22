package org.rhq.plugins.server;

import java.util.Map;

import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * The resource component that represents the GroupDefinition / DynaGroup subsystem.
 * 
 * @author Joseph Marques
 */
public class GroupDefinitionResourceComponent extends MBeanResourceComponent {

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("retrieveStatistics".equals(name)) {

            Map<String, Map<String, Object>> allData;
            allData = (Map<String, Map<String, Object>>) getEmsBean().getAttribute("Statistics").refresh();
            
            OperationResult result = new OperationResult();
            PropertyList statistics = new PropertyList("statistics");
            result.getComplexResults().put(statistics);

            for (String groupDefinitionName : allData.keySet()) {

                PropertyMap stat = new PropertyMap("stat");
                stat.put(new PropertySimple("groupDefinitionName", groupDefinitionName));
                
                for (Map.Entry<String, Object> groupDefinitionStats : allData.get(groupDefinitionName).entrySet()) {
                    stat.put(new PropertySimple(groupDefinitionStats.getKey(), groupDefinitionStats.getValue()));
                }

                statistics.add(stat);
            }
            
            return result;
        }

        // isn't an operation we know about, must be an MBean operation that EMS can handle
        return super.invokeOperation(name, parameters);
    }

}
