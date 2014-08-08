package org.rhq.enterprise.server.storage.maintenance.step;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;

/**
 * @author John Sanda
 */
public class BaseStepRunner {

    private ObjectMapper mapper;

    private TypeReference<HashMap<String,String>> typeReference;

    protected void executeOperation(StorageNode node, String operation, Configuration arguments) {

    }

    protected List<StorageNode> getCluster() {
        return null;
    }

    protected Map<String, String> convertArgs(String args) {
        try {
            return mapper.readValue(args, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
