package org.safehaus.subutai.core.peer.api;


import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManagerFactory;

import org.safehaus.subutai.common.peer.Peer;
import org.safehaus.subutai.common.peer.PeerException;
import org.safehaus.subutai.common.peer.PeerInfo;


public interface PeerManager
{

    boolean trustRequest( UUID peerId,String root_server_px1) throws PeerException;

    boolean trustResponse( UUID peerId,String root_server_px1,short status) throws PeerException;

    boolean register( PeerInfo peerInfo ) throws PeerException;

    boolean update( PeerInfo peerInfo );

    public List<PeerInfo> peers();

    public PeerInfo getLocalPeerInfo();

    public PeerInfo getPeerInfo( UUID uuid );

    boolean unregister( String uuid ) throws PeerException;

    public Peer getPeer( UUID peerId );

    public Peer getPeer( String peerId );

    public List<Peer> getPeers();

    public LocalPeer getLocalPeer();

    public void addRequestListener( RequestListener listener );

    public void removeRequestListener( RequestListener listener );

    public EntityManagerFactory getEntityManagerFactory();

    public EnvironmentContext prepareEnvironment( UUID environmentId, String email );
}
