package TP1_CAR;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class FTPServer {

    private static final Map<String, String> users = new HashMap<>();
    private static String dataIp; // IP du client pour la connexion de données
    private static int dataPort; // Port de connexion de données
    private static Socket dataSocket; // Connexion de données (pour get et autres commandes)

    static {
        users.put("admin", "1234"); 
    }

    public static void main(String[] args) {
        int port = 21; 

        try {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Serveur FTP démarré sur le port : " + port);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // Client accepté, afficher son adresse
                    System.out.println("Client connecté : " + clientSocket.getInetAddress());
                    new Thread(() -> handleClient(clientSocket)).start();
                }
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
                    } else if (command.startsWith("EPRT")) {
                        // Gestion de la commande EPRT en mode actif
                        handleEPRT(command, output);
                    } else if (command.startsWith("RETR")) {
                        // Gestion de la commande RETR (obtenir un fichier)
                        handleRETR(command, output);
                    } else if (command.startsWith("LIST")) {
                        // Gestion de la commande LIST (lister les fichiers)
                        handleLIST(command, output);
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

    private static void handleEPRT(String command, OutputStream output) {
        try { 
            
            // Nettoyer la commande pour supprimer les espaces potentiels avant et après
        command = command.trim();

        // Extraire les informations de la commande EPRT
        String[] parts = command.split("\\|");

        // Affiche les parties pour diagnostiquer
        System.out.println("Nombre de parties extraites : " + parts.length);
        for (int i = 0; i < parts.length; i++) {
            System.out.println("Part[" + i + "] = '" + parts[i] + "'");
        }

        // Vérification de la structure correcte de la commande EPRT
        if (parts.length != 4) {
            sendResponse(output, "502 Commande EPRT mal formée.\r\n");
            System.out.println("Commande mal formée : " + command);
            return;
        }
            
            // Extraction des paramètres
            String clientIp = parts[2];  // Adresse IP du client
            int clientPort;
            try {
                clientPort = Integer.parseInt(parts[3]);  // Le port de connexion de données
            } catch (NumberFormatException e) {
                sendResponse(output, "522 Port invalide.\r\n");
                return;
            }
    
            // Validation du port
            if (clientPort < 1 || clientPort > 65535) {
                sendResponse(output, "522 Port invalide.\r\n");
                return;
            }
    
            // Validation de l'adresse IP (autoriser IPv6 et IPv4)
            if (!isValidIp(clientIp)) {
                sendResponse(output, "522 Adresse IP invalide.\r\n");
                return;
            }
    
            System.out.println("Commande EPRT reçue avec IP : " + clientIp + " et port : " + clientPort);
    
            // Sauvegarde des informations de la connexion de données
            dataIp = clientIp;
            dataPort = clientPort;
    
            // Tenter de créer la connexion de données
            try {
                dataSocket = new Socket(dataIp, dataPort);
                System.out.println("Connexion de données établie avec le client : " + dataSocket.getInetAddress());
                sendResponse(output, "200 Commande EPRT acceptée\r\n");
            } catch (IOException e) {
                System.err.println("Erreur réseau lors de la connexion de données : " + e.getMessage());
                sendResponse(output, "425 Impossible d'établir la connexion de données.\r\n");
            }
    
        } catch (Exception e) {
            System.err.println("Erreur inattendue dans EPRT : " + e.getMessage());
            e.printStackTrace();
            try {
                sendResponse(output, "502 Commande non supportée : EPRT\r\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
    
    // Fonction pour valider l'IP (IPv4 ou IPv6)
    private static boolean isValidIp(String ip) {
        return ip.matches("^(\\d+\\.\\d+\\.\\d+\\.\\d+)$") || ip.matches("^([0-9a-fA-F:]+)$");
    }

    // Gérer la commande RETR
    private static void handleRETR(String command, OutputStream output) {
        // Récupérer le fichier demandé
        String fileName = command.substring(5).trim(); // Supposer que le nom du fichier suit "RETR "
        File file = new File(fileName);

        if (!file.exists()) {
            try {
                sendResponse(output, "550 Fichier non trouvé.\r\n");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Répondre pour indiquer que le serveur va commencer à envoyer le fichier
        try {
            sendResponse(output, "150 Ouverture de la connexion de données pour " + fileName + "\r\n");

            // Créer un flux de données sur la connexion de données
            try (OutputStream dataOutput = dataSocket.getOutputStream();
                 FileInputStream fileInput = new FileInputStream(file)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    dataOutput.write(buffer, 0, bytesRead);
                }

                // Envoi terminé, on ferme la connexion de données
                sendResponse(output, "226 Transfert terminé.\r\n");
            } catch (IOException e) {
                sendResponse(output, "425 Impossible d'ouvrir la connexion de données.\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Fermer la connexion de données à la fin du transfert
            try {
                if (dataSocket != null) {
                    dataSocket.close();
                    System.out.println("Connexion de données fermée.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Gerer la commande LIST
    private static void handleLIST(String command, OutputStream output) {
        try {
            File currentDirectory = new File("."); // Répertoire courant du serveur
            File[] files = currentDirectory.listFiles();
    
            if (files == null || files.length == 0) {
                sendResponse(output, "150 Aucun fichier ou répertoire dans le répertoire courant.\r\n");
                sendResponse(output, "226 Liste terminée.\r\n");
                return;
            }
    
            // Envoyer une réponse initiale pour indiquer que le transfert va commencer
            sendResponse(output, "150 Ouverture de la connexion de données pour la liste des fichiers.\r\n");
    
            // Ouvrir un flux de sortie pour envoyer la liste via la connexion de données
            try (OutputStream dataOutput = dataSocket.getOutputStream()) {
                for (File file : files) {
                    String fileInfo = (file.isDirectory() ? "Dossier " : "Fichier ") + file.getName() + "\r\n";
                    dataOutput.write(fileInfo.getBytes());
                }
            } catch (IOException e) {
                sendResponse(output, "425 Impossible d'ouvrir la connexion de données.\r\n");
                return;
            }
    
            // Indiquer que la liste est terminée
            sendResponse(output, "226 Liste terminée.\r\n");
        } catch (IOException e) {
            try {
                sendResponse(output, "451 Erreur pendant l'envoi de la liste.\r\n");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            // Fermer la connexion de données à la fin du transfert
            try {
                if (dataSocket != null) {
                    dataSocket.close();
                    System.out.println("Connexion de données fermée après LIST.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }    

        

}

