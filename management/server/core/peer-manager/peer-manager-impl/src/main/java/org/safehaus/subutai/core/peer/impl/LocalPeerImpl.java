package org.safehaus.subutai.core.peer.impl;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.safehaus.subutai.common.command.CommandCallback;
import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.protocol.Criteria;
import org.safehaus.subutai.common.protocol.Template;
import org.safehaus.subutai.core.agent.api.AgentManager;
import org.safehaus.subutai.core.executor.api.CommandExecutor;
import org.safehaus.subutai.core.hostregistry.api.ContainerHostInfo;
import org.safehaus.subutai.core.hostregistry.api.ContainerHostState;
import org.safehaus.subutai.core.hostregistry.api.HostInfo;
import org.safehaus.subutai.core.hostregistry.api.HostListener;
import org.safehaus.subutai.core.hostregistry.api.HostRegistry;
import org.safehaus.subutai.core.hostregistry.api.ResourceHostInfo;
import org.safehaus.subutai.core.lxc.quota.api.QuotaEnum;
import org.safehaus.subutai.core.lxc.quota.api.QuotaException;
import org.safehaus.subutai.core.lxc.quota.api.QuotaManager;
import org.safehaus.subutai.core.peer.api.CloneParam;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.core.peer.api.Host;
import org.safehaus.subutai.core.peer.api.HostEvent;
import org.safehaus.subutai.core.peer.api.HostEventListener;
import org.safehaus.subutai.core.peer.api.HostNotFoundException;
import org.safehaus.subutai.core.peer.api.HostTask;
import org.safehaus.subutai.core.peer.api.LocalPeer;
import org.safehaus.subutai.core.peer.api.ManagementHost;
import org.safehaus.subutai.core.peer.api.Payload;
import org.safehaus.subutai.core.peer.api.PeerException;
import org.safehaus.subutai.core.peer.api.PeerInfo;
import org.safehaus.subutai.core.peer.api.PeerManager;
import org.safehaus.subutai.core.peer.api.RequestListener;
import org.safehaus.subutai.core.peer.api.ResourceHost;
import org.safehaus.subutai.core.peer.api.ResourceHostException;
import org.safehaus.subutai.core.peer.api.Task;
import org.safehaus.subutai.core.peer.impl.dao.ManagementHostDataService;
import org.safehaus.subutai.core.peer.impl.dao.PeerDAO;
import org.safehaus.subutai.core.peer.impl.dao.ResourceHostDataService;
import org.safehaus.subutai.core.peer.impl.model.ContainerHostEntity;
import org.safehaus.subutai.core.peer.impl.model.ManagementHostEntity;
import org.safehaus.subutai.core.peer.impl.model.ResourceHostEntity;
import org.safehaus.subutai.core.registry.api.RegistryException;
import org.safehaus.subutai.core.registry.api.TemplateRegistry;
import org.safehaus.subutai.core.strategy.api.ServerMetric;
import org.safehaus.subutai.core.strategy.api.StrategyException;
import org.safehaus.subutai.core.strategy.api.StrategyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


/**
 * Local peer implementation
 */
public class LocalPeerImpl implements LocalPeer, HostListener, HostEventListener
{
    private static final Logger LOG = LoggerFactory.getLogger( LocalPeerImpl.class );

    private static final String SOURCE_MANAGEMENT_HOST = "MANAGEMENT_HOST";
    private static final String SOURCE_RESOURCE_HOST = "RESOURCE_HOST";
    private static final long HOST_INACTIVE_TIME = 5 * 1000 * 60; // 5 min
    private static final int MAX_LXC_NAME = 15;
    private PeerManager peerManager;
    private TemplateRegistry templateRegistry;
    //    private CommunicationManager communicationManager;
    private PeerDAO peerDAO;
    private ManagementHost managementHost;
    private Set<ResourceHost> resourceHosts = Sets.newHashSet();
    private CommandExecutor commandExecutor;
    private AgentManager agentManager;
    private StrategyManager strategyManager;
    private QuotaManager quotaManager;
    private ConcurrentMap<String, AtomicInteger> sequences;
    private ManagementHostDataService managementHostDataService;
    private ResourceHostDataService resourceHostDataService;
    private HostRegistry hostRegistry;
    private Set<RequestListener> requestListeners;
    private List<HostTask> tasks = Lists.newCopyOnWriteArrayList();


    public LocalPeerImpl( PeerManager peerManager, AgentManager agentManager, TemplateRegistry templateRegistry,
                          PeerDAO peerDao, QuotaManager quotaManager, StrategyManager strategyManager,
                          Set<RequestListener> requestListeners, CommandExecutor commandExecutor,
                          HostRegistry hostRegistry )

    {
        this.agentManager = agentManager;
        this.strategyManager = strategyManager;
        this.peerManager = peerManager;
        //        this.containerManager = containerManager;
        this.templateRegistry = templateRegistry;
        this.peerDAO = peerDao;
        //        this.communicationManager = communicationManager;
        this.quotaManager = quotaManager;
        this.requestListeners = requestListeners;
        //        this.managementHostDataService = managementHostDataService;
        this.commandExecutor = commandExecutor;
        this.hostRegistry = hostRegistry;
    }


    @Override
    public void init()
    {
        managementHostDataService = new ManagementHostDataService( peerManager.getEntityManagerFactory() );
        Collection allManagementHostEntity = managementHostDataService.getAll();
        if ( allManagementHostEntity != null && allManagementHostEntity.size() > 0 )
        {
            managementHost = ( ManagementHost ) allManagementHostEntity.iterator().next();
            managementHost.addListener( this );
        }

        resourceHostDataService = new ResourceHostDataService( peerManager.getEntityManagerFactory() );
        resourceHosts = Sets.newHashSet();
        resourceHosts.addAll( resourceHostDataService.getAll() );

        for ( ResourceHost resourceHost : resourceHosts )
        {
            resourceHost.addListener( this );
        }

        for ( ResourceHost resourceHost : resourceHosts )
        {
            //            resourceHost.resetHeartbeat();
        }
        hostRegistry.addHostListener( this );
        sequences = new ConcurrentHashMap<>();
    }


    @Override
    public void shutdown()
    {
        //        communicationManager.removeListener( this );
        hostRegistry.removeHostListener( this );
    }


    @Override
    public UUID getId()
    {
        return peerManager.getLocalPeerInfo().getId();
    }


    @Override
    public String getName()
    {
        return peerManager.getLocalPeerInfo().getName();
    }


    @Override
    public UUID getOwnerId()
    {
        return peerManager.getLocalPeerInfo().getOwnerId();
    }


    @Override
    public PeerInfo getPeerInfo()
    {
        return peerManager.getLocalPeerInfo();
    }


    public void submit( Task task )
    {
        checkSecurity( task );
        accept( task );
    }


    private void runTasks( final Task task )
    {

    }


    private void accept( final Task task )
    {

    }


    private void checkSecurity( Task task )
    {

    }


    @Override
    public ContainerHost createContainer( final String hostName, final String templateName, final String cloneName,
                                          final UUID environmentId ) throws PeerException
    {
        Preconditions.checkNotNull( hostName, "Host name is null." );
        Preconditions.checkNotNull( environmentId, "Environment ID is null." );
        Preconditions.checkNotNull( templateName, "Template list is null." );

        CloneParam cloneParam = new CloneParam( cloneName, Lists.newArrayList( getTemplate( templateName ) ) );
        ResourceHost resourceHost = getResourceHostByName( hostName );


        String taskGroupId = UUID.randomUUID().toString();
        CloneTask cloneTask = new CloneTask( taskGroupId, resourceHost, cloneParam );
        cloneTask.start();

        try
        {
            return waitCloneTasks( Lists.newArrayList( cloneTask ) ).iterator().next();
        }
        catch ( Exception e )
        {
            LOG.error( "Clone fail", e );
            throw new PeerException( "Clone fail.", e.toString() );
        }
    }


    @Override
    public Set<ContainerHost> createContainers( final UUID creatorPeerId, final UUID environmentId,
                                                final List<Template> templates, final int quantity,
                                                final String strategyId, final List<Criteria> criteria,
                                                String nodeGroupName ) throws PeerException
    {
        Preconditions.checkNotNull( creatorPeerId, "Creator peer ID is null." );
        Preconditions.checkNotNull( nodeGroupName, "Node group name is null." );
        Preconditions.checkNotNull( environmentId, "Environment ID is null." );
        Preconditions.checkNotNull( templates, "Template list is null." );
        Preconditions.checkState( templates.size() > 0, "Template list is empty" );

        LOG.info( String.format( "=============> Order received: %s %d %s", nodeGroupName, quantity,
                creatorPeerId.toString() ) );

        try
        {
            for ( Template t : templates )
            {
                if ( t.isRemote() )
                {
                    tryToRegister( t );
                }
            }
            String templateName = templates.get( templates.size() - 1 ).getTemplateName();


            List<ServerMetric> serverMetricMap = new ArrayList<>();
            for ( ResourceHost resourceHost : getResourceHosts() )
            {
                if ( resourceHost.isConnected() )
                {
                    serverMetricMap.add( resourceHost.getMetric() );
                }
            }
            Map<ServerMetric, Integer> slots;
            try
            {
                slots = strategyManager.getPlacementDistribution( serverMetricMap, quantity, strategyId, criteria );
            }
            catch ( StrategyException e )
            {
                throw new PeerException( e.getMessage() );
            }

            Set<String> existingContainerNames = getContainerNames();

            // clone specified number of instances and store their names
            Map<ResourceHost, Set<String>> cloneNames = new HashMap<>();

            for ( Map.Entry<ServerMetric, Integer> e : slots.entrySet() )
            {
                Set<String> hostCloneNames = new HashSet<>();
                for ( int i = 0; i < e.getValue(); i++ )
                {
                    String newContainerName = nextHostName( templateName, existingContainerNames );
                    hostCloneNames.add( newContainerName );
                }
                ResourceHost resourceHost = getResourceHostByName( e.getKey().getHostname() );
                cloneNames.put( resourceHost, hostCloneNames );
            }

            List<CloneTask> cloneTasks = new ArrayList<>();
            Map<ResourceHost, List<CloneParam>> orders = new HashMap<>();
            String taskGroupId = UUID.randomUUID().toString();
            for ( final Map.Entry<ResourceHost, Set<String>> e : cloneNames.entrySet() )
            {
                ResourceHost rh = e.getKey();
                Set<String> clones = e.getValue();
                ResourceHost resourceHost = getResourceHostByName( rh.getHostname() );
                List<CloneParam> cloneParams = new ArrayList<>();
                for ( String cloneName : clones )
                {
                    CloneParam cloneParam = new CloneParam( cloneName, templates );
                    cloneParams.add( cloneParam );
                    CloneTask cloneTask = new CloneTask( taskGroupId, resourceHost, cloneParam );
                    cloneTask.start();
                    cloneTasks.add( cloneTask );
                }
                orders.put( rh, cloneParams );
            }
            tasks.addAll( cloneTasks );
            Set<ContainerHost> result = waitCloneTasks( cloneTasks );
            for ( ContainerHost containerHost : result )
            {
                containerHost.setCreatorPeerId( creatorPeerId.toString() );
                containerHost.setNodeGroupName( nodeGroupName );
                containerHost.setEnvironmentId( environmentId.toString() );
                containerHost.setTemplateName( templateName );
            }
            return result;
        }
        catch ( Exception e )
        {
            LOG.error( "Clone fail.", e );
            //TODO: destroy environment containers
            throw new PeerException( e.toString() );
        }
    }


    private Set<ContainerHost> waitCloneTasks( final List<CloneTask> cloneTasks ) throws Exception
    {
        int quantity = cloneTasks.size();
        long threshold = System.currentTimeMillis() + 180 * quantity * 1000;
        DateFormat formatter = new SimpleDateFormat( "HH:mm:ss" );
        formatter.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        int i = 0;
        while ( i < quantity && threshold - System.currentTimeMillis() > 0 )
        {
            i = 0;
            for ( CloneTask cloneTask : cloneTasks )
            {
                LOG.info(
                        String.format( "Clone task %s  %s:%s", cloneTask.getPhase(), cloneTask.getHost().getHostname(),
                                cloneTask.getParameter().getHostname() ) );
                if ( cloneTask.getPhase() == HostTask.Phase.DONE )
                {
                    if ( cloneTask.getResult().isOk() )
                    {
                        i++;
                    }
                    else
                    {
                        LOG.error( "Container clone error.", cloneTask.getException() );
                        throw cloneTask.getException();
                    }
                }
            }

            try
            {
                Thread.sleep( 5000 );
            }
            catch ( InterruptedException ignore )
            {
            }
            LOG.info( String.format( "Waiting clone tasks. Timeout: %s ",
                    formatter.format( new Date( threshold - System.currentTimeMillis() ) ) ) );
        }
        Set<ContainerHost> result = new HashSet<>();
        for ( CloneTask cloneTask : cloneTasks )
        {
            result.add( cloneTask.getResult().getValue() );
        }
        return result;
    }


    @Override
    public ContainerHost getContainerHost( final HostInfo hostInfo, final String creatorPeerId,
                                           final String environmentId, final String nodeGroupName )
    {
        Host host = null;

        if ( getId().equals( creatorPeerId ) )
        {
            try
            {
                host = bindHost( hostInfo.getId() );
            }
            catch ( HostNotFoundException ignore )
            {

            }
        }
        if ( host == null )
        {
            host = new ContainerHostEntity( getId().toString(), creatorPeerId, environmentId.toString(), nodeGroupName,
                    hostInfo );
        }
        return ( ContainerHost ) host;
    }


    @Override
    public void onHostEvent( final HostEvent hostEvent )
    {
        LOG.info( String.format( "HostEvent received: %s %s", hostEvent.getType(), hostEvent.getObject() ) );
    }


    private String nextHostName( String templateName, Set<String> existingNames )
    {
        AtomicInteger i = sequences.putIfAbsent( templateName, new AtomicInteger() );
        if ( i == null )
        {
            i = sequences.get( templateName );
        }
        while ( true )
        {
            String suffix = String.valueOf( i.incrementAndGet() );
            int prefixLen = MAX_LXC_NAME - suffix.length();
            String name = ( templateName.length() > prefixLen ? templateName.substring( 0, prefixLen ) : templateName )
                    + suffix;
            if ( !existingNames.contains( name ) )
            {
                return name;
            }
        }
    }


    private Set<String> getContainerNames() throws PeerException
    {
        Set<String> result = new HashSet<>();
        for ( ResourceHost resourceHost : getResourceHosts() )
        {
            for ( ContainerHost containerHost : resourceHost.getContainerHosts() )
            {
                result.add( containerHost.getHostname() );
            }
        }
        return result;
    }


    private void tryToRegister( final Template template ) throws RegistryException
    {
        if ( templateRegistry.getTemplate( template.getTemplateName() ) == null )
        {
            templateRegistry.registerTemplate( template );
        }
    }


    @Override
    public ContainerHost getContainerHostByName( String hostname ) throws HostNotFoundException
    {
        ContainerHost result = null;
        Iterator<ResourceHost> iterator = getResourceHosts().iterator();
        while ( result == null && iterator.hasNext() )
        {
            result = iterator.next().getContainerHostByName( hostname );
        }
        if ( result == null )
        {
            throw new HostNotFoundException( String.format( "Container host %s not found.", hostname ) );
        }
        return result;
    }


    @Override
    public ContainerHost getContainerHostById( final String hostId ) throws HostNotFoundException
    {
        ContainerHost result = null;
        Iterator<ResourceHost> iterator = getResourceHosts().iterator();
        while ( result == null && iterator.hasNext() )
        {
            result = iterator.next().getContainerHostById( hostId );
        }
        if ( result == null )
        {
            throw new HostNotFoundException( String.format( "Container host by id %s not found.", hostId ) );
        }
        return result;
    }


    @Override
    public ResourceHost getResourceHostByName( String hostname ) throws HostNotFoundException
    {
        ResourceHost result = null;
        Iterator iterator = getResourceHosts().iterator();

        while ( result == null && iterator.hasNext() )
        {
            ResourceHost host = ( ResourceHost ) iterator.next();

            if ( host.getHostname().equals( hostname ) )
            {
                result = host;
            }
        }
        if ( result == null )
        {
            throw new HostNotFoundException( String.format( "Resource host %s not found.", hostname ) );
        }
        return result;
    }


    @Override
    public ResourceHost getResourceHostByContainerName( final String containerName ) throws HostNotFoundException
    {
        ContainerHost c = getContainerHostByName( containerName );
        ContainerHostEntity containerHostEntity = ( ContainerHostEntity ) c;
        return containerHostEntity.getParent();
    }


    @Override
    public Set<ContainerHost> getContainerHostsByEnvironmentId( final UUID environmentId )
    {
        Set<ContainerHost> result = new HashSet<>();
        for ( ResourceHost resourceHost : getResourceHosts() )
        {
            result.addAll( resourceHost.getContainerHostsByEnvironmentId( environmentId ) );
        }
        return result;
    }


    @Override
    public Host bindHost( String id ) throws HostNotFoundException
    {
        Host result = null;
        Iterator<ResourceHost> iterator = getResourceHosts().iterator();
        while ( result == null && iterator.hasNext() )
        {
            ResourceHost rh = iterator.next();
            if ( rh.getHostId().equals( id ) )
            {
                result = rh;
            }
            else
            {
                result = rh.getContainerHostById( id );
            }
        }

        if ( result == null )
        {
            if ( !getManagementHost().getHostId().equals( id ) )
            {
                throw new HostNotFoundException( String.format( "Host by id %s is not registered.", id ) );
            }
            else
            {
                result = getManagementHost();
            }
        }


        return result;
    }


    @Override
    public Host bindHost( UUID id ) throws HostNotFoundException
    {
        return bindHost( id.toString() );
    }


    @Override
    public <T extends Host> T bindHost( T host ) throws HostNotFoundException
    {
        Host result = null;
        Iterator<ResourceHost> iterator = getResourceHosts().iterator();
        while ( result == null && iterator.hasNext() )
        {
            ResourceHost rh = iterator.next();
            if ( rh.getHostId().equals( host.getHostId() ) )
            {
                result = rh;
            }
            else
            {
                result = rh.getContainerHostById( host.getHostId() );
            }
        }

        if ( result == null )
        {
            if ( !getManagementHost().getHostId().equals( host.getHostId() ) )
            {
                throw new HostNotFoundException(
                        String.format( "Host by id %s is not registered.", host.getHostId() ) );
            }
            else
            {
                result = getManagementHost();
            }
        }
        return ( T ) result;
    }


    @Override
    public void startContainer( final ContainerHost host ) throws PeerException
    {
        Host c = bindHost( host );
        ContainerHostEntity containerHost = ( ContainerHostEntity ) c;
        ResourceHost resourceHost = containerHost.getParent();
        try
        {
            if ( resourceHost.startContainerHost( containerHost ) )
            {
                //                containerHost.setState( ContainerHostState.RUNNING );
            }
        }
        catch ( ResourceHostException e )
        {
            //            containerHost.setState( ContainerState.UNKNOWN );
            throw new PeerException( String.format( "Could not start LXC container [%s]", e.toString() ) );
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Could not stop LXC container [%s]", e.toString() ) );
        }
    }


    @Override
    public void stopContainer( final ContainerHost host ) throws PeerException
    {
        Host c = bindHost( host.getHostId() );
        ContainerHostEntity containerHost = ( ContainerHostEntity ) c;
        ResourceHost resourceHost = containerHost.getParent();
        try
        {
            if ( resourceHost.stopContainerHost( containerHost ) )
            {
                //                containerHost.setState( ContainerState.STOPPED );
            }
        }
        catch ( ResourceHostException e )
        {
            //            containerHost.setState( ContainerState.UNKNOWN );
            throw new PeerException( String.format( "Could not stop LXC container [%s]", e.toString() ) );
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Could not stop LXC container [%s]", e.toString() ) );
        }
    }


    @Override
    public void destroyContainer( final ContainerHost containerHost ) throws PeerException
    {
        try
        {
            ContainerHost result = bindHost( containerHost );

            ContainerHostEntity entity = ( ContainerHostEntity ) result;
            ResourceHost resourceHost =
                    entity.getParent(); //getResourceHostByName( containerHost.getAgent().getParentHostName() );
            resourceHost.destroyContainerHost( containerHost );
            resourceHost.removeContainerHost( result );
            entity.setParent( null );
            resourceHostDataService.update( ( ResourceHostEntity ) resourceHost );
            //            peerDAO.saveInfo( SOURCE_RESOURCE_HOST, resourceHost.getId().toString(), resourceHost );
        }
        catch ( ResourceHostException e )
        {
            throw new PeerException( e.toString() );
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Could not stop LXC container [%s]", e.toString() ) );
        }
    }


    @Override
    public boolean isConnected( final Host host ) throws PeerException
    {
        try
        {
            Host h = bindHost( host.getId() );

            if ( h instanceof ContainerHost )
            {
                if ( ContainerHostState.RUNNING.equals( ( ( ContainerHost ) h ).getState() ) )
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }

            if ( isTimedOut( h.getLastHeartbeat(), HOST_INACTIVE_TIME ) )
            {
                return false;
            }
            else
            {
                return true;
            }
        }
        catch ( HostNotFoundException e )
        {
            return false;
        }
    }


    private boolean isTimedOut( long lastHeartbeat, long timeoutInMillis )
    {
        return ( System.currentTimeMillis() - lastHeartbeat ) > timeoutInMillis;
    }


    @Override
    public String getQuota( ContainerHost host, final QuotaEnum quota ) throws PeerException
    {
        try
        {
            Host c = bindHost( host.getHostId() );
            return quotaManager.getQuota( c.getHostname(), quota );
        }
        catch ( QuotaException e )
        {
            throw new PeerException( e.toString() );
        }
    }


    @Override
    public void setQuota( ContainerHost host, final QuotaEnum quota, final String value ) throws PeerException
    {
        try
        {
            Host c = bindHost( host.getHostId() );
            quotaManager.setQuota( c.getHostname(), quota, value );
        }
        catch ( QuotaException e )
        {
            throw new PeerException( e.toString() );
        }
    }


    @Override
    public ManagementHost getManagementHost() throws HostNotFoundException
    {
        if ( managementHost == null )
        {
            throw new HostNotFoundException( "Management host not found." );
        }
        return managementHost;
    }


    @Override
    public Set<ResourceHost> getResourceHosts()
    {
        return resourceHosts;
    }


    @Override
    public List<String> getTemplates()
    {
        List<Template> templates = templateRegistry.getAllTemplates();

        List<String> result = new ArrayList<>();
        for ( Template template : templates )
        {
            result.add( template.getTemplateName() );
        }
        return result;
    }


    public void addResourceHost( final ResourceHost host )
    {
        if ( host == null )
        {
            throw new IllegalArgumentException( "Resource host could not be null." );
        }
        resourceHosts.add( host );
    }


    @Override
    public CommandResult execute( final RequestBuilder requestBuilder, final Host host ) throws CommandException
    {
        return execute( requestBuilder, host, null );
    }


    @Override
    public CommandResult execute( final RequestBuilder requestBuilder, final Host aHost,
                                  final CommandCallback callback ) throws CommandException
    {
        Preconditions.checkNotNull( requestBuilder );
        Preconditions.checkNotNull( hashCode() );

        Host host;
        try
        {
            host = bindHost( aHost.getId() );
        }
        catch ( PeerException e )
        {
            throw new CommandException( "Host is not registered" );
        }
        if ( !host.isConnected() )
        {
            throw new CommandException( "Host is not connected" );
        }


        CommandResult result;

        if ( callback == null )
        {
            result = commandExecutor.execute( host.getId(), requestBuilder );
        }
        else
        {
            result = commandExecutor.execute( host.getId(), requestBuilder, callback );
        }

        return result;
    }


    @Override
    public void executeAsync( final RequestBuilder requestBuilder, final Host aHost, final CommandCallback callback )
            throws CommandException
    {
        Host host;
        try
        {
            host = bindHost( aHost.getId() );
        }
        catch ( PeerException e )
        {
            throw new CommandException( "Host not register." );
        }
        if ( !host.isConnected() )
        {
            throw new CommandException( "Host disconnected." );
        }


        if ( callback == null )
        {
            commandExecutor.executeAsync( host.getId(), requestBuilder );
        }
        else
        {
            commandExecutor.executeAsync( host.getId(), requestBuilder, callback );
        }
    }


    @Override
    public void executeAsync( final RequestBuilder requestBuilder, final Host host ) throws CommandException
    {
        executeAsync( requestBuilder, host, null );
    }


    @Override
    public boolean isLocal()
    {
        return true;
    }


    @Override
    public void clean()
    {
        if ( managementHost != null && managementHost.getId() != null )
        {
            peerDAO.deleteInfo( SOURCE_MANAGEMENT_HOST, managementHost.getId().toString() );
            managementHost = null;
        }

        for ( ResourceHost resourceHost : getResourceHosts() )
        {
            peerDAO.deleteInfo( SOURCE_RESOURCE_HOST, resourceHost.getId().toString() );
        }
        resourceHosts.clear();
    }


    @Override
    public Template getTemplate( final String templateName )
    {
        return templateRegistry.getTemplate( templateName );
    }


    @Override
    public boolean isOnline() throws PeerException
    {
        return true;
    }


    @Override
    public <T, V> V sendRequest( final T request, final String recipient, final int timeout,
                                 final Class<V> responseType ) throws PeerException
    {
        Preconditions.checkNotNull( responseType, "Invalid response type" );

        return sendRequestInternal( request, recipient, timeout, responseType );
    }


    @Override
    public <T> void sendRequest( final T request, final String recipient, final int timeout ) throws PeerException
    {
        sendRequestInternal( request, recipient, timeout, null );
    }


    private <T, V> V sendRequestInternal( final T request, final String recipient, final int timeout,
                                          final Class<V> responseType ) throws PeerException
    {
        Preconditions.checkNotNull( request, "Invalid request" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( recipient ), "Invalid recipient" );
        Preconditions.checkArgument( timeout > 0, "Timeout must be greater than 0" );


        for ( RequestListener requestListener : requestListeners )
        {
            if ( recipient.equalsIgnoreCase( requestListener.getRecipient() ) )
            {
                try
                {
                    Object response = requestListener.onRequest( new Payload( request, getId() ) );

                    if ( response != null && responseType != null )
                    {
                        return responseType.cast( response );
                    }
                }
                catch ( Exception e )
                {
                    throw new PeerException( e );
                }
            }
        }

        return null;
    }


    @Override
    public void onHeartbeat( final ResourceHostInfo resourceHostInfo )
    {
        LOG.info( String.format( "Received heartbeat: %s", resourceHostInfo ) );
        if ( resourceHostInfo.getHostname().equals( "management" ) )
        {
            if ( managementHost == null )
            {
                managementHost = new ManagementHostEntity( getId().toString(), resourceHostInfo );
                try
                {
                    managementHost.init();
                }
                catch ( Exception e )
                {
                    LOG.error( e.toString() );
                }
                managementHostDataService.persist( ( ManagementHostEntity ) managementHost );
                managementHost.addListener( this );
            }
            managementHost.updateHostInfo( resourceHostInfo );
            //            peerDAO.saveInfo( SOURCE_MANAGEMENT_HOST, managementHost.getId().toString(), managementHost );
            return;
        }
        else
        {
            ResourceHost host;
            try
            {
                host = getResourceHostByName( resourceHostInfo.getHostname() );
                if ( !resourceHostInfo.getContainers().isEmpty() )
                {
                    boolean newContainer = false;
                    for ( ContainerHostInfo containerHostInfo : resourceHostInfo.getContainers() )
                    {
                        if ( containerHostInfo.getInterfaces().size() == 0 )
                        {
                            continue;
                        }
                        Host containerHost;
                        try
                        {
                            containerHost = bindHost( containerHostInfo.getId() );
                        }
                        catch ( HostNotFoundException hnfe )
                        {
                            containerHost = new ContainerHostEntity( getId().toString(), containerHostInfo );
                            host.addContainerHost( ( ContainerHostEntity ) containerHost );
                            newContainer = true;
                        }
                        containerHost.updateHostInfo( containerHostInfo );
                    }
                    if ( newContainer )
                    {
                        resourceHostDataService.update( ( ResourceHostEntity ) host );
                    }
                }
            }
            catch ( HostNotFoundException e )
            {
                host = new ResourceHostEntity( getId().toString(), resourceHostInfo );
                host.init();
                resourceHostDataService.persist( ( ResourceHostEntity ) host );
                addResourceHost( host );
                host.addListener( this );
            }
            host.updateHostInfo( resourceHostInfo );
            //            peerDAO.saveInfo( SOURCE_RESOURCE_HOST, host.getId().toString(), host );
            return;
        }
    }
}

