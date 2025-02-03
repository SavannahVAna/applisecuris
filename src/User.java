import java.io.BufferedReader;
import java.io.PrintStream;

public class User {
    private PrintStream out;
    private BufferedReader in;
    private String name;

    public User(PrintStream out, BufferedReader in, String name) {
        this.out = out;
        this.in = in;
        this.name = name;
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
}
