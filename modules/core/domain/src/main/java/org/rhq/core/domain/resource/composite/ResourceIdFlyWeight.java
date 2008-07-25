package org.rhq.core.domain.resource.composite;

import org.rhq.core.domain.resource.Resource;

public class ResourceIdFlyWeight extends Resource {

    public ResourceIdFlyWeight(int id, String uuid) {
        super(id);
        setUuid(uuid);
    }

}