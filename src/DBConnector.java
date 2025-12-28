import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnector {

    // --- CRITICAL: UPDATE THESE THREE LINES ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/uno_project"; 
    // If testing remotely, use the host laptop's IP address instead of 'localhost'
    
    private static final String USER = "root"; // Replace with your MySQL username
    private static final String PASS = "527112Hh++"; // Replace with your MySQL password

    /**
     * Establishes a connection to the MySQL database.
     * @return Connection object or null if connection fails.
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            System.out.println("Attempting to connect to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("Connection successful!");
            return conn;
        } catch (SQLException e) {
            System.err.println("Connection Failed! Check URL, User, Password, and MySQL network permissions.");
            e.printStackTrace();
            return null;
        }
    }

            /**
         * Helper method to safely close the database connection.
         */
        public static void closeConnection(Connection conn) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing database connection: " + e.getMessage());
                }
            }
        }

    /**
     * Simple test function to prove the connection works by reading card data.
     */
    public static void testConnection() {
        Connection conn = getConnection();
        if (conn == null) {
            return;
        }
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT card_id, color, value FROM Cards LIMIT 5")) {
            
            System.out.println("\n--- Database Read Test (First 5 Cards) ---");
            while (rs.next()) {
                int id = rs.getInt("card_id");
                String color = rs.getString("color");
                String value = rs.getString("value");
                
                System.out.printf("ID: %d, Color: %s, Value: %s%n", id, color, value);
            }
            System.out.println("--- Test complete. ---");
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        testConnection();
    }
}