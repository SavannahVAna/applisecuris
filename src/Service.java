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
    private User user;
    //private Server server;
    private Socket socket;
    private boolean use = true;
    //private static ArrayList<PrintStream> clientOutputs;
    public Service( Socket clientSocket) throws IOException {

        this.socket = clientSocket;

    }

    private void initService() throws IOException {
        this.in= new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintStream(socket.getOutputStream());
        if (Main.nbusers > Main.nbudserma)  {
            PrintStream out = new PrintStream(socket.getOutputStream());
            out.println("nombre maximum de clients atteints ressayez plus tard");
            out.close();
            in.close();
        }
        this.sc = new Scanner(in);
        queryNmae();
        this.user = new User(out,in,name);
        synchronized (Main.clientUsers) {  // Protéger l'accès à la liste
            Main.clientUsers.add(user);
            Main.nbusers++;
        }
        Main.printConnect(name);
    }

    private void queryNmae(){
        boolean correct = false;
        String nam = null;
        synchronized (Main.clientUsers) {
            do {
                out.println("enter your name");
                nam = sc.nextLine();
                correct = true;
                for (User user : Main.clientUsers) {
                    if (nam.equals(user.getName())) {
                        correct = false;
                        out.println("name already in use please choose another");
                    }
                }
            } while (!correct);

            this.name = nam;
        }
    }

    private void checkLogin(){

    }

    private void remove(){
        synchronized (Main.clientUsers) {
            Main.clientUsers.remove(user);
        }
    }

    private String getAction(String input){
        int i = input.indexOf(' ');
        return input.substring(0, i);
    }

    private String getRest(String input){
        int i = input.indexOf(' ');
        return input.substring(i+1);
    }

    private void mainLoop() throws IOException {
        String input;
        String action;
        String rest;
        while (use) {
            input = sc.nextLine();
            action = getAction(input);
            rest = getRest(input);
            if (action.equals("/msgAll")) {
                synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                    for (User clientOut : Main.clientUsers) {
                        if(clientOut.getOut() != out){
                            clientOut.getOut().println(this.name + " : " + rest);}
                    }

                }
                Main.broadcastMessage(this.name + " : " + input);
            }
            else if (action.equals("/list")) {
                out.print("[SERVER] Connected users :");
                synchronized (Main.clientUsers) {
                    for (User clientOut : Main.clientUsers) {
                        if (!clientOut.getName().equals(name)) {
                            out.print(" " + clientOut.getName() + " ");
                        }
                    }
                }
            }
            else if (action.equals("/msgTo")) {
                action = getAction(rest); //prendre le nom d'utilisateur
                rest = getRest(rest);//prendre le reste du message
                synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                    for (User client : Main.clientUsers) {
                        if(client.getName().equals(action)){
                            client.getOut().println(this.name + " : " + rest);}
                    }

                }
            }
            else if (action.equals("/quit")){
                use = false;
                out.println("[SERVER] Goodbye!");
                synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                    for (User clientOut : Main.clientUsers) {
                        if(clientOut.getOut() != out){
                            clientOut.getOut().println(this.name + " has left the chat");}
                    }

                }
                Main.broadcastMessage(this.name + "has left the room");
                Main.nbusers--;
                remove();
                out.close();
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                socket.close();
            }
            else {
                synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                    for (User clientOut : Main.clientUsers) {
                        if(clientOut.getOut() != out){
                            clientOut.getOut().println(this.name + " : " + input);}
                    }

                }
                Main.broadcastMessage(this.name + " : " + input);
            }
        }
    }

    public void run() {
        try {
            initService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        out.println("[SERVER] hello, welcome to the chat server!, " + Main.nbusers + " connectés, exit to leave");
        out.print("[SERVER] Connected users :");
        synchronized (Main.clientUsers) {
            for (User clientOut : Main.clientUsers) {
                if (!clientOut.getName().equals(name)) {
                    out.print(" " + clientOut.getName() + " ");
                }
            }
        }
        try {
            mainLoop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
