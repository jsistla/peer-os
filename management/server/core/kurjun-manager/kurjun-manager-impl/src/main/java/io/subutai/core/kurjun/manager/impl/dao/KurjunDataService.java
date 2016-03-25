package io.subutai.core.kurjun.manager.impl.dao;


import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.subutai.common.dao.DaoManager;
import io.subutai.core.kurjun.manager.api.model.Kurjun;
import io.subutai.core.kurjun.manager.impl.model.KurjunEntity;


/**
 *
 */
public class KurjunDataService
{
    private static final Logger LOG = LoggerFactory.getLogger( KurjunDataService.class.getName() );

    private DaoManager daoManager = null;


    /* *************************************************
     *
     */
    public KurjunDataService( DaoManager daoManager )
    {
        this.daoManager = daoManager;
    }


    /* *************************************************
     *
     */
    public Kurjun getKurjunData( final String id )
    {
        EntityManager em = daoManager.getEntityManagerFromFactory();

        Kurjun result = null;
        try
        {
            Query query = em.createQuery( "SELECT c FROM KurjunEntity c" )
                            .setParameter( "owner_fprint", id );
            //            daoManager.startTransaction( em );
            result = ( Kurjun ) query.getSingleResult();
            //                    result = em.find( KurjunEntity.class, id );
            //            daoManager.commitTransaction( em );
        }
        catch ( Exception e )
        {
            daoManager.rollBackTransaction( em );
        }
        finally
        {
            daoManager.closeEntityManager( em );
        }
        return result;
    }


    /* *************************************************
     *
     */
    public List<Kurjun> getAllKurjunData()
    {
        EntityManager em = daoManager.getEntityManagerFromFactory();

        List<Kurjun> result = Lists.newArrayList();
        try
        {
            result = em.createQuery( "select h from KurjunEntity h", Kurjun.class ).getResultList();
        }
        catch ( Exception e )
        {
        }
        finally
        {
            daoManager.closeEntityManager( em );
        }
        return result;
    }


    /* *************************************************
     *
     */
    public void persistKurjunData( Kurjun item )
    {
        EntityManager em = daoManager.getEntityManagerFromFactory();
        try
        {
            daoManager.startTransaction( em );
            em.persist( item );
            em.flush();

            daoManager.commitTransaction( em );
        }
        catch ( Exception e )
        {
            daoManager.rollBackTransaction( em );
            e.printStackTrace();
            LOG.error( "Error while saving Kurjun Data:" + e.getMessage() );
        }
        finally
        {
            daoManager.closeEntityManager( em );
        }
    }


    /* *************************************************
     *
     */
    public void removeKurjunData( final String id )
    {
        EntityManager em = daoManager.getEntityManagerFromFactory();
        try
        {
            daoManager.startTransaction( em );
            KurjunEntity item = em.find( KurjunEntity.class, id );
            em.remove( item );
            daoManager.commitTransaction( em );
        }
        catch ( Exception e )
        {
            daoManager.rollBackTransaction( em );
        }
        finally
        {
            daoManager.closeEntityManager( em );
        }
    }


    /* *************************************************
     *
     */
    public void updateKurjunData( final Kurjun item )
    {
        EntityManager em = daoManager.getEntityManagerFromFactory();
        try
        {
            daoManager.startTransaction( em );
            em.merge( item );
            daoManager.commitTransaction( em );
        }
        catch ( Exception e )
        {
            daoManager.rollBackTransaction( em );
        }
        finally
        {
            daoManager.closeEntityManager( em );
        }
    }
}
