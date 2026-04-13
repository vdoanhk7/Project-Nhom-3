package com.nhom3.shared.model;
import com.nhom3.shared.model.Item.Vehicle;
import com.nhom3.shared.model.User.Bidder;
import com.nhom3.shared.model.User.Seller;

public class Main {
    public static void main( String[] args )
    {
        Seller seller = new Seller("seller1", "Seller One", "password", "Seller One", "seller1@example.com", "1234567890");
        seller.createItem(new Vehicle("vehicle1", "Vehicle One", "Description", 10000));
        
        Bidder bidder = new Bidder("bidder1", "Bidder One", "password", "Bidder One", "bidder1@example.com", "0987654321");
        seller.createAuction(seller.getManagedItems().get(0), "auction1", null, null);
        seller.runAuction("auction1");
        bidder.createBidTransaction("bid1", seller.getManagedAuctions().get(0), 11000, "First bid");
        System.out.println(seller.getManagedAuctions().get(0).getItem().getStartPrice());
        System.out.println(seller.getManagedAuctions().get(0).getItem().getCurHighest());
        seller.getManagedAuctions().get(0).getBidHistory().get(0).displayInfo();
    }
}
