public class Vehicle extends Item{
	// Constructor
	protected Item(String id, String name, String info, double startPrice, LocalDate start, LocalDate end){
		super(id, name, info, startPrice, start, end);
	}

	protected void setName(){
		this.name = name;
	}
	protected String getName(){
		return name;	
	}
	
	protected void setInfo(){
		this.info = info;
	}
	protected String getInfo(){
		return info;
	}

	protected void setStartPrice(){
		this.startPrice = startPrice;
	}
	protected double getStartPrice(){
		return startPrice;
	}
	
	protected double getCurHighest(){
		return curHighest;
	}
	protected void setCurHighest(){
		this.curHighest = curHighest;
	}

	protected void setStartAuction(){
		this.startAuction = startAuction;
	}
	protected LocalDate getStartAuction(){
		return startAuction;
	}

	protected void setEndAuction(){
		this.endAuction = endAuction;
	}
	protected LocalDate getEndAuction(){
		return endAuction;
	}
}