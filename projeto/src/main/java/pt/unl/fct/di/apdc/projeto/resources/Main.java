package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.UserConstants;

public class Main {

	/** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** The data store to store users in */
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	/** The User kind key factory */
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public static void main(String[] args) {
        LOG.fine("Startup: attempt to register root user.");
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = userKeyFactory.newKey("root");
			if (txn.get(userKey) == null) {
				AuthToken token = new AuthToken("root", UserConstants.SU);
				Entity user = Entity.newBuilder(userKey)
						.set("username", "root")
						.set("password", DigestUtils.sha3_512Hex("Super_Password_85264"))
						.set("email", "mfe.vieira@campus.fct.unl.pt")
						.set("name", "Root User")
						.set("phone", "")
						.set("profile", UserConstants.PRIVATE)
						.set("work", "Root User")
						.set("workplace", "FCT UNL")
						.set("address", "")
						.set("postalCode", "")
						.set("fiscal", "")
						.set("role", UserConstants.SU)
						.set("state", UserConstants.ACTIVE)
						.set("userCreationTime", Timestamp.now())
						.set("tokenID", StringValue.newBuilder(token.tokenID).setExcludeFromIndexes(true).build())
						.build();
				txn.add(user);
				txn.commit();
				LOG.fine("Startup: root user was registered in the database.");
			} else {
				txn.rollback();
				LOG.fine("Startup: root user already registered.");
			}
		} catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Startup: " + e.getMessage());
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Startup: Internal server error.");
			}
		}
    }
}
