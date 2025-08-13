/*
I found this problem is of efficient resource allocation.  
At first look, one might think of graphs, but this is not a problem of path finding, 
it’s about allocating drivers when they become free.  

Initially, I thought of using a simple queue to implement FIFO for driver allocation.  
A circular queue also came to mind for efficiency, but the challenge here is that  
drivers become free at different times and not necessarily in the same order they were assigned.  

So, instead of using just a queue, I chose a **min-heap (priority queue)** to keep track of busy drivers,  
sorted by the time they become free. This way, we can quickly find and release the driver  
who gets free the earliest, which ensures efficient allocation.
*/

import java.util.*;

public class FoodDelivery {

    // Represents a driver currently busy with an order
    static class BusyDriver {
        int freeTime;    
        int driverIndex;
        BusyDriver(int freeTime, int driverIndex) {
            this.freeTime = freeTime;
            this.driverIndex = driverIndex;
        }
    }

    public static void assignOrders(int N, int M, int[][] orders) {
        // Step 1: Sort orders by placement time (earliest order first)
        Arrays.sort(orders, Comparator.comparingInt(a -> a[0]));

        // Step 2: Queue for available drivers (FIFO, lowest index gets priority)
        Queue<Integer> available = new LinkedList<>();
        for (int i = 1; i <= M; i++) {
            available.offer(i);
        }

        // Step 3: Min-heap for busy drivers
        // Primary sort by freeTime, secondary by driver index
        PriorityQueue<BusyDriver> busy = new PriorityQueue<>(
            (a, b) -> a.freeTime != b.freeTime ?
                       Integer.compare(a.freeTime, b.freeTime) :
                       Integer.compare(a.driverIndex, b.driverIndex)
        );

        // Step 4: Process each order
        for (int i = 0; i < N; i++) {
            int orderTime = orders[i][0];
            int travelTime = orders[i][1];

            // Step 4.1: Release drivers who are free by the current order time
            while (!busy.isEmpty() && busy.peek().freeTime <= orderTime) {
                available.offer(busy.poll().driverIndex);
            }

            // Step 4.2: Assign driver if available
            if (!available.isEmpty()) {
                int driver = available.poll();
                // Mark this driver as busy until orderTime + travelTime
                busy.offer(new BusyDriver(orderTime + travelTime, driver));
                System.out.println("C" + (i + 1) + " -> D" + driver);
            } else {
                // No driver available
                System.out.println("C" + (i + 1) + " -> No Food :-(");
            }
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of orders (N): ");
        int N = sc.nextInt();

        System.out.print("Enter number of drivers (M): ");
        int M = sc.nextInt();

        int[][] orders = new int[N][2];
        for (int i = 0; i < N; i++) {
            System.out.print("Enter Order Time and Travel Time for Customer " + (i + 1) + ": ");
            orders[i][0] = sc.nextInt(); // Order place time
            orders[i][1] = sc.nextInt(); // Travel time
        }

        assignOrders(N, M, orders);
        sc.close();
    }
}
