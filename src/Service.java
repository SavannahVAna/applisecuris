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
    private String name;
    //private Server server;
    private boolean use = true;
    //private static ArrayList<PrintStream> clientOutputs;
    public Service( Socket clientSocket) throws IOException {
        this.in= new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintStream(clientSocket.getOutputStream());
        if (Main.nbusers > Main.nbudserma)  {
            PrintStream out = new PrintStream(clientSocket.getOutputStream());
            out.println("nombre maximum de clients atteints ressayez plus tard");
            out.close();
            in.close();
        }

            this.sc = new Scanner(in);
        queryNmae();
        synchronized (Main.clientOutputs) {  // Protéger l'accès à la liste
            Main.clientOutputs.add(out);
            Main.nbusers++;
            Main.clientNames.add(name);
        }
        Main.printConnect(name);
    }
    public Service(){

    }

    private void queryNmae(){
        boolean correct = false;
        String nam = null;
        synchronized (Main.clientNames) {
            do {
                out.println("enter your name");
                nam = sc.nextLine();
                correct = true;
                for (String user : Main.clientNames) {
                    if (nam.equals(user)) {
                        correct = false;
                        out.println("name already in use please choose another");
                    }
                }
            } while (!correct);

            this.name = nam;
        }
    }

    private void removeName(){
        synchronized (Main.clientNames) {
            Main.clientNames.remove(name);
        }
    }

    public void run() {
        out.println("hello, welcome to the chat server!, " + Main.nbusers + " connectés, exit to leave");
        String input;
        while (use) {
            input = sc.nextLine();
            if (!input.equals("exit")) {
            synchronized (Main.clientOutputs) {  // Synchroniser l'accès à la liste
                for (PrintStream clientOut : Main.clientOutputs) {
                    if(clientOut != out){
                    clientOut.println(this.name + " : " + input);}
                }

            }
                Main.broadcastMessage(this.name + " : " + input);
            }
            else {
                use = false;
                Main.broadcastMessage(this.name);
                synchronized (Main.clientOutputs) {
                    Main.clientOutputs.remove(out);  // Retirer le flux lors de la déconnexion*
                    Main.nbusers--;
                }
                removeName();
                out.close();
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
