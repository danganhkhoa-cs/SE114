# LocaSOS - Ứng dụng Hỗ trợ Cộng đồng và Kết nối Dịch vụ Địa phương

**LocaSOS** là ứng dụng di động được xây dựng nhằm giải quyết bài toán kết nối cứu trợ khẩn cấp và tìm kiếm dịch vụ dân sinh trong khu vực lân cận. Ứng dụng hoạt động như một nền tảng trung gian, cho phép người dùng đăng tin yêu cầu hỗ trợ hoặc quảng bá dịch vụ, đồng thời cung cấp môi trường giao tiếp an toàn thông qua cơ chế lọc tin nhắn spam.

Dự án thuộc khuôn khổ môn học **Nhập môn Ứng dụng Di động (SE114)** tại Trường Đại học Công nghệ Thông tin, ĐHQG-HCM.

## Các tính năng chính

### 1. Quản lý Định danh & Bảo mật
* **Đăng ký và Xác thực:** Hỗ trợ tạo tài khoản qua Email, kiểm tra độ mạnh mật khẩu và xác thực người dùng.
* **Khôi phục tài khoản:** Quy trình quên mật khẩu an toàn sử dụng mã OTP gửi qua Email thông qua Firebase Cloud Functions.
* **Hồ sơ người dùng:** Quản lý thông tin cá nhân, cập nhật ảnh đại diện, nghề nghiệp và địa chỉ.

### 2. Kết nối và Tương tác (Newsfeed)
* **Đăng tin Cứu trợ (SOS):** Người dùng có thể tạo bài viết khẩn cấp kèm vị trí và danh mục sự cố để tìm kiếm sự giúp đỡ từ cộng đồng xung quanh.
* **Đăng tin Dịch vụ:** Cho phép người cung cấp (thợ sửa chữa, dịch vụ) đăng bài quảng bá kỹ năng và dịch vụ tới cư dân địa phương.
* **Tương tác:** Hỗ trợ tính năng lưu tin, ẩn tin và bình luận trao đổi thông tin dưới bài viết.

### 3. Hệ thống Giao tiếp (Communication)
* **Chat thời gian thực:** Nhắn tin trực tiếp giữa các người dùng với độ trễ thấp.
* **Bộ lọc tin nhắn chờ (Spam Filter):** Tin nhắn từ người lạ (chưa kết bạn) tự động được chuyển vào hộp thư chờ. Người nhận cần chấp nhận yêu cầu mới có thể bắt đầu hội thoại.
* **Quyền riêng tư:** Tính năng chặn người dùng và xóa lịch sử trò chuyện (soft delete).

## Công nghệ sử dụng

Dự án áp dụng các công nghệ và kiến trúc phát triển ứng dụng Android hiện đại:

* **Ngôn ngữ lập trình:** Kotlin.
* **Giao diện người dùng:** Jetpack Compose (Declarative UI).
* **Kiến trúc:** MVVM (Model-View-ViewModel).
* **Backend (Firebase):**
    * **Authentication:** Quản lý xác thực.
    * **Cloud Firestore:** Cơ sở dữ liệu NoSQL lưu trữ thông tin người dùng, bài viết và tin nhắn.
    * **Firebase Storage:** Lưu trữ hình ảnh.
    * **Cloud Messaging (FCM):** Hệ thống thông báo đẩy.

## Cài đặt và Triển khai

1.  **Clone repository:**
    ```bash
    git clone [https://github.com/username/LocaSOS-UserApp.git](https://github.com/username/LocaSOS-UserApp.git)
    ```
2.  **Cấu hình môi trường:**
    * Tải file cấu hình `google-services.json` từ Firebase Console và đặt vào thư mục `app/`.
    * Đảm bảo máy ảo hoặc thiết bị chạy Android 12.0 trở lên.
3.  **Biên dịch:**
    * Mở dự án bằng Android Studio.
    * Sync Gradle và chạy ứng dụng.

## Nhóm thực hiện

* **Đặng Anh Khoa** – 23520732
* **Lê Văn Phong** – 23521165
* **Hồ Ngọc Thiên Phước** – 23521230

---
Khoa Công nghệ Phần mềm - Trường Đại học Công nghệ Thông tin
