package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.*;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.ChangeData;
import pt.unl.fct.di.apdc.projeto.util.PasswordData;
import pt.unl.fct.di.apdc.projeto.util.RoleData;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;
import pt.unl.fct.di.apdc.projeto.util.UsernameData;

@Path("/user")
public class UserResource {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	
	/** The converter to JSON */
	private final Gson g = new Gson();

    public UserResource() {

    }

    @POST
    @Path("/change/data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response alterData(ChangeData data) {
        AuthToken token = data.token;
        LOG.fine("Data change: " + token.username + " attempted to change user data.");
        if ( token.role.equals(UserConstants.USER) && !data.username.equals(token.username) ) {
            LOG.warning("Data change: " + token.username + " cannot change .");
            return Response.status(Status.UNAUTHORIZED).entity("User role cannot change other users data.").build();
        }
        int validData = data.validData();
		if ( validData != 0 ) {
			LOG.warning("Register: Register attempt using invalid " + data.getInvalidReason(validData) + ".");
			return Response.status(Status.BAD_REQUEST).entity("Invalid " + data.getInvalidReason(validData) + ".").build();
		}
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(data.username);
            Key adminKey = userKeyFactory.newKey(token.username);
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            if ( user == null ) {
                txn.rollback();
				LOG.warning("Data change: " + data.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            } else if ( admin == null ) {
                txn.rollback();
				LOG.warning("Data change: " + token.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            }
            String adminRole = admin.getString("role");
            int validation = token.isStillValid(admin.getString("tokenID"), adminRole);
            if ( validation == 1 ) {
                if ( adminRole.equals(UserConstants.USER) ) {
                    user = Entity.newBuilder(userKey)
                        .set("username", user.getString("username"))
                        .set("password", user.getString("password"))
                        .set("email", user.getString("email"))
                        .set("name", user.getString("name"))
                        .set("phone", data.phone == null || data.phone.trim().isEmpty() ? user.getString("phone") : data.phone)
                        .set("profile", data.profile == null || data.profile.trim().isEmpty() ? user.getString("profile") : data.profile)
                        .set("work", data.work == null || data.work.trim().isEmpty() ? user.getString("work") : data.work)
                        .set("workplace", data.workplace == null || data.workplace.trim().isEmpty() ? user.getString("workplace") : data.workplace)
                        .set("address", data.address == null || data.address.trim().isEmpty() ? user.getString("address") : data.address)
                        .set("postalcode", data.postalcode == null || data.postalcode.trim().isEmpty() ? user.getString("postalcode") : data.postalcode)
                        .set("fiscal", data.fiscal == null || data.fiscal.trim().isEmpty() ? user.getString("fiscal") : data.fiscal)
                        .set("role", user.getString("role"))
                        .set("state", user.getString("state"))
                        .set("userCreationTime", user.getTimestamp("userCreationTime"))
                        .set("tokenID", StringValue.newBuilder(user.getString("tokenID")).setExcludeFromIndexes(true).build())
						.set("photo", StringValue.newBuilder(data.photo == null || data.photo.trim().isEmpty() ? user.getString("photo") : data.photo).setExcludeFromIndexes(true).build())
						.build();
                    txn.update(user);
                    txn.commit();
                    LOG.fine("Data change: " + data.username + "'s data was updated in the database.");
                    return Response.ok().entity("User's data updated.").build();
                } else if ( adminRole.equals(UserConstants.GBO) ) {
                    if ( !user.getString("role").equals(UserConstants.USER) ) {
                        txn.rollback();
                        LOG.warning("Data change: " + token.username + " cannot change non USER users data.");
                        return Response.status(Status.UNAUTHORIZED).entity("GBO users cannot change data of non USER users.").build();
                    }
                } else if ( adminRole.equals(UserConstants.GA) ) {
                    if ( !user.getString("role").equals(UserConstants.USER) && !user.getString("role").equals(UserConstants.GBO) ) {
                        txn.rollback();
                        LOG.warning("Data change: " + token.username + " cannot change GA or SU users data.");
                        return Response.status(Status.UNAUTHORIZED).entity("GA users cannot change data of GA or SU users.").build();
                    }
                } else if ( adminRole.equals(UserConstants.SU) ) {
                    if ( !user.getString("role").equals(UserConstants.USER) && !user.getString("role").equals(UserConstants.GBO) && !user.getString("role").equals(UserConstants.GA) ) {
                        txn.rollback();
                        LOG.warning("Data change: " + token.username + " cannot change SU users data.");
                        return Response.status(Status.UNAUTHORIZED).entity("SU users cannot change data of SU users.").build();
                    }
                } else {
                    txn.rollback();
                    LOG.severe("Data change: Unrecognized role.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
                user = Entity.newBuilder(userKey)
                        .set("username", user.getString("username"))
                        .set("password", data.password == null || data.password.trim().isEmpty() ? user.getString("password") : DigestUtils.sha3_512Hex(data.password))
                        .set("email", data.email == null || data.email.trim().isEmpty() ? user.getString("email") : data.email)
                        .set("name", data.name == null || data.name.trim().isEmpty() ? user.getString("name") : data.name)
                        .set("phone", data.phone == null || data.phone.trim().isEmpty() ? user.getString("phone") : data.phone)
                        .set("profile", data.profile == null || data.profile.trim().isEmpty() ? user.getString("profile") : data.profile)
                        .set("work", data.work == null || data.work.trim().isEmpty() ? user.getString("work") : data.work)
                        .set("workplace", data.workplace == null || data.workplace.trim().isEmpty() ? user.getString("workplace") : data.workplace)
                        .set("address", data.address == null || data.address.trim().isEmpty() ? user.getString("address") : data.address)
                        .set("postalcode", data.postalcode == null || data.postalcode.trim().isEmpty() ? user.getString("postalcode") : data.postalcode)
                        .set("fiscal", data.fiscal == null || data.fiscal.trim().isEmpty() ? user.getString("fiscal") : data.fiscal)
                        .set("role", data.role == null || data.role.trim().isEmpty() ? user.getString("role") : data.role)
                        .set("state", data.state == null || data.state.trim().isEmpty() ? user.getString("state") : data.state)
                        .set("userCreationTime", user.getTimestamp("userCreationTime"))
                        .set("tokenID", StringValue.newBuilder(user.getString("tokenID")).setExcludeFromIndexes(true).build())
						.set("photo", StringValue.newBuilder(data.photo == null || data.photo.trim().isEmpty() ? user.getString("photo") : data.photo).setExcludeFromIndexes(true).build())
                        .build();
                txn.update(user);
                txn.commit();
                LOG.fine("Data change: " + data.username + "'s data was updated in the database.");
                return Response.ok().entity("User's data updated.").build();
            } else if ( validation == 0 ) { // token time has run out
                txn.rollback();
                LOG.fine("Data change: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                LOG.warning("Data change: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                txn.rollback();
                LOG.severe("Data change: " + token.username + "'s' authentication token has different tokenID, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("Data change: " + token.username + "'s' authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Data change: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Data change: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/change/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response alterPassword(PasswordData data) {
        AuthToken token = data.token;
        LOG.fine("Password change: " + token.username + " attempted to change their password.");
        if ( !data.validPasswordData() ) {
			LOG.warning("Password change: password change attempt using missing or invalid parameters.");
			return Response.status(Status.BAD_REQUEST).entity("Missing or invalid parameter.").build();
        }
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(token.username);
            Entity user = txn.get(userKey);
            if ( user == null ) {
                txn.rollback();
				LOG.warning("Password change: " + token.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            }
            String userRole = user.getString("role");
            int validation = token.isStillValid(user.getString("tokenID"), userRole);
            if ( validation == 1 ) {
                String hashedPassword = (String) user.getString("password");
                if ( hashedPassword.equals(DigestUtils.sha3_512Hex(data.oldPassword)) ) {
                    String username = token.username;
                    token = new AuthToken(username, userRole);
                    user = Entity.newBuilder(user)
                            .set("username", user.getString("username"))
                            .set("password", DigestUtils.sha3_512Hex(data.newPassword))
                            .set("email", user.getString("email"))
                            .set("name", user.getString("name"))
                            .set("phone", user.getString("phone"))
                            .set("profile", user.getString("profile"))
                            .set("work", user.getString("work"))
                            .set("workplace", user.getString("workplace"))
                            .set("address", user.getString("address"))
                            .set("postalcode", user.getString("postalcode"))
                            .set("fiscal", user.getString("fiscal"))
                            .set("role", user.getString("role"))
                            .set("state", user.getString("state"))
                            .set("userCreationTime", user.getTimestamp("userCreationTime"))
                            .set("tokenID", StringValue.newBuilder(user.getString("tokenID")).setExcludeFromIndexes(true).build())
                            .set("photo", StringValue.newBuilder(user.getString("photo")).setExcludeFromIndexes(true).build())
                            .build();
                    txn.update(user);
                    txn.commit();
                    LOG.fine("Password change: " + token.username + "'s' password was changed.");
                    return Response.ok(g.toJson(token)).build();
			    } else {
                    txn.rollback();
                    LOG.warning("Password change: " + token.username + " provided wrong password.");
                    return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
			    }
            } else if ( validation == 0 ) { // Token time has run out
                txn.rollback();
                LOG.fine("Password change: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                LOG.warning("Password change: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                txn.rollback();
                LOG.severe("Password change: " + token.username + "'s' authentication token has different tokenID, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("Password change: " + token.username + "'s' authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Password change: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Password change: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/change/role")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response changeRole(RoleData data) {
        AuthToken token = data.token;
        LOG.fine("Role change: attempt to change role of " + data.username + " by " + token.username + ".");
        if ( token.role.equals(UserConstants.USER) || token.role.equals(UserConstants.GBO) || 
            ( token.role.equals(UserConstants.GA) && (data.role.equals(UserConstants.GA) || data.role.equals(UserConstants.SU) ) ) ) {
            LOG.warning("Role change: unauthorized attempt to change the role of a user.");
            return Response.status(Status.UNAUTHORIZED).entity("User is not authorized to change user accounts role.").build();
        }
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(data.username);
            Key adminKey = userKeyFactory.newKey(token.username);
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            if ( admin == null ) {
                txn.rollback();
                LOG.warning("Role change: Admin is not registered.");
                return Response.status(Status.NOT_FOUND).entity("Admin is not registered.").build();
            } else if ( user == null ) {
                txn.rollback();
                LOG.warning("Role change: User is not registered.");
                return Response.status(Status.NOT_FOUND).entity("User is not registered.").build();
            }
            if ( user.getString("role").equals(data.role) ) {
                txn.rollback();
                LOG.fine("Role change: User already has the same role.");
                return Response.status(Status.NOT_MODIFIED).entity("User already had the same role, role remains unchanged.").build();
            }
            String adminRole = admin.getString("role");
            int validation = token.isStillValid(admin.getString("tokenID"), adminRole);
            if ( validation == 1 ) {
                user = Entity.newBuilder(user)
                            .set("username", user.getString("username"))
                            .set("password", user.getString("password"))
                            .set("email", user.getString("email"))
                            .set("name", user.getString("name"))
                            .set("phone", user.getString("phone"))
                            .set("profile", user.getString("profile"))
                            .set("work", user.getString("work"))
                            .set("workplace", user.getString("workplace"))
                            .set("address", user.getString("address"))
                            .set("postalcode", user.getString("postalcode"))
                            .set("fiscal", user.getString("fiscal"))
                            .set("role", data.role)
                            .set("state", user.getString("state"))
                            .set("userCreationTime", user.getTimestamp("userCreationTime"))
                            .set("tokenID", StringValue.newBuilder(user.getString("tokenID")).setExcludeFromIndexes(true).build())
                            .set("photo", StringValue.newBuilder(user.getString("photo")).setExcludeFromIndexes(true).build())
                            .build();
                txn.update(user);
                txn.commit();
                LOG.fine("Role change: " + data.username + "'s' role was changed to " + data.role + ".");
                return Response.ok().entity("User's role changed.").build();
            } else if (validation == 0 ) { // Token time has run out
                txn.rollback();
                LOG.fine("Role change: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                LOG.warning("Role change: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                txn.rollback();
                LOG.severe("Role change: " + token.username + "'s' authentication token has different tokenID, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("Role change: " + token.username + "'s' authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Role change: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Role change: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }


    @POST
    @Path("/change/state")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response changeState(UsernameData data) {
        AuthToken token = data.token;
        LOG.fine("State changing attempt of: " + data.username + " by " + token.username);
        if ( token.role.equals(UserConstants.USER) ) {
            LOG.warning("State change: unauthorized attempt to change the state of a user.");
            return Response.status(Status.UNAUTHORIZED).entity("USER roles cannot change any user states.").build();
        }
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(data.username);
            Key adminKey = userKeyFactory.newKey(token.username);
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            if ( admin == null ) {
                txn.rollback();
				LOG.warning("State change: " + token.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("Admin is not registered as a user.").build();
            } else if ( user == null ) {
                txn.rollback();
				LOG.warning("State change: " + data.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("User is not registered as a user.").build();
            }
            String adminRole = admin.getString("role");
            int validation = token.isStillValid(admin.getString("tokenID"), adminRole);
            if ( validation == 1 ) {
                if ( adminRole.equals(UserConstants.GBO) ) {
                    if ( !user.getString("role").equals(UserConstants.USER) ) { // GBO users can only change USER states
                        txn.rollback();
                        LOG.warning("State change: " + token.username + " attmepted to change the state of a non USER role.");
                        return Response.status(Status.UNAUTHORIZED).entity("GBO users cannot change non USER roles' states.").build();
                    }
                } else if ( adminRole.equals(UserConstants.GA) ) {
                    String userRole = user.getString("role");
                    if ( !userRole.equals(UserConstants.USER) && !userRole.equals(UserConstants.GBO) ) { // GA users can change USER and GBO states
                        txn.rollback();
                        LOG.warning("State change: " + token.username + " attmepted to change the state of a non USER or GBO role.");
                        return Response.status(Status.UNAUTHORIZED).entity("GA users cannot change non USER and GBO roles' states.").build();
                    }
                } else if ( adminRole.equals(UserConstants.SU) ) {                    
                } else if ( adminRole.equals(UserConstants.USER) ) {
                    txn.rollback();
                    LOG.warning("State change: " + token.username + " attmepted to change the state of a user as a USER role.");
                    return Response.status(Status.UNAUTHORIZED).entity("USER users cannot change states.").build();
                } else {
                    txn.rollback();
                    LOG.severe("State change: Unrecognized role.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
                user = Entity.newBuilder(user)
                    .set("username", user.getString("username"))
                    .set("password", user.getString("password"))
                    .set("email", user.getString("email"))
                    .set("name", user.getString("name"))
                    .set("phone", user.getString("phone"))
                    .set("profile", user.getString("profile"))
                    .set("work", user.getString("work"))
                    .set("workplace", user.getString("workplace"))
                    .set("address", user.getString("address"))
                    .set("postalcode", user.getString("postalcode"))
                    .set("fiscal", user.getString("fiscal"))
                    .set("role", user.getString("role"))
                    .set("state", user.getString("state").equals(UserConstants.ACTIVE) ? UserConstants.INACTIVE : UserConstants.ACTIVE)
                    .set("userCreationTime", user.getTimestamp("userCreationTime"))
                    .set("tokenID", StringValue.newBuilder(user.getString("tokenID")).setExcludeFromIndexes(true).build())
                    .set("photo", StringValue.newBuilder(user.getString("photo")).setExcludeFromIndexes(true).build())
                    .build();
                txn.update(user);
                txn.commit();
                LOG.fine("State change: " + data.username + "'s role changed by " + token.username + ".");
                return Response.ok().entity("User state changed.").build();
            } else if (validation == 0 ) { // Token time has run out
                txn.rollback();
                LOG.fine("State change: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                LOG.warning("State change: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                txn.rollback();
                LOG.severe("State change: " + token.username + "'s' authentication token has different tokenID, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("State change: " + token.username + "'s' authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("State change: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("State change: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response removeUser(UsernameData data) {
        AuthToken token = data.token;
        LOG.fine("Remove User: removal attempt of " + data.username + " by " + token.username + ".");
        if ( token.role.equals(UserConstants.GBO) || ( token.role.equals(UserConstants.USER) && !token.username.equals(data.username) ) ) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(data.username);
            Key adminKey = userKeyFactory.newKey(token.username);
            Key statsKey = datastore.newKeyFactory()
                    .addAncestor(PathElement.of("User", data.username))
                    .setKind("LoginStats").newKey("counters");
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            if ( admin == null ) {
                txn.rollback();
				LOG.warning("Remove User: " + token.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            } else if ( user == null ) {
                txn.rollback();
				LOG.warning("Remove User: " + data.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            }
            String adminRole = admin.getString("role");
            int validation = token.isStillValid(admin.getString("tokenID"), adminRole);
            if ( validation == 1 ) {
                if ( adminRole.equals(UserConstants.USER) ) {
                    String role = user.getString("role");
                    if ( !role.equals(UserConstants.USER) || !user.equals(admin) ) {
                        txn.rollback();
                        LOG.warning("Remove User: " + token.username + " (USER role) attempted to delete other user.");
                        return Response.status(Status.UNAUTHORIZED).entity("USER roles cannot remove other users from the database.").build();
                    }
                } else if ( adminRole.equals(UserConstants.GA) ) {
                    String role = user.getString("role");
                    if ( !role.equals(UserConstants.GBO) && !role.equals(UserConstants.USER) ) {
                        txn.rollback();
                        LOG.warning("Remove User: " + token.username + " (GA role) attempted to delete SU or GA user.");
                        return Response.status(Status.UNAUTHORIZED).entity("GA roles cannot remove GA or SU users from the database.").build();
                    }
                } else if ( adminRole.equals(UserConstants.SU) ) {
                } else {
                    txn.rollback();
                    LOG.severe("Remove User: Unrecognized role.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
                txn.delete(userKey, statsKey);
                txn.commit();
                LOG.fine("Remove User: " + data.username + " removed from the database.");
                return Response.ok().entity("User removed from database.").build();
            } else if (validation == 0 ) { // Token time has run out
                txn.rollback();                
                LOG.fine("Remove User: " + token.username + "'s authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                LOG.warning("Remove User: " + token.username + "'s authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                txn.rollback();
                LOG.severe("Remove User: " + token.username + "'s authentication token has different tokenID, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("Remove User: authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Remove User: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Remove User: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}