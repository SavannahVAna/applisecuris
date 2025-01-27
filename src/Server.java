import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private ServerSocket serverSocket;
    protected static ArrayList<PrintStream> clientOutputs;
    protected static ArrayList<String> clientNames;
    static final int nbudserma = 100;
    protected static int nbusers;
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        clientOutputs = new ArrayList<>();
        clientNames = new ArrayList<>();
        nbusers = 0;
    }

    public void start() {
        System.out.println("Server started on port " + serverSocket.getLocalPort());
        while (true) {
            try {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected");

                if (nbusers < nbudserma)  {

                    // Lancer un nouveau thread pour chaque client
                    Thread clientThread = new Thread(new Service(clientSocket));
                    clientThread.start();
                }
                else {
                    PrintStream out = new PrintStream(clientSocket.getOutputStream());
                    out.println("nombre maximum de clients atteints ressayez plus tard");
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}

