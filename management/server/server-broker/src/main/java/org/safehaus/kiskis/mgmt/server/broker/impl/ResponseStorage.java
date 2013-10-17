package org.safehaus.kiskis.mgmt.server.broker.impl;

import java.util.HashSet;
import org.safehaus.kiskismgmt.protocol.Agent;
import org.safehaus.kiskismgmt.protocol.Response;
import org.safehaus.kiskis.mgmt.shared.protocol.interfaces.server.RegisteredHostInterface;

import java.util.Set;
import org.safehaus.kiskis.mgmt.server.broker.Activator;
import org.safehaus.kiskis.mgmt.shared.protocol.commands.CommandEnum;
import org.safehaus.kiskismgmt.protocol.Request;
import org.safehaus.kiskismgmt.protocol.RequestType;

/**
 * Created with IntelliJ IDEA. User: daralbaev Date: 10/10/13 Time: 4:48 PM To
 * change this template use File | Settings | File Templates.
 */
public class ResponseStorage implements RegisteredHostInterface {

    Set<Agent> agents;
    CommandEnum commands;

    public CommandEnum getCommands() {
        return commands;
    }

    public ResponseStorage() {
        agents = new HashSet<Agent>();
    }

    @Override
    public Set<Agent> getRegisteredHosts() {
        //To change body of implemented methods use File | Settings | File Templates.
        return agents;
    }

    @Override
    public Request sendAgentResponse(Response response) {
        Request req = null;
        //TO-DO Create result from Response for ui
        System.out.println("Received response from communication: " + response.toString());

        switch (response.getType()) {
            case REGISTRATION_REQUEST: {
//                Agent agent = new Agent();
//                agent.setUuid(response.getUuid());
//                agents.add(agent);
//                System.out.println("Agents count " + agents.size());
                req = new Request();
                req.setUuid(response.getUuid());
                req.setType(RequestType.REGISTRATION_REQUEST_DONE);
                System.out.println(response.getType() + " executing!!!");
                

//{
//  command:
//    {
//      "type":"REGISTRATION_REQUEST_DONE",
//      "uuid":"3394ef08-7b0a-4971-a428-2b241f36fe73"
//    }
//}                
                Activator.getCommandSender().sendCommandToAgent(req);
                break;
            }
            case HEARTBEAT_RESPONSE: {
                System.out.println("HB");
                break;
            }
            case EXECUTE_RESPONSE_DONE: {
                System.out.println("ERD");
                break;
            }
            case EXECUTE_RESPONSE: {
                System.out.println("ER");
                break;
            }
        }

        return req;  //To change body of implemented methods use File | Settings | File Templates.
    }
    //private void registerAgent(Response response) {
//        response.getUuid();
    //}
}
