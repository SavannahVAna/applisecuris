import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Server serv = new Server(1232);
        serv.start();
    }
}
