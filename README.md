# 🃏 UNO Game - Advanced DBMS Project
[![Java](https://img.shields.io/badge/Language-Java%2017-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/Database-MySQL-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-None-red.svg)](#)

An immersive, database-driven implementation of the classic UNO card game. This project demonstrates high-performance **Real-Time State Synchronization**, **ACID-compliant transactions**, and a **Heuristic-based AI engine**.

---

## 📽️ Cinematic Intro
This project features a custom-built **Bomb Animation Intro** developed purely in Java 2D Graphics.
1. **The Drop:** A physical-modeled bomb falls from the screen top.
2. **The Flash:** A programmatic white-flash lighting effect simulates the explosion.
3. **The Reveal:** Smooth alpha-blending reveals the "UNO GAME" branding.



---

## 🛠️ Tech Stack & Architecture
| Component | Technology |
| :--- | :--- |
| **Frontend** | Java Swing / AWT (Custom Graphics2D) |
| **Backend** | Java 17 (JDBC) |
| **Database** | MySQL 8.0+ |
| **AI Strategy** | Greedy Heuristic Algorithm |

### **The "Single Source of Truth" Logic**
Unlike local games, this project uses the database as the only authority.
* **Polling Mechanism:** The UI refreshes its state by querying the DB every few seconds to stay synced with other players.
* **Transactional Integrity:** Card plays and turn hand-offs are wrapped in atomic transactions to prevent game desync.



---

## 🧠 Smart AI (Greedy Heuristic)
The AI opponents don't just pick random cards. They evaluate their hands using a weighted scoring system:
* 🚀 **Priority 1 (100 pts):** Wild Draw 4
* ⚔️ **Priority 2 (80 pts):** Draw 2
* 🛡️ **Priority 3 (70 pts):** Skip / Reverse
* 🔢 **Priority 4 (Face Value):** Number Cards (0-9)

---

## 🚀 Getting Started

### **Prerequisites**
- **JDK 17** or higher.
- **MySQL Server** (XAMPP recommended).
- **MySQL Connector J 9.1.0** (Found in the `/JDBC` folder).

### **Installation**

1. **Clone the repo:**
   ```bash
   git clone [https://github.com/CodeAddict-cpu01/UNO-Game-DBMS-Project.git](https://github.com/CodeAddict-cpu01/UNO-Game-DBMS-Project.git)

Setup Database: Import database_setup.sql into your local MySQL instance.

Configure Connection: Update your credentials in src/DBConnector.java.

# Compile
javac -d classes -cp "JDBC\mysql-connector-j-9.1.0.jar" src\*.java

# Run with Intro
java -cp "JDBC\mysql-connector-j-9.1.0.jar;classes" AnimatedSplashScreen


📂 Folder Structure
├── src/                # Core Java Logic & UI
├── classes/            # Compiled Bytecode (ignored by git)
├── JDBC/               # Database Driver
├── images/             # Card Assets & Power-ups
└── database_setup.sql  # SQL Schema & Initial 
