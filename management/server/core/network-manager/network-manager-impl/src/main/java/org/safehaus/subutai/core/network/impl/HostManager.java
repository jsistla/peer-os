package org.safehaus.subutai.core.network.impl;


import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.safehaus.subutai.common.command.Command;
import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.protocol.Agent;

import com.google.common.collect.Sets;


/**
 * HostManager enables to register agent's hostname in /etc/hosts file of other agents
 */
public class HostManager
{
    private static final Logger LOG = Logger.getLogger( HostManager.class.getName() );

    private List<Agent> agentList;
    private String domainName;
    private Commands commands;


    public HostManager( Commands commands, List<Agent> agentList, String domainName )
    {
        this.agentList = agentList;
        this.domainName = domainName;
        this.commands = commands;
    }


    public boolean execute()
    {
        if ( agentList != null && !agentList.isEmpty() )
        {
            return write();
        }

        return false;
    }


    private boolean write()
    {

        Command command = commands.getAddIpHostToEtcHostsCommand( domainName, Sets.newHashSet( agentList ) );
        try
        {
            command.execute();
        }
        catch ( CommandException e )
        {
            LOG.log( Level.SEVERE, String.format( "Error in write: %s", e.getMessage() ), e );
        }

        return command.hasSucceeded();
    }


    public boolean execute( Agent agent )
    {
        if ( agentList != null && !agentList.isEmpty() && agent != null )
        {
            agentList.add( agent );
            return write();
        }

        return false;
    }
}
