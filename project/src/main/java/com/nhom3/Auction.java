package com.nhom3;
import java.util.ArrayList;
import com.nhom3.User.Seller;

public class Auction extends Entity{
    private ArrayList<Seller> sellers;

    public Auction(String id) {
        super(id);
        this.sellers = new ArrayList<>();
    }

    @Override
    public void displayInfo() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'displayInfo'");
    }

    
}
