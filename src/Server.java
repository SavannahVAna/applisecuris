import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private ServerSocket serverSocket;

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);

    }

    public void start() {
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


}

