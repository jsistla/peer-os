package org.safehaus.subutai.core.peer.impl.model;


import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.safehaus.subutai.common.protocol.Template;
import org.safehaus.subutai.core.hostregistry.api.ContainerHostInfo;
import org.safehaus.subutai.core.hostregistry.api.ContainerHostState;
import org.safehaus.subutai.core.hostregistry.api.HostInfo;
import org.safehaus.subutai.core.lxc.quota.api.QuotaEnum;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.core.peer.api.HostKey;
import org.safehaus.subutai.core.peer.api.Peer;
import org.safehaus.subutai.core.peer.api.PeerException;
import org.safehaus.subutai.core.peer.api.ResourceHost;


/**
 * ContainerHost class.
 */
@Entity
@Table( name = "container_host" )
@Access( AccessType.FIELD )
public class ContainerHostEntity extends AbstractSubutaiHost implements ContainerHost
{
    @ManyToOne( targetEntity = ResourceHostEntity.class )
    @JoinColumn( name = "parent_id" )
    private ResourceHost parent;

    @Column( name = "env_id", nullable = false )
    private String environmentId = "UNKNOWN";
    @Column( name = "creator_id", nullable = false )
    private String creatorPeerId = "UNKNOWN";
    @Column( name = "template_name", nullable = false )
    private String templateName = "UNKNOWN";
    @Column( name = "template_arch", nullable = false )
    private String templateArch = "UNKNOWN";

    @Transient
    private volatile ContainerHostState state = ContainerHostState.STOPPED;
    @Column( name = "node_group_name", nullable = false )
    private String nodeGroupName = "UNKNOWN";
    //    @Column( name = "parent_host_name", nullable = false )
    //    protected String parentHostname;


    private ContainerHostEntity()
    {
    }


    public ContainerHostEntity( String peerId, HostInfo hostInfo )
    {
        super( peerId, hostInfo );
        this.creatorPeerId = "UNKNOWN";
        this.environmentId = "UNKNOWN";
        this.nodeGroupName = "UNKNOWN";
        this.templateArch = "amd64";
        this.templateName = "UNKNOWN";
        //        this.parentHostname = parentHostname;
    }


    public ContainerHostEntity( final String peerId, final String creatorPeerId, final String environmentId,
                                final String nodeGroupName, final HostInfo hostInfo )
    {
        super( peerId, hostInfo );
        this.creatorPeerId = creatorPeerId;
        this.environmentId = environmentId;
        this.nodeGroupName = nodeGroupName;
        this.templateArch = "amd64";
        this.templateName = "UNKNOWN";
    }


    public ContainerHostEntity( final HostKey hostKey )
    {
        this.hostId = hostKey.getHostId();
        this.peerId = hostKey.getPeerId();
        this.creatorPeerId = hostKey.getCreatorId();
        this.environmentId = hostKey.getEnvironmentId();
        this.nodeGroupName = hostKey.getNodeGroupName();
    }


    public String getNodeGroupName()
    {
        return nodeGroupName;
    }


    public void setNodeGroupName( final String nodeGroupName )
    {
        this.nodeGroupName = nodeGroupName;
    }


    public String getEnvironmentId()
    {
        return environmentId;
    }


    public void setEnvironmentId( final String environmentId )
    {
        this.environmentId = environmentId;
    }


    public String getCreatorPeerId()
    {
        return creatorPeerId;
    }


    public void setCreatorPeerId( final String creatorPeerId )
    {
        this.creatorPeerId = creatorPeerId;
    }


    public String getTemplateName()
    {
        return templateName;
    }


    public void setTemplateName( final String templateName )
    {
        this.templateName = templateName;
    }


    @Override
    public String getTemplateArch()
    {
        return templateArch;
    }


    @Override
    public void setTemplateArch( final String templateArch )
    {
        this.templateArch = templateArch;
    }


    public ContainerHostState getState()
    {
        return state;
    }


    public ResourceHost getParent()
    {
        return parent;
    }


    public void setParent( final ResourceHost parent )
    {
        this.parent = ( ResourceHostEntity ) parent;
    }


    public String getQuota( final QuotaEnum quota ) throws PeerException
    {
        Peer peer = getPeer();
        return peer.getQuota( this, quota );
    }


    public void setQuota( final QuotaEnum quota, final String value ) throws PeerException
    {
        Peer peer = getPeer();
        peer.setQuota( this, quota, value );
    }


    public Template getTemplate() throws PeerException
    {
        Peer peer = getPeer();
        return peer.getTemplate( getTemplateName() );
    }


    public void dispose() throws PeerException
    {
        Peer peer = getPeer();
        peer.destroyContainer( this );
    }


    @Override
    public void updateHostInfo( final HostInfo hostInfo )
    {
        super.updateHostInfo( hostInfo );

        ContainerHostInfo conatinerHostInfo = ( ContainerHostInfo ) hostInfo;
        this.state = conatinerHostInfo.getStatus();
    }


    @Override
    public String getParentHostname()
    {
        return parent.getHostname();
    }
}