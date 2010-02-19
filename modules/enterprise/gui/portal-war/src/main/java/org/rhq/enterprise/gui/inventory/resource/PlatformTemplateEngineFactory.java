package org.rhq.enterprise.gui.inventory.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.template.TemplateEngine;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class PlatformTemplateEngineFactory {

    public static TemplateEngine fetchTemplateEngine(ResourceManagerLocal resourceManager, Resource resource) {
        TemplateEngine engine;
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        Resource platform = resourceManager.getPlaformOfResource(subject, resource.getId());
        MeasurementDataManagerLocal measurementDataManager = LookupUtil.getMeasurementDataManager();
        List<MeasurementDataTrait> traits = measurementDataManager.findCurrentTraitsForResource(subject, platform
            .getId(), null);

        Map<String, String> tokens = new HashMap<String, String>();
        for (MeasurementData data : traits) {
            String name = data.getName().toLowerCase().replace(' ', '_');
            tokens.put("rhq.system." + name, data.getValue().toString());
        }

        engine = new TemplateEngine(tokens);
        return engine;
    }

}
