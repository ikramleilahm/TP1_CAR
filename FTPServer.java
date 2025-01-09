package TP1_CAR;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class FTPServer {

    private static final Map<String, String> users = new HashMap<>();

    static {
        users.put("admin", "1234"); 
    }

    public static void main(String[] args) {
        int port = 21; 

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur FTP démarré sur le port : " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Client accepté, afficher son adresse
                System.out.println("Client connecté : " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();
            Scanner scanner = new Scanner(input)
        ) {
            // Message de bienvenue
            output.write("220 Bienvenue sur le serveur FTP\r\n".getBytes());

            // Authentification de l'utilisateur
            if (authenticateUser(scanner, output)) {
                boolean running = true;
                while (running) {
                    String command = scanner.hasNextLine() ? scanner.nextLine() : "";

                    // **Afficher la commande reçue dans la console**
                    System.out.println("Commande reçue : " + command);

                    if (command.equalsIgnoreCase("QUIT")) {
                        output.write("221 Deconnexion en cours. Au revoir!\r\n".getBytes());
                        running = false; // Quitter la boucle et fermer la connexion
                    }
                     else {
                        // Commande non supportée
                        sendResponse(output, "502 Commande non supportee : " + command + "\r\n");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client déconnecté");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Authentification
    private static boolean authenticateUser(Scanner scanner, OutputStream output) throws IOException {
        String username = null;
        while (true) {
            String input = receiveMessage(scanner);

            if (input.toUpperCase().startsWith("USER")) {
                username = input.substring(5).trim(); // Extraire le nom d'utilisateur après "USER "
                sendResponse(output, "331 Nom d'utilisateur accepte. Veuillez entrer le mot de passe.\r\n");
            } else if (input.toUpperCase().startsWith("PASS")) {
                if (username == null) {
                    sendResponse(output, "503 Mauvaise sequence de commande. Veuillez d'abord envoyer USER.\r\n");
                } else {
                    String password = input.substring(5).trim(); // Extraire le mot de passe après "PASS "
                    if (users.containsKey(username) && users.get(username).equals(password)) {
                        sendResponse(output, "230 Authentification reussie. Bienvenue !\r\n");
                        return true;
                    } else {
                        sendResponse(output, "530 Nom d'utilisateur ou mot de passe incorrect. Veuillez reessayer.\r\n");
                    }
                }
            } else {
                sendResponse(output, "530 Vous devez vous authentifier avec USER et PASS.\r\n");
            }
        }
    }

    private static String receiveMessage(Scanner scanner) {
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }

    // Méthode d'envoi de réponse
    private static void sendResponse(OutputStream output, String message) throws IOException {
        output.write(message.getBytes());
    }
}
