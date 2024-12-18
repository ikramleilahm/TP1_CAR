package TP1_CAR;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class FTPServer {

    public static void main(String[] args) {
        int port = 21; // Port FTP standard
        String username = "test"; // Nom d'utilisateur prédéfini
        String password = "1234"; // Mot de passe prédéfini

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur FTP en attente de connexions sur le port " + port);

            while (true) {
                // Accepter une connexion client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());

                try (Scanner in = new Scanner(clientSocket.getInputStream());
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    // Envoyer le message de bienvenue
                    out.println("220 Bienvenue sur le serveur FTP");

                    while (true) {
                        // Lire la commande du client
                        String command = in.nextLine();
                        System.out.println("Commande reçue : " + command);

                        // commandes FTP
                        if (command.equals("USER " + username)) { // Vérifier le username
                            out.println("331 Username OK, need password.");
                        } else if (command.equals("PASS " + password)) { // Vérifier le password
                            out.println("230 User logged in successfully.");
                        } else if (command.equals("QUIT")) { // Déconnexion
                            out.println("221 Goodbye.");
                            break; // Quitter la boucle
                        } else if (command.startsWith("USER") || command.startsWith("PASS")) { 
                            // Gérer un username ou un password incorrect
                            if (command.startsWith("USER")) {
                                out.println("530 Invalid username."); // Message d'erreur pour mauvais username
                            } else {
                                out.println("530 Invalid password."); // Message d'erreur pour mauvais password
                            }
                        } else {
                            // Toute autre commande non reconnue ou sans authentification
                            out.println("530 Invalid command or please login first.");
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Erreur dans la gestion du client : " + e.getMessage());
                } finally {
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la fermeture du socket client : " + e.getMessage());
                    }
                    System.out.println("Client déconnecté.");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur au niveau du serveur : " + e.getMessage());
        }
    }
}
