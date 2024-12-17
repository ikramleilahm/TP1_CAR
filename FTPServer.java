package TP1_CAR;
import java.io.*;
import java.net.*;

public class FTPServer {

    public static void main(String[] args) {
        int port = 21; // Port FTP standard
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur FTP en attente de connexions sur le port " + port);

            while (true) {
                // Accepter une connexion client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());

                try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    // Envoyer le message de bienvenue
                    out.println("220 Bienvenue sur le serveur FTP");

                    boolean loggedIn = false;

                    while (true) {
                        // Lire la commande du client
                        String command = in.readLine();
                        if (command == null) break;

                        System.out.println("Commande reçue : " + command);

                        // Gérer les commandes FTP
                        if (command.startsWith("USER")) {
                            out.println("331 Username OK, need password.");
                        } else if (command.startsWith("PASS")) {
                            loggedIn = true;
                            out.println("230 User logged in successfully.");
                        } else if (command.equals("QUIT")) {
                            out.println("221 Goodbye.");
                            break;
                        } else {
                            out.println("502 Command not implemented.");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Client déconnecté.");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
