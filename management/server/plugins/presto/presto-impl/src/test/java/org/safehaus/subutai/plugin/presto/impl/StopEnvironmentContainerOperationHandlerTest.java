package org.safehaus.subutai.plugin.presto.impl;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.common.tracker.ProductOperationState;
import org.safehaus.subutai.plugin.presto.api.PrestoClusterConfig;
import org.safehaus.subutai.plugin.presto.impl.handler.StopNodeOperationHandler;
import org.safehaus.subutai.plugin.presto.impl.mock.PrestoImplMock;


public class StopEnvironmentContainerOperationHandlerTest
{
    private PrestoImplMock mock;
    private AbstractOperationHandler handler;


    @Before
    public void setUp()
    {
        mock = new PrestoImplMock();
        handler = new StopNodeOperationHandler( mock, "test-cluster", "test-host" );
    }


    @Test
    public void testWithoutCluster()
    {

        handler.run();

        TrackerOperation po = handler.getTrackerOperation();
        Assert.assertTrue( po.getLog().contains( "not exist" ) );
        Assert.assertEquals( po.getState(), ProductOperationState.FAILED );
    }


    @Test
    public void testWithNotConnectedAgents()
    {
        PrestoClusterConfig config = new PrestoClusterConfig();
        mock.setClusterConfig( config );
        handler.run();

        TrackerOperation po = handler.getTrackerOperation();
        Assert.assertTrue( po.getLog().contains( "not connected" ) );
        Assert.assertEquals( po.getState(), ProductOperationState.FAILED );
    }
}
