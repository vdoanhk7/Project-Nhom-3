package com.nhom3.shared.model.Item;

public class Vehicle extends Item{
	// Constructor
	public Vehicle(String id, String iName, String info, double startPrice){
		super("Vehicle-" + id, iName, info, startPrice);
		type = "Vehicle";
	}


}