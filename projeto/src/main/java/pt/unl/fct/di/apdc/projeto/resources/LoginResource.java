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
import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.LoginData;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	/**
	 * Logger Object
	 */
	private static Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** 24 hours in milliseconds */
	public static final long HOURS24 = 1000*60*60*24;

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	/** The key factory for users */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	
	/** The converter to JSON */
	private final Gson g = new Gson();

	public LoginResource() {
	}
	
	/*@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response loginV1(LoginData data) {
		LOG.fine("Login attempt by: " + data.username);
		Key userKey = userKeyFactory.newKey(data.username);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if ( user == null ) {
				LOG.warning("No such user exists.");
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("No such user exists.").build();
			}
			String hashedPassword = (String) user.getString("password");
			if ( hashedPassword.equals(DigestUtils.sha3_512Hex(data.password)) ) {
				KeyFactory logKeyFactory = datastore.newKeyFactory()
						.addAncestor(PathElement.of("User", data.username))
						.setKind("UserLog");
				Key logKey = datastore.allocateId(logKeyFactory.newKey());
				Entity userLog = Entity.newBuilder(logKey)
						.set("loginTime", Timestamp.now())
						.build();
				AuthToken at = new AuthToken(data.username, user.getString("role"));
				txn.put(userLog);
				txn.commit();
				LOG.info("User '" + data.username + "' logged in successfully.");
				return Response.ok(g.toJson(at)).build();
			} else {
				txn.commit();
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
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
	
	
	@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response loginV2(LoginData data, 
			@Context HttpServletRequest request,
			@Context HttpHeaders headers) {
		LOG.fine("Login attempt by: " + data.username);
		Key userKey = userKeyFactory.newKey(data.username);
		Key statsKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", data.username))
				.setKind("LoginStats").newKey("counters");
		Key logKey = datastore.allocateId(
				datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", data.username))
				.setKind("UserLog")
				.newKey());
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if ( user == null ) {
				LOG.warning("No such user exists.");
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("No such user exists.").build();
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
				// TODO: Error in the creation of the log!
				Entity loginStats = Entity.newBuilder(statsKey)
						.set("successfulLogins", 1L + stats.getLong("successfulLogins"))
						.set("failedLogins", stats.getLong("failedLogins"))
						.set("userFirstLogin", stats.getTimestamp("userFirstLogin"))
						.set("userLastLogin", Timestamp.now())
						.build();
				String loginIP = request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
				String loginHost = request.getRemoteHost() != null ? request.getRemoteHost() : "";
				StringValue loginLatLon = StringValue.newBuilder(headers.getHeaderString("X-AppEngine-CityLatLong"))
						.setExcludeFromIndexes(true).build() != null ? StringValue.newBuilder(
								headers.getHeaderString("X-AppEngine-CityLatLong"))
								.setExcludeFromIndexes(true).build() : StringValue.of("");
				String loginCity = headers.getHeaderString("X-AppEngine-City") != null ? headers.getHeaderString("X-AppEngine-City") : "";
				String loginCountry = headers.getHeaderString("X-AppEngine-Country") != null ? headers.getHeaderString("X-AppEngine-Country") : "";
				Entity log = Entity.newBuilder(logKey)
					.set("loginIP", loginIP)
					.set("loginHost", loginHost)
					.set("LoginLatlon", loginLatLon)
					.set("loginCity", loginCity)
					.set("loginCountry", loginCountry)
					.set("loginTime", Timestamp.now())
					.build();
				AuthToken at = new AuthToken(data.username, user.getString("role"));
				txn.put(log, loginStats);
				txn.commit();
				LOG.info("User '" + data.username + "' logged in successfully.");
				return Response.ok(g.toJson(at)).build();
			} else {
				LOG.warning("Wrong password.");
				Entity userStats = Entity.newBuilder(statsKey)
						.set("successfulLogins", stats.getLong("successfulLogins"))
						.set("failedLogins", 1L + stats.getLong("failedLogins"))
						.set("userFirstLogin", stats.getTimestamp("userFirstLogin"))
						.set("userLastLogin", stats.getTimestamp("userLastLogin"))
						.set("lastLoginAttempt", Timestamp.now())
						.build();
				txn.put(userStats);
				txn.commit();
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
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
	
	@GET
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getLast24HourLoginData(LoginData data) {
		LOG.fine("Attempt to get the last 24 hours login timestamps of user: " + data.username);
		Key userKey = userKeyFactory.newKey(data.username);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if ( user == null ) {
				LOG.warning("No such user exists.");
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("No such user exists.").build();
			}
			String hashedPassword = (String) user.getString("password");
			if ( hashedPassword.equals(DigestUtils.sha3_512Hex(data.password)) ) {
				Query<ProjectionEntity> projectionQuery = Query.newProjectionEntityQueryBuilder()
				        .setKind("UserLogs")
				        .setFilter(CompositeFilter.and(
				        		PropertyFilter.hasAncestor(userKey),
				        		PropertyFilter.ge("loginTime", Timestamp.of(
								new Date(System.currentTimeMillis() - HOURS24)))))
				        .setOrderBy(OrderBy.desc("created"))
				        .setProjection("loginTime")
				        .build();
				List<Timestamp> timestamps = new LinkedList<>();
				QueryResults<ProjectionEntity> results = datastore.run(projectionQuery);
				while ( results.hasNext() ) {
					ProjectionEntity timestamp = results.next();
					timestamps.add(timestamp.getTimestamp("loginTime"));
				}
				/*Query<Entity> query = Query.newEntityQueryBuilder()
						.setKind("UserLogs")
						.setFilter(CompositeFilter.and(
								PropertyFilter.hasAncestor(userKey), 
								PropertyFilter.ge("loginTime", Timestamp.of(
								new Date(System.currentTimeMillis() - HOURS24)))))
						.setOrderBy(OrderBy.desc("created"))
						.build();
				QueryResults<Entity> logs = txn.run(query);
				txn.commit();
				LOG.info("User '" + data.username + "' logged in successfully.");
				return Response.ok(g.toJson(timestamps)).build();
			} else {
				txn.commit();
				LOG.warning("Wrong password for username: " + data.username);
				return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
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
	}*/

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
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
				Entity loginStats = Entity.newBuilder(statsKey)
						.set("successfulLogins", 1L + stats.getLong("successfulLogins"))
						.set("failedLogins", stats.getLong("failedLogins"))
						.set("userFirstLogin", stats.getTimestamp("userFirstLogin"))
						.set("userLastLogin", Timestamp.now())
						.build();
				user = Entity.newBuilder(userKey).set("tokenID", token.tokenID).build();
				txn.update(loginStats, user);
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
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if ( txn.isActive() ) {
				txn.rollback();
                LOG.severe("Login: Internal server error.");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}

	@POST
	@Path("/token")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getToken(AuthToken token) {
		LOG.fine("Token: token display attempt by: " + token.username + ".");
		return Response.ok(g.toJson(token)).build();
	}
}