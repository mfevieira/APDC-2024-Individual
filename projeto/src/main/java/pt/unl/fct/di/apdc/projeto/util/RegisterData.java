package pt.unl.fct.di.apdc.projeto.util;

public class RegisterData extends LoginData {
	
	public String confirmation;
	
	public String email;
	
	public String name;

	public String phone;
	
	public RegisterData() {
		
	}
	
	public RegisterData(String username, String password, String confirmation, String email, String name, String phone) {
		super(username, password);
		this.confirmation = confirmation;
		this.email = email;
		this.name = name;
		this.phone = phone;
	}
	
	/**
	 * Method to check if the data is valid for registry.
	 * @return true if all the data fields are not null and the password and confirmation password are the same, false otherwise.
	 */
	public boolean validRegistration() {
		if ( this.username == null || this.phone == null || this.invalidEmail() || this.invalidName() || this.invalidPassword() ) {
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
		if ( this.email == null )
			return true;
		String[] format1 = this.email.split("@");
		if ( format1.length != 2 )
			return true;
		String[] format2 = format1[1].split(".");
		if ( format2.length < 2 )
			return true;
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
		if ( this.password == null || this.confirmation == null || !this.password.equals(this.confirmation) )
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
}