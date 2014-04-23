package org.safehaus.kiskis.mgmt.impl.hadoop;

import org.safehaus.kiskis.mgmt.api.hadoop.Config;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.CommandFactory;
import org.safehaus.kiskis.mgmt.shared.protocol.Request;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.OutputRedirection;
import org.safehaus.kiskis.mgmt.shared.protocol.enums.RequestType;

import java.util.Arrays;

/**
 * Created by daralbaev on 02.04.14.
 */
public class Commands {
    public static Request getRequestTemplate() {
        return CommandFactory.newRequest(
                RequestType.EXECUTE_REQUEST, // type
                null, //                        !! agent uuid
                HadoopImpl.MODULE_NAME, //     source
                null, //                        !! task uuid
                1, //                           !! request sequence number
                "/", //                         cwd
                null, //                        program
                OutputRedirection.RETURN, //    std output redirection
                OutputRedirection.RETURN, //    std error redirection
                null, //                        stdout capture file path
                null, //                        stderr capture file path
                "root", //                      runas
                null, //                        arg
                null, //                        env vars
                30); //
    }

    public static Request getInstallCommand() {
        Request req = getRequestTemplate();
        req.setProgram(
                "sleep 10;" +
                        "apt-get --force-yes --assume-yes install ksks-hadoop"
        );
        req.setTimeout(180);
        return req;
    }

    public static Request getClearMastersCommand() {
        Request req = getRequestTemplate();
        req.setProgram(
                ". /etc/profile && " +
                        "hadoop-master-slave.sh masters clear"
        );
        return req;
    }

    public static Request getClearSlavesCommand() {
        Request req = getRequestTemplate();
        req.setProgram(
                ". /etc/profile && " +
                        "hadoop-master-slave.sh slaves clear"
        );
        return req;
    }

    public static Request getSetMastersCommand(Agent nameNode, Agent jobTracker, Integer replicationFactor) {
        Request req = getRequestTemplate();

        req.setProgram(
                ". /etc/profile && " +
                        "hadoop-configure.sh"
        );
        req.setArgs(Arrays.asList(
                String.format("%s:%d", nameNode.getHostname(), Config.NAME_NODE_PORT),
                String.format("%s:%d", jobTracker.getHostname(), Config.JOB_TRACKER_PORT),
                String.format("%d", replicationFactor)
        ));
        System.out.println(req.toString());
        return req;
    }

    public static Request getAddSecondaryNamenodeCommand(Agent secondaryNameNode) {
        Request req = getRequestTemplate();
        req.setProgram(String.format(
                ". /etc/profile && " +
                        "hadoop-master-slave.sh masters %s",
                secondaryNameNode.getHostname()
        ));
        return req;
    }

    public static Request getAddSlaveCommand(Agent agent) {
        Request req = getRequestTemplate();
        req.setProgram(String.format(
                ". /etc/profile && " +
                        "hadoop-master-slave.sh slaves %s", agent.getHostname()
        ));
        return req;
    }

    public static Request getRemoveSlaveCommand(Agent agent) {
        Request req = getRequestTemplate();
        req.setProgram(String.format(
                ". /etc/profile && " +
                        "hadoop-master-slave.sh slaves clear %s", agent.getHostname()
        ));
        return req;
    }

    public static Request getExcludeDataNodeCommand(Agent agent) {
        Request req = getRequestTemplate();
        req.setProgram(String.format(
                        ". /etc/profile && " +
                                "hadoop-master-slave.sh dfs.exclude clear %s", agent.getHostname()
                )
        );
        return req;
    }

    public static Request getExcludeTaskTrackerCommand(Agent agent) {
        Request req = getRequestTemplate();
        req.setProgram(String.format(
                        ". /etc/profile && " +
                                "hadoop-master-slave.sh mapred.exclude clear %s", agent.getHostname()
                )
        );
        return req;
    }

    public static Request getIncludeDataNodeCommand(Agent agent) {
        Request req = getRequestTemplate();
        req.setProgram(String.format(
                        ". /etc/profile && " +
                                "hadoop-master-slave.sh dfs.exclude %s", agent.getHostname()
                )
        );
        return req;
    }

    public static Request getIncludeTaskTrackerCommand(Agent agent) {
        Request req = getRequestTemplate();
        req.setProgram(String.format(
                        ". /etc/profile && " +
                                "hadoop-master-slave.sh mapred.exclude %s", agent.getHostname()
                )
        );
        return req;
    }

    public static Request getFormatNameNodeCommand() {
        Request req = getRequestTemplate();
        req.setProgram(
                ". /etc/profile && " +
                        "hadoop namenode -format"
        );
        return req;
    }

    public static Request getRefreshNameNodeCommand() {
        Request req = getRequestTemplate();
        req.setProgram(
                ". /etc/profile && " +
                        "hadoop dfsadmin -refreshNodes"
        );
        return req;
    }

    public static Request getRefreshJobTrackerCommand() {
        Request req = getRequestTemplate();
        req.setProgram(
                ". /etc/profile && " +
                        "hadoop mradmin -refreshNodes"
        );
        return req;
    }

    public static Request getStartNameNodeCommand() {
        Request req = getRequestTemplate();
        req.setProgram(". /etc/profile && " +
                        "hadoop-daemons.sh start datanode"
        );
        req.setTimeout(20);
        return req;
    }

    public static Request getStartTaskTrackerCommand() {
        Request req = getRequestTemplate();
        req.setProgram(". /etc/profile && " +
                        "hadoop-daemons.sh start tasktracker"
        );
        req.setTimeout(20);
        return req;
    }

    public static Request getNameNodeCommand(String command) {
        Request req = getRequestTemplate();
        req.setProgram(
                String.format("service hadoop-dfs %s", command)
        );
        req.setTimeout(20);
        return req;
    }

    public static Request getJobTrackerCommand(String command) {
        Request req = getRequestTemplate();
        req.setProgram(
                String.format("service hadoop-mapred %s", command)
        );
        req.setTimeout(20);
        return req;
    }
}
