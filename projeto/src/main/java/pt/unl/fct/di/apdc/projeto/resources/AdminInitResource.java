package pt.unl.fct.di.apdc.projeto.resources;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.projeto.util.UserConstants;

public class AdminInitResource extends HttpServlet {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    @Override
    public void init() throws ServletException {
        Key rootKey = userKeyFactory.newKey("root");
        Transaction txn = datastore.newTransaction();
        try {
            Entity root = txn.get(rootKey);
            if ( root == null ) {
                root = Entity.newBuilder(rootKey)
                        .set("username", "root")
						.set("password", DigestUtils.sha3_512Hex("password"))
						.set("email", "root")
						.set("name", "root")
						.set("phone", "root")
						.set("profile", UserConstants.PRIVATE)
						.set("work", "root")
						.set("workplace", "root")
						.set("address", "root")
						.set("postalCode", "root")
						.set("fiscal", "root")
						.set("role", UserConstants.SU)
						.set("state", UserConstants.ACTIVE)
						.set("userCreationTime", Timestamp.now())
						.set("tokenID", StringValue.newBuilder("").setExcludeFromIndexes(true).build())
						.build();
                txn.put(root);
                txn.commit();
                LOG.fine("Root register: root user was registered in the database.");
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Root register: " + e.getMessage());
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Root register: Internal server error.");
			}
		}
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Root user registration complete.");
    }
}
