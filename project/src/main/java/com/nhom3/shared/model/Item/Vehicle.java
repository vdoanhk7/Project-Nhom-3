package com.nhom3.shared.model.Item;

public class Vehicle extends Item{
	// Constructor
	public Vehicle(int id, String name, String info, double startPrice){
		super(id, name, info, startPrice);
		type = "Vehicle";
	}


}