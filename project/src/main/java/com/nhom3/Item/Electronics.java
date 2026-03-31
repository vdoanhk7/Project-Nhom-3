package com.nhom3.Item;

import java.time.LocalDate;

public class Electronics extends Item{
	// Constructor
	public Electronics(String id, String iName, String info, double startPrice, LocalDate startAuction, LocalDate endAuction){
		super(id, iName, info, startPrice, startAuction, endAuction);
	}

	@Override
	public void displayInfo() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'displayInfo'");
	}
}