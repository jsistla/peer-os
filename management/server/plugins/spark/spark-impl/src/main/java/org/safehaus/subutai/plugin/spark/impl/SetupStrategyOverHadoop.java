package org.safehaus.subutai.plugin.spark.impl;


import java.util.Iterator;
import java.util.Set;

import org.safehaus.subutai.common.exception.ClusterSetupException;
import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.protocol.ConfigBase;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.settings.Common;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.common.util.CollectionUtil;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.spark.api.SparkClusterConfig;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;


public class SetupStrategyOverHadoop implements ClusterSetupStrategy
{
    final TrackerOperation po;
    final SparkImpl manager;
    final SparkClusterConfig config;
    private Environment environment;
    private Set<ContainerHost> allNodes;


    public SetupStrategyOverHadoop( TrackerOperation po, SparkImpl manager, SparkClusterConfig config,
                                    Environment environment )
    {
        Preconditions.checkNotNull( config, "Cluster config is null" );
        Preconditions.checkNotNull( po, "Product operation tracker is null" );
        Preconditions.checkNotNull( manager, "Manager is null" );
        Preconditions.checkNotNull( environment, "Environment is null" );


        this.po = po;
        this.manager = manager;
        this.config = config;
        this.environment = environment;
    }


    @Override
    public ConfigBase setup() throws ClusterSetupException
    {
        check();
        configure();
        return config;
    }


    private void check() throws ClusterSetupException
    {

        if ( manager.getCluster( config.getClusterName() ) != null )
        {
            throw new ClusterSetupException( String.format( "Cluster %s already exists", config.getClusterName() ) );
        }
        if ( config.getMasterNodeId() == null )
        {
            throw new ClusterSetupException( "Master node not specified" );
        }
        if ( CollectionUtil.isCollectionEmpty( config.getSlaveIds() ) )
        {
            throw new ClusterSetupException( "No slave nodes" );
        }

        ContainerHost master = environment.getContainerHostByUUID( config.getMasterNodeId() );
        if ( master == null )
        {
            throw new ClusterSetupException( "Master not found in the environment" );
        }
        if ( !master.isConnected() )
        {
            throw new ClusterSetupException( "Master is not connected" );
        }

        Set<ContainerHost> slaves = environment.getHostsByIds( config.getSlaveIds() );

        if ( slaves.size() > config.getSlaveIds().size() )
        {
            throw new ClusterSetupException( "Fewer slaves found in the environment than indicated" );
        }

        for ( ContainerHost slave : slaves )
        {
            if ( !slave.isConnected() )
            {
                throw new ClusterSetupException(
                        String.format( "Container %s is not connected", slave.getHostname() ) );
            }
        }

        // check Hadoop cluster
        HadoopClusterConfig hc = manager.hadoopManager.getCluster( config.getHadoopClusterName() );
        if ( hc == null )
        {
            throw new ClusterSetupException( "Could not find Hadoop cluster " + config.getHadoopClusterName() );
        }
        if ( !hc.getAllNodes().containsAll( config.getAllNodesIds() ) )
        {
            throw new ClusterSetupException(
                    "Not all nodes belong to Hadoop cluster " + config.getHadoopClusterName() );
        }
        //        config.setHadoopNodeIds( new HashSet<>( hc.getAllNodes() ) );

        po.addLog( "Checking prerequisites..." );

        //check installed subutai packages
        allNodes = Sets.newHashSet( master );
        allNodes.addAll( slaves );

        RequestBuilder checkInstalledCommand = manager.getCommands().getCheckInstalledCommand();

        for ( Iterator<ContainerHost> iterator = allNodes.iterator(); iterator.hasNext(); )
        {
            final ContainerHost node = iterator.next();
            try
            {
                CommandResult result = node.execute( checkInstalledCommand );
                if ( result.getStdOut().contains( Commands.PACKAGE_NAME ) )
                {
                    po.addLog(
                            String.format( "Node %s already has Spark installed. Omitting this node from installation",
                                    node.getHostname() ) );
                    config.getSlaveIds().remove( node.getId() );
                    iterator.remove();
                }
                else if ( !result.getStdOut()
                                 .contains( Common.PACKAGE_PREFIX + HadoopClusterConfig.PRODUCT_NAME.toLowerCase() ) )
                {
                    po.addLog(
                            String.format( "Node %s has no Hadoop installation. Omitting this node from installation",
                                    node.getHostname() ) );
                    config.getSlaveIds().remove( node.getId() );
                    iterator.remove();
                }
            }
            catch ( CommandException e )
            {
                throw new ClusterSetupException( "Failed to check presence of installed subutai packages" );
            }
        }

        if ( config.getSlaveIds().isEmpty() )
        {
            throw new ClusterSetupException( "No nodes eligible for installation\nInstallation aborted" );
        }
        if ( !allNodes.contains( master ) )
        {
            throw new ClusterSetupException( "Master node was omitted\nInstallation aborted" );
        }
    }


    private void configure() throws ClusterSetupException
    {
        po.addLog( "Updating db..." );
        //save to db
        manager.getPluginDAO().saveInfo( SparkClusterConfig.PRODUCT_KEY, config.getClusterName(), config );
        po.addLog( "Cluster info saved to DB\nInstalling Spark..." );

        SetupHelper helper = new SetupHelper( manager, config, environment, po );
        po.addLog( "Installing Spark..." );
        //install spark
        RequestBuilder installCommand = manager.getCommands().getInstallCommand();
        for ( ContainerHost node : allNodes )
        {
            try
            {
                helper.processResult( node, node.execute( installCommand ) );
            }
            catch ( CommandException e )
            {
                throw new ClusterSetupException(
                        String.format( "Error while installing Spark on container %s; %s", node.getHostname(),
                                e.getMessage() ) );
            }
        }

        po.addLog( "Configuring cluster..." );

        helper.configureMasterIP();
        helper.registerSlaves();
        helper.startCluster();
    }
}
