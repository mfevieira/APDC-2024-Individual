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

@Path("/list")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ListUserResource {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public ListUserResource() {

    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listUsers(AuthToken token) {
        // TODO: Make the lists
        return Response.status(Status.NOT_FOUND).build();
    }
}
