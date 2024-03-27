package pt.unl.fct.di.apdc.projeto.util;

public class OptionalRegisterData extends RegisterData {

    public String profile;

    public String work;

    public String workPlace;

    public String address;

    public String postalCode;

    public String fiscal;

    //public File photo;
    
    public OptionalRegisterData() {

    }

    public OptionalRegisterData(String username, String password, String confirmation, String email, String name, String phone, String profile, String work, String workPlace, String address, String postalCode, String fiscal) {
        super(username, password, confirmation, email, name, phone);
        this.profile = profile;
        this.work = work;
        this.workPlace = workPlace;
        this.address = address;
        this.postalCode = postalCode;
        this.fiscal = fiscal;
    }

    /**
	 * Method to check if the data is valid for registry.
	 * @return true if all the relevant data fields are valid, false otherwise.
	 */
    public boolean validRegistration() {
        if ( this.username == null || this.phone == null || this.invalidEmail() || this.invalidName() || this.invalidPassword() || this.invalidOptionals() ) {
			return false;
		} else {
			return true;
		}
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
