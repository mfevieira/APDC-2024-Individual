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
import pt.unl.fct.di.apdc.projeto.util.UsernameData;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;

@Path("/remove")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RemoveResource {

    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public RemoveResource() {

    }
    
    @POST
    @Path("/")
    public Response removeUser(UsernameData data, AuthToken token) {
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
            } else if (validation == 0 ) {
                // TODO: Send the admin back to the login page
                txn.rollback();                
                LOG.fine("Remove User: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                // TODO: Send the admin back to the login page
                txn.rollback();
                LOG.warning("Remove User: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                // TODO: Send the admin back to the login page
                txn.rollback();
                LOG.severe("Remove User: " + token.username + "'s' authentication token has different tokenID, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("Remove User: " + token.username + "'s' authentication token validity error.");
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
