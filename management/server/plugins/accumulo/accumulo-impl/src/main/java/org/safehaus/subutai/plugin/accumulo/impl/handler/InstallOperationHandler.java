package org.safehaus.subutai.plugin.accumulo.impl.handler;


import java.util.UUID;

import org.safehaus.subutai.common.exception.ClusterSetupException;
import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.core.environment.api.exception.EnvironmentBuildException;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import org.safehaus.subutai.plugin.accumulo.api.SetupType;
import org.safehaus.subutai.plugin.accumulo.impl.AccumuloImpl;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.zookeeper.api.ZookeeperClusterConfig;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


/**
 * Sets up Accumulo cluster either oer existing Hadoop & Zk clusters or with newly created Hadoop & ZK cluters
 */
public class InstallOperationHandler extends AbstractOperationHandler<AccumuloImpl>
{
    private final AccumuloClusterConfig config;
    private HadoopClusterConfig hadoopClusterConfig;
    private ZookeeperClusterConfig zookeeperClusterConfig;


    public InstallOperationHandler( final AccumuloImpl manager, final AccumuloClusterConfig config,
                                    final HadoopClusterConfig hadoopClusterConfig,
                                    final ZookeeperClusterConfig zookeeperClusterConfig )
    {
        this( manager, config );
        Preconditions.checkNotNull( hadoopClusterConfig, "Hadoop config is null" );
        Preconditions.checkNotNull( zookeeperClusterConfig, "Zookeeper config is null" );
        this.hadoopClusterConfig = hadoopClusterConfig;
        this.zookeeperClusterConfig = zookeeperClusterConfig;
    }


    public InstallOperationHandler( AccumuloImpl manager, AccumuloClusterConfig config )
    {
        super( manager, config.getClusterName() );
        this.config = config;
        trackerOperation = manager.getTracker().createTrackerOperation( AccumuloClusterConfig.PRODUCT_KEY,
                String.format( "Setting up %s cluster...", config.getClusterName() ) );
    }


    @Override
    public UUID getTrackerId()
    {
        return trackerOperation.getId();
    }


    @Override
    public void run()
    {
        if ( Strings.isNullOrEmpty( config.getClusterName() ) ||
                Strings.isNullOrEmpty( config.getZookeeperClusterName() ) ||
                Strings.isNullOrEmpty( config.getHadoopClusterName() ) ||
                Strings.isNullOrEmpty( config.getInstanceName() ) ||
                Strings.isNullOrEmpty( config.getPassword() ) )
        {
            trackerOperation.addLogFailed( "Malformed configuration" );
            return;
        }

        if ( config.getSetupType() == SetupType.OVER_HADOOP_N_ZK )
        {
            setupOverHadoopNZk();
        }
        else
        {
            setupWithHadoopNZk();
        }
    }


    private void setupOverHadoopNZk()
    {
        try
        {
            //setup up Accumulo cluster
            ClusterSetupStrategy setupStrategy = manager.getClusterSetupStrategy( null, config, trackerOperation );
            setupStrategy.setup();

            trackerOperation.addLogDone( String.format( "Cluster %s set up successfully", clusterName ) );
        }
        catch ( ClusterSetupException e )
        {
            trackerOperation
                    .addLogFailed( String.format( "Failed to setup cluster %s : %s", clusterName, e.getMessage() ) );
        }
    }


    private void setupWithHadoopNZk()
    {
        try
        {
            final String COMBO_TEMPLATE_NAME = "hadoopnzknaccumulo";
            hadoopClusterConfig.setTemplateName( COMBO_TEMPLATE_NAME );
            //create environment
            Environment env = manager.getEnvironmentManager().buildEnvironment(
                    manager.getHadoopManager().getDefaultEnvironmentBlueprint( hadoopClusterConfig ) );

            //setup Hadoop cluster
            ClusterSetupStrategy hadoopClusterSetupStrategy =
                    manager.getHadoopManager().getClusterSetupStrategy( trackerOperation, hadoopClusterConfig, env );
            hadoopClusterSetupStrategy.setup();

            //setup ZK cluster
            ClusterSetupStrategy zkClusterSetupStrategy =
                    manager.getZkManager().getClusterSetupStrategy( env, zookeeperClusterConfig, trackerOperation );
            zkClusterSetupStrategy.setup();

            //setup up Accumulo cluster
            ClusterSetupStrategy accumuloSetupStrategy =
                    manager.getClusterSetupStrategy( env, config, trackerOperation );
            accumuloSetupStrategy.setup();

            trackerOperation.addLogDone( String.format( "Cluster %s set up successfully", clusterName ) );
        }
        catch ( EnvironmentBuildException | ClusterSetupException e )
        {
            trackerOperation
                    .addLogFailed( String.format( "Failed to setup cluster %s : %s", clusterName, e.getMessage() ) );
        }
    }
}
