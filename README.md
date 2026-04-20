# Project-Nhom-3
 

## 📋 Bảng Phân Công và Tiến Độ Chi Tiết

# Bảng Phân Công Công Việc & Theo Dõi Tiến Độ

| Thành Viên | Vai Trò Chính | Nhiệm Vụ Cụ Thể (Bám sát Rubric) | Đã Hoàn Thành | Đang Thực Hiện |
| :--- | :--- | :--- | :--- | :--- |
| **Phúc Anh** | **Backend & Concurrency** | - **Core:** Logic đặt giá (Bidding logic).<br>- **Bắt buộc (1đ):** Xử lý đấu giá đồng thời (Concurrency, Thread-safe).<br>- **Nâng cao:** Auto-Bidding (PriorityQueue).<br>- **Mạng:** Setup Socket/REST API Server, Realtime Update (Observer). | | |
| **Doanh** | **Database & Core Logic** | - **Core:** Thiết kế Database & viết các lớp DAO.<br>- **Core:** Quản lý Sản phẩm (CRUD) & Quản lý Phiên đấu giá (Tự động đóng, chuyển trạng thái).<br>- **Nâng cao:** Gia hạn phiên (Anti-sniping).<br>- **OOP:** Áp dụng Design Patterns (Singleton, Factory). | | |
| **Dũng** | **DevOps, Test & User Mng** | - **Core:** Quản lý Người dùng (Đăng ký/Đăng nhập, Phân quyền).<br>- **Nâng cao:** Bid History Visualization (Vẽ biểu đồ JavaFX).<br>- **Chất lượng mã:** Setup CI/CD (GitHub Actions), viết Unit Test (JUnit).<br>- Quản lý Git convention. | | |
| **Quang Anh**| **Frontend (JavaFX Client)** | - **Kiến trúc:** Thiết kế toàn bộ UI bằng JavaFX + FXML (MVC Client).<br>- **Màn hình:** Login, Danh sách phiên, Chi tiết SP, Màn hình đấu giá trực tiếp, Dashboard Admin/Seller.<br>- Tích hợp API/Socket từ Server lên UI. | | |
| **Cả Nhóm** | **Tích hợp & Hoàn thiện** | - Ghép nối Client - Server.<br>- Xử lý ngoại lệ (Exception Handling - 1đ).<br>- Code Review chéo để **tất cả đều hiểu code**.<br>- Viết báo cáo/Slide thuyết trình. | | |

![Class UML Diagram](images/diagram.png)