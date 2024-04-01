package pt.unl.fct.di.apdc.projeto.util;

public class RoleData extends UsernameData {
    
    public String role;


    public RoleData() {

    }

    public RoleData(String username, String role) {
        super(username);
        this.role = role;
    }
}
