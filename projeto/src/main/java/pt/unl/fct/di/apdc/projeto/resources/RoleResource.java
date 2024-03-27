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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeRole(String username, String role, AuthToken token) {
        LOG.fine("Role changing attempt of: " + username + " by " + token.username);
        if ( token.role.equals(UserConstants.USER) || token.role.equals(UserConstants.GBO) || 
            ( token.role.equals(UserConstants.GA) && (role.equals(UserConstants.GA) || role.equals(UserConstants.SU) ) ) ) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(username);
            Key adminKey = userKeyFactory.newKey(token.username);
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            if ( admin == null || user == null ) {
                txn.rollback();
                return Response.status(Status.BAD_REQUEST).build();
            }
            if ( user.getString("role").equals(role) ) {
                txn.rollback();
                return Response.status(Status.CONFLICT).build();
            }
            String adminRole = admin.getString("role");
            int validation = token.isStillValid(admin.getString("tokenID"), adminRole);
            if ( validation == 1 ) {
                user = Entity.newBuilder(user).set("role", role).build();
                txn.update(user);
                txn.commit();
                // TODO: send the proper confirmation back
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
