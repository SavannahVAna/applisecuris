import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Main {
    private static ServerSocket serverSocket;
    static final int nbudserma = 100;
    protected static int nbusers;
    protected static ArrayList<User> clientUsers;
    protected static ArrayList<Login> serverLogins;
    public static void main(String[] args) throws IOException {
        clientUsers = new ArrayList<>();
        if (args.length == 0) {
            serverSocket = new ServerSocket(1234);
        }
        else {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        }
        nbusers = 0;

        System.out.println("Server started on port " + serverSocket.getLocalPort());
        while (true) {
            try {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected");

                // Lancer un nouveau thread pour chaque client
                Thread clientThread = new Thread(new Service(clientSocket));
                clientThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public static void broadcastMessage(String message) {
        System.out.println(message);
    }
    public static void printConnect(String message) {
        System.out.println(message + " connected");
    }

    private void initLogin() {
        serverLogins = new ArrayList<>();

    }

}
