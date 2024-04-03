package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

public class ServerConstants {

    public static final String USER = "USER", GBO = "GBO", GA = "GA", SU = "SU";

	public static final String ACTIVE = "ACTIVE", INACTIVE = "INACTIVE";

    public static final String PUBLIC = "PUBLIC", PRIVATE = "PRIVATE";

    public static final Datastore datastore = DatastoreOptions.newBuilder().setProjectId("apdc-64320").setHost("localhost:8081").build().getService();

    //public static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
}
