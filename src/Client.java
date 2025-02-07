import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private String hostname = "localhost";
    private int port = 1234;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public Client() {}

    public void start() {
        try {
            String message = "";
            socket = new Socket(hostname, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connecté au serveur de chat");

            // Thread pour écouter les messages du serveur
            new Thread(new ReadMessages()).start();

            // Lire les messages de l'utilisateur et les envoyer au serveur
            Scanner scanner = new Scanner(System.in);
            message = "568476548675";
            writer.println(message);
            while (!message.equals("/quit")) {
                message = scanner.nextLine();
                writer.println(message);
                if (message.startsWith("/sendFile")) {
                    //se préparer a envoyer le fichier
                    message = message.substring("/sendFile".length() +1);
                    //nom du fichier en premier nom du destinataire en deuxieme
                    try {
                        InputStream inputStream = new FileInputStream(getAction(message));
                        message = getRest(message);
                        String dest = getAction(message);
                        message = getRest(message);
                        //send message to server to indicate we are sending a file
                        writer.println("555FILE555");
                        //read file and send it
                    } catch (FileNotFoundException e) {
                        System.out.println("An error occurred.");
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur de connexion au serveur: " + e.getMessage());
        }
    }

    private class ReadMessages implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Connexion au serveur perdue.");
            }
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

    public static void main(String[] args) {
        new Client().start();
    }

    private void sendFile(Scanner scanner, String filePath, String dest) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("Fichier introuvable.");
            return;
        }

        try {
            //Socket fileSocket = new Socket(hostname, port);
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            FileInputStream fileIn = new FileInputStream(file);

            dataOut.writeUTF("555FILE555 " + file.getName() + " " + dest);
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

}
