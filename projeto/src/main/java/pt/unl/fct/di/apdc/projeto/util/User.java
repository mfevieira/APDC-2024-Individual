package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.Timestamp;

public class User {
    
    public String username;

	public String password;
	
	public String email;
	
	public String name;

	public String phone;

    public String profile;

    public String work;

    public String workplace;

    public String address;

    public String postalcode;

    public String fiscal;

    public String role;

    public String state;

    public Timestamp userCreationTime;

    public String tokenID;


    public User() {

    }

    public User(String username, String password, String email, String name, String phone, String profile, String work, String workplace, 
                String address, String postalcode, String fiscal, String role, String state, Timestamp userCreationTime, String tokenID) {
        this.username = username;
		this.password = password;
		this.email = email;
		this.name = name;
		this.phone = phone;
        this.profile = profile;
        this.work = work;
        this.workplace = workplace;
        this.address = address;
        this.postalcode = postalcode;
        this.fiscal = fiscal;
        this.role = role;
        this.state = state;
        this.userCreationTime = userCreationTime;
        this.tokenID = tokenID;
    }
}
