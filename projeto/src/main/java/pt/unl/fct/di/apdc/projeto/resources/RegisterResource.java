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

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;
import pt.unl.fct.di.apdc.projeto.util.RegisterData;

@Path("/register")
public class RegisterResource {

	/** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	/** The converter to JSON */
	private final Gson g = new Gson();

	public RegisterResource() {
	}

	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response register(RegisterData data) {
		LOG.fine("Resgister: attempt to register " + data.username + ".");
		if (!data.validRegistration()) {
			LOG.warning("Register: Register attempt using missing or invalid parameters.");
			return Response.status(Status.BAD_REQUEST).entity("Missing or invalid parameter.").build();
		}
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = userKeyFactory.newKey(data.username);
			if (txn.get(userKey) == null) {
				AuthToken token = new AuthToken(data.username, UserConstants.USER);
				Entity user = Entity.newBuilder(userKey)
						.set("username", data.username)
						.set("password", DigestUtils.sha3_512Hex(data.password))
						.set("email", data.email)
						.set("name", data.name)
						.set("phone", data.phone)
						.set("profile", data.profile == null ? UserConstants.PRIVATE : data.profile)
						.set("work", data.work == null ? "" : data.work)
						.set("workplace", data.workPlace == null ? "" : data.workPlace)
						.set("address", data.address == null ? "" : data.address)
						.set("postalCode", data.postalCode == null ? "" : data.postalCode)
						.set("fiscal", data.fiscal == null ? "" : data.fiscal)
						.set("role", UserConstants.USER)
						.set("state", UserConstants.INACTIVE)
						.set("userCreationTime", Timestamp.now())
						.set("tokenID", StringValue.newBuilder(token.tokenID).setExcludeFromIndexes(true).build())
						.build();
				txn.add(user);
				txn.commit();
				LOG.fine("Register: " + data.username + "'s was registered in the database.");
				return Response.ok(g.toJson(token)).entity("User Registered.").build();
			} else {
				txn.rollback();
				LOG.fine("Register: duplicate username.");
				// TODO: send user back to register page
				return Response.status(Status.CONFLICT)
						.entity("User already exists. Pick a different username.")
						.build();
			}
		} catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Register: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Register: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
}
