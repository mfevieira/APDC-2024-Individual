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

@Path("/alter")
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
        // TODO: Make the alteration
        return Response.status(Status.NOT_FOUND).build();
    }

    @POST
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response alterPassword(PasswordData data, AuthToken token) {
        // TODO: Make the alteration
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
