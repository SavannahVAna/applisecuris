import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Service implements Runnable {
    private Scanner sc;
    private PrintStream out;
    private BufferedReader in;
    private Socket socket;
    //private Server server;
    private boolean use = true;
    //private static ArrayList<PrintStream> clientOutputs;
    public Service( Socket clientSocket) throws IOException {
        this.in= new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintStream(clientSocket.getOutputStream());
        this.sc = new Scanner(in);
        synchronized (Server.clientOutputs) {  // Protéger l'accès à la liste
            Server.clientOutputs.add(out);
            Server.nbusers++;
        }
    }
    public Service(){

    }

    public void run() {
        out.println("hello, welcome to the chat server!");
        String input;
        while (use) {
            input = sc.nextLine();
            if (!input.equals("exit")) {
            synchronized (Server.clientOutputs) {  // Synchroniser l'accès à la liste
                for (PrintStream clientOut : Server.clientOutputs) {
                    if(clientOut != out){
                    clientOut.println(input);}
                }
            }}
            else {
                use = false;
                synchronized (Server.clientOutputs) {
                    Server.clientOutputs.remove(out);  // Retirer le flux lors de la déconnexion*
                    Server.nbusers--;
                }
                out.close();
            }
        }
    }
}
