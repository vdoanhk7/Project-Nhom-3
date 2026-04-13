package com.nhom3.shared.model.Item;

public class Electronics extends Item{
	public Electronics(int id, String name, String info, double startPrice){
		super(id, name, info, startPrice);
		type = "Electronics";
	}


}