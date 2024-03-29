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
import pt.unl.fct.di.apdc.projeto.util.OptionalRegisterData;
import pt.unl.fct.di.apdc.projeto.util.PasswordData;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;

@Path("/change")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class AlterUserResource {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	
	/** The converter to JSON */
	private final Gson g = new Gson();

    public AlterUserResource() {

    }

    @POST
    @Path("/data")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response alterData(OptionalRegisterData data, AuthToken token) {
        LOG.fine("Data change: " + token.username + " attempted to change their data.");
        if ( token.role.equals(UserConstants.USER) && !data.username.equals(token.username) ) {
            LOG.warning("Data change: " + token.username + " cannot change .");
            return Response.status(Status.UNAUTHORIZED).entity("User role cannot change other users data.").build();
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
                    Entity newUser = Entity.newBuilder(user)
						.set("phone", data.phone == null ? user.getString("phone") : data.phone)
						.set("profile", data.profile == null ? user.getString("profile") : data.profile)
						.set("work", data.work == null ? user.getString("work") : data.work)
						.set("workplace", data.workPlace == null ? user.getString("workPlace") : data.workPlace)
						.set("address", data.address == null ? user.getString("address") : data.address)
						.set("postalCode", data.postalCode == null ? user.getString("postalCode") : data.postalCode)
						.set("fiscal", data.fiscal == null ? user.getString("fiscal") : data.fiscal)
						.build();
                    txn.update(newUser);
                    txn.commit();
                    LOG.fine("Data change: " + data.username + "'s data was updated in the database.");
                    return Response.ok().entity("User's data updated.").build();
                } else if ( adminRole.equals(UserConstants.GBO) ) {
                    if ( user.getString("role").equals(UserConstants.USER) ) {
                        Entity newUser = Entity.newBuilder(user)
                                .set("email", data.email == null ? user.getString("email") : data.email)
                                .set("name", data.name == null ? user.getString("name") : data.name)
                                .set("phone", data.phone == null ? user.getString("phone") : data.phone)
                                .set("profile", data.profile == null ? user.getString("profile") : data.profile)
                                .set("work", data.work == null ? user.getString("work") : data.work)
                                .set("workplace", data.workPlace == null ? user.getString("workPlace") : data.workPlace)
                                .set("address", data.address == null ? user.getString("address") : data.address)
                                .set("postalCode", data.postalCode == null ? user.getString("postalCode") : data.postalCode)
                                .set("fiscal", data.fiscal == null ? user.getString("fiscal") : data.fiscal)
                                .set("role", data.role == null ? user.getString("role") : data.role)
                                .set("state", data.state == null ? user.getString("state") : data.state)
                                .build();
                        txn.update(newUser);
                        txn.commit();
                        LOG.fine("Data change: " + data.username + "'s data was updated in the database.");
                        return Response.ok().entity("User's data updated.").build();
                    } else {
                        txn.rollback();
                        LOG.warning("Data change: " + token.username + " cannot change non USER users data.");
                        return Response.status(Status.UNAUTHORIZED).entity("GBO users cannot change data of non USER users.").build();
                    }
                } else if ( adminRole.equals(UserConstants.GA) ) {
                    if ( user.getString("role").equals(UserConstants.USER) || user.getString("role").equals(UserConstants.GBO) ) {
                        Entity newUser = Entity.newBuilder(user)
                                .set("email", data.email == null ? user.getString("email") : data.email)
                                .set("name", data.name == null ? user.getString("name") : data.name)
                                .set("phone", data.phone == null ? user.getString("phone") : data.phone)
                                .set("profile", data.profile == null ? user.getString("profile") : data.profile)
                                .set("work", data.work == null ? user.getString("work") : data.work)
                                .set("workplace", data.workPlace == null ? user.getString("workPlace") : data.workPlace)
                                .set("address", data.address == null ? user.getString("address") : data.address)
                                .set("postalCode", data.postalCode == null ? user.getString("postalCode") : data.postalCode)
                                .set("fiscal", data.fiscal == null ? user.getString("fiscal") : data.fiscal)
                                .set("role", data.role == null ? user.getString("role") : data.role)
                                .set("state", data.state == null ? user.getString("state") : data.state)
                                .build();
                        txn.update(newUser);
                        txn.commit();
                        LOG.fine("Data change: " + data.username + "'s data was updated in the database.");
                        return Response.ok().entity("User's data updated.").build();
                    } else {
                        txn.rollback();
                        LOG.warning("Data change: " + token.username + " cannot change GA or SU users data.");
                        return Response.status(Status.UNAUTHORIZED).entity("GA users cannot change data of GA or SU users.").build();
                    }
                } else if ( adminRole.equals(UserConstants.SU) ) {
                    if ( user.getString("role").equals(UserConstants.USER) || user.getString("role").equals(UserConstants.GBO) || user.getString("role").equals(UserConstants.GA) ) {
                        Entity newUser = Entity.newBuilder(user)
                                .set("email", data.email == null ? user.getString("email") : data.email)
                                .set("name", data.name == null ? user.getString("name") : data.name)
                                .set("phone", data.phone == null ? user.getString("phone") : data.phone)
                                .set("profile", data.profile == null ? user.getString("profile") : data.profile)
                                .set("work", data.work == null ? user.getString("work") : data.work)
                                .set("workplace", data.workPlace == null ? user.getString("workPlace") : data.workPlace)
                                .set("address", data.address == null ? user.getString("address") : data.address)
                                .set("postalCode", data.postalCode == null ? user.getString("postalCode") : data.postalCode)
                                .set("fiscal", data.fiscal == null ? user.getString("fiscal") : data.fiscal)
                                .set("role", data.role == null ? user.getString("role") : data.role)
                                .set("state", data.state == null ? user.getString("state") : data.state)
                                .build();
                        txn.update(newUser);
                        txn.commit();
                        LOG.fine("Data change: " + data.username + "'s data was updated in the database.");
                        return Response.ok().entity("User's data updated.").build();
                    } else {
                        txn.rollback();
                        LOG.warning("Data change: " + token.username + " cannot change SU users data.");
                        return Response.status(Status.UNAUTHORIZED).entity("SU users cannot change data of SU users.").build();
                    }
                } else {
                    txn.rollback();
                    LOG.severe("Data change: Unrecognized role.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            } else if ( validation == 0 ) {
                // TODO: Send the user back to the login page
                txn.rollback();
                LOG.fine("Data change: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                // TODO: Send the user back to the login page
                txn.rollback();
                LOG.warning("Data change: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                // TODO: Send the user back to the login page
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
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response alterPassword(PasswordData data, AuthToken token) {
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
                            .set("password", DigestUtils.sha3_512Hex(data.newPassword))
                            .set("tokenID", token.tokenID)
                            .build();
                    txn.update(user);
                    txn.commit();
                    LOG.fine("Password change: " + token.username + "'s' password was changed.");
                    return Response.ok(g.toJson(token)).entity("User's role changed.").build();
			    } else {
                    txn.rollback();
                    LOG.warning("Password change: " + token.username + " provided wrong password.");
                    return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
			    }
            } else if ( validation == 0 ) {
                // TODO: Send the user back to the login page
                txn.rollback();
                LOG.fine("Password change: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                // TODO: Send the user back to the login page
                txn.rollback();
                LOG.warning("Password change: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                // TODO: Send the user back to the login page
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
}
