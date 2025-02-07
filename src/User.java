import java.io.BufferedReader;
import java.io.PrintStream;

public class User {
    private PrintStream out;
    private BufferedReader in;
    private String name;
    private boolean fileable = false;

    public User(PrintStream out, BufferedReader in, String name, boolean fileable) {
        this.out = out;
        this.in = in;
        this.name = name;
        this.fileable = fileable;
    }

    public String getName() {
        return name;
    }

    public BufferedReader getIn() {
        return in;
    }

    public PrintStream getOut() {
        return out;
    }

    public void setFileable() {
        fileable = true;
    }

    public boolean isFileable() {
        return fileable;
    }
}
