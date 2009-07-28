package org.rhq.enterprise.server.jaxb;

import java.util.ArrayList;
import java.util.Map;

import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.rhq.core.domain.configuration.Configuration;

@XmlRootElement
@Stateless
@WebService(endpointInterface = "org.rhq.enterprise.server.jaxb.SimpleManagerRemote")
public class SimpleManagerBean implements SimpleManagerRemote {

    //    @Override
    //    public int testMethodWithMap(Map<Integer, Configuration> configMap) {
    //        int count = 0;
    //        if (configMap != null) {
    //            count = configMap.size();
    //        }
    //        return count;
    //    }

    //    @XmlJavaTypeAdapter(WebServiceTypeAdapter.class)
    //    @XmlElement
    private ArrayList<Map<Integer, Configuration>> mapBag = new ArrayList<Map<Integer, Configuration>>();

    public int addMapping(//
        @XmlJavaTypeAdapter(WebServiceTypeAdapter.class)//
        Map<Integer, Configuration> map) {
        mapBag.add(map);
        return 0;
    }
    //        @XmlJavaTypeAdapter(WebServiceTypeAdapter.class)//
}
