package com.example.se114.data

object SelectionData {
    val locations = mapOf(
        "Hồ Chí Minh" to listOf("Quận 1", "Quận 3", "Quận 5", "Quận 10", "Bình Thạnh", "Tân Bình", "Thủ Đức", "Gò Vấp"),
        "Hà Nội" to listOf("Ba Đình", "Hoàn Kiếm", "Tây Hồ", "Cầu Giấy", "Đống Đa", "Hai Bà Trưng"),
        "Đà Nẵng" to listOf("Hải Châu", "Thanh Khê", "Sơn Trà", "Ngũ Hành Sơn"),
        "Cần Thơ" to listOf("Ninh Kiều", "Bình Thủy", "Cái Răng"),
        "Hải Phòng" to listOf("Hồng Bàng", "Ngô Quyền", "Lê Chân")
    )

    fun getCategories(type: PostType): List<String> {
        return when (type) {
            PostType.SUPPORT -> listOf(
                "Cứu hộ khẩn cấp", "Y tế", "Tìm đồ thất lạc", "Quyên góp", "Hỗ trợ di chuyển", "Khác"
            )
            PostType.SERVICE -> listOf(
                "Sửa chữa", "Vệ sinh", "Gia sư", "Vận chuyển", "Chăm sóc thú cưng", "Khác"
            )
        }
    }
}