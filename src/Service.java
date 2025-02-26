import javax.crypto.Cipher;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

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

    private void initService() throws Exception {
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

    private void queryNmae() throws Exception {
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
                                //send challenge
                            if (generateChallenge(user.getPublicKey())) {
                                completed = true;
                                user.setConnected();
                            }

                            else {
                                out.println("wrong key");
                            }
                        }
                        else {
                            out.println(user.getName() + " is already connected");
                        }

                    }
                }
                if (!exist) {
                    out.println("[SERVER] New user detected, please send your public key");
                    //pass = sc.nextLine();
                    //send getpubkey instruction
                    out.println("GET_PUBKEY");
                    Main.serverLogins.add(new Login(nam));
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

    private void mainLoop() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        String input;
        String action;
        String rest;
        String dest;
        String filename;
        boolean no_user;
        String key;
        //boolean no_file;
        boolean transfer;
        String sender;
        User user_send = null;
        User user_received = null;
        while (use) {
            if(sc.hasNextLine()) {
                input = sc.nextLine();

                if (input.startsWith("/msgAll")) {
                    rest = getRest(input);
                    synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                        for (User clientOut : Main.clientUsers) {
                            if (clientOut.getOut() != out) {
                                clientOut.getOut().println(this.name + " : " + rest);
                            }
                        }

                    }
                    Main.broadcastMessage(this.name + " : " + input);
                } else if (input.startsWith("/list")) {
                    out.print("[SERVER] Connected users :");
                    synchronized (Main.clientUsers) {
                        for (User clientOut : Main.clientUsers) {
                            if (!clientOut.getName().equals(name)) {
                                out.print(" " + clientOut.getName() + " ");
                            }
                        }
                    }
                    out.print("\n");
                } else if (input.startsWith("/msgTo")) {
                    rest = getRest(input);
                    action = getAction(rest); //prendre le nom d'utilisateur
                    rest = getRest(rest);//prendre le reste du message
                    synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                        for (User client : Main.clientUsers) {
                            if (client.getName().equals(action)) {
                                client.getOut().println("[whisper] " + this.name + " : " + rest);
                            }
                        }

                    }
                } else if (input.startsWith("555FILE555")) {
                    //se préparer a recevoir le fichier et le transférer
                    rest = getRest(input);
                    //filename = getAction(rest);
                    //rest = getRest(rest);
                    //filename = getAction(rest);

                    dest = getAction(rest);
                    rest = getRest(rest);
                    filename = getRest(rest);
                    //out.println(dest);
                    //out.println(filename + " : " + dest);
                    //dest = getAction(rest);
                    transfer = false;
                    //no_file = false;
                    no_user = true;
                    //check to see if dest can receive a file
                    synchronized (Main.clientUsers) {
                        for (User client : Main.clientUsers) {
                            if (client.getName().equals(dest)) {
                                no_user = false;
                                if (client.isFileable()) {
                                    transfer = true;
                                    user_received = client;
                                } else {
                                    out.println("NO_FILE_TRANFER");
                                }
                            }
                        }
                        if (no_user) {
                            out.println("NO_USER");
                        }
                    }
                    if (transfer) {
                        user_received.getOut().println("555_TRANFER_REQ_555 " + name + " " + filename);
                    }
                } else if (input.startsWith("666_ACCEPTED_666")) {
                    //si transaction acceptée alors envoyer fichier?
                    sender = getRest(input);
                    user_send = null;
                    for (User client : Main.clientUsers) {
                        if (client.getName().equals(sender)) {
                            user_send = client;
                        }
                    }
                    //open socket
                    if (user_send != null) {
                        handleTransfer(user_send);
                    }

                } else if (input.startsWith("PUBKEY")) {
                    //prend l'input de la public key et la met dans le login
                    key = getRest(input);
                    System.out.println("received key : " +key);
                    setKey(key);
                } else if (input.startsWith("/quit")) {
                    use = false;
                    out.println("[SERVER] Goodbye!");
                    synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                        for (User clientOut : Main.clientUsers) {
                            if (clientOut.getOut() != out) {
                                clientOut.getOut().println(this.name + " has left the chat");
                            }
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
                } else {
                    synchronized (Main.clientUsers) {  // Synchroniser l'accès à la liste
                        for (User clientOut : Main.clientUsers) {
                            if (clientOut.getOut() != out) {
                                clientOut.getOut().println(this.name + " : " + input);
                            }
                        }

                    }
                    Main.broadcastMessage(this.name + " : " + input);
                }
            }
        }
    }

    private void handleTransfer(User sender) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        //start new connection and send port

        //ask receiver whether he wants to receive the file

        ServerSocket s = new ServerSocket(0);
        sender.getOut().println("TRANSACTION_ACCEPTED "+s.getLocalPort());
        Socket soc2 = s.accept();
        DataInputStream dataIn = new DataInputStream(soc2.getInputStream());
        out.println("555_TRANSFER_555 "+s.getLocalPort());
        Socket soc3 = s.accept();
        DataOutputStream dataOut = new DataOutputStream(soc3.getOutputStream());
        while ((bytesRead = dataIn.read(buffer)) != -1) {
            dataOut.write(buffer, 0, bytesRead);
        }
        dataOut.close();
        soc3.close();
        dataIn.close();
        soc2.close();
        s.close();


    }
    //change it bc it goes to mainloop instead
    private boolean askUser(User user) throws IOException {
        user.getOut().println("555_TRANFER_REQ_555 "+ name);
        //user.getOut().println(name + " wants to send you a file, accept? y/n");
        return user.getIn().readLine().equals("666_ACCEPTED_666");
    }

    private void setKey(String encodedKey) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        //String cleanedKey = encodedKey.replaceAll("\\s", "");
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey key =  keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        synchronized (Main.serverLogins) {
            for (Login login : Main.serverLogins) {
                if(login.getName().equals(name)){
                    login.setPublicKey(key);
                }
            }
        }
    }

    private boolean generateChallenge(PublicKey pub) throws Exception {
        Random r = new Random((new Date()).getTime());
        byte[] challengeBytes = new byte[64];
        r.nextBytes(challengeBytes);
        challengeBytes[0] = (byte)((byte) (new Random().nextInt(0x8f + 0x01)));//ça a changé donc be careful
        System.out.println("Generated challenge:\n" + Base64.getEncoder().encodeToString(challengeBytes) + "\n");
        String y = Base64.getEncoder().encodeToString(challengeBytes);
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, pub);
        byte[] ciphered = cipher.doFinal(challengeBytes);

        System.out.println("Encrypted challenge:\n" + Base64.getEncoder().encodeToString(ciphered) + "\n");

        String cha =  Base64.getEncoder().encodeToString(ciphered);
        out.println("CHALLENGE_SERVER "+cha);
        String decede = sc.nextLine();
        System.out.println("Decrypted challenge:\n" + decede);

        return decede.equals(y);
    }

    public String encryptChallenge(byte[] challengeBytes, PublicKey pub) throws Exception {
        // Utilisation du provider par défaut
        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");

        cipher.init(Cipher.ENCRYPT_MODE, pub);
        byte[] ciphered = cipher.doFinal(challengeBytes);

        System.out.println("Encrypted challenge:\n" + Base64.getEncoder().encodeToString(ciphered) + "\n");

        return Base64.getEncoder().encodeToString(ciphered);
    }

    public void run() {
        try {
            initService();
        } catch (Exception e) {
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
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
