# Weather-Pipeline - Etude de cas Memoire M2

✍️ **Auteur**: [Dihia Derbal]

Mise en place d'une pipeline pour experimenter le streaming de données avec Kafka et Flink.

## 🚀 Lancement du projet

Voici les étapes pour lancer l'application en local avec Docker Compose :

1. **Construire les images Docker :**

   ```bash
   docker-compose build
   ```

2. **Lancer les conteneurs :**

   ```bash
   docker-compose up -d
   ```

3. ** (Optionnel) Vérifier l'état des conteneurs :**

   ```bash
   docker-compose ps -a
   ```

Ensuite ouvrir Grafana à l'address: http://localhost:3000 .

* CSV x Kafka x Apache Flink × Influxdb × Grafana *

---

## 0 . Prerequisites

| Tool | Version testé | Role |
|------|----------------|-----|
| Docker | ≥ 24.0 | Container runtime |
| Docker Compose | Multi-service orchestration |
| JDK 21 | Compilation du code java kafka|

---
