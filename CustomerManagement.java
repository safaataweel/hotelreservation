package com.example.hotel;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Predicate;

public class CustomerManagement extends Application {
    private TableView<Customer> customerTable = new TableView<>();
    private ObservableList<Customer> customerData = FXCollections.observableArrayList();
    private TextField searchField;
    private ChoiceBox<String> searchCriteriaBox;

    private FilteredList<Customer> filteredCustomerData = new FilteredList<>(customerData);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Customer Management");

        // Main layout container
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // Initialize the TableView with data
        initializeCustomerTable();

        // Create search bar
        HBox searchBar = createSearchBar();

        // Test the database connection and fetch data
        testDatabaseConnection();

        // Enable sorting and searching
        enableSorting();
        enableSearching();

        // Add everything to main layout
        mainLayout.getChildren().addAll(searchBar, customerTable);

        // Scene setup
        Scene scene = new Scene(mainLayout, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createSearchBar() {
        // Create search components
        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchCriteriaBox = new ChoiceBox<>();
        searchCriteriaBox.getItems().addAll("All", "Name", "Date of Birth", "Phone", "Address");
        searchCriteriaBox.setValue("All");

        // Add a listener to update the filtered list based on search criteria
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilteredCustomers());

        searchCriteriaBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> updateFilteredCustomers());

        // Create and return the search bar layout
        HBox searchBar = new HBox(10);
        searchBar.getChildren().addAll(searchField, searchCriteriaBox);
        return searchBar;
    }

    private void updateFilteredCustomers() {
        String searchCriteria = searchCriteriaBox.getValue();
        String keyword = searchField.getText().trim().toLowerCase();

        // Create a new Predicate for the FilteredList
        Predicate<Customer> predicate = customer -> {
            if (keyword.isEmpty() || "All".equals(searchCriteria)) {
                return true; // No filtering
            }

            try {
                Connection connection = new Connector().getConnection();
                String query = "";

                switch (searchCriteria) {
                    case "Name":
                        query = "SELECT * FROM customer WHERE name LIKE ?";
                        break;
                    case "Date of Birth":
                        query = "SELECT * FROM customer WHERE dateOfBirth LIKE ?";
                        break;
                    case "Phone":
                        query = "SELECT * FROM customer WHERE phone LIKE ?";
                        break;
                    case "Address":
                        query = "SELECT * FROM customer WHERE address LIKE ?";
                        break;
                }

                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, "%" + keyword + "%");

                ResultSet resultSet = preparedStatement.executeQuery();

                return resultSet.next(); // If there is a match, return true

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        };

        // Apply the new predicate to the FilteredList
        filteredCustomerData.setPredicate(predicate);
    }
     private void enableSorting() {
        customerTable.getSortOrder().addListener((ListChangeListener<? super TableColumn<Customer, ?>>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    applySorting();
                }
            }
        });
    }

    private void applySorting() {
        // Get the current sorting order
        StringBuilder sortOrder = new StringBuilder();
        customerTable.getSortOrder().forEach(column -> {
            if (sortOrder.length() != 0) sortOrder.append(", ");
            sortOrder.append(column.getText());
        });

        // Create a SortedList from the sorted SQL data
        SortedList<Customer> sortedData = new SortedList<>(fetchSortedData(sortOrder.toString()));

        // Bind the sorted data to the table
        customerTable.setItems(sortedData);
    }

    private ObservableList<Customer> fetchSortedData(String sortOrder) {
        ObservableList<Customer> sortedData = FXCollections.observableArrayList();

        try {
            Connection connection = new Connector().getConnection();
            String query = "SELECT * FROM customer ORDER BY " + sortOrder;
            ResultSet resultSet = connection.createStatement().executeQuery(query);

            while (resultSet.next()) {
                // Create Customer object and populate its fields from the database
                Customer customer = new Customer(
                        resultSet.getInt("customerId"),
                        resultSet.getString("name"),
                        resultSet.getDate("dateOfBirth").toLocalDate(),
                        resultSet.getString("phone"),
                        resultSet.getString("address")
                );

                // Add the customer to the sortedData list
                sortedData.add(customer);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sortedData;
    }

    private void testDatabaseConnection() {
        Connector connector = new Connector();
        Connection connection = connector.getConnection();
        if (connection != null) {
            System.out.println("Connection successful!");
            fetchData(connection);
        } else {
            System.out.println("Connection failed!");
        }
    }

    private void fetchData(Connection connection) {
        try {
            // Update the table name to "customer"
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM customer");
            while (rs.next()) {
                // Assuming you have the correct columns in your "customer" table
                Customer customer = new Customer(
                        rs.getInt("customerId"),
                        rs.getString("name"),
                        rs.getDate("dateOfBirth").toLocalDate(),
                        rs.getString("phone"),
                        rs.getString("address")
                );
                customerData.add(customer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeCustomerTable() {
        // Setting up columns for the table
        TableColumn<Customer, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Customer, LocalDate> dobColumn = new TableColumn<>("Date of Birth");
        dobColumn.setCellValueFactory(new PropertyValueFactory<>("dateOfBirth"));

        TableColumn<Customer, String> phoneColumn = new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Customer, String> addressColumn = new TableColumn<>("Address");
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));

        customerTable.getColumns().addAll(nameColumn, dobColumn, phoneColumn, addressColumn);
        customerTable.setItems(customerData);

        // Enable sorting by name column by default
        customerTable.getSortOrder().add(nameColumn);
        customerTable.sort();
    }

    private void enableSearching() {
        // Create a FilteredList to hold the filtered customers
        FilteredList<Customer> filteredCustomers = new FilteredList<>(customerData, p -> true);

        // Add a listener to the search field to update the filtered list
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredCustomers.setPredicate(customer -> customer.matchesKeyword(newValue.trim())));

        // Bind the filtered list to the table
        SortedList<Customer> sortedData = new SortedList<>(filteredCustomers);
        sortedData.comparatorProperty().bind(customerTable.comparatorProperty());
        customerTable.setItems(sortedData);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
