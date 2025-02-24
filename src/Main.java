import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.Scanner;

public class Main {
    private static ServerSocket serverSocket;
    static final int nbudserma = 100;
    protected static int nbusers;
    protected static ArrayList<User> clientUsers;
    protected static ArrayList<Login> serverLogins;
    public static void main(String[] args) throws IOException {
        clientUsers = new ArrayList<>();
        serverLogins = new ArrayList<>();
        nbusers = 0;
        loadBdd();
        if (args.length == 0) {
            serverSocket = new ServerSocket(1234);
        }
        else {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
        }


        System.out.println("Server started on port " + serverSocket.getLocalPort());
        new Thread(new ServerConsole()).start();
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


    }

    private static synchronized void saveBdd() {

        String bddFile = "database.txt";
        try {
            new File(bddFile).delete();

            BufferedWriter writer = new BufferedWriter(new FileWriter(bddFile));
            for (Login client: serverLogins) {
                writer.write(client.getName() + ":" + client.getPass() + ":" + client.getPublicKeyString() + "\n");
            }
            writer.close();

        } catch (IOException e) {
            e.getMessage();
        }
    }

    private static synchronized void loadBdd() {


        String bddFile = "database.txt";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(bddFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] splitLine = line.split(":");
                Login log = new Login(splitLine[0], splitLine[1], getKey(splitLine[2]));
                log.setConnected();
                serverLogins.add(log);
            }
            reader.close();


        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.getMessage();
        }
    }

    public static PublicKey getKey(String encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
    }

    public static void shutdownServer() {
        saveBdd();
        try {
            synchronized (clientUsers) {
                for (User client : clientUsers) {
                    client.getOut().println("[SYSTEM] Server is shutting down.");
                    client.getOut().close();
                    client.getIn().close();
                }
                clientUsers.clear();
            }
            serverSocket.close();
            System.out.println("[SYSTEM] Server has shut down.");
            System.exit(0); // Arrête le programme
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

class ServerConsole implements Runnable {
    @Override
    public void run() {
        String uname;
        int minu;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                if (message.startsWith("/list")) {
                    System.out.print("Connected users :");
                    synchronized (Main.clientUsers) {
                        for (User clientOut : Main.clientUsers) {
                            System.out.print(" " + clientOut.getName() + " ");
                        }
                    }
                    System.out.print("\n");
                }
                else if (message.startsWith("/kill")) {
                    uname = message.split(" ")[1];
                    synchronized (Main.clientUsers) {
                        Iterator<User> iterator = Main.clientUsers.iterator();
                        while (iterator.hasNext()) {
                            User clientOut = iterator.next();
                            if (clientOut.getName().equals(uname)) {
                                clientOut.getOut().println("you've been kicked");
                                iterator.remove(); // Supprimer l'utilisateur de la liste
                                clientOut.getOut().close();
                                try {
                                    clientOut.getIn().close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }

                else if (message.startsWith("/shutdown")) {
                    minu = Integer.parseInt(message.split(" ")[1]);
                    //a compléter
                    broadcastShutdownWarning(minu);
                } else {
                    synchronized (Main.clientUsers) {
                        for (User client : Main.clientUsers) {
                            client.getOut().println("[SYSTEM] " + message);
                        }
                    }
                }
            }
        }
    }
    private void broadcastShutdownWarning(int minutes) {
        new Thread(() -> {
            try {
                for (int i = minutes; i > 0; i--) {
                    synchronized (Main.clientUsers) {
                        for (User client : Main.clientUsers) {
                            client.getOut().println("[SYSTEM] Server will shut down in " + i + " minute(s).");
                        }
                    }
                    System.out.println("[SYSTEM] Server will shut down in " + i + " minute(s).");
                    Thread.sleep(60000 * minutes); // Attendre 1 minute
                }

                // Informer tout le monde que le serveur s'arrête
                synchronized (Main.clientUsers) {
                    for (User client : Main.clientUsers) {
                        client.getOut().println("[SYSTEM] Server is shutting down now!");
                    }
                }
                System.out.println("[SYSTEM] Server is shutting down now!");

                // Fermer proprement les connexions
                Main.shutdownServer();

            } catch (InterruptedException e) {
                System.out.println("[SYSTEM] Shutdown interrupted.");
            }
        }).start();
    }




}
