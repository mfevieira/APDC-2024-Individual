package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.LoginData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;

@Path("/login")
public class LoginResource {

	/**
	 * Logger Object
	 */
	private static Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** 24 hours in milliseconds */
	public static final long HOURS24 = 1000*60*60*24;

	/** The data store to store users in */
	private static final Datastore datastore = ServerConstants.datastore;
	
	/** The key factory for users */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	
	/** The converter to JSON */
	private final Gson g = new Gson();

	public LoginResource() {
	}

	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response login(LoginData data) {
		LOG.fine("Login: login attempt by: " + data.username + ".");
		Key userKey = userKeyFactory.newKey(data.username);
		Key statsKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", data.username))
				.setKind("LoginStats").newKey("counters");
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if ( user == null ) {
				LOG.warning("Login: " + data.username + " not registered as user.");
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
			}
			Entity stats = txn.get(statsKey);
			if ( stats == null ) {
				stats = Entity.newBuilder(statsKey)
						.set("successfulLogins", 0L)
						.set("failedLogins", 0L)
						.set("userFirstLogin", Timestamp.now())
						.set("userLastLogin", Timestamp.now())
						.build();
			}
			String hashedPassword = (String) user.getString("password");
			if ( hashedPassword.equals(DigestUtils.sha3_512Hex(data.password)) ) {
				AuthToken token = new AuthToken(data.username, user.getString("role"));
				stats = Entity.newBuilder(statsKey)
						.set("successfulLogins", 1L + stats.getLong("successfulLogins"))
						.set("failedLogins", stats.getLong("failedLogins"))
						.set("userFirstLogin", stats.getTimestamp("userFirstLogin"))
						.set("userLastLogin", Timestamp.now())
						.build();
				user = Entity.newBuilder(userKey)
						.set("username", data.username)
						.set("password", DigestUtils.sha3_512Hex(data.password))
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
						.set("tokenID", StringValue.newBuilder(token.tokenID).setExcludeFromIndexes(true).build())
						.set("photo", StringValue.newBuilder(user.getString("photo")).setExcludeFromIndexes(true).build())
						.build();
				txn.put(user);
				txn.commit();
				LOG.info("Login: " + data.username + " logged in successfully.");
				return Response.ok(g.toJson(token)).build();
			} else {
				Entity userStats = Entity.newBuilder(statsKey)
						.set("successfulLogins", stats.getLong("successfulLogins"))
						.set("failedLogins", 1L + stats.getLong("failedLogins"))
						.set("userFirstLogin", stats.getTimestamp("userFirstLogin"))
						.set("userLastLogin", stats.getTimestamp("userLastLogin"))
						.set("lastLoginAttempt", Timestamp.now())
						.build();
				txn.put(userStats);
				txn.commit();
				LOG.warning("Login: " + data.username + " provided wrong password.");
				return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
			}
		} catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Login: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		} finally {
			if ( txn.isActive() ) {
				txn.rollback();
                LOG.severe("Login: Internal server error.");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}

	@POST
	@Path("/check")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkToken(AuthToken token) {
		LOG.fine("Check: token check attempt by " + token.username + ".");
		Key userKey = userKeyFactory.newKey(token.username);
		Entity user = datastore.get(userKey);
		if ( user == null ) {
			LOG.warning("Check: " + token.username + " is not a registered user.");
			return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
		}
		String role = user.getString("role");
		String tokenID = user.getString("tokenID");
		int validation = token.isStillValid(tokenID, role);
		if ( validation == 1 ) {
			LOG.fine("Check: " + token.username + " is still logged in.");
			return Response.ok().build();
		} else if ( validation == 0 ) { // Token time has run out
			LOG.fine("Check: " + token.username + "'s authentication token expired.");
			return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
		} else if ( validation == -1 ) { // Role is different
			LOG.warning("Check: " + token.username + "'s authentication token has different role.");
			return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
		} else if ( validation == -2 ) { // tokenID is false
			LOG.severe("Check: " + token.username + "'s authentication token has different tokenID, possible attempted breach.");
			return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
		} else {
			LOG.fine("Check: authentication token validity error.");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}