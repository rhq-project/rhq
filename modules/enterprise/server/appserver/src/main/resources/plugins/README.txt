You can copy agent plugins or server plugins to this directory.

When the server is running, it will periodically scan this
directory and hot-deploy the plugins it finds.

The jar files will be removed from this directory after they have
been detected.

Note that the plugins ARE NOT automagically pushed out to agents once they are deployed to the server.
Making the agents use a new plugin requires a restart of its plugin container which effectively means a short period of
time when the agent is down.

The agents will update their plugins after one of the following actions:
* agent is restarted and its "rhq.agent.update-plugins-at-startup" configuration property is set to true (the default)
* The "plugins update" operation is executed from the agent's interactive commandline prompt
* "Update All Plugins" operation is executed on the inventoried RHQ agent resource
* Clicking the "Update Plugins On Agents" button on the Administration -> Agent Plugins page
* Invoking "PluginManager.scheduleUpdateOnAgents" method in RHQ CLI
* Invoking "PluginManagerRemote.scheduleUpdateOnAgents" method from the remote interface
* specifying a non-negative "pushOutDelay" parameter to /content/{handle}/plugins in the REST API.
