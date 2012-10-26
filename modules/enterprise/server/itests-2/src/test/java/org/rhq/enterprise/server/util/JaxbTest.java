package org.rhq.enterprise.server.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.testng.annotations.Test;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.util.PageList;

public class JaxbTest {

    @Test
    public void writeFragments() throws Exception {
        Resource resource = new ResourceBuilder()
            .createRandomServer()
            .with(2).randomChildServers()
            .build();

        PageList<Resource> resources = new PageList<Resource>();
        resources.add(resource);
        resources.addAll(resource.getChildResources());

        JAXBContext context = JAXBContext.newInstance(Resource.class, PageList.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        System.out.println("<collection>\n");
        for (Resource r : resources) {
            marshaller.marshal(r, System.out);
        }
        System.out.println("</collection>\n");

    }

}
