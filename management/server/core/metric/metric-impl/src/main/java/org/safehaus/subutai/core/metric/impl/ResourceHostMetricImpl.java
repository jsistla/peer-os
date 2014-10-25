package org.safehaus.subutai.core.metric.impl;


import java.util.UUID;

import org.safehaus.subutai.core.metric.api.ResourceHostMetric;

import com.google.common.base.Preconditions;


/**
 * Implementation of ResourceHostMetric
 */
public class ResourceHostMetricImpl extends ResourceHostMetric
{

    protected void setPeerId( final UUID peerId )
    {
        Preconditions.checkNotNull( peerId, "Invalid peer id" );

        this.peerId = peerId;
    }
}
