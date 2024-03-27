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

import pt.unl.fct.di.apdc.projeto.util.LoginData;
import pt.unl.fct.di.apdc.projeto.util.RegisterData;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;
import pt.unl.fct.di.apdc.projeto.util.OptionalRegisterData;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {

	/** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

	public RegisterResource() {
	}

	@POST
	@Path("/v1")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerV1(LoginData data) {
		LOG.fine("Resgistry attempt by: " + data.username);
		if (!data.validRegistration()) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = userKeyFactory.newKey(data.username);
			if ( txn.get(userKey) == null ) {
				Entity user = Entity.newBuilder(userKey)
						.set("password", DigestUtils.sha3_512Hex(data.password))
						.set("userCreationTime", Timestamp.now())
						.build();
				txn.add(user);
				LOG.info("User Registered: " + data.username);
				txn.commit();
				return Response.ok(Status.CREATED).build();
			} else {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST)
						.entity("User already exists. Pick a different username.")
						.build();
			}
		} finally {
			if ( txn.isActive() ) {
				txn.rollback();
			}
		}
	}

	@POST
	@Path("/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response registerV2(RegisterData data) {
		LOG.fine("Resgistry attempt by: " + data.username);
		if (!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = userKeyFactory.newKey(data.username);
			if (txn.get(userKey) == null) {
				Entity user = Entity.newBuilder(userKey)
						.set("password", DigestUtils.sha3_512Hex(data.password))
						.set("email", data.email)
						.set("name", data.name)
						.set("userCreationTime", Timestamp.now())
						.build();
				txn.add(user);
				LOG.info("User Registered: " + data.username);
				txn.commit();
				return Response.ok(Status.CREATED).build();
			} else {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST)
						.entity("User already exists. Pick a different username.")
						.build();
			}
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}


	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response register(OptionalRegisterData data) {
		LOG.fine("Resgistry attempt by: " + data.username);
		if (!data.validRegistration()) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or invalid parameter.").build();
		}
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = userKeyFactory.newKey(data.username);
			if (txn.get(userKey) == null) {
				Entity user = Entity.newBuilder(userKey)
						.set("password", DigestUtils.sha3_512Hex(data.password))
						.set("email", data.email)
						.set("name", data.name)
						.set("phone", data.phone)
						.set("profile", data.profile == null ? UserConstants.INACTIVE : data.profile)
						.set("work", data.work == null ? "" : data.work)
						.set("workplace", data.workPlace == null ? "" : data.workPlace)
						.set("address", data.address == null ? "" : data.address)
						.set("postalCode", data.postalCode == null ? "" : data.postalCode)
						.set("fiscal", data.fiscal == null ? "" : data.fiscal)
						.set("role", UserConstants.USER)
						.set("state", UserConstants.INACTIVE)
						.set("userCreationTime", Timestamp.now())
						.build();
				txn.add(user);
				txn.commit();
				LOG.info("User Registered: " + data.username);
				return Response.ok(Status.CREATED).build();
			} else {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST)
						.entity("User already exists. Pick a different username.")
						.build();
			}
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
