# Customer Batch Import

Ce projet Spring Boot Batch importe des données client à partir d'un ensemble de fichiers Excel (nommés par date) et les enregistre dans une base PostgreSQL. Il fournit des profils d'environnement dédiés (`dev`, `uat`, `pred`) avec une configuration spécifique pour chaque contexte.

## Prérequis

- Java 17+
- Maven 3.9+
- Une base de données PostgreSQL accessible selon l'environnement
- Des fichiers Excel (`.xlsx`) dans le répertoire configuré contenant les colonnes suivantes :
  1. Identifiant externe
  2. Prénom
  3. Nom
  4. Email
  5. Date d'inscription (`yyyy-MM-dd`, `dd/MM/yyyy` ou `MM/dd/yyyy`)

## Configuration

Le fichier `application.yml` définit la configuration par défaut. Les propriétés principales peuvent être surchargées par des variables d'environnement :

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_BATCH_INPUT_DIR`
- `APP_BATCH_FILE_PATTERN`

Des fichiers spécifiques par environnement sont fournis :

- `application-dev.yml`
- `application-uat.yml`
- `application-pred.yml`

Activez un profil à l'exécution en utilisant l'option JVM `-Dspring.profiles.active=dev` (ou `uat`, `pred`).

## Lancer le job

1. Positionnez les fichiers Excel à importer dans le répertoire défini par `app.batch.input-directory`.
2. Exécutez l'application :
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

Au démarrage, le job Spring Batch `importCustomersJob` traite tous les fichiers correspondant au pattern configuré et enregistre les données en base via JPA.
