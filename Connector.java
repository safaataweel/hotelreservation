package com.example.hotel;

import java.sql.*;

public class Connector {
    public Connection databaseLink;

    public Connection getConnection() {
        String databaseName = "hotel";
        String databaseUser = "root";
        String databasePassword = "16112002";
        String url = "jdbc:mysql://localhost/" + databaseName;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            databaseLink = DriverManager.getConnection(url, databaseUser, databasePassword);
            System.out.println("Connected to the database successfully.");
        } catch (Exception e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
            e.printStackTrace();
        }

        return databaseLink;
    }
}
