package io.subutai.core.channel.impl.util;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import io.subutai.common.settings.ChannelSettings;
import io.subutai.core.security.api.crypto.EncryptionTool;
import io.subutai.core.security.api.crypto.KeyManager;
import io.subutai.core.security.api.SecurityManager;


/**
 *
 */
public class MessageContentUtil
{
    private static final Logger LOG = LoggerFactory.getLogger( MessageContentUtil.class );


    public static void abortChain(Message message,int errorStatus, String error)
    {
        HttpServletResponse response = ( HttpServletResponse ) message.getExchange().getInMessage()
                                                                      .get( AbstractHTTPDestination
                                                                              .HTTP_RESPONSE );
        try
        {
            response.setStatus( errorStatus );
            response.getOutputStream().write( error.getBytes( Charset.forName( "UTF-8" ) ) );
            response.getOutputStream().flush();
            LOG.warn( error );
        }
        catch ( Exception e )
        {
            LOG.error( "Error writing to response: " + e.toString(), e );
        }

        message.getInterceptorChain().abort();

    }

    public static int checkUrlAccessibility( final int currentStatus, final URL url, final String basePath )
    {
        int status = currentStatus;

        if ( url.getPort() == Integer.parseInt( ChannelSettings.SECURE_PORT_X1 ) )
        {
            if ( ChannelSettings.checkURLArray( basePath, ChannelSettings.URL_ACCESS_PX1 ) == 0 )
            {
                status = 1;
            }
        }
        else if ( url.getPort() == Integer.parseInt( ChannelSettings.SECURE_PORT_X2 ) )
        {
        }
        else if ( url.getPort() == Integer.parseInt( ChannelSettings.SECURE_PORT_X3 ) )
        {
            //----------------------------------------------------------------------
        }
        else if ( url.getPort() == Integer.parseInt( ChannelSettings.SPECIAL_PORT_X1 ) || url.getPort() == Integer
                .parseInt( ChannelSettings.SPECIAL_SECURE_PORT_X1 ) )
        {
        }
        else
        {
            status = 0;
        }
        return status;
    }


    /* ******************************************************
     *
     */
    public static void decryptContent( SecurityManager securityManager, Message message, String hostIdSource,
                                       String hostIdTarget )
    {

        InputStream is = message.getContent( InputStream.class );
        CachedOutputStream os = new CachedOutputStream();

        try
        {
            IOUtils.copyAndCloseInput( is, os );
            os.flush();

            byte[] data = decryptData( securityManager, hostIdSource, hostIdTarget, os.getBytes() );
            org.apache.commons.io.IOUtils.closeQuietly( os );

            if ( data != null )
            {
                message.setContent( InputStream.class, new ByteArrayInputStream( data ) );
            }
        }
        catch ( IOException e )
        {

        }
        catch ( PGPException pe )
        {

        }
    }


    /* ******************************************************
     *
     */
    private static byte[] decryptData( SecurityManager securityManager, String hostIdSource, String hostIdTarget,
                                       byte[] data ) throws PGPException
    {

        try
        {
            if ( data == null || data.length == 0 )
            {
                return null;
            }
            else
            {
                EncryptionTool encTool = securityManager.getEncryptionTool();

                //encTool.

                KeyManager keyMan = securityManager.getKeyManager();
                PGPSecretKeyRing secKey = keyMan.getSecretKeyRing( hostIdSource );

                if(secKey!=null)
                {
                    LOG.debug( " ****** Decrypting with: " + hostIdSource + " ****** ");
                    byte[] outData = encTool.decrypt( data, secKey, "" );
                    //byte[] outData = encTool.decryptAndVerify();
                    return outData;
                }
                else
                {
                    LOG.debug( String.format( " ****** Decryption error. Could not find Secret key : %s ****** ", hostIdSource ) );
                    throw new PGPException("Cannot find Secret Key");
                }




            }
        }
        catch ( Exception ex )
        {
            throw new PGPException( ex.toString() );
        }
    }


    /* ******************************************************
    *
    */
    public static void encryptContent( SecurityManager securityManager, String hostIdSource, String hostIdTarget,
                                       String ip, Message message )
    {
        OutputStream os = message.getContent( OutputStream.class );

        CachedStream cs = new CachedStream();
        message.setContent( OutputStream.class, cs );

        message.getInterceptorChain().doIntercept( message );

        try
        {
            cs.flush();
            CachedOutputStream csnew = ( CachedOutputStream ) message.getContent( OutputStream.class );

            byte[] originalMessage = org.apache.commons.io.IOUtils.toByteArray( csnew.getInputStream() );
            csnew.flush();
            org.apache.commons.io.IOUtils.closeQuietly( cs );
            org.apache.commons.io.IOUtils.closeQuietly( csnew );

            //do something with original message to produce finalMessage
            byte[] finalMessage = encryptData( securityManager, hostIdSource, hostIdTarget, ip, originalMessage );

            if ( finalMessage != null )
            {
                InputStream replaceInStream = new ByteArrayInputStream( finalMessage );

                org.apache.commons.io.IOUtils.copy( replaceInStream, os );
                replaceInStream.close();
                org.apache.commons.io.IOUtils.closeQuietly( replaceInStream );

                os.flush();
                message.setContent( OutputStream.class, os );
            }

            org.apache.commons.io.IOUtils.closeQuietly( os );
        }
        catch ( IOException ioe )
        {
            throw new RuntimeException( ioe );
        }
        catch ( PGPException pe )
        {

        }
    }


    /* ******************************************************
     *
     */
    private static byte[] encryptData( SecurityManager securityManager, String hostIdSource, String hostIdTarget,
                                       String ip, byte[] data ) throws PGPException
    {
        try
        {
            if ( data == null || data.length == 0 )
            {
                return null;
            }
            else
            {
                EncryptionTool encTool = securityManager.getEncryptionTool();
                KeyManager keyMan = securityManager.getKeyManager();
                PGPPublicKey pubKey = keyMan.getRemoteHostPublicKey( hostIdTarget, ip );

                if(pubKey != null)
                {
                    LOG.debug( String.format( " ****** Encrypting with %s ****** ", hostIdTarget ) );
                    byte[] outData = encTool.encrypt( data, pubKey, true );
                    //byte[] outData = encTool.signAndEncrypt(  data, pubKey, false );
                    return outData;
                }
                else
                {
                    LOG.debug( String.format( " ****** Encryption error. Could not find Public key : %s ****** ", hostIdTarget ) );
                    throw new PGPException("Cannot find Public Key");
                }
            }
        }
        catch ( Exception ex )
        {
            throw new PGPException( ex.toString() );
        }
    }
}