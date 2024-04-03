package pt.unl.fct.di.apdc.projeto.resources;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.*;

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.User;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;
import pt.unl.fct.di.apdc.projeto.util.UserQuery;

@Path("/list")
public class ListUserResource {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	
	/** The converter to JSON */
	private final Gson g = new Gson();

    public ListUserResource() {

    }

    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response listUsers(AuthToken token) {
        LOG.fine("List users: " + token.username + " attempted to list users.");
        try {
            Key userKey = userKeyFactory.newKey(token.username);
            Entity user = datastore.get(userKey);
            if ( user == null ) {
				LOG.warning("List users: " + token.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            }
            String userRole = user.getString("role");
            int validation = token.isStillValid(user.getString("tokenID"), userRole);
            if ( validation == 1 ) {
                Query<Entity> query;
                if ( userRole.equals(UserConstants.USER) ) {
                    Query<ProjectionEntity> projectionQuery = Query.newProjectionEntityQueryBuilder()
				            .setKind("User")
				            .setFilter(CompositeFilter.and(
                                        PropertyFilter.eq("state", UserConstants.ACTIVE),
                                        PropertyFilter.eq("profile", UserConstants.PUBLIC),
                                        PropertyFilter.eq("role", UserConstants.USER)))
				            .setProjection("username", "email", "name")
				            .build();
                    List<UserQuery> projection = new LinkedList<>();
                    QueryResults<ProjectionEntity> results = datastore.run(projectionQuery);
                    while ( results.hasNext() ) {
                        ProjectionEntity result = results.next();
                        projection.add(new UserQuery(result.getString("username"), result.getString("email"), result.getString("name")));
                    }
				    LOG.info("List users: " + token.username + " received list of active and public USER users.");
				    return Response.ok(g.toJson(projection)).build();
                } else if ( userRole.equals(UserConstants.GBO) ) {
                    query = Query.newEntityQueryBuilder()
				            .setKind("User")
				            .setFilter(PropertyFilter.eq("role", UserConstants.USER))
				            .build();
                } else if ( userRole.equals(UserConstants.GA) ) {
                    query = Query.newEntityQueryBuilder()
				            .setKind("User")
				            .setFilter(PropertyFilter.in("role", ListValue.of(UserConstants.USER, UserConstants.GBO, UserConstants.GA)))
                            .setOrderBy(OrderBy.desc("role"))
				            .build();
                } else if ( userRole.equals(UserConstants.SU) ) {
                    query = Query.newEntityQueryBuilder()
				            .setKind("User")
                            .setOrderBy(OrderBy.desc("role"))
				            .build();
                } else {
                    LOG.severe("List users: Unrecognized role.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
                List<User> userList = new LinkedList<>();
                QueryResults<Entity> results = datastore.run(query);
                while ( results.hasNext() ) {
                    Entity next = results.next();
                    userList.add(new User(next.getString("username"), next.getString("password"), next.getString("email"), 
                                    next.getString("name"), next.getString("phone"), next.getString("profile"), 
                                    next.getString("work"), next.getString("workplace"), next.getString("address"), 
                                    next.getString("postalcode"), next.getString("fiscal"), next.getString("role"), 
                                    next.getString("state"), next.getTimestamp("userCreationTime").toDate(), next.getString("tokenID"), next.getString("photo")));
                }
                LOG.info("List users: " + token.username + " received list of all users.");
                return Response.ok(g.toJson(userList)).build();
            } else if ( validation == 0 ) { // Token time has run out
                LOG.fine("List users: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                LOG.warning("List users: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // tokenID is false
                LOG.severe("List users: " + token.username + "'s' authentication token has different tokenID, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("TokenId incorrect, make new login").build();
            } else {
                LOG.severe("List users: " + token.username + "'s' authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			LOG.severe("List users: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
    }
}