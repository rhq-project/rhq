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
package org.rhq.core.domain.configuration.test;

import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

public class DataSourceExample {
    /* This class exercises the Configuration domain model from a number of different actors and goals.
     * The following overview describes the different use cases of the domain model:
     *
     * 1. Plugin populating a value set to describe the resource's current state 2. Plugin reading a value set to write
     * the changes to the resource 3. Plugin Container creating a definition 4. Server storing a value set to the
     * database 5. Server storing a definition to the database 6. Server rendering a edit properties form for a new
     * resource (just uses definition) 7. Server rendering an edit properties form with existing resource properties
     *
     * The following methods attempt to simulate such usage in each of these cases.
     *
     */

    @Test
    public Configuration sampleValueSetDemonstration() {
        Configuration configuration = new Configuration();

        configuration.put(new PropertySimple("jndi-name", "DefaultDS"));

        configuration.put(new PropertySimple("connection-url",
            "jdbc:postgresql://localhost:9432/rhq14?protocolVersion=2"));
        configuration.put(new PropertySimple("use-java-context", Boolean.TRUE));
        configuration.put(new PropertySimple("min-pool-size", 2));
        configuration.put(new PropertySimple("max-pool-size", null));
        configuration.put(new PropertySimple("track-statements", "Yes"));

        PropertyMap connectionProperties = new PropertyMap("connection-properties");
        connectionProperties.put(new PropertySimple("TNSNamesFile", "c:\\temp\\tnsnames.ora"));
        connectionProperties.put(new PropertySimple("ConnectionRetryDelay", "3"));

        configuration.put(connectionProperties);

        return configuration;
    }
    /*
     * @Test public void renderCreatePageDemo() {   PropertyValueSet valueSet = new PropertyValueSet();
     * ConfigurationDefinition defn = new ConfigurationDefinition();
     *
     * // NOTE: the trouble with returning a Set of names is that we probably lose the ordering   // of the properties
     * which we once had. May be a list would be better. We could always   // use the orderIndex on the returned
     * ConfigurationProperty but i'd prefer just to fetch   // things in the correct order   // ccrouch, Dec 4, 2006
     *
     * // NOTE: iterating through the groups is not helpful since we can't iterate through the   // properties contained
     * in a group   // ccrouch, Dec 4,2006   for (String groupName : defn.groupNameSet())   {      PropertyGroup group =
     * defn.getPropertyGroup(groupName);      //group.getPropertyNames()   }
     *
     * // at some level of rendering we're going to want to convert properties into   // widgets we can display.   //
     * This widget stuff is all pie-in-the-sky until we get the UI rendering   // nailed down more, its just trying to
     * exercise the configurationDefinition.   List widgets = new ArrayList();
     *
     * for (String propName : defn.propertyNameSet())   {      // NOTE: we really need a
     * ConfigurationDefinition.getProperty which returns      // a ConfigurationProperty, since for a given property
     * name we're not going      // to know what sort of property it is. Just upcast to ConfigurationProperty      //
     * for now, to mimic this method.      ConfigurationProperty prop = defn.getCompositeProperty(propName);
     *
     *    // don't see any way to avoid this in the general case where we don't know the types      // of the properties
     * we are retrieving.      if (prop instanceof CompositeProperty)      {         CompositeProperty compositeProperty
     * = (CompositeProperty) prop;         Object parentWidget;         if (compositeProperty.isMultiplicity())
     * {            // make sure to display a list of CompositeProperty widgets            parentWidget = new Object();
     * // new MultiValuedCompositeWidget();         }         else         {            parentWidget = new Object(); //
     * new SingleValuedCompositeWidget();         }
     *
     *       // now lets see what properties will go into this parent         List childWidgets = new ArrayList();
     *   for (SimpleProperty childProp : compositeProperty.getProperties())         {            String type =
     * childProp.getPropertyType(); // e.g. "integer"
     *
     *          Object widget = new Object(); // WidgetFactory.getWidget(type)            childWidgets.add(widget);
     *
     *          // if this was an edit page we'd need the value of the property too            SimpleValue value =
     * valueSet.getSimpleValue(childProp.getName());            // widget.setValue(value.getValue)         }
     *
     *       // parentWidget.addChildren(childWidgets);
     *
     *       widgets.add(parentWidget);      }      else // SimpleProperty      {         SimpleProperty simpleProperty
     * = (SimpleProperty) prop;         String type = simpleProperty.getPropertyType(); // e.g. "integer"
     *
     *       // this next stuff is all pie-in-the-sky until we get the UI rendering nailed down more
     *
     *       Object widget = new Object(); // WidgetFactory.getWidget(type)
     *
     *       // if this was an edit page we'd need the value of the property too         SimpleValue value =
     * valueSet.getSimpleValue(simpleProperty.getName());         // widget.setValue(value.getValue)
     *
     *       widgets.add(widget);      }   }
     *
     * // render(widgets)
     *
     * }
     *
     */

    /**
    * Overview Numbers:  2
    * Actor:             Plugin
    * Description:       Extract values from a value set and use them in configuring the resource.
    */
    /*
     * @Test public void populateResourceWithValues() {
     */
    /* Value Set is sent from the server and arrives in the plugin code. */
    /*
     * PropertyValueSet valueSet = sampleValueSetDemonstration();
     *
     */
    /* If the operation is to create a new datasource, the entire -ds.xml file will
     * be created and written out. xmlWriter below refers to some form of XML package;println is used as placeholders
     * here. */
    /*
     * PrintStream xmlWriter = System.out;
     *
     * xmlWriter.println("<datasources>");
     *
     */
    /* Decide what type of datasource to create. This is currently not the cleanest looking descision,
     *and we need to investigate utilities to make this cleaner. */
    /*
     * CompositeValue dsRootValue;
     *
     * if ((dsRootValue = valueSet.getCompositeValue("no-tx-ds")) != null) { xmlWriter.println("<no-tx-datasource>");
     *
     * xmlWriter.println("<jndi-name>" + dsRootValue.get("jndi-name") + "</jndi-name>");
     * xmlWriter.println("<connection-url>" + dsRootValue.get("connection-url") + "</connection-url>");
     *
     */
    /* Demonstrates using a boolean type. The null check ensures that the user entered a value.
     * If the user did not enter a value, the entire XML tag would be omitted. If the user did enter a value, the tag is
     * written with the value.
     *
     * If the property was defined as required, the null check could be skipped as the userwill have been required to
     * enter a value of true or false. */
    /*
     * SimpleValue javaContextValue = dsRootValue.get("use-java-context"); if (javaContextValue.getBoolean() != null) {
     * xmlWriter.println("<use-java-context>" + javaContextValue.getBoolean() + "</use-java-context>"); }
     *
     */
    /* Demonstrates plugin-side validation. The min and max pool sizes are checked for logical
     * consistency. The API will include a mechanism for specifying there was an error in thisvalidation. */
    /*
     * Integer minPoolSize = dsRootValue.get("min-pool-size").getInteger(); Integer maxPoolSize =
     * dsRootValue.get("max-pool-size").getInteger();
     *
     */
    /* The pool size variables are not required and therefor null checks are necessary. */
    /*
     * if (minPoolSize != null && maxPoolSize != null && minPoolSize > maxPoolSize) {
     */
    /* Add to running list of issues with the value set, to be returned to the server. */
    /*
     * }
     *
     */
    /* There are no checks to ensure the pool size is greater than 0 as the integer range
     *constraint will ensure that server-side. */
    /*
     *
     * if (minPoolSize != null) xmlWriter.println("<min-pool-size>" + minPoolSize + "</min-pool-size>");
     *
     * if (maxPoolSize != null) xmlWriter.println("<max-pool-size>" + maxPoolSize + "</max-pool-size>");
     *
     */
    /* Demonstrates iterating over a map of user entered keys and values. */
    /*
     * SimpleValue connPropertiesValue = dsRootValue.get("connection-properties"); Map<String, SimpleValue>
     * connPropertiesMap = connPropertiesValue.getMap();
     *
     * Set<String> propNames = connPropertiesMap.keySet(); for (String name : propNames) { SimpleValue value =
     * connPropertiesMap.get(name); xmlWriter.println("<connection-property name=\"" + name + "\">" + value +
     * "</connection-property>"); }
     *
     * xmlWriter.println("</no-tx-datasource>"); } else if ((dsRootValue = valueSet.getCompositeValue("local-tx-ds")) !=
     * null) { // Handling for local TX databases } else if ((dsRootValue = valueSet.getCompositeValue("xa-tx-ds")) !=
     * null) { // Handling for XA databases }
     *
     * xmlWriter.println("</datasources>");}*/
}