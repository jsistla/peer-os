/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.kiskis.mgmt.ui.accumulo.wizard;

import com.google.common.base.Strings;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.*;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.Util;
import org.safehaus.kiskis.mgmt.ui.accumulo.AccumuloUI;
import org.safehaus.kiskis.mgmt.ui.accumulo.common.UiUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author dilshat
 */
public class ConfigurationStep extends Panel {
    final Property.ValueChangeListener masterNodeComboChangeListener;
    final Property.ValueChangeListener gcNodeComboChangeListener;

    public ConfigurationStep(final Wizard wizard) {

        setSizeFull();

        GridLayout content = new GridLayout(5, 4);
        content.setSizeFull();
        content.setSpacing(true);
        content.setMargin(true);

        //hadoop combo
        final ComboBox hadoopClustersCombo = UiUtil.getCombo("Hadoop cluster");
        //zookeeper combo
        final ComboBox zkClustersCombo = UiUtil.getCombo("Zookeeper cluster");
        //master nodes
        final ComboBox masterNodeCombo = UiUtil.getCombo("Master node");
        final ComboBox gcNodeCombo = UiUtil.getCombo("GC node");
        final ComboBox monitorNodeCombo = UiUtil.getCombo("Monitor node");
        final TwinColSelect tracersSelect = UiUtil.getTwinSelect("Tracers", "hostname", "Available Nodes", "Selected Nodes", 4);
        //slave nodes
        final TwinColSelect slavesSelect = UiUtil.getTwinSelect("Slaves", "hostname", "Available Nodes", "Selected Nodes", 4);

        //get hadoop clusters from db
        List<org.safehaus.kiskis.mgmt.api.hadoop.Config> clusters = AccumuloUI.getHadoopManager().getClusters();
        List<org.safehaus.kiskis.mgmt.api.zookeeper.Config> zkClusters = AccumuloUI.getZookeeperManager().getClusters();

        //fill hadoopClustersCombo with hadoop cluster infos
        if (clusters.size() > 0) {
            for (org.safehaus.kiskis.mgmt.api.hadoop.Config hadoopClusterInfo : clusters) {
                hadoopClustersCombo.addItem(hadoopClusterInfo);
                hadoopClustersCombo.setItemCaption(hadoopClusterInfo,
                        hadoopClusterInfo.getClusterName());
            }
        }

        //fill zkClustersCombo with hadoop cluster infos
        if (zkClusters.size() > 0) {
            for (org.safehaus.kiskis.mgmt.api.zookeeper.Config zkInfo : zkClusters) {
                zkClustersCombo.addItem(zkInfo);
                zkClustersCombo.setItemCaption(zkInfo, zkInfo.getClusterName());
            }
        }

        //try to find hadoop cluster info based on one saved in the configuration
        org.safehaus.kiskis.mgmt.api.hadoop.Config info = AccumuloUI.getHadoopManager().getCluster(wizard.getConfig().getClusterName());

        //select if saved found
        if (info != null) {
            hadoopClustersCombo.setValue(info);
            hadoopClustersCombo.setItemCaption(info,
                    info.getClusterName());
        } else if (clusters.size() > 0) {
            //select first one if saved not found
            hadoopClustersCombo.setValue(clusters.iterator().next());
        }
        //try to find zk cluster info based on one saved in the configuration
        org.safehaus.kiskis.mgmt.api.zookeeper.Config zkInfo = AccumuloUI.getZookeeperManager().getCluster(wizard.getConfig().getZkClusterName());
        //select if saved found
        if (zkInfo != null) {
            zkClustersCombo.setValue(zkInfo);
            zkClustersCombo.setItemCaption(zkInfo,
                    zkInfo.getClusterName());
        } else if (zkClusters.size() > 0) {
            //select first one if saved not found
            zkClustersCombo.setValue(zkClusters.iterator().next());
        }


        //fill selection controls with hadoop nodes
        if (hadoopClustersCombo.getValue() != null) {
            org.safehaus.kiskis.mgmt.api.hadoop.Config hadoopInfo = (org.safehaus.kiskis.mgmt.api.hadoop.Config) hadoopClustersCombo.getValue();

            wizard.getConfig().setClusterName(hadoopInfo.getClusterName());

            setComboDS(masterNodeCombo, hadoopInfo.getAllNodes());
            setComboDS(gcNodeCombo, hadoopInfo.getAllNodes());
            setComboDS(monitorNodeCombo, hadoopInfo.getAllNodes());
            setTwinSelectDS(tracersSelect, hadoopInfo.getAllNodes());
            setTwinSelectDS(slavesSelect, hadoopInfo.getAllNodes());
        }

        if (zkClustersCombo.getValue() != null) {
            org.safehaus.kiskis.mgmt.api.zookeeper.Config zookeeperInfo = (org.safehaus.kiskis.mgmt.api.zookeeper.Config) zkClustersCombo.getValue();
            wizard.getConfig().setZkClusterName(zookeeperInfo.getClusterName());
        }

        //on hadoop cluster change reset all controls and config
        hadoopClustersCombo.addListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    org.safehaus.kiskis.mgmt.api.hadoop.Config hadoopInfo = (org.safehaus.kiskis.mgmt.api.hadoop.Config) event.getProperty().getValue();
                    setComboDS(masterNodeCombo, hadoopInfo.getAllNodes());
                    setComboDS(gcNodeCombo, hadoopInfo.getAllNodes());
                    setComboDS(monitorNodeCombo, hadoopInfo.getAllNodes());
                    setTwinSelectDS(tracersSelect, hadoopInfo.getAllNodes());
                    setTwinSelectDS(slavesSelect, hadoopInfo.getAllNodes());
                    String zkClusterName = wizard.getConfig().getZkClusterName();
                    wizard.getConfig().reset();
                    wizard.getConfig().setClusterName(hadoopInfo.getClusterName());
                    wizard.getConfig().setZkClusterName(zkClusterName);
                }
            }
        });

        zkClustersCombo.addListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    org.safehaus.kiskis.mgmt.api.zookeeper.Config zookeeperInfo = (org.safehaus.kiskis.mgmt.api.zookeeper.Config) event.getProperty().getValue();
                    wizard.getConfig().setZkClusterName(zookeeperInfo.getClusterName());
                }
            }
        });

        //restore master node if back button is pressed
        if (wizard.getConfig().getMasterNode() != null) {
            masterNodeCombo.setValue(wizard.getConfig().getMasterNode());
        }
        //restore gc node if back button is pressed
        if (wizard.getConfig().getGcNode() != null) {
            gcNodeCombo.setValue(wizard.getConfig().getGcNode());
        }
        //restore monitor node if back button is pressed
        if (wizard.getConfig().getMonitor() != null) {
            monitorNodeCombo.setValue(wizard.getConfig().getMonitor());
        }

        //add value change handler
        masterNodeComboChangeListener = new Property.ValueChangeListener() {

            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    Agent masterNode = (Agent) event.getProperty().getValue();
                    wizard.getConfig().setMasterNode(masterNode);
                    org.safehaus.kiskis.mgmt.api.hadoop.Config hadoopInfo = (org.safehaus.kiskis.mgmt.api.hadoop.Config) hadoopClustersCombo.getValue();
                    List<Agent> hadoopNodes = hadoopInfo.getAllNodes();
                    hadoopNodes.remove(masterNode);
                    gcNodeCombo.removeListener(gcNodeComboChangeListener);
                    setComboDS(gcNodeCombo, hadoopNodes);
                    if (!masterNode.equals(wizard.getConfig().getGcNode())) {
                        gcNodeCombo.setValue(wizard.getConfig().getGcNode());
                    } else {
                        wizard.getConfig().setGcNode(null);
                    }
                    gcNodeCombo.addListener(gcNodeComboChangeListener);
                }
            }
        };
        masterNodeCombo.addListener(masterNodeComboChangeListener);
        //add value change handler
        gcNodeComboChangeListener = new Property.ValueChangeListener() {

            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    Agent gcNode = (Agent) event.getProperty().getValue();
                    wizard.getConfig().setGcNode(gcNode);
                    org.safehaus.kiskis.mgmt.api.hadoop.Config hadoopInfo = (org.safehaus.kiskis.mgmt.api.hadoop.Config) hadoopClustersCombo.getValue();
                    List<Agent> hadoopNodes = hadoopInfo.getAllNodes();
                    hadoopNodes.remove(gcNode);
                    masterNodeCombo.removeListener(masterNodeComboChangeListener);
                    setComboDS(masterNodeCombo, hadoopNodes);
                    if (!gcNode.equals(wizard.getConfig().getMasterNode())) {
                        masterNodeCombo.setValue(wizard.getConfig().getMasterNode());
                    } else {
                        wizard.getConfig().setMasterNode(null);
                    }
                    masterNodeCombo.addListener(masterNodeComboChangeListener);
                }
            }
        };
        gcNodeCombo.addListener(gcNodeComboChangeListener);
        //add value change handler
        monitorNodeCombo.addListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    Agent monitor = (Agent) event.getProperty().getValue();
                    wizard.getConfig().setMonitor(monitor);
                }
            }
        });

        //restore tracers if back button is pressed
        if (!Util.isCollectionEmpty(wizard.getConfig().getTracers())) {
            tracersSelect.setValue(wizard.getConfig().getTracers());
        }
        //restore slaves if back button is pressed
        if (!Util.isCollectionEmpty(wizard.getConfig().getSlaves())) {
            slavesSelect.setValue(wizard.getConfig().getSlaves());
        }

        //add value change handler
        tracersSelect.addListener(new Property.ValueChangeListener() {

            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    Set<Agent> agentList = new HashSet((Collection) event.getProperty().getValue());
                    wizard.getConfig().setTracers(agentList);
                }
            }
        });
        //add value change handler
        slavesSelect.addListener(new Property.ValueChangeListener() {

            public void valueChange(Property.ValueChangeEvent event) {
                if (event.getProperty().getValue() != null) {
                    Set<Agent> agentList = new HashSet((Collection) event.getProperty().getValue());
                    wizard.getConfig().setSlaves(agentList);
                }
            }
        });

        Button next = new Button("Next");
        //check valid configuration
        next.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {

                if (Strings.isNullOrEmpty(wizard.getConfig().getClusterName())) {
                    show("Please, select Hadoop cluster");
                } else if (Strings.isNullOrEmpty(wizard.getConfig().getZkClusterName())) {
                    show("Please, select Zookeeper cluster");
                } else if (wizard.getConfig().getMasterNode() == null) {
                    show("Please, select master node");
                } else if (wizard.getConfig().getGcNode() == null) {
                    show("Please, select gc node");
                } else if (wizard.getConfig().getMonitor() == null) {
                    show("Please, select monitor");
                } else if (Util.isCollectionEmpty(wizard.getConfig().getTracers())) {
                    show("Please, select tracer(s)");
                } else if (Util.isCollectionEmpty(wizard.getConfig().getSlaves())) {
                    show("Please, select slave(s)");
                } else {
                    wizard.next();
                }
            }
        });

        Button back = new Button("Back");
        back.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                wizard.back();
            }
        });

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.addComponent(new Label("Please, specify installation settings"));
        layout.addComponent(content);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.addComponent(back);
        buttons.addComponent(next);

        content.addComponent(hadoopClustersCombo, 0, 0);
        content.addComponent(zkClustersCombo, 1, 0);
        content.addComponent(masterNodeCombo, 2, 0);
        content.addComponent(gcNodeCombo, 3, 0);
        content.addComponent(monitorNodeCombo, 4, 0);
        content.addComponent(tracersSelect, 0, 1, 4, 1);
        content.addComponent(slavesSelect, 0, 2, 4, 2);
        content.addComponent(buttons, 0, 3, 4, 3);

        addComponent(layout);

    }

    private void setComboDS(ComboBox target, List<Agent> hadoopNodes) {
        target.removeAllItems();
        target.setValue(null);
        for (Agent agent : hadoopNodes) {
            target.addItem(agent);
            target.setItemCaption(agent, agent.getHostname());
        }
    }

    private void setTwinSelectDS(TwinColSelect target, List<Agent> hadoopNodes) {
        target.setValue(null);
        target.setContainerDataSource(
                new BeanItemContainer<Agent>(
                        Agent.class, hadoopNodes)
        );

    }

    private void show(String notification) {
        getWindow().showNotification(notification);
    }

}
