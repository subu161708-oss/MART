
package com.subamart.util;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class DatabaseInitializer {

    public static void initialize() throws SQLException {

        String[] queries = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(150) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(20) DEFAULT 'CUSTOMER'
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS products (
                id INT PRIMARY KEY AUTO_INCREMENT,
                name VARCHAR(150) NOT NULL,
                description TEXT,
                price DECIMAL(10,2) NOT NULL,
                image_url VARCHAR(500),
                stock INT DEFAULT 0
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS orders (
                id INT PRIMARY KEY AUTO_INCREMENT,
                user_id INT,
                total DECIMAL(10,2) NOT NULL,
                status VARCHAR(30) DEFAULT 'PLACED',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS reviews (
                id INT PRIMARY KEY AUTO_INCREMENT,
                product_id INT NOT NULL,
                user_id INT NOT NULL,
                rating INT NOT NULL,
                comment TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id) REFERENCES products(id),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """
        };

        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {

            for (String query : queries) {
                statement.executeUpdate(query);
            }

            System.out.println("Suba Mart database tables created successfully!");
        }
    }

    public static void main(String[] args) {
        try {
            initialize();
        } catch (SQLException e) {
            System.err.println("Database initialization failed.");
            e.printStackTrace();
        }
    }
}
