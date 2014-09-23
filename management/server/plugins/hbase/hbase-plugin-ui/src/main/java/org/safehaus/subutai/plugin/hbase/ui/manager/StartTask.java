package org.safehaus.subutai.plugin.hbase.ui.manager;


import org.safehaus.subutai.common.tracker.ProductOperationState;
import org.safehaus.subutai.common.tracker.ProductOperationView;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.hbase.api.HBase;
import org.safehaus.subutai.plugin.hbase.api.HBaseClusterConfig;

import java.util.UUID;


public class StartTask implements Runnable
{

    private final String clusterName;
    private final CompleteEvent completeEvent;
    private final HBase hbase;
    private final Tracker tracker;


    public StartTask( final HBase hBase, final Tracker tracker, String clusterName,
                      CompleteEvent completeEvent )
    {
        this.clusterName = clusterName;
        this.completeEvent = completeEvent;
        this.hbase = hBase;
        this.tracker = tracker;
    }


    @Override
    public void run()
    {

        UUID trackID = hbase.startCluster( clusterName );

        long start = System.currentTimeMillis();

        while ( !Thread.interrupted() )
        {
            ProductOperationView po = tracker.getProductOperation( HBaseClusterConfig.PRODUCT_KEY, trackID );
            if ( po != null )
            {
                if ( po.getState() != ProductOperationState.RUNNING )
                {
                    completeEvent.onComplete( po.getLog() );
                    break;
                }
            }

            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
            if ( System.currentTimeMillis() - start > 60 * 1000 )
            {
                break;
            }
        }
    }
}
