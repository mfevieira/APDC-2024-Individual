package pt.unl.fct.di.apdc.projeto.util;

public class ChangeData {
    
    public String username;

	public String password;
	
	public String email;
	
	public String name;

	public String phone;

    public String profile;

    public String work;

    public String workPlace;

    public String address;

    public String postalCode;

    public String fiscal;

    public String role;

    public String state;

    public AuthToken token;
	
	public ChangeData() {
		
	}
	
	public ChangeData(String username, String password, String email, String name, String phone, String profile, String work, String workPlace, String address, String postalCode, String fiscal, String role, String state, AuthToken token) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.name = name;
		this.phone = phone;
        this.profile = profile;
        this.work = work;
        this.workPlace = workPlace;
        this.address = address;
        this.postalCode = postalCode;
        this.fiscal = fiscal;
        this.role = role;
        this.state = state;
        this.token = token;
	}
}