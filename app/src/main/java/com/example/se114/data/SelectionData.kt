package com.example.se114.data

object SelectionData {
    val locations = mapOf(
        // --- 5 Thành phố trực thuộc trung ương ---
        "city_hcm" to listOf(
            "dist_1",
            "dist_3",
            "dist_4",
            "dist_5",
            "dist_6",
            "dist_7",
            "dist_8",
            "dist_10",
            "dist_11",
            "dist_12",
            "dist_binh_thanh",
            "dist_go_vap",
            "dist_phu_nhuan",
            "dist_tan_binh",
            "dist_tan_phu",
            "dist_binh_tan",
            "dist_thu_duc",
            "dist_cu_chi",
            "dist_hoc_mon",
            "dist_binh_chanh",
            "dist_nha_be"
        ),
        "city_hn" to listOf(
            "dist_ba_dinh",
            "dist_hoan_kiem",
            "dist_tay_ho",
            "dist_cau_giay",
            "dist_dong_da",
            "dist_hai_ba_trung",
            "dist_hoang_mai",
            "dist_thanh_xuan",
            "dist_long_bien",
            "dist_nam_tu_liem",
            "dist_bac_tu_liem",
            "dist_ha_dong",
            "dist_son_tay",
            "dist_gia_lam",
            "dist_dong_anh",
            "dist_soc_son"
        ),
        "city_dn" to listOf(
            "dist_hai_chau",
            "dist_thanh_khe",
            "dist_son_tra",
            "dist_ngu_hanh_son",
            "dist_lien_chieu",
            "dist_cam_le",
            "dist_hoa_vang"),
        "city_hp" to listOf(
            "dist_hong_bang",
            "dist_ngo_quyen",
            "dist_le_chan",
            "dist_hai_an",
            "dist_kien_an",
            "dist_do_son",
            "dist_thuy_nguyen"
        ),
        "city_ct" to listOf(
            "dist_ninh_kieu",
            "dist_binh_thuy",
            "dist_cai_rang",
            "dist_o_mon",
            "dist_thot_not"
        ),

        // --- Các tỉnh còn lại (Xếp theo ABC cho dễ quản lý) ---
        "city_ag" to listOf("dist_long_xuyen", "dist_chau_doc", "dist_tan_chau"),
        "city_brvt" to listOf("dist_vung_tau", "dist_ba_ria", "dist_phu_my", "dist_long_dien", "dist_dat_do", "dist_con_dao"),
        "city_bl" to listOf("dist_bac_lieu_tp", "dist_gia_rai"),
        "city_bk" to listOf("dist_bac_kan_tp"),
        "city_bg" to listOf("dist_bac_giang_tp", "dist_viet_yen"),
        "city_bn" to listOf("dist_bac_ninh_tp", "dist_tu_son", "dist_yen_phong"),
        "city_btre" to listOf("dist_ben_tre_tp", "dist_ba_tri"),
        "city_bd" to listOf("dist_thu_dau_mot", "dist_di_an", "dist_thuan_an", "dist_ben_cat", "dist_tan_uyen"),
        "city_bdi" to listOf("dist_quy_nhon", "dist_an_nhon"),
        "city_bp" to listOf("dist_dong_xoai", "dist_phuoc_long", "dist_binh_long"),
        "city_bth" to listOf("dist_phan_thiet", "dist_la_gi"),
        "city_cm" to listOf("dist_ca_mau_tp", "dist_nam_can"),
        "city_cb" to listOf("dist_cao_bang_tp"),
        "city_dl" to listOf("dist_buon_ma_thuot", "dist_buon_ho"),
        "city_dno" to listOf("dist_gia_nghia"),
        "city_db" to listOf("dist_dien_bien_phu"),
        "city_dnai" to listOf("dist_bien_hoa", "dist_long_khanh", "dist_long_thanh"),
        "city_dt" to listOf("dist_cao_lanh", "dist_sa_dec"),
        "city_gl" to listOf("dist_pleiku", "dist_an_khe"),
        "city_hg" to listOf("dist_ha_giang_tp"),
        "city_hnam" to listOf("dist_phu_ly"),
        "city_ht" to listOf("dist_ha_tinh_tp", "dist_hong_linh"),
        "city_hdu" to listOf("dist_hai_duong_tp", "dist_chi_linh"),
        "city_hgi" to listOf("dist_vi_thanh", "dist_nga_bay"),
        "city_hb" to listOf("dist_hoa_binh_tp"),
        "city_hy" to listOf("dist_hung_yen_tp"),
        "city_kh" to listOf("dist_nha_trang", "dist_cam_ranh"),
        "city_kg" to listOf("dist_rach_gia", "dist_ha_tien", "dist_phu_quoc"),
        "city_kt" to listOf("dist_kon_tum_tp"),
        "city_lc" to listOf("dist_lai_chau_tp"),
        "city_ld" to listOf("dist_da_lat", "dist_bao_loc"),
        "city_ls" to listOf("dist_lang_son_tp"),
        "city_lca" to listOf("dist_lao_cai_tp", "dist_sa_pa"),
        "city_la" to listOf("dist_tan_an", "dist_kien_tuong"),
        "city_nd" to listOf("dist_nam_dinh_tp"),
        "city_na" to listOf("dist_vinh", "dist_cua_lo"),
        "city_nb" to listOf("dist_ninh_binh_tp", "dist_tam_diep"),
        "city_nt" to listOf("dist_phan_rang"),
        "city_pt" to listOf("dist_viet_tri", "dist_phu_tho_tx"),
        "city_py" to listOf("dist_tuy_hoa", "dist_song_cau"),
        "city_qb" to listOf("dist_dong_hoi", "dist_ba_don"),
        "city_qnam" to listOf("dist_tam_ky", "dist_hoi_an"),
        "city_qng" to listOf("dist_quang_ngai_tp"),
        "city_qn" to listOf("dist_ha_long", "dist_mong_cai", "dist_cam_pha"),
        "city_qt" to listOf("dist_dong_ha"),
        "city_st" to listOf("dist_soc_trang_tp"),
        "city_sl" to listOf("dist_son_la_tp"),
        "city_tn" to listOf("dist_tay_ninh_tp"),
        "city_tb" to listOf("dist_thai_binh_tp"),
        "city_tng" to listOf("dist_thai_nguyen_tp", "dist_song_cong"),
        "city_th" to listOf("dist_thanh_hoa_tp", "dist_sam_son"),
        "city_hue" to listOf("dist_hue"),
        "city_tg" to listOf("dist_my_tho", "dist_go_cong"),
        "city_tv" to listOf("dist_tra_vinh_tp"),
        "city_tq" to listOf("dist_tuyen_quang_tp"),
        "city_vl" to listOf("dist_vinh_long_tp"),
        "city_vt" to listOf("dist_vinh_yen", "dist_phuc_yen"),
        "city_yb" to listOf("dist_yen_bai_tp")
    )

    fun getCategoryKeys(type: PostType): List<String> {
        return when (type) {
            PostType.SUPPORT -> listOf(
                "cat_emergency", "cat_medical", "cat_lost_found", "cat_donation",
                "cat_transport_help", "cat_food_water", "cat_volunteer", "report_other"
            )
            PostType.SERVICE -> listOf(
                "cat_repair", "cat_cleaning", "cat_tutor", "cat_delivery",
                "cat_pet_care", "cat_beauty", "cat_it_support", "cat_driver", "report_other"
            )
        }
    }
}

