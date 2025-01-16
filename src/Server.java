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
    static final int nbudserma = 100;
    protected static int nbusers;
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        clientOutputs = new ArrayList<>();
        nbusers = 0;
    }

    public void start() {
        System.out.println("Server started...");
        while (true) {
            try {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected");
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintStream out = new PrintStream(clientSocket.getOutputStream());
                if (nbusers < nbudserma)  {
                    synchronized (clientOutputs) {  // Protéger l'accès à la liste
                        clientOutputs.add(out);
                        nbusers++;
                    }
                    // Lancer un nouveau thread pour chaque client
                    Thread clientThread = new Thread(new Service(in, out));
                    clientThread.start();
                }
                else {
                    out.println("nombre maximum de clients atteints ressayez plus tard");
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

