package org.rhq.enterprise.server.jaxb;

import java.util.Map;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.rhq.core.domain.configuration.Configuration;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface SimpleManagerRemote {

    //    @WebMethod
    //    int testMethodWithMap(//
    //        @WebParam(name = "cMap") Map<Integer, Configuration> configMap);

    @WebMethod
    int addMapping(//
        @XmlJavaTypeAdapter(WebServiceTypeAdapter.class)//
        Map<Integer, Configuration> newMap);

    //    @XmlJavaTypeAdapter(WebServiceTypeAdapter.class)//
}
