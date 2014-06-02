package org.safehaus.subutai.api.gitmanager;


/**
 * Contains common git commands
 */
public enum GitCommand {
    INIT( "init" ),
    ADD( "add" ),
    DELETE( "rm" ),
    COMMIT( "commit" ),
    BRANCH( "branch" ),
    CHECKOUT( "checkout" ),
    CLONE( "clone" ),
    REVERT( "revert" ),
    FETCH( "fetch" ),
    MERGE( "merge" ),
    STASH( "stash" ),
    PULL( "pull" ),
    PUSH( "push" );

    private String command;


    private GitCommand( final String command ) {
        this.command = command;
    }


    /**
     * Returns corresponding to this enum git command
     */
    public String getCommand() {
        return command;
    }
}
