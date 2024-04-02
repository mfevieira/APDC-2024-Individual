package pt.unl.fct.di.apdc.projeto.util;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000*60*60*2; //2h
	
	public String username;
	public String role;
	public String tokenID;
	public long creationDate;
	public long expirationDate;
	
	public AuthToken() {
		
	}

	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.tokenID = UUID.randomUUID().toString();
		this.creationDate = System.currentTimeMillis();
		this.expirationDate = this.creationDate + AuthToken.EXPIRATION_TIME;
	}

	/**
	 * Method to check if the token is still valid.
	 * @param tokenID the tokenID store in the database.
	 * @param role the role of the user attempting to use this token.
	 * @return 1 if the token is still valid, 0 if the time has run out, -1 if the role is different and -2 if the tokenID is false.
	 */
	public int isStillValid(String tokenID, String role) {
		if ( tokenID != this.tokenID ) {
			return -2;
		} else if ( !role.equals(this.role) ) {
			return -1;
		} else if ( System.currentTimeMillis() >= this.expirationDate ) {
			return 0;
		} else {
			return 1;
		}
	}
}
