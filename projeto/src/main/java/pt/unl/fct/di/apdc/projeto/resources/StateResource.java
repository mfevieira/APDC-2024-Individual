package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.*;

import com.google.cloud.datastore.*;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;
import pt.unl.fct.di.apdc.projeto.util.UsernameData;

@Path("/state")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class StateResource {
 
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public StateResource() {

    }

    @POST
    @Path("/")
    public Response changeState(UsernameData data, AuthToken token) {
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
                    if ( user.getString("role").equals(UserConstants.USER) ) {
                        String state = user.getString("state").equals(UserConstants.ACTIVE) ? UserConstants.INACTIVE : UserConstants.ACTIVE;
                        user = Entity.newBuilder(user)
                            .set("state", state)
                            .build();
                    } else { // GBO users can only change USER states
                        txn.rollback();
                        LOG.warning("State change: " + token.username + " attmepted to change the state of a non USER role.");
                        return Response.status(Status.UNAUTHORIZED).entity("GBO users cannot change non USER roles' states.").build();
                    }
                } else if ( adminRole.equals(UserConstants.GA) ) {
                    String userRole = user.getString("role");
                    if ( userRole.equals(UserConstants.USER) || userRole.equals(UserConstants.GBO) ) {
                        String state = user.getString("state").equals(UserConstants.ACTIVE) ? UserConstants.INACTIVE : UserConstants.ACTIVE;
                        user = Entity.newBuilder(user)
                            .set("state", state)
                            .build();
                    } else { // GA users can change USER and GBO states
                        txn.rollback();
                        LOG.warning("State change: " + token.username + " attmepted to change the state of a non USER or GBO role.");
                        return Response.status(Status.UNAUTHORIZED).entity("GA users cannot change non USER and GBO roles' states.").build();
                    }
                } else if ( adminRole.equals(UserConstants.SU) ) {
                    String state = user.getString("state").equals(UserConstants.ACTIVE) ? UserConstants.INACTIVE : UserConstants.ACTIVE;
                        user = Entity.newBuilder(user)
                            .set("state", state)
                            .build();
                } else if ( adminRole.equals(UserConstants.USER) ) {
                    txn.rollback();
                    LOG.warning("State change: " + token.username + " attmepted to change the state of a user as a USER role.");
                    return Response.status(Status.UNAUTHORIZED).entity("USER users cannot change states.").build();
                } else {
                    txn.rollback();
                    LOG.severe("State change: Unrecognized role.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
                txn.update(user);
                txn.commit();
                LOG.fine("State change: " + data.username + "'s role changed by " + token.username + ".");
                return Response.ok().entity("User state changed.").build();
            } else if (validation == 0 ) {
                // TODO: Send the admin back to the login page
                txn.rollback();
                LOG.fine("State change: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                // TODO: Send the admin back to the login page
                txn.rollback();
                LOG.warning("State change: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                // TODO: Send the admin back to the login page
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
}
