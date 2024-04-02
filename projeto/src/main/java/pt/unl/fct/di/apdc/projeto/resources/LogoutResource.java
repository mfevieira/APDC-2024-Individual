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

@Path("/logout")
public class LogoutResource {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public LogoutResource() {

    }

    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response logout(AuthToken token) {
        LOG.fine("Logout: " + token.username + " attempt to logout.");
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = userKeyFactory.newKey(token.username);
            Entity user = txn.get(userKey);
            if ( user == null ) {
                txn.rollback();
				LOG.warning("Logout: " + token.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            }
            String userRole = user.getString("role");
            int validation = token.isStillValid(user.getString("tokenID"), userRole);
            if ( validation == 1 ) {
                user = Entity.newBuilder(userKey)
						.set("username", user.getString("username"))
						.set("password", user.getString("password"))
						.set("email", user.getString("email"))
						.set("name", user.getString("name"))
						.set("phone", user.getString("phone"))
						.set("profile", user.getString("profile"))
						.set("work", user.getString("work"))
						.set("workplace", user.getString("workplace"))
						.set("address", user.getString("address"))
						.set("postalCode", user.getString("postalCode"))
						.set("fiscal", user.getString("fiscal"))
						.set("role", user.getString("role"))
						.set("state", user.getString("state"))
						.set("userCreationTime", user.getTimestamp("userCreationTime"))
						.set("tokenID", "")
						.build();
                txn.put(user);
                txn.commit();
                LOG.fine("Logout: " + token.username + " logged out.");
                return Response.ok().entity("User logged out.").build();
            } else if ( validation == 0 ) {
                txn.rollback();
                LOG.fine("Logout: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                LOG.warning("Logout: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                txn.rollback();
                LOG.severe("Logout: " + token.username + "'s' authentication token has different tokenID, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("Logout: " + token.username + "'s' authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Logout: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Logout: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}
