public class Login {
    String name;
    String pass;
    boolean connected;
    public Login(String name, String pass) {
        this.name = name;
        this.pass = pass;
        this.connected = true;
    }
    public String getName() {
        return name;
    }
    public String getPass() {
        return pass;
    }
    public boolean isConnected() {return connected;}

    public void setConnected() {
        this.connected = !connected;
    }
}
