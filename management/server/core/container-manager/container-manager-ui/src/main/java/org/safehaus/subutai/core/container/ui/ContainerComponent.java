package org.safehaus.subutai.core.container.ui;


import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import org.safehaus.subutai.common.protocol.Disposable;
import org.safehaus.subutai.common.util.ServiceLocator;
import org.safehaus.subutai.core.agent.api.AgentManager;
import org.safehaus.subutai.core.container.api.ContainerManager;
import org.safehaus.subutai.core.container.ui.clone.Cloner;
import org.safehaus.subutai.core.container.ui.manage.Manager;
import org.safehaus.subutai.core.lxc.quota.api.QuotaManager;
import org.safehaus.subutai.core.strategy.api.StrategyManager;
import org.safehaus.subutai.server.ui.component.AgentTree;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Runo;


public class ContainerComponent extends CustomComponent implements Disposable
{

    private static final String MANAGER_TAB_CAPTION = "Manage";
    private final AgentTree agentTree;


    public ContainerComponent( ExecutorService executorService, ServiceLocator serviceLocator,
                               final QuotaManager quotaManager ) throws NamingException
    {

        final ContainerManager containerManager = serviceLocator.getService( ContainerManager.class );
        final AgentManager agentManager = serviceLocator.getService( AgentManager.class );
        final StrategyManager strategyManager = serviceLocator.getService( StrategyManager.class );
        setHeight( 100, Unit.PERCENTAGE );

        HorizontalSplitPanel horizontalSplit = new HorizontalSplitPanel();
        horizontalSplit.setStyleName( Runo.SPLITPANEL_SMALL );
        horizontalSplit.setSplitPosition( 200, Unit.PIXELS );
        agentTree = new AgentTree( agentManager );
        horizontalSplit.setFirstComponent( agentTree );

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSpacing( true );
        verticalLayout.setSizeFull();
        TabSheet commandsSheet = new TabSheet();
        commandsSheet.setStyleName( Runo.TABSHEET_SMALL );
        commandsSheet.setSizeFull();
        final Manager manager = new Manager( executorService, agentManager, containerManager, quotaManager );
        commandsSheet.addTab( new Cloner( containerManager, strategyManager, agentTree ), "Clone" );
        commandsSheet.addTab( manager, MANAGER_TAB_CAPTION );
        commandsSheet.addSelectedTabChangeListener( new TabSheet.SelectedTabChangeListener()
        {
            @Override
            public void selectedTabChange( TabSheet.SelectedTabChangeEvent event )
            {
                TabSheet tabsheet = event.getTabSheet();
                String caption = tabsheet.getTab( event.getTabSheet().getSelectedTab() ).getCaption();
                if ( caption.equals( MANAGER_TAB_CAPTION ) )
                {
                    manager.getLxcInfo();
                }
            }
        } );
        verticalLayout.addComponent( commandsSheet );

        horizontalSplit.setSecondComponent( verticalLayout );
        setCompositionRoot( horizontalSplit );
    }


    public void dispose()
    {
        agentTree.dispose();
    }
}
