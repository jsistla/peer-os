/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.ui.cassandra.wizard;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import org.safehaus.subutai.api.cassandra.Config;
import org.safehaus.subutai.server.ui.component.ProgressWindow;
import org.safehaus.subutai.ui.cassandra.CassandraUI;

import java.util.UUID;

/**
 * @author dilshat
 */
public class VerificationStep extends VerticalLayout {

    public VerificationStep(final Wizard wizard) {

        setSizeFull();

        GridLayout grid = new GridLayout(1, 5);
        grid.setSpacing(true);
        grid.setMargin(true);
        grid.setSizeFull();

        Label confirmationLbl = new Label("<strong>Please verify the installation settings "
                + "(you may change them by clicking on Back button)</strong><br/>");
        confirmationLbl.setContentMode(ContentMode.HTML);

        ConfigView cfgView = new ConfigView("Installation configuration");
        cfgView.addStringCfg("Cluster Name", wizard.getConfig().getClusterName());
        cfgView.addStringCfg("Domain Name", wizard.getConfig().getDomainName());
        cfgView.addStringCfg("Data directory", wizard.getConfig().getDataDirectory());
        cfgView.addStringCfg("Saved caches directory", wizard.getConfig().getSavedCachesDirectory());
        cfgView.addStringCfg("Commit log directory", wizard.getConfig().getCommitLogDirectory());
        cfgView.addStringCfg("Number of seeds", wizard.getConfig().getNumberOfSeeds() + "");

        Button install = new Button("Install");
        install.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                UUID trackID = CassandraUI.getCassandraManager().installCluster(wizard.getConfig());
                ProgressWindow window = new ProgressWindow(CassandraUI.getExecutor(), CassandraUI.getTracker(), trackID, Config.PRODUCT_KEY);
                window.getWindow().addCloseListener(new Window.CloseListener() {
                    @Override
                    public void windowClose(Window.CloseEvent closeEvent) {
                        wizard.init();
                    }
                });
                getUI().addWindow(window.getWindow());
            }
        });

        Button back = new Button("Back");
        back.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                wizard.back();
            }
        });

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent(back);
        buttons.addComponent(install);

        grid.addComponent(confirmationLbl, 0, 0);

        grid.addComponent(cfgView.getCfgTable(), 0, 1, 0, 3);

        grid.addComponent(buttons, 0, 4);

        addComponent(grid);

    }

}
