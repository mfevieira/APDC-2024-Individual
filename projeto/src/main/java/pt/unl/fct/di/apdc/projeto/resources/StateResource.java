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

@Path("/role")
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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeState(String username, AuthToken token) {
        LOG.fine("State changing attempt of: " + username + " by " + token.username);
        if ( token.role.equals(UserConstants.USER) ) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(username);
            Key adminUserKey = userKeyFactory.newKey(token.username);
            Entity user = txn.get(userKey);
            Entity adminUser = txn.get(adminUserKey);
            if ( adminUser == null || user == null ) {
                txn.rollback();
                return Response.status(Status.BAD_REQUEST).build();
            }
            String adminRole = adminUser.getString("role");
            int validation = token.isStillValid(adminUser.getString("tokenID"), adminRole);
            if ( validation == 1 ) {
                if ( adminRole.equals(UserConstants.GBO) ) {
                    if ( user.getString("role").equals(UserConstants.USER) ) {
                        String state = user.getString("state").equals(UserConstants.ACTIVE) ? UserConstants.INACTIVE : UserConstants.ACTIVE;
                        user = Entity.newBuilder(user)
                            .set("state", state)
                            .build();
                    } else { // GBO users can only change USER states
                        txn.rollback();
                        return Response.status(Status.UNAUTHORIZED).build();
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
                        return Response.status(Status.UNAUTHORIZED).build();
                    }
                } else if ( adminRole.equals(UserConstants.SU) ) {
                    String state = user.getString("state").equals(UserConstants.ACTIVE) ? UserConstants.INACTIVE : UserConstants.ACTIVE;
                        user = Entity.newBuilder(user)
                            .set("state", state)
                            .build();
                } else {
                    txn.rollback();
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
                txn.update(user);
                txn.commit();
                // TODO: Send the proper confirmation back
                return Response.ok().build();
            } else if (validation == 0 ) {
                // TODO: Send the admin back to the login page
                txn.rollback();
                return Response.status(Status.CONFLICT).build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                return Response.status(Status.CONFLICT).build();
            } else if ( validation == -2 ) { // tokenID is false
                txn.rollback();
                return Response.status(Status.CONFLICT).build();
            } else {
                txn.rollback();
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}
