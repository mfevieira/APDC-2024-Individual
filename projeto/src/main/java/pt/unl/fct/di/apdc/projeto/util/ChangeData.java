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
	
	/**
	 * Method to check if the data is valid for registry.
	 * @return true if all the data fields are not null and the password and confirmation password are the same, false otherwise.
	 */
	public boolean validRegistration() {
		if ( this.username == null || this.username.trim().isEmpty() || this.phone == null || this.phone.trim().isEmpty() || this.invalidEmail() || this.invalidName() || this.invalidPassword() || this.invalidOptionals() ) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Method to check if the email provided is invalid.
	 * Email is invalid if it's null, has no @ or more than 1 @ or if it has no domain.
	 * @return true if the email is invalid, false otherwise.
	 */
	protected boolean invalidEmail() {
		if (this.email == null || this.email.trim().isEmpty()) {
			return true;
		}
		String[] parts = this.email.split("@");
		if (parts.length != 2) {
			return true;
		}
		String domain = parts[1];
		String[] domainParts = domain.split("\\.");
		if (domainParts.length < 2) {
			return true;
		}
		for (String part : domainParts) {
			if (part.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method to check if the name provided is invalid.
	 * Name is invalid if the user provides only one name, or if it's null.
	 * @return true if the name is invalid, false otherwise.
	 */
	protected boolean invalidName() {
		if ( this.name == null )
			return true;
		String[] name = this.name.split(" ");
		if ( name.length < 2 )
			return true;
		return false;
	}

	/**
	 * Method to check if the password is invalid.
	 * Password is invalid if it's null, the confirmation password is null, the password and confirmation don't match,
	 * the password has fewer than 10 characters, has only lower case, only upper case characters or has fewer than 4 numbers.
	 * @return true if the password is invalid, false otherwise.
	 */
	protected boolean invalidPassword() {
		if ( this.password == null )
			return true;
		if ( this.password.length() < 10 || 
			this.password.equals(this.password.toLowerCase()) || 
			this.password.equals(this.password.toUpperCase()) ||
			this.invalidPasswordNumbers() )
			return true;
		return false;
	}

	/**
	 * Method to check if the password has less than 4 numbers.
	 * @return true if the password has less than 4 numbers, false otherwise.
	 */
	protected boolean invalidPasswordNumbers() {
		String password = this.password;
		int passwordCount = password.length();
		int numberCount = 0;
		for ( int i = 0; i < passwordCount; i++ ) {
			for ( int j = 0; j < 10; j++ ) {
				if ( password.charAt(i) == (char) ('0' + j) ) {
					numberCount++;
					break;
				}
				if ( numberCount > 3 )
					return false;
			}
		}
		return true;
	}

    /**
     * Method to check if the optional registry data supplied is invalid.
     * @return true if the optional data is invalid, false otherwise.
     */
    protected boolean invalidOptionals() {
        if ( this.profile != null ) {
            if ( !this.profile.equals("PUBLIC") && !this.profile.equals("PRIVATE") ) {
                return true;
            }
        }
        if ( this.postalCode != null ) {
            String[] format = this.postalCode.split("-");
            if ( format.length != 2 || format[0].length() != 4 || format[1].length() != 3 )
                return true;
        }
        if ( this.fiscal != null ) {
            if ( this.fiscal.length() != 9 )
                return true;
        }
        return false;
    }
}