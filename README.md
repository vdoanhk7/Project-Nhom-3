# Hệ Thống Đấu Giá Trực Tuyến - Nhóm 3

## 1. Mô Tả Bài Toán Và Phạm Vi Hệ Thống

Hệ thống đấu giá trực tuyến cho phép người bán đăng sản phẩm, tạo phiên đấu giá và người mua tham gia đặt giá theo thời gian thực. Hệ thống được xây dựng theo mô hình Client/Server, trong đó server xử lý nghiệp vụ, quản lý dữ liệu và đồng bộ trạng thái đấu giá; client là ứng dụng JavaFX để người dùng thao tác trực tiếp.

Phạm vi thực hiện:

- Server TCP xử lý nhiều client đồng thời.
- Client JavaFX cho các vai trò Bidder, Seller và Admin.
- Quản lý tài khoản, đăng ký, đăng nhập, đổi mật khẩu và cập nhật hồ sơ.
- Quản lý sản phẩm, phiên đấu giá, đặt giá, auto bid, lịch sử đấu giá và lịch sử mua hàng.
- Cập nhật realtime cho các phiên đấu giá đang diễn ra.
- Ghi log hệ thống và hỗ trợ màn hình quản trị.

## 2. Công Nghệ Sử Dụng, Môi Trường Chạy Và Yêu Cầu Cài Đặt

Công nghệ chính:

- Java 21
- Maven
- JavaFX 21
- MySQL
- Socket TCP
- Gson
- HikariCP
- SLF4J và Logback
- jBCrypt
- JUnit 5, Mockito
- GitHub Actions

Môi trường yêu cầu:

- JDK 21
- Maven 3.8+ hoặc bản mới hơn
- MySQL Server hoặc một MySQL instance mà server có thể kết nối tới
- Hệ điều hành Windows, Linux hoặc macOS

Khởi tạo cơ sở dữ liệu:

```bash
mysql -u <user> -p < project/database/schema.sql
mysql -u <user> -p < project/database/mock_data.sql
```

Server có thể nhận cấu hình database qua environment variables:

```text
AUCTION_DB_URL
AUCTION_DB_USER
AUCTION_DB_PASSWORD
AUCTION_DB_DRIVER
AUCTION_DB_POOL_MAX
AUCTION_DB_POOL_MIN
```

Hoặc qua Java system properties:

```text
auction.db.url
auction.db.user
auction.db.password
auction.db.driver
auction.db.pool.max
auction.db.pool.min
```

Client mặc định kết nối tới `localhost:8080`. Có thể đổi host/port bằng environment variables:

```text
AUCTION_SERVER_HOST
AUCTION_SERVER_PORT
```

Hoặc Java system properties:

```text
auction.server.host
auction.server.port
```

## 3. Cấu Trúc Thư Mục Và Các Module Chính

```text
Project-Nhom-3/
|-- .github/workflows/
|   |-- ci.yml                 # Kiểm tra build/test với Maven
|   `-- package.yml            # Build và upload server/client fat JAR
|-- images/                    # Hình ảnh, sơ đồ kiến trúc/UML
|-- project/
|   |-- database/
|   |   |-- schema.sql         # Cấu trúc database
|   |   `-- mock_data.sql      # Dữ liệu mẫu
|   |-- dist/                  # Nơi sinh ra file JAR sau khi build
|   |-- packaging/             # Script đóng gói native package nếu cần
|   |-- src/main/java/com/nhom3/
|   |   |-- client/            # Ứng dụng JavaFX client
|   |   |-- server/            # Server, DAO, service, network handler
|   |   `-- shared/            # Model, packet, payload dùng chung
|   |-- src/main/resources/    # FXML, ảnh, cấu hình logback
|   |-- src/test/java/         # Unit test và integration test
|   `-- pom.xml                # Cấu hình Maven và fat JAR
`-- README.md
```

## 4. Vị Trí Các File JAR

Sau khi build, Maven tạo 2 fat JAR tại:

```text
project/dist/server-app.jar
project/dist/client-app.jar
```

Hai file này là fat JAR/uber JAR, đã đóng gói các dependencies cần thiết để chạy bằng lệnh `java -jar`.

GitHub Actions workflow `Build Fat JARs` cũng tự động build và upload artifact tên `auction-fat-jars`, gồm:

```text
server-app.jar
client-app.jar
```

## 5. Hướng Dẫn Build

Từ thư mục gốc repository:

```bash
cd project
mvn --batch-mode -DskipTests clean package
```

Nếu muốn chạy kèm test:

```bash
cd project
mvn --batch-mode test
```

## 6. Hướng Dẫn Chạy Server/Client

Cần chạy database trước, sau đó chạy server, rồi mới chạy client.

Bước 1: chạy server

```bash
cd project
java -jar dist/server-app.jar
```

Server mặc định lắng nghe cổng `8080`.

Nếu cần chỉ định port:

```bash
java -Dauction.server.port=8080 -jar dist/server-app.jar
```

Bước 2: chạy client

Mở terminal khác:

```bash
cd project
java -jar dist/client-app.jar
```

Nếu client cần kết nối tới server khác `localhost`:

```bash
java -Dauction.server.host=<server-host> -Dauction.server.port=8080 -jar dist/client-app.jar
```

Bước 3: chạy nhiều client

Mở thêm các terminal khác và chạy lại:

```bash
cd project
java -jar dist/client-app.jar
```

## 7. Danh Sách Chức Năng Đã Hoàn Thành

- Đăng ký, đăng nhập và phân quyền người dùng theo vai trò Bidder, Seller, Admin.
- Đổi mật khẩu và cập nhật thông tin hồ sơ người dùng.
- Mã hóa mật khẩu bằng BCrypt.
- Seller thêm, sửa, xóa và quản lý sản phẩm.
- Seller tạo và hủy phiên đấu giá.
- Bidder xem danh sách phiên đấu giá, xem chi tiết sản phẩm và lịch sử đặt giá.
- Bidder đặt giá trực tiếp trong phiên đấu giá.
- Hỗ trợ auto bid.
- Tự động cập nhật trạng thái phiên đấu giá theo thời gian.
- Tự động đóng phiên đấu giá khi hết hạn.
- Hỗ trợ anti-sniping bằng cách gia hạn phiên khi có lượt đặt giá sát thời điểm kết thúc.
- Cập nhật realtime cho các client đang theo dõi phiên đấu giá.
- Quản lý lịch sử mua hàng và xác nhận thanh toán.
- Admin xem danh sách người dùng, xóa người dùng và theo dõi log hệ thống.
- Server xử lý nhiều client đồng thời bằng virtual threads và giới hạn số kết nối.
- Giao tiếp Client/Server bằng packet JSON qua Socket TCP.
- Ghi log bằng SLF4J/Logback.
- Quản lý connection pool tới MySQL bằng HikariCP.
- Có unit test/integration test cho các lớp service, DAO và model chính.
- Có GitHub Actions để build/test và build fat JAR artifact.

## 8. Báo Cáo PDF Và Video Demo

- Báo cáo PDF: TODO - cập nhật link báo cáo PDF
- Video demo: TODO - cập nhật link video demo

