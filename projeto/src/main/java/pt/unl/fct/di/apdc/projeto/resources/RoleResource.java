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
import pt.unl.fct.di.apdc.projeto.util.RoleData;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;

@Path("/role")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RoleResource {

	/** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public RoleResource() {
	}

    @POST
    @Path("/")
    public Response changeRole(RoleData data, AuthToken token) {
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
                user = Entity.newBuilder(user).set("role", data.role).build();
                txn.update(user);
                txn.commit();
                LOG.fine("Role change: " + data.username + "'s' role was changed to " + data.role + ".");
                return Response.ok().entity("User's role changed.").build();
            } else if (validation == 0 ) {
                // TODO: Send the admin back to the login page
                txn.rollback();
                LOG.fine("Role change: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                // TODO: Send the admin back to the login page
                txn.rollback();
                LOG.warning("Role change: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                // TODO: Send the admin back to the login page
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
}
