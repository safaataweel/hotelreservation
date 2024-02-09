package com.example.hotel;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.Predicate;
import javafx.scene.control.ChoiceBox;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.collections.transformation.FilteredList;

public class RoomManagement extends Application {
    private TableView<Room> roomTable = new TableView<>();
    private ObservableList<Room> roomData = FXCollections.observableArrayList();
    private TextField searchField;
    private ChoiceBox<String> searchCriteriaBox;

    private FilteredList<Room> filteredRoomData = new FilteredList<>(roomData);


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Room Management");

        // Main layout container
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // Initialize the TableView with data
        initializeRoomTable();

        // Create search bar
        HBox searchBar = createSearchBar();

        // Test the database connection and fetch data
        testDatabaseConnection();

        // Enable sorting and searching
        enableSorting();
        enableSearching();

        // Add everything to the main layout
        mainLayout.getChildren().addAll(searchBar, roomTable);

        // Scene setup
        Scene scene = new Scene(mainLayout, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createSearchBar() {
        // Create search components
        searchField = new TextField();
        searchField.setPromptText("Search...");
        searchCriteriaBox = new ChoiceBox<>();
        searchCriteriaBox.getItems().addAll("All", "Room Type", "Amenities", "Max Occupancy", "Room Rate");
        searchCriteriaBox.setValue("All");

        // Add a listener to update the filtered list based on search criteria
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateFilteredRooms());

        searchCriteriaBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> updateFilteredRooms());

        // Create and return the search bar layout
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getChildren().addAll(searchField, searchCriteriaBox);
        return searchBar;
    }

    private void updateFilteredRooms() {
        String searchCriteria = searchCriteriaBox.getValue();
        String keyword = searchField.getText().trim().toLowerCase();

        // Create a new Predicate for the FilteredList
        Predicate<Room> predicate = room -> {
            if (keyword.isEmpty() || "All".equals(searchCriteria)) {
                return true; // No filtering
            }

            try {
                Connection connection = new Connector().getConnection();
                String query = "";

                switch (searchCriteria) {
                    case "Room Type":
                        query = "SELECT * FROM Room WHERE RoomType LIKE ?";
                        break;
                    case "Amenities":
                        query = "SELECT * FROM Room WHERE FIND_IN_SET(?, RoomAmenities) > 0";
                        break;
                    case "Max Occupancy":
                        query = "SELECT * FROM Room WHERE MaximumOccupancy = ?";
                        break;
                    case "Room Rate":
                        query = "SELECT * FROM Room WHERE RoomRate = ?";
                        break;
                }

                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, keyword);

                ResultSet resultSet = preparedStatement.executeQuery();

                return resultSet.next(); // If there is a match, return true

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        };

        // Apply the new predicate to the FilteredList
        filteredRoomData.setPredicate(predicate);
    }

    private void enableSorting() {
        // Allow sorting by column
        roomTable.getSortOrder().addListener((ListChangeListener<TableColumn<Room, ?>>) change -> {
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
        roomTable.getSortOrder().forEach(column -> {
            if (sortOrder.length() != 0) sortOrder.append(", ");
            sortOrder.append(column.getText());
        });

        // Create a SortedList from the sorted SQL data
        SortedList<Room> sortedData = new SortedList<>(fetchSortedData(sortOrder.toString()));

        // Bind the sorted data to the table
        roomTable.setItems(sortedData);
    }

    private ObservableList<Room> fetchSortedData(String sortOrder) {
        ObservableList<Room> sortedData = FXCollections.observableArrayList();

        try {
            Connection connection = new Connector().getConnection();
            String query = "SELECT * FROM Room ORDER BY " + sortOrder;
            ResultSet resultSet = connection.createStatement().executeQuery(query);

            while (resultSet.next()) {
                // Assuming you have the correct columns in your "Room" table
                Room room = new Room(
                        resultSet.getInt("RoomId"),
                        resultSet.getString("RoomType"),
                        Arrays.asList(resultSet.getString("roomAmenities").split(",")),
                        resultSet.getInt("floorNumber"),
                        resultSet.getInt("maximumOccupancy"),
                        resultSet.getBigDecimal("roomRate"),
                        resultSet.getBoolean("isAvailable")
                );
                sortedData.add(room);
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
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM Room");
            while (rs.next()) {
                // Assuming you have the correct columns in your "Room" table
                Room room = new Room(
                        rs.getInt("RoomId"),
                        rs.getString("RoomType"),
                        Arrays.asList(rs.getString("roomAmenities").split(",")),
                        rs.getInt("floorNumber"),
                        rs.getInt("maximumOccupancy"),
                        rs.getBigDecimal("roomRate"),
                        rs.getBoolean("isAvailable")
                );
                roomData.add(room);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeRoomTable() {


        // Setting up columns for the table
        TableColumn<Room, Integer> roomIdColumn = new TableColumn<>("Room ID");
        roomIdColumn.setCellValueFactory(new PropertyValueFactory<>("roomId"));

        TableColumn<Room, String> roomTypeColumn = new TableColumn<>("Room Type");
        roomTypeColumn.setCellValueFactory(new PropertyValueFactory<>("roomType"));

        TableColumn<Room, List<String>> amenitiesColumn = new TableColumn<>("Amenities");
        amenitiesColumn.setCellValueFactory(new PropertyValueFactory<>("roomAmenities"));


        TableColumn<Room, Integer> floorNumberColumn = new TableColumn<>("Floor Number");
        floorNumberColumn.setCellValueFactory(new PropertyValueFactory<>("floorNumber"));

        TableColumn<Room, Integer> maxOccupancyColumn = new TableColumn<>("Maximum Occupancy");
        maxOccupancyColumn.setCellValueFactory(new PropertyValueFactory<>("maximumOccupancy"));

        TableColumn<Room, BigDecimal> rateColumn = new TableColumn<>("Room Rate");
        rateColumn.setCellValueFactory(new PropertyValueFactory<>("roomRate"));
        rateColumn.setCellFactory(column -> new TableCell<Room, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    // Example formatting - adjust as necessary
                    setText(String.format("$%.2f", item));
                }
            }
        });
        TableColumn<Room, Boolean> availabilityColumn = new TableColumn<>("Availability");
        availabilityColumn.setCellValueFactory(new PropertyValueFactory<>("isAvailable")); // Keep this as "isAvailable"

        // Add columns to the table
        roomTable.getColumns().addAll(roomIdColumn, roomTypeColumn, amenitiesColumn,
                floorNumberColumn, maxOccupancyColumn, rateColumn, availabilityColumn);

        // Set data to the table
        roomTable.setItems(roomData);

        amenitiesColumn.setCellFactory(column -> new TableCell<Room, List<String>>() {
            @Override
            protected void updateItem(List<String> item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(String.join(", ", item));
                }
            }
        });

        // Enable sorting
        roomTable.getSortOrder().add(roomIdColumn);
        roomTable.sort();
    }

    public static void main(String[] args) {
        launch(args);
    }
    private void enableSearching() {
        // Create a FilteredList to hold the filtered rooms
        FilteredList<Room> filteredRooms = new FilteredList<>(roomData, p -> true);

        // Add a listener to the search field to update the filtered list
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredRooms.setPredicate(room -> room.matchesKeyword(newValue.trim())));

        // Bind the filtered list to the table
        SortedList<Room> sortedData = new SortedList<>(filteredRooms);
        sortedData.comparatorProperty().bind(roomTable.comparatorProperty());
        roomTable.setItems(sortedData);
    }

}