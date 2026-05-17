package com.nhom3.shared.network.payload;
import java.util.List;

public class SellerItemsResponsePayload {
    
    // Class DTO (Data Transfer Object) chỉ chứa dữ liệu thô để tránh lỗi Gson
    public static class SellerItemDTO {
        public int id;
        public String name;
        public String description;
        public String type;
        public double startPrice;
        public double curHighest;
        public String status; 
        public String imageBase64;

        public SellerItemDTO(int id, String name, String type, double startPrice, double curHighest, String status) {
            this(id, name, "", type, startPrice, curHighest, status, null);
        }

        public SellerItemDTO(int id, String name, String type, double startPrice, double curHighest, String status, String imageBase64) {
            this(id, name, "", type, startPrice, curHighest, status, imageBase64);
        }

        public SellerItemDTO(int id, String name, String description, String type, double startPrice,
                double curHighest, String status, String imageBase64) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.startPrice = startPrice;
            this.curHighest = curHighest;
            this.status = status;
            this.imageBase64 = imageBase64;
        }
    }

    private List<SellerItemDTO> items;

    public SellerItemsResponsePayload(List<SellerItemDTO> items) {
        this.items = items;
    }
    public List<SellerItemDTO> getItems() { return items; }
}
