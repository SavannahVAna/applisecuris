import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private String hostname = "localhost";
    private int port = 1234;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    protected String fna;
    protected static String dest;
    protected boolean isAsked = false;
    protected String user_received;
    protected String file_r;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }



    public Client() {}

    public String getFname() {
        return fna;
    }
    public void start() {
        try {
            String message = "";
            String line = "";
            socket = new Socket(hostname, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connecté au serveur de chat");

            // Thread pour écouter les messages du serveur
            new Thread(new ReadMessages(this)).start();

            // Lire les messages de l'utilisateur et les envoyer au serveur
            Scanner scanner = new Scanner(System.in);
            message = "568476548675";
            writer.println(message);
            while (!message.equals("/quit")) {
                message = scanner.nextLine();
                if(isAsked){
                    isAsked = false;
                    if(message.equals("y")){
                        message = "666_ACCEPTED_666 " + user_received;
                        writer.println(message);
                        //System.out.println("under which name should the file be saved?");
                        //file_r = scanner.nextLine();
                    }
                }
                else if (message.startsWith("/sendFile")) {
                    //se préparer a envoyer le fichier
                    message = getRest(message);
                    //nom du fichier en premier nom du destinataire en deuxieme

                    fna = getAction(message);
                    line = getFileName(fna);
                    //InputStream inputStream = new FileInputStream(getAction(message));
                    dest = getRest(message);
                    //System.out.println(fna + " " + dest);
                    //dest = getAction(message);
                    //send message to server to indicate we are sending a file
                    writer.println("555FILE555 "+dest + " " + line);
                    //read file and send it NOT YET

                }
                else {
                    writer.println(message);
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur de connexion au serveur: " + e.getMessage());
        }
    }

    private class ReadMessages implements Runnable {
        private Client client;

        public ReadMessages(Client client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                String message;
                String rt;
                //String fff;
                while ((message = reader.readLine()) != null) {
                    //ad differnt cases for protocol mesages
                    if (message.startsWith("NO_FILE_TRANFER")) {
                        System.out.println("You caant send a file to this user");
                    }
                    else if (message.startsWith("NO_USER")) {
                        System.out.println("This user does not exist");
                    }
                    else if (message.startsWith("TRANSACTION_ACCEPTED")) {
                        sendFile(client.fna , getRest(message));
                    }
                    else if (message.startsWith("555_TRANFER_REQ_555")) {
                        Acknoledge(getRest(message));
                    }
                    else if (message.startsWith("555_TRANSFER_555")) {
                        System.out.println(message);
                        message = getRest(message);
                        rt = getRest(message);
                        //fff = getRest(message);
                        receiveFile(rt);
                    }
                    else {
                        System.out.println(message);
                    }

                }
            } catch (IOException e) {
                System.out.println("Connexion au serveur perdue.");
            }
        }

        private void Acknoledge(String req){
            user_received = getAction(req);
            System.out.println(user_received + " wants to send you a file, accapt? y/n");

            file_r = getRest(req);
            isAsked = true;
        }

        private void sendFile(String filePath, String port) {
            File file = new File(filePath);
            System.out.println("path " +filePath);
            if (!file.exists()) {
                System.out.println("Fichier introuvable.");
                return;
            }

            int por = Integer.parseInt(port);

            try {
                Socket fileSocket = new Socket(hostname, por);
                //create a new socket and connect
                DataOutputStream dataOut = new DataOutputStream(fileSocket.getOutputStream());
                FileInputStream fileIn = new FileInputStream(file);

                //dataOut.writeUTF("555FILE555 " + file.getName() + " " + dest);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }

                fileIn.close();
                dataOut.close();
                //fileSocket.close();

                System.out.println("Fichier envoyé avec succès.");
            } catch (IOException e) {
                System.out.println("Erreur lors de l'envoi du fichier: " + e.getMessage());
            }
        }

        private void receiveFile(String po) {
            int port = Integer.parseInt(po);
            File file = new File(file_r);
            try {
                Socket fileSocket = new Socket(hostname, port);
                //create a new socket and connect
                FileOutputStream dataOut = new FileOutputStream(file);
                DataInputStream fileIn = new DataInputStream(fileSocket.getInputStream());

                //dataOut.writeUTF("555FILE555 " + file.getName() + " " + dest);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    dataOut.write(buffer, 0, bytesRead);
                }

                fileIn.close();
                dataOut.close();
                fileSocket.close();

                System.out.println("Fichier reçu !");
            } catch (IOException e) {
                System.out.println("Erreur lors de la reception " + e.getMessage());
            }
        }


    }

    public static String getFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return new File(path).getName();
    }

    private String getAction(String input){
        int i = input.indexOf(' ');
        return input.substring(0, i);
    }

    private String getRest(String input){
        int i = input.indexOf(' ');
        return input.substring(i+1);
    }

    public static void main(String[] args) {
        new Client().start();
    }



}
