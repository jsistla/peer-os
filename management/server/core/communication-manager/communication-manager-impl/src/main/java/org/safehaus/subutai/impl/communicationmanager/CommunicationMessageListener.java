package org.safehaus.subutai.impl.communicationmanager;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.safehaus.subutai.api.communicationmanager.CommandJson;
import org.safehaus.subutai.api.communicationmanager.ResponseListener;
import org.safehaus.subutai.shared.protocol.Response;
import org.safehaus.subutai.shared.protocol.enums.ResponseType;

import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.RemoveInfo;


/**
 * This class is used internally by CommunicationManagerImpl to notify response listener on new message.
 */
class CommunicationMessageListener implements MessageListener {

    private static final Logger LOG = Logger.getLogger( CommunicationMessageListener.class.getName() );
    private final ConcurrentLinkedQueue<ResponseListener> listeners = new ConcurrentLinkedQueue<>();


    /**
     * New message handler called by amq broker
     *
     * @param message - received message
     */
    @Override
    public void onMessage( Message message ) {
        try {
            if ( message instanceof BytesMessage ) {
                BytesMessage msg = ( BytesMessage ) message;
                byte[] msg_bytes = new byte[( int ) msg.getBodyLength()];
                msg.readBytes( msg_bytes );
                String jsonCmd = new String( msg_bytes, "UTF-8" );
                Response response = CommandJson.getResponse( jsonCmd );
                if ( response != null ) {
                    if ( response.getType() != ResponseType.HEARTBEAT_RESPONSE ) {
                        LOG.log( Level.INFO, "\nReceived {0}",
                                CommandJson.getJson( CommandJson.getCommand( jsonCmd ) ) );
                    }
                    response.setTransportId( ( ( ActiveMQMessage ) message ).getProducerId().toString() );
                    notifyListeners( response );
                }
                else {
                    LOG.log( Level.WARNING, "Could not parse response{0}", jsonCmd );
                }
            }
            else if ( message instanceof ActiveMQMessage ) {
                ActiveMQMessage aMsg = ( ActiveMQMessage ) message;
                if ( aMsg.getDataStructure() instanceof RemoveInfo ) {
                    Response agentDisconnect = new Response();
                    agentDisconnect.setType( ResponseType.AGENT_DISCONNECT );
                    agentDisconnect
                            .setTransportId( ( ( RemoveInfo ) aMsg.getDataStructure() ).getObjectId().toString() );
                    notifyListeners( agentDisconnect );
                }
            }
        }
        catch ( Exception ex ) {
            LOG.log( Level.SEVERE, "Error in onMessage", ex );
        }
    }


    /**
     * Notifies listeners on new response
     *
     * @param response - response to notify listeners
     */
    private void notifyListeners( Response response ) {
        try {
            for ( Iterator<ResponseListener> it = listeners.iterator(); it.hasNext(); ) {
                ResponseListener ai = it.next();
                try {
                    ai.onResponse( response );
                }
                catch ( Exception e ) {
                    it.remove();
                    LOG.log( Level.SEVERE, "Error notifying message listeners, removing faulting listener", e );
                }
            }
        }
        catch ( Exception ex ) {
            LOG.log( Level.SEVERE, "Error in notifyListeners", ex );
        }
    }


    /**
     * Adds response listener
     *
     * @param listener - listener to add
     */
    public void addListener( ResponseListener listener ) {
        try {
            listeners.add( listener );
        }
        catch ( Exception ex ) {
            LOG.log( Level.SEVERE, "Error in addListener", ex );
        }
    }


    /**
     * Removes response listener
     *
     * @param listener - - listener to remove
     */
    public void removeListener( ResponseListener listener ) {
        try {
            listeners.remove( listener );
        }
        catch ( Exception ex ) {
            LOG.log( Level.SEVERE, "Error in removeListener", ex );
        }
    }


    /**
     * Returns collection of listeners
     *
     * @return - listeners added
     */
    Collection<ResponseListener> getListeners() {
        return Collections.unmodifiableCollection( listeners );
    }


    /**
     * Disposes message listener
     */
    public void destroy() {
        listeners.clear();
    }
}
