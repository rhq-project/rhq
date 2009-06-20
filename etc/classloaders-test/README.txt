These artifacts are here to help test the plugin classloading features of the
plugin container (RHQ-2059).

Build the three dummy jar dependencies using mvn. They can be used to test what
classloader is used to load and invoke them.

The plugin1 and plugin2 modules can be built via mvn and then deployed in the
agent's /plugins directory. It is best if you use the standalone plugin
container - so copy the standalone.[sh,bat] script to your agent /bin
directory and run that to test.