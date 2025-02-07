import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Service implements Runnable {
    private Scanner sc;
    private PrintStream out;
    private BufferedReader in;
    private String name;
    private User user;
    private boolean firstmesage = true;
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
        boolean f = false;
        if (Main.nbusers > Main.nbudserma)  {
            PrintStream out = new PrintStream(socket.getOutputStream());
            out.println("nombre maximum de clients atteints ressayez plus tard");
            out.close();
            in.close();
        }
        this.sc = new Scanner(in);
        //check here?
        if (sc.hasNextLine()) {
            f = checkClient(sc.nextLine());
        }
        queryNmae();
        this.user = new User(out,in,name,f);
        synchronized (Main.clientUsers) {  // Protéger l'accès à la liste
            Main.clientUsers.add(user);
            Main.nbusers++;
        }
        Main.printConnect(name);
    }

    private void queryNmae(){
        boolean exist;
        boolean completed = false;
        String nam = null;
        String pass = null;
        synchronized (Main.serverLogins) {
            do {
                out.println("enter your name");
                nam = sc.nextLine();
                exist = false;
                for (Login user : Main.serverLogins) {
                    if (nam.equals(user.getName())) {
                        exist = true;


                        if (!user.isConnected()) {
                            out.println("welcome back ! \nPlease enter your password : ");
                            pass = sc.nextLine();
                            if (pass.equals(user.getPass())){
                                completed = true;
                                user.setConnected();
                            }
                            else {
                                out.println("wrong password");
                            }
                        }
                        else {
                            out.println(user.getName() + " is already connected");
                        }

                    }
                }
                if (!exist) {
                    out.println("welcome to the server, please set a password : ");
                    pass = sc.nextLine();
                    Main.serverLogins.add(new Login(nam,pass));
                    completed = true;
                }
            } while (!completed);

            this.name = nam;
        }
    }

    private void checkLogin(){

    }

    private boolean checkClient(String input){
        return input.equals("568476548675");
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

    private void disconnect(){
        synchronized (Main.serverLogins) {
            for (Login user : Main.serverLogins) {
                if(user.getName().equals(name)) {
                    user.setConnected();
                }
            }
        }
    }

    private void mainLoop() throws IOException {
        String input;
        String action;
        String rest;
        String dest;
        String filename;
        boolean transfer;
        User user_received = null;
        while (use) {
            input = sc.nextLine();

            if (input.startsWith("/msgAll")) {
                rest = getRest(input);
                synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                    for (User clientOut : Main.clientUsers) {
                        if(clientOut.getOut() != out){
                            clientOut.getOut().println(this.name + " : " + rest);}
                    }

                }
                Main.broadcastMessage(this.name + " : " + input);
            }
            else if (input.startsWith("/list")) {
                out.print("[SERVER] Connected users :");
                synchronized (Main.clientUsers) {
                    for (User clientOut : Main.clientUsers) {
                        if (!clientOut.getName().equals(name)) {
                            out.print(" " + clientOut.getName() + " ");
                        }
                    }
                }
                out.print("\n");
            }
            else if (input.startsWith("/msgTo")) {
                rest = getRest(input);
                action = getAction(rest); //prendre le nom d'utilisateur
                rest = getRest(rest);//prendre le reste du message
                synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                    for (User client : Main.clientUsers) {
                        if(client.getName().equals(action)){
                            client.getOut().println("[whisper] " + this.name + " : " + rest);}
                    }

                }
            }
            else if(input.startsWith("555FILE555")) {
                //se préparer a recevoir le fichier et le transférer
                rest = getRest(input);
                filename = getAction(rest);
                rest = getRest(rest);
                dest = getAction(rest);
                transfer = false;
                //check to see if dest can receive a file
                synchronized (Main.clientUsers) {
                    for (User client : Main.clientUsers) {
                        if(client.getName().equals(dest)){
                            if(client.isFileable()){
                                transfer = true;
                                user_received = client;

                            }
                            else {
                                out.println("NO_FILE_TRANFER");
                            }
                        }else {
                            out.println("NO_USER");
                        }
                    }
                }
                if(transfer){
                    handleTransfer(user_received,filename);
                }
            }
            else if (input.startsWith("/quit")){
                use = false;
                out.println("[SERVER] Goodbye!");
                synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                    for (User clientOut : Main.clientUsers) {
                        if(clientOut.getOut() != out){
                            clientOut.getOut().println(this.name + " has left the chat");}
                    }

                }
                Main.broadcastMessage(this.name + " has left the room");
                Main.nbusers--;
                remove();
                disconnect();
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

    private void handleTransfer(User receiver, String filen) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        //start new connection and send port
        ServerSocket s = new ServerSocket(0);
        out.println(s.getLocalPort());
        Socket soc2 = s.accept();
        DataInputStream dataIn = new DataInputStream(soc2.getInputStream());
        //ask receiver whether he wants to receive the file
        if(askUser(receiver)){
            user.getOut().println(s.getLocalPort() + " " + filen);
            Socket soc3 = s.accept();
            DataOutputStream dataOut = new DataOutputStream(soc3.getOutputStream());
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
            dataOut.close();
            soc3.close();
        }
        dataIn.close();
        soc2.close();
        s.close();

    }

    private boolean askUser(User user) throws IOException {
        user.getOut().println(name + " wants to send you a file, accept? y/n");
        return user.getIn().readLine().equals("y");
    }

    public void run() {
        try {
            initService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        out.println("[SERVER] hello, welcome to the chat server!, " + Main.nbusers + " connectés, /quit to leave");
        out.print("[SERVER] Connected users :");
        synchronized (Main.clientUsers) {
            for (User clientOut : Main.clientUsers) {
                if (!clientOut.getName().equals(name)) {
                    out.print(" " + clientOut.getName() + " ");
                }
            }
        }
        out.print("\n");
        synchronized (Main.clientUsers) {
            for (User clientOut : Main.clientUsers) {
                if(clientOut.getOut() != out){
                    clientOut.getOut().println(this.name + " just joined the chat!" );}
            }
        }
        try {
            mainLoop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
