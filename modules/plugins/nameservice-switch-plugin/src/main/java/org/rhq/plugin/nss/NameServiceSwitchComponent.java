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
package org.rhq.plugin.nss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Allows the RHQ server to manage the Name Server Switch Configuration for a Linux Platform 
 * 
 * @author Adam Young
 */
public class NameServiceSwitchComponent implements ResourceComponent<NameServiceSwitchComponent>, OperationFacet, ResourceConfigurationFacet {
    private static final String ETC_NSSWITCH_CONF = "/tmp/nsswitch.conf";
    //private static final String ETC_NSSWITCH_CONF = "/etc/nsswitch.conf";

    private final Log log = LogFactory.getLog(NameServiceSwitchComponent.class);

    private static Pattern linePattern = Pattern.compile("\\s*(\\p{Alpha}*):((?:\\s|\\p{Alpha}|\\[|\\]|\\=)*)");

    /**
     * Represents the resource configuration of the custom product being managed.
     */
    private Configuration resourceConfiguration;

    /**
     * All AMPS plugins are stateful - this context contains information that your resource component can use when
     * performing its processing.
     */
    private ResourceContext resourceContext;

    /**
     * This is called when your component has been started with the given context. You normally initialize some internal
     * state of your component as well as attempt to make a stateful connection to your managed resource.
     *
     * @see ResourceComponent#start(ResourceContext)
     */
    public void start(ResourceContext context) {
        resourceContext = context;
    }

    /**
     * This is called when the component is being stopped, usually due to the plugin container shutting down. You can
     * perform some cleanup here; though normally not much needs to be done here.
     *
     * @see ResourceComponent#stop()
     */
    public void stop() {
    }

    /**
     * All resource components must be able to tell the plugin container if the managed resource is available or not.
     * This method is called by the plugin container when it needs to know if the managed resource is actually up and
     * available.
     *
     * @see ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        // TODO: here you normally make some type of connection attempt to the managed resource
        //       to determine if it is really up and running.
        return AvailabilityType.UP;
    }

    /**
     * The plugin container will call this method when it wants to invoke an operation on your managed resource. Your
     * plugin will connect to the managed resource and invoke the analogous operation in your own custom way.
     *
     * @see OperationFacet#invokeOperation(String, Configuration)
     */
    public OperationResult invokeOperation(String name, Configuration configuration) {
        return null;
    }

    static public String getContents(File aFile) {
        //...checks on aFile are elided
        StringBuilder contents = new StringBuilder();

        try {
            //use buffering, reading one line at a time
            //FileReader always assumes default encoding is OK!
            BufferedReader input = new BufferedReader(new FileReader(aFile));
            try {
                String line = null; //not declared within while loop
                /*
                * readLine is a bit quirky :
                * it returns the content of a line MINUS the newline.
                * it returns null only for the END of the stream.
                * it returns an empty String if two newlines appear in a row.
                */
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return contents.toString();
    }

    public Set<RawConfiguration> loadRawConfigurations() {
   
        Set<RawConfiguration> raws = new HashSet<RawConfiguration>();
        RawConfiguration raw = new RawConfiguration();

        raw.setPath(ETC_NSSWITCH_CONF);

        raw.setContents(getContents(new File(raw.getPath())));

        raws.add(raw);
        return raws;
    }

    public Configuration loadStructuredConfiguration() {
        Configuration configuration = new Configuration();
        for (RawConfiguration raw : loadRawConfigurations()) {
            mergeStructuredConfiguration(raw,configuration);       
        }

        return configuration;
    }

    public RawConfiguration mergeRawConfiguration(Configuration from, RawConfiguration to) {
        // TODO Auto-generated method stub
        return null;
    }

    public void mergeStructuredConfiguration(RawConfiguration from, Configuration to) {

        ArrayList<Property> properties = new ArrayList<Property>();
        
        to.addRawConfiguration(from);
        try {
            BufferedReader input = new BufferedReader(new StringReader(from.getContents()));
            String line = null; //not declared within while loop
            while ((line = input.readLine()) != null) {
                Matcher matcher = linePattern.matcher(line);
                if (matcher.matches()) {
                    properties.add(new PropertySimple(matcher.group(1), matcher.group(2).trim()));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        to.setProperties(properties);

    }

    public void persistRawConfiguration(RawConfiguration rawConfiguration) {

        
        
    }

    public void persistStructuredConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub

    }

    public void validateRawConfiguration(RawConfiguration rawConfiguration) throws RuntimeException {
        // TODO Auto-generated method stub

    }

    public void validateStructuredConfiguration(Configuration configuration) {
        // TODO Auto-generated method stub

    }
    
    
    public static void main(String[] args){
        String noMatch = "# abcd: val";
        String yesMatch = "bootparams: nisplus [NOTFOUND=return] files";

        Matcher matcher = linePattern.matcher(noMatch);
        if (matcher.matches()) {
            System.exit(1);
        }
        matcher = linePattern.matcher(yesMatch);

        
        if (matcher.matches()){
            System.out.println("Group0="+matcher.group(1));    
            System.out.println("Group1="+matcher.group(2).trim());    
        }
        
        
    }
}
