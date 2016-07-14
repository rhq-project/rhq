package org.rhq.plugins.storage;

import java.util.Arrays;
import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.cassandra.ColumnFamilyComponent;

/**
 * @author Ruben Vargas
 */
public class RhqColumnFamilyComponent extends ColumnFamilyComponent {
    private final List<String> obsoleteColumnFamilies = Arrays.asList("twenty_four_hour_metrics", "one_hour_metrics",
        "six_hour_metrics", "metrics_index");

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
       if(obsoleteColumnFamilies.contains(name)){
            // Ignore it because is an obsolete column family, just return success.
            return new OperationResult();
       }else{
            return super.invokeOperation(name, parameters);
       }
    }
}
