package net.hyperic.hq.product.apache;

import java.util.Properties;

import net.hyperic.util.StringUtil;

import net.hyperic.hq.product.Metric;
import net.hyperic.hq.product.MetricNotFoundException;
import net.hyperic.hq.product.MetricUnreachableException;
import net.hyperic.hq.product.MetricValue;
import net.hyperic.hq.product.SNMPMeasurementPlugin;
import net.hyperic.hq.product.PluginException;

public class ApacheMeasurementPlugin
    extends SNMPMeasurementPlugin {

    public MetricValue getValue(Metric metric)
        throws PluginException,
        MetricNotFoundException,
        MetricUnreachableException {

        //XXX backward compat since metrics dont get rescheduled
        String assoc = "applInboundAssociations";        
        if (metric.getAttributeName().equals(assoc)) {
            Properties props = metric.getProperties(); 
            String type = props.getProperty(PROP_VARTYPE);
            if (type.equals("single")) {
                props.setProperty(PROP_VARTYPE, "next");
            }
        }

        try {
            return super.getValue(metric);
        } catch (MetricNotFoundException e) {
            //make the generic snmp error message easier to understand
            //specific to apache
            String msg =
                StringUtil.replace(e.getMessage(),
                                   ApacheSNMP.TCP_PROTO_ID, "");
            msg = StringUtil.replace(msg, "->", ":");
            throw new MetricNotFoundException(msg, e);
        }
    }
}
