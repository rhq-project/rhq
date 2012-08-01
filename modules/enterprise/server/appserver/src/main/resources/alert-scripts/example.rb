
require 'java'

def sendAlert(alert, url, conditions)

  puts alert.alertDefinition.name
  puts url
  puts conditions

  result = org.rhq.enterprise.server.plugin.pc.alert.SenderResult.new
  state = org.rhq.enterprise.server.plugin.pc.alert.ResultState::SUCCESS
  result.setState(state)
  result.setMessage("Sending via ruby succeeded")

  return result
end