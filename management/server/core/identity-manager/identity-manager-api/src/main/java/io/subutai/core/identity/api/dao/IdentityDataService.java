package io.subutai.core.identity.api.dao;


import java.util.List;

import io.subutai.core.identity.api.model.Permission;
import io.subutai.core.identity.api.model.Role;
import io.subutai.core.identity.api.model.Session;
import io.subutai.core.identity.api.model.User;
import io.subutai.core.identity.api.model.UserToken;


/**
 *
 */
public interface IdentityDataService
{

    /* ******User *************************************
     *
     */
    User getUserByUsername( String userName );


    /* *************************************************
     *
     */
    User getUser( long userId );

    /* *************************************************
     *
     */
    void assignUserRole( long userId, Role role );


    /* *************************************************
     *
     */
    void removeUserRole( long userId, Role role );


    /* *************************************************
     *
     */
    List<User> getAllUsers();


    /* *************************************************
     *
     */
    List<User> getAllSystemUsers();


    /* *************************************************
     *
     */
    void persistUser( User item );


    /* *************************************************
     *
     */
    void removeUser( long id );


    /* *************************************************
     *
     */
    void updateUser( User item );


    /* ***********Roles ********************************
     */
    Role getRole( long roleId );

    /* *************************************************
     *
     */
    List<Role> getAllRoles();


    /* *************************************************
     *
     */
    void persistRole( Role item );


    /* *************************************************
     *
     */
    void removeRole( long id );


    /* *************************************************
     *
     */
    void updateRole( Role item );


    /* *************************************************
     *
     */
    void assignRolePermission( long roleId, Permission permission );


    /* *************************************************
     *
     */
    void removeAllRolePermissions( long roleId );


    /*
     * ******Permission*********************************
     */
    Permission getPermission( long permissionId );


    /* *************************************************
     *
     */
    List<Permission> getAllPermissions();


    /* *************************************************
     *
     */
    void persistPermission( Permission item );


    /* *************************************************
     *
     */
    void removePermission( long id );


    /* *************************************************
     *
     */
    void updatePermission( Permission item );


    /* *************************************************
     */
    void removeRolePermission( long roleId, Permission permission );

    /* ******Session************************
     *
     */
    List<Session> getAllSessions();


    /* *************************************************
     */
    Session getSession( long sessionId );



    /* *************************************************
     *
     */
    List<Session> getSessionsByUserId( long userId );

    /* *************************************************
     *
     */
    Session getValidSession( long userId );

    /* *************************************************
     *
     */
    void persistSession( Session item );


    /* *************************************************
     *
     */
    void removeSession( long id );


    /* *************************************************
     *
     */
    void updateSession( Session item );

    /* ******UserToken *********************************
    *
    */
    List<UserToken> getAllUserTokens();


    /* *************************************************
     *
     */
    UserToken getUserToken( String token );


    /* *************************************************
     *
     */
    UserToken getValidUserToken( String token );

    /* *************************************************
     *
     */
    UserToken getUserToken( long userId );

    /* *************************************************
     *
     */
    UserToken getValidUserToken( long userId );

    /* *************************************************
     *
     */
    void persistUserToken( UserToken item );


    /* *************************************************
     *
     */
    void updateUserToken( UserToken item );

    /* *************************************************
     *
     */
    void removeUserToken( String token );


    /* *************************************************
     *
     */
    void removeInvalidTokens();


    /* *************************************************
     *
     */
    void invalidateSessions();
}
