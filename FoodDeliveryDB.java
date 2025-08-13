/*
To add more practicality, I integrated MySQL with the driver allocation logic.  
The schema for this setup is as follows:

CREATE TABLE drivers (
    driver_id INT PRIMARY KEY AUTO_INCREMENT,
    driver_name VARCHAR(50) NOT NULL,
    status ENUM('free', 'busy') DEFAULT 'free',
    free_time INT DEFAULT NULL
);

CREATE TABLE orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    order_time INT NOT NULL,
    travel_time INT NOT NULL,
    driver_id INT DEFAULT NULL,
    status ENUM('delivered', 'pending', 'no_food') DEFAULT 'pending',
    FOREIGN KEY (driver_id) REFERENCES drivers(driver_id)
);

The logic remains the same:
- Use a **queue** to store available drivers (free).
- Use a **min-heap (priority queue)** to store busy drivers, sorted by when they become free.
- At each order, release drivers whose free time has passed and assign available ones.
- Update driver and order statuses directly in the MySQL database.
*/

import java.sql.*;
import java.util.*;

public class FoodDeliveryDB {

    static class BusyDriver {
        int freeTime;
        int driverId;
        BusyDriver(int freeTime, int driverId) {
            this.freeTime = freeTime;
            this.driverId = driverId;
        }
    }

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/roof_top";
        String user = "root";
        String password = "bhav@sql"; 

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            assignOrdersFromDB(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void assignOrdersFromDB(Connection conn) throws SQLException {
        // Queue for available drivers
        Queue<Integer> available = new LinkedList<>();

        // Min-heap for busy drivers
        PriorityQueue<BusyDriver> busy = new PriorityQueue<>(
            (a, b) -> a.freeTime != b.freeTime ?
                       Integer.compare(a.freeTime, b.freeTime) :
                       Integer.compare(a.driverId, b.driverId)
        );

        Statement stmt = conn.createStatement();

        // Load free drivers from DB
        ResultSet rsDrivers = stmt.executeQuery("SELECT driver_id FROM drivers WHERE status='free' ORDER BY driver_id");
        while (rsDrivers.next()) {
            available.offer(rsDrivers.getInt("driver_id"));
        }

        // Load orders sorted by order time
        ResultSet rsOrders = stmt.executeQuery("SELECT order_id, order_time, travel_time FROM orders ORDER BY order_time");
        while (rsOrders.next()) {
            int orderId = rsOrders.getInt("order_id");
            int orderTime = rsOrders.getInt("order_time");
            int travelTime = rsOrders.getInt("travel_time");

            // Release drivers who are free by this order's time
            while (!busy.isEmpty() && busy.peek().freeTime <= orderTime) {
                int freedDriver = busy.poll().driverId;
                available.offer(freedDriver);
                conn.prepareStatement("UPDATE drivers SET status='free', free_time=NULL WHERE driver_id=" + freedDriver).executeUpdate();
            }

            // Assign driver if available
            if (!available.isEmpty()) {
                int driver = available.poll();
                busy.offer(new BusyDriver(orderTime + travelTime, driver));

                PreparedStatement psOrder = conn.prepareStatement("UPDATE orders SET driver_id=?, status='delivered' WHERE order_id=?");
                psOrder.setInt(1, driver);
                psOrder.setInt(2, orderId);
                psOrder.executeUpdate();

                PreparedStatement psDriver = conn.prepareStatement("UPDATE drivers SET status='busy', free_time=? WHERE driver_id=?");
                psDriver.setInt(1, orderTime + travelTime);
                psDriver.setInt(2, driver);
                psDriver.executeUpdate();

                System.out.println("Order " + orderId + " -> Driver " + driver);
            } else {
                conn.prepareStatement("UPDATE orders SET status='no_food' WHERE order_id=" + orderId).executeUpdate();
                System.out.println("Order " + orderId + " -> No Food :-(");
            }
        }
    }
}

