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
package test;

import java.util.Set;
import java.util.Map;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.KnownComponentTypes;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.DeploymentTemplateInfo;
import org.jboss.managed.api.ComponentType;
import org.jboss.metatype.api.values.SimpleValue;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.security.SecurityAssociation;
import org.jboss.security.SimplePrincipal;

/**
 * @author Ian Springer
 */
public class Worker implements Runnable
{
    private ManagementView managementView;

    public Worker(ManagementView managementView)
    {
        this.managementView = managementView;
    }

    public void run()
    {
        System.out.println("Starting worker " + Thread.currentThread() + "...");
        try
        {
            // Uncomment the below line to reproduce https://jira.jboss.org/jira/browse/JOPR-263.
            resetSecurityAssociation();

            this.managementView.load();
            ComponentType QUEUE_COMPONENT_TYPE = KnownComponentTypes.JMSDestination.Queue.getType();
            Set<ManagedComponent> queueComponents =
                    this.managementView.getComponentsForType(QUEUE_COMPONENT_TYPE);
            ManagedComponent queueComponent = queueComponents.iterator().next();
            queueComponent.getRunState();

            // Modify a property...
            queueComponent.getProperties();
            ManagedProperty maxSizeProp = queueComponent.getProperty("maxSize");
            SimpleValueSupport maxSizeValue = (SimpleValueSupport)maxSizeProp.getValue();
            maxSizeValue.setValue(99);
            this.managementView.updateComponent(queueComponent);

            // Run some operations...
            Set<ManagedOperation> operations = queueComponent.getOperations();
            try
            {
                for (ManagedOperation operation : operations) {
                    if (operation.getName().equals("stop")) {
                        operation.invoke();
                    }
                }
            }
            catch (Exception e)
            {
                System.out.println("Exception when invoking stop(): " + e);
            }
            for (ManagedOperation operation : operations) {
                if (operation.getName().equals("start")) {
                    operation.invoke();
                }
            }

            // Create a new MO...
            DeploymentTemplateInfo template = this.managementView.getTemplate("QueueTemplate");
            Map<String,ManagedProperty> props = template.getProperties();
            ManagedProperty jndiNameProp = props.get("JNDIName");
            String currentThreadName = Thread.currentThread().getName();
            SimpleValue newValue = new SimpleValueSupport(SimpleMetaType.STRING, currentThreadName);
            jndiNameProp.setValue(newValue);
            this.managementView.applyTemplate(currentThreadName, template);
            this.managementView.process();

            // Now delete the MO...
            queueComponent = this.managementView.getComponent(currentThreadName, QUEUE_COMPONENT_TYPE);
            this.managementView.removeComponent(queueComponent);            
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void resetSecurityAssociation()
    {
        System.out.println("BEFORE RESET: principal=" + SecurityAssociation.getPrincipal()
                + ", credential=" + SecurityAssociation.getCredential()
                + ", callerPrincipal=" + SecurityAssociation.getCallerPrincipal()
                + ", subject=" + SecurityAssociation.getSubject()
                + ", runAsIdentity=" + SecurityAssociation.peekRunAsIdentity());
        SecurityAssociation.clear();
        SecurityAssociation.setPrincipal(new SimplePrincipal("admin"));
        SecurityAssociation.setCredential("admin");
        System.out.println("AFTER RESET: principal=" + SecurityAssociation.getPrincipal() 
                + ", credential=" + SecurityAssociation.getCredential()
                + ", callerPrincipal=" + SecurityAssociation.getCallerPrincipal()
                + ", subject=" + SecurityAssociation.getSubject()
                + ", runAsIdentity=" + SecurityAssociation.peekRunAsIdentity());
    }
}
