# TP1_CAR
## Étendre le Serveur FTP : Ajout de Nouvelles Commandes

Ce guide vous explique comment enrichir mon serveur FTP en implémentant des commandes supplémentaires non encore prises en charge.

### 1. Modèle de Commandes FTP
Chaque commande FTP suit une structure simple :
- **Commande** : Action à effectuer (ex. : `PWD`, `MKD`, `DELE`).
- **Argument(s)** : Information complémentaire (ex. : chemin pour `MKD`, nom de fichier pour `DELE`).

Le serveur doit être capable d'analyser ces commandes et d'exécuter la logique correspondante.

---

### 2. Ajouter des Commandes au Serveur

Pour implémenter une nouvelle commande FTP :
1. **Créer une méthode dédiée** qui gère la logique de la commande.
2. **Modifier le gestionnaire de commandes** pour intégrer la nouvelle commande.

---

### 3. Commandes FTP à Ajouter

Voici quelques exemples de commandes utiles à implémenter :  

#### **Commande `PWD` (Print Working Directory)**
Affiche le répertoire courant du serveur.

```java
private static void handlePWD(OutputStream output) {
    try {
        String currentDir = System.getProperty("user.dir");
        sendResponse(output, "257 \"" + currentDir + "\" est le répertoire actuel.\r\n");
    } catch (IOException e) {
        sendResponse(output, "451 Erreur lors de l'affichage du répertoire actuel.\r\n");
    }
}
```

#### **Commande `MKD` (Make Directory)**
Crée un nouveau répertoire.

```java
private static void handleMKD(String command, OutputStream output) {
    try {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            sendResponse(output, "501 Chemin non spécifié.\r\n");
            return;
        }
        String dirPath = parts[1].trim();
        File dir = new File(dirPath);
        if (dir.exists()) {
            sendResponse(output, "550 Le répertoire existe déjà : " + dirPath + "\r\n");
        } else if (dir.mkdir()) {
            sendResponse(output, "257 Répertoire créé : " + dirPath + "\r\n");
        } else {
            sendResponse(output, "550 Impossible de créer le répertoire : " + dirPath + "\r\n");
        }
    } catch (IOException e) {
        sendResponse(output, "451 Erreur lors de la création du répertoire.\r\n");
    }
}
```

#### **Commande `DELE` (Delete File)**
Supprime un fichier existant.

```java
private static void handleDELE(String command, OutputStream output) {
    try {
        String[] parts = command.split(" ", 2);
        if (parts.length < 2) {
            sendResponse(output, "501 Nom de fichier non spécifié.\r\n");
            return;
        }
        String filePath = parts[1].trim();
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                sendResponse(output, "250 Fichier supprimé : " + filePath + "\r\n");
            } else {
                sendResponse(output, "550 Impossible de supprimer le fichier : " + filePath + "\r\n");
            }
        } else {
            sendResponse(output, "550 Le fichier n'existe pas : " + filePath + "\r\n");
        }
    } catch (IOException e) {
        sendResponse(output, "451 Erreur lors de la suppression du fichier.\r\n");
    }
}
```

---

### 4. Gestionnaire de Commandes

Mettre à jour le gestionnaire central pour inclure les nouvelles commandes :

```java
private static void handleCommand(String command, OutputStream output) {
    if (command.startsWith("LIST")) {
        handleLIST(command, output);
    } else if (command.startsWith("CWD")) {
        handleCWD(command, output);
    } else if (command.startsWith("PWD")) {
        handlePWD(output);
    } else if (command.startsWith("MKD")) {
        handleMKD(command, output);
    } else if (command.startsWith("DELE")) {
        handleDELE(command, output);
    } else {
        sendResponse(output, "502 Commande non implémentée.\r\n");
    }
}
```

---

### 5. Gestion des Erreurs

Prévoyer des réponses FTP adaptées aux erreurs :
- `500` : Commande mal formée.
- `501` : Argument manquant ou incorrect.
- `550` : Fichier/répertoire introuvable ou inaccessible.

---

### 6. Tests et Débogage

Une fois les nouvelles commandes ajoutées :
- **Tester chaque commande** pour vérifier qu’elle fonctionne correctement.
- Couvrer les cas d’erreurs comme les fichiers/répertoires inexistants ou protégés.

---

### Conclusion

En suivant ces étapes, on pourra étendre efficacement le programme FTP pour supporter une gamme complète de commandes et de mécanismes, tout en garantissant la robustesse et la sécurité de son implémentation.
