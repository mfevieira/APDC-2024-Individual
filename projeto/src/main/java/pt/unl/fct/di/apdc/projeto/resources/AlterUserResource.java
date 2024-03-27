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

    public AlterUserResource() {

    }

    @POST
    @Path("/data")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response alterData(AuthToken token, OptionalRegisterData data) {
        // TODO: Make the alteration
        return Response.status(Status.NOT_FOUND).build();
    }

    @POST
    @Path("/password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response alterPassword(AuthToken token, PasswordData data) {
        // TODO: Make the alteration
        return Response.status(Status.NOT_FOUND).build();
    }
}
