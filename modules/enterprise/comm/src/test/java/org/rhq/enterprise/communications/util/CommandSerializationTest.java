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
package org.rhq.enterprise.communications.util;

import java.io.Serializable;

import org.testng.annotations.Test;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.client.CommandAndCallback;
import org.rhq.enterprise.communications.command.client.CommandResponseCallback;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommand;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;
import org.rhq.enterprise.communications.command.param.ParameterRenderingInformation;

/**
 * Tests that commands can be serialized.
 *
 * @author John Mazzitelli
 */
@Test
public class CommandSerializationTest {
    /**
     * Tests serializing commands.
     *
     * @throws Exception
     */
    public void testSerializeCommands() throws Exception {
        GenericCommand gc = new GenericCommand();
        ParameterDefinition def = new ParameterDefinition("hello", String.class.getName(),
            new ParameterRenderingInformation("a", "b"));

        gc.setCommandType(new CommandType("foo", 3));
        gc.setParameterDefinitions(new ParameterDefinition[] { def });
        gc.setParameterValue("hello", "world");
        gc.getConfiguration().put("config1", "config1value");
        gc.getConfiguration().put("config2", "config2value");
        gc = (GenericCommand) serializeDeserialize(gc);

        assert gc.getCommandType().equals(new CommandType("foo", 3));
        assert gc.getParameterDefinition("hello").getType().equals(String.class.getName());
        assert gc.getParameterDefinition("hello").getRenderingInfo().getLabelKey().equals("a");
        assert gc.getParameterDefinition("hello").getRenderingInfo().getDescriptionKey().equals("b");
        assert gc.getParameterValue("hello").equals("world");
        assert gc.getConfiguration().getProperty("config1").equals("config1value");
        assert gc.getConfiguration().getProperty("config2").equals("config2value");

        return;
    }

    /**
     * Tests serializing commands.
     *
     * @throws Exception
     */
    public void testSerializeCommandsWithNoConfig() throws Exception {
        GenericCommand gc = new GenericCommand();
        ParameterDefinition def = new ParameterDefinition("hello", String.class.getName(),
            new ParameterRenderingInformation("a", "b"));

        gc.setCommandType(new CommandType("foo", 3));
        gc.setParameterDefinitions(new ParameterDefinition[] { def });
        gc.setParameterValue("hello", "world");
        gc = (GenericCommand) serializeDeserialize(gc);

        assert gc.getCommandType().equals(new CommandType("foo", 3));
        assert gc.getParameterDefinition("hello").getType().equals(String.class.getName());
        assert gc.getParameterDefinition("hello").getRenderingInfo().getLabelKey().equals("a");
        assert gc.getParameterDefinition("hello").getRenderingInfo().getDescriptionKey().equals("b");
        assert gc.getParameterValue("hello").equals("world");

        return;
    }

    /**
     * Tests serializing commands and callbacks.
     */
    public void testSerializeCommandAndCallback() {
        GenericCommand gc = new GenericCommand();
        CommandAndCallback cnc = new CommandAndCallback(gc, new DummyCommandResponseCallback());

        cnc = (CommandAndCallback) serializeDeserialize(cnc);
        assert cnc.getCommand() != null;
        assert cnc.getCallback() != null;
        assert ((DummyCommandResponseCallback) cnc.getCallback()).foo.equals("bar");

        return;
    }

    /**
     * Serializes and then deserializes object and returns the reconstituted object. After the deserialization, this
     * method does not check for object equality but it does check that the reconstituted object is not the same as
     * <code>o</code> (that is, the returned object != <code>o</code>).
     *
     * @param  o object to test
     *
     * @return the object after its been serialized and then deserialized
     */
    private Object serializeDeserialize(Serializable o) {
        byte[] b = StreamUtil.serialize(o);
        assert b != null;
        Object o2 = StreamUtil.deserialize(b);
        assert o2 != null;
        assert o != o2;

        return o2;
    }
}

/**
 * Just a dummy callback that we will use to ensure callbacks are serializable.
 */
class DummyCommandResponseCallback implements CommandResponseCallback {
    private static final long serialVersionUID = 1L;

    /**
     * We will use this to check its value after serialization to make sure its reconstituted successfully
     */
    public String foo = "bar";

    /**
     * @see CommandResponseCallback#commandSent(CommandResponse)
     */
    public void commandSent(CommandResponse response) {
    }
}