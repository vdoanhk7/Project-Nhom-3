package com.nhom3;

public class Main {
    public static void main(String[] args) {
        // Create some auctions
        Auction auction1 = new Auction("001", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 10));
        Auction auction2 = new Auction("002", LocalDate.of(2024, 7, 5), LocalDate.of(2024, 7, 15));

        // Display auction information
        auction1.displayInfo();
        System.out.println();
        auction2.displayInfo();
    }
}
