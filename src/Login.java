import java.security.PublicKey;
import java.util.Base64;

public class Login {
    String name;
    String pass;
    boolean connected;
    PublicKey publicKey;
    public Login(String name, String pass) {
        this.name = name;
        this.pass = pass;
        this.connected = true;
    }

    public Login(String name) {
        this.name = name;
        this.connected = true;
    }

    public Login(String name, String pass, PublicKey publicKey) {
        this.name = name;
        this.pass = pass;
        this.connected = true;
        this.publicKey = publicKey;
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

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyString() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}
