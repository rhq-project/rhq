There are some pre-requisite steps that must be followed prior to running the test scripts.

* A jopr server needs to be runnig on localhost (assuming you are running tests on 
localhost).

* An agent needs to be running on localhost. The agent needs to register with the server.
The configurable-alphaomega scenario from the perftest plugin needs to be loaded. The agent
can be started with the following command line,

rhq-agent.sh --purgedata \
             -Don.perftest.scenario=configurable-alphaomega \
             -Don.perftest.server-omega-count=10 \
             -Don.perftest.service-alpha-count=10 \
             -Don.perftest.server-beta-count=10 \
             -Don.perftest.omega-content0-count=10 \
             -Don.perftest.omega-content1-count=10 \
             -Don.perftest.alpha-content0-count=10 \
             -Don.perftest.alpha-content1-count=10

* The jopr server should be committed into inventory.

* Create the following alert definitions (for now these need to be created manually)

  * Resource - service-alpha-0 (parent - server-omega-0)
    * Alert Name: service-alpha-0-alert-def-1
    * Alert Description: Test alert definition 1 for service-alpha-0
    * If condition: select 'Event Severity' and choose 'WARN'
  * Resource - service-alpha-0 (parent - server-omega-0)
    * Alert Name: service-alpha-0-alert-def-2
    * Alert Description: Test alert definition 2 for service-alpha-0
    * If condition: select 'Operation', choose 'Create Events', and equal to 'SUCCESS'
