package org.safehaus.subutai.plugin.mongodb.impl.handler;


import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.protocol.Response;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.command.api.command.AgentResult;
import org.safehaus.subutai.core.command.api.command.Command;
import org.safehaus.subutai.core.command.api.command.CommandCallback;
import org.safehaus.subutai.core.container.api.lxcmanager.LxcCreateException;
import org.safehaus.subutai.plugin.mongodb.api.MongoClusterConfig;
import org.safehaus.subutai.plugin.mongodb.api.NodeType;
import org.safehaus.subutai.plugin.mongodb.impl.MongoDbSetupStrategy;
import org.safehaus.subutai.plugin.mongodb.impl.MongoImpl;
import org.safehaus.subutai.plugin.mongodb.impl.common.CommandType;

import com.google.common.base.Strings;


/**
 * Handles add mongo node operation
 */
public class AddNodeOperationHandler extends AbstractOperationHandler<MongoImpl>
{
    private final TrackerOperation po;
    private final NodeType nodeType;


    public AddNodeOperationHandler( MongoImpl manager, String clusterName, NodeType nodeType )
    {
        super( manager, clusterName );
        this.nodeType = nodeType;
        po = manager.getTracker().createTrackerOperation( MongoClusterConfig.PRODUCT_KEY,
                String.format( "Adding %s to %s...", nodeType, clusterName ) );
    }


    @Override
    public UUID getTrackerId()
    {
        return po.getId();
    }


    @Override
    public void run()
    {
        MongoClusterConfig config = manager.getCluster( clusterName );
        if ( config == null )
        {
            po.addLogFailed( String.format( "Cluster with name %s does not exist", clusterName ) );
            return;
        }
        if ( nodeType == NodeType.CONFIG_NODE )
        {
            po.addLogFailed( "Can not add config server" );
            return;
        }
        if ( nodeType == NodeType.DATA_NODE && config.getDataNodes().size() == 7 )
        {
            po.addLogFailed( "Replica set cannot have more than 7 members" );
            return;
        }
        try
        {

            po.addLog( "Creating lxc container..." );

            Set<Agent> agents = manager.getContainerManager().clone( config.getTemplateName(), 1, null,
                    MongoDbSetupStrategy.getNodePlacementStrategyByNodeType( nodeType ) );

            Agent agent = agents.iterator().next();

            if ( nodeType == NodeType.DATA_NODE )
            {
                config.getDataNodes().add( agent );
                config.setNumberOfDataNodes( config.getNumberOfDataNodes() + 1 );
            }
            else if ( nodeType == NodeType.ROUTER_NODE )
            {
                config.getRouterServers().add( agent );
                config.setNumberOfRouters( config.getNumberOfRouters() + 1 );
            }
            po.addLog( "Lxc container created successfully\nConfiguring cluster..." );

            boolean result = true;
            //add node
            if ( nodeType == NodeType.DATA_NODE )
            {
                result = addDataNode( po, config, agent );
            }
            else if ( nodeType == NodeType.ROUTER_NODE )
            {
                result = addRouter( po, config, agent );
            }

            if ( result )
            {
                po.addLog( "Updating cluster information in database..." );

                manager.getPluginDAO().saveInfo( MongoClusterConfig.PRODUCT_KEY, config.getClusterName(), config );
                po.addLogDone( "Cluster information updated in database" );
            }
            else
            {
                po.addLogFailed( "Node addition failed" );
            }
        }
        catch ( LxcCreateException ex )
        {
            po.addLogFailed( ex.getMessage() );
        }
    }


    private boolean addDataNode( TrackerOperation po, final MongoClusterConfig config, Agent agent )
    {
        List<Command> commands = manager.getCommands().getAddDataNodeCommands( config, agent );

        boolean additionOK = true;
        Command findPrimaryNodeCommand = null;

        for ( Command command : commands )
        {
            po.addLog( String.format( "Running command: %s", command.getDescription() ) );
            final AtomicBoolean commandOK = new AtomicBoolean();

            if ( command.getData() == CommandType.FIND_PRIMARY_NODE )
            {
                findPrimaryNodeCommand = command;
            }

            if ( command.getData() == CommandType.START_DATA_NODES )
            {
                manager.getCommandRunner().runCommand( command, new CommandCallback()
                {

                    @Override
                    public void onResponse( Response response, AgentResult agentResult, Command command )
                    {
                        if ( agentResult.getStdOut().contains( "child process started successfully, parent exiting" ) )
                        {
                            commandOK.set( true );
                            stop();
                        }
                    }
                } );
            }
            else
            {
                manager.getCommandRunner().runCommand( command );
            }

            if ( command.hasSucceeded() || commandOK.get() )
            {
                po.addLog( String.format( "Command %s succeeded", command.getDescription() ) );
            }
            else
            {
                po.addLog( String.format( "Command %s failed: %s", command.getDescription(), command.getAllErrors() ) );
                additionOK = false;
                break;
            }
        }

        //parse result of findPrimaryNodeCommand
        if ( additionOK )
        {
            if ( findPrimaryNodeCommand != null && !findPrimaryNodeCommand.getResults().isEmpty() )
            {
                Agent primaryNodeAgent = null;
                Pattern p = Pattern.compile( "primary\" : \"(.*)\"" );
                AgentResult result = findPrimaryNodeCommand.getResults().entrySet().iterator().next().getValue();
                Matcher m = p.matcher( result.getStdOut() );
                if ( m.find() )
                {
                    String primaryNodeHost = m.group( 1 );
                    if ( !Strings.isNullOrEmpty( primaryNodeHost ) )
                    {
                        String hostname = primaryNodeHost.split( ":" )[0].replace( "." + config.getDomainName(), "" );
                        primaryNodeAgent = manager.getAgentManager().getAgentByHostname( hostname );
                    }
                }

                if ( primaryNodeAgent != null )
                {
                    Command registerSecondaryNodeWithPrimaryCommand = manager.getCommands()
                                                                             .getRegisterSecondaryNodeWithPrimaryCommand(
                                                                                     agent, config.getDataNodePort(),
                                                                                     config.getDomainName(),
                                                                                     primaryNodeAgent );

                    manager.getCommandRunner().runCommand( registerSecondaryNodeWithPrimaryCommand );
                    if ( registerSecondaryNodeWithPrimaryCommand.hasSucceeded() )
                    {
                        po.addLog( String.format( "Command %s succeeded",
                                registerSecondaryNodeWithPrimaryCommand.getDescription() ) );

                        return true;
                    }
                    else
                    {
                        po.addLog( String.format( "Command %s failed: %s",
                                registerSecondaryNodeWithPrimaryCommand.getDescription(),
                                registerSecondaryNodeWithPrimaryCommand.getAllErrors() ) );
                    }
                }
                else
                {
                    po.addLog( "Could not find primary node" );
                }
            }
            else
            {
                po.addLog( "Could not find primary node" );
            }
        }

        return false;
    }


    private boolean addRouter( TrackerOperation po, final MongoClusterConfig config, Agent agent )
    {
        List<Command> commands = manager.getCommands().getAddRouterCommands( config, agent );

        boolean additionOK = true;

        for ( Command command : commands )
        {
            po.addLog( String.format( "Running command: %s", command.getDescription() ) );
            final AtomicBoolean commandOK = new AtomicBoolean();

            if ( command.getData() == CommandType.START_ROUTERS )
            {
                manager.getCommandRunner().runCommand( command, new CommandCallback()
                {

                    @Override
                    public void onResponse( Response response, AgentResult agentResult, Command command )
                    {
                        if ( agentResult.getStdOut().contains( "child process started successfully, parent exiting" ) )
                        {
                            commandOK.set( true );
                            stop();
                        }
                    }
                } );
            }
            else
            {
                manager.getCommandRunner().runCommand( command );
            }

            if ( command.hasSucceeded() || commandOK.get() )
            {
                po.addLog( String.format( "Command %s succeeded", command.getDescription() ) );
            }
            else
            {
                po.addLog( String.format( "Command %s failed: %s", command.getDescription(), command.getAllErrors() ) );
                additionOK = false;
                break;
            }
        }

        return additionOK;
    }
}
