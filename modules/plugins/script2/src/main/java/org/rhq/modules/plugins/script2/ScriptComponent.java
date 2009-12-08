
package org.rhq.modules.plugins.script2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;


public class ScriptComponent implements ResourceComponent, MeasurementFacet, OperationFacet
{
    private final Log log = LogFactory.getLog(this.getClass());

    private static final int CHANGEME = 1; // TODO remove or change this

    private String scriptName;
    ScriptEngine engine;
    Object scriptObject;




    /**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {

        Object ret = null;

        try {
            Invocable inv = (Invocable) engine;
            ret = inv.invokeFunction("avail");
            if (ret instanceof Number) {
                int tmp = ((Number)ret).intValue();
                if (tmp>0)
                    return AvailabilityType.UP;
            }
        } catch (ScriptException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (NoSuchMethodException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return AvailabilityType.DOWN;
    }


    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {

        Configuration conf = context.getPluginConfiguration();
        // TODO add code to start the resource / connection to it

        String engineName = conf.getSimpleValue("language",null);
        if (engineName==null)
            throw new InvalidPluginConfigurationException("No (valid) language given ");

        ScriptEngineManager seMgr = new ScriptEngineManager();
        engine = seMgr.getEngineByName(engineName);

        scriptName = conf.getSimpleValue("scriptName",null);
        if (scriptName==null)
            throw new InvalidPluginConfigurationException("No (valid) script name given");

        File dataDir = context.getDataDirectory();
        File scriptFile = new File(dataDir.getAbsolutePath() + "/"+ scriptName); // TODO find a better directory for this.
        if (!scriptFile.exists())
            throw new InvalidPluginConfigurationException("Script does not exist at " + scriptFile.getAbsolutePath());

        try {
            Reader reader = new BufferedReader(new FileReader(scriptFile));
            engine.eval(reader);
//            scriptObject = engine.get("new MyClass");
        }
        catch (ScriptException se) {
            throw new InvalidPluginConfigurationException(se.getMessage());
        }

    }


    /**
     * Tear down the rescource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {


    }



    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

         for (MeasurementScheduleRequest req : metrics) {

             Invocable inv = (Invocable) engine;
             if (req.getDataType()== DataType.MEASUREMENT) {
                 MeasurementDataNumeric res;
                 try {
                     Object ret = inv.invokeFunction("metric",req.getName());
                     if (ret instanceof Number) {
                         Double num = ((Number)ret).doubleValue();
                         res = new MeasurementDataNumeric(req,num);
                         report.addData(res);
                     }
                     else {
                         log.debug("Returned value " + ret.toString() + " is no number for metric " + req.getName());
                     }
                 }
                 catch (Exception e) {
                     log.debug(e.getMessage());
                 }
             }
             else if (req.getDataType()==DataType.TRAIT) {
                 MeasurementDataTrait res;
                 try {
                     Object ret = inv.invokeFunction("trait",req.getName());
                     if (ret instanceof String) {
                         String val = ((String)ret);
                         res = new MeasurementDataTrait(req,val);
                         report.addData(res);
                     }
                     else {
                         log.debug("Returned value " + ret.toString() + " is no string for metric " + req.getName());
                     }
                 }
                 catch (Exception e) {
                     log.debug(e.getMessage());
                 }
             }
         }
    }



    public void startOperationFacet(OperationContext context) {

    }


    /**
     * Invokes the passed operation on the managed resource
     * @param name Name of the operation
     * @param params The method parameters
     * @return An operation result
     * @see org.rhq.core.pluginapi.operation.OperationFacet
     */
    public OperationResult invokeOperation(String name, Configuration params) throws Exception {

        OperationResult res = new OperationResult();
        if ("dummyOperation".equals(name)) {
            // TODO implement me

        }
        return res;
    }





}
