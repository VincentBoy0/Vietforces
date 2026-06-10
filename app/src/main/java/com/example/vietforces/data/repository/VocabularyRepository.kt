package com.example.vietforces.data.repository

import com.example.vietforces.R
import com.example.vietforces.data.model.SentenceItem
import com.example.vietforces.data.model.VocabularyItem

/**
 * Repository containing all vocabulary data.
 * This is where you add new words - just add to the appropriate list!
 *
 * In the future, this can be migrated to a database or loaded from JSON.
 */
object VocabularyRepository {

    /**
     * All vocabulary items - the single source of truth for word data.
     * Used for: Image to Word, Word to Image, Syllable Matching games
     */
    val allVocabulary: List<VocabularyItem> = listOf(
        // ==================== ANIMALS (20 words) ====================
        VocabularyItem(
            id = "animal_001", word = "mèo", classifier = "con",
            imageResId = R.drawable.animal_001,
            distractors = listOf("chó", "gà", "vịt"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_002", word = "chó", classifier = "con",
            imageResId = R.drawable.animal_002,
            distractors = listOf("mèo", "gà", "vịt"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_003", word = "gà", classifier = "con",
            imageResId = R.drawable.animal_003,
            distractors = listOf("mèo", "chó", "vịt"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_004", word = "vịt", classifier = "con",
            imageResId = R.drawable.animal_004,
            distractors = listOf("mèo", "chó", "gà"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_005", word = "cá", classifier = "con",
            imageResId = R.drawable.animal_005,
            distractors = listOf("tôm", "cua", "ếch"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_006", word = "heo", classifier = "con",
            imageResId = R.drawable.animal_006,
            distractors = listOf("bò", "dê", "thỏ"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_007", word = "bò", classifier = "con",
            imageResId = R.drawable.animal_007,
            distractors = listOf("heo", "dê", "ngựa"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_008", word = "dê", classifier = "con",
            imageResId = R.drawable.animal_008,
            distractors = listOf("bò", "heo", "thỏ"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_009", word = "thỏ", classifier = "con",
            imageResId = R.drawable.animal_009,
            distractors = listOf("mèo", "sóc", "chuột"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_010", word = "voi", classifier = "con",
            imageResId = R.drawable.animal_010,
            distractors = listOf("hổ", "ngựa", "khỉ"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_011", word = "hổ", classifier = "con",
            imageResId = R.drawable.animal_011,
            distractors = listOf("voi", "sư tử", "báo"), category = "animals", difficulty = 2
        ),
        VocabularyItem(
            id = "animal_012", word = "ngựa", classifier = "con",
            imageResId = R.drawable.animal_012,
            distractors = listOf("bò", "dê", "voi"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_013", word = "chim", classifier = "con",
            imageResId = R.drawable.animal_013,
            distractors = listOf("ong", "bướm", "chuồn chuồn"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_014", word = "ong", classifier = "con",
            imageResId = R.drawable.animal_014,
            distractors = listOf("chim", "bướm", "kiến"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_015", word = "cua", classifier = "con",
            imageResId = R.drawable.animal_015,
            distractors = listOf("tôm", "cá", "ốc"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_016", word = "tôm", classifier = "con",
            imageResId = R.drawable.animal_016,
            distractors = listOf("cua", "cá", "mực"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_017", word = "ếch", classifier = "con",
            imageResId = R.drawable.animal_017,
            distractors = listOf("cóc", "rắn", "cá"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_018", word = "rắn", classifier = "con",
            imageResId = R.drawable.animal_018,
            distractors = listOf("ếch", "cá sấu", "thằn lằn"), category = "animals", difficulty = 2
        ),
        VocabularyItem(
            id = "animal_019", word = "sóc", classifier = "con",
            imageResId = R.drawable.animal_019,
            distractors = listOf("thỏ", "chuột", "khỉ"), category = "animals", difficulty = 1
        ),
        VocabularyItem(
            id = "animal_020", word = "khỉ", classifier = "con",
            imageResId = R.drawable.animal_020,
            distractors = listOf("vượn", "sóc", "thỏ"), category = "animals", difficulty = 1
        ),

        // ==================== SCHOOL SUPPLIES (12 words) ====================
        VocabularyItem(
            id = "school_001", word = "bút", classifier = "cây",
            imageResId = R.drawable.school_001,
            distractors = listOf("bút chì", "thước", "kéo"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_002", word = "bút chì", classifier = null,
            imageResId = R.drawable.school_002,
            distractors = listOf("bút", "tẩy", "thước"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_003", word = "tẩy", classifier = "cục",
            imageResId = R.drawable.school_003,
            distractors = listOf("bút chì", "phấn", "kéo"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_004", word = "sách", classifier = "quyển",
            imageResId = R.drawable.school_004,
            distractors = listOf("vở", "truyện", "báo"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_005", word = "vở", classifier = "cuốn",
            imageResId = R.drawable.school_005,
            distractors = listOf("sách", "giấy", "truyện"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_006", word = "thước", classifier = "cái",
            imageResId = R.drawable.school_006,
            distractors = listOf("kéo", "bút", "tẩy"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_007", word = "kéo", classifier = "cái",
            imageResId = R.drawable.school_007,
            distractors = listOf("thước", "dao", "bút"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_008", word = "cặp", classifier = "cái",
            imageResId = R.drawable.school_008,
            distractors = listOf("túi", "ba lô", "hộp"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_009", word = "bàn học", classifier = null,
            imageResId = R.drawable.school_009,
            distractors = listOf("ghế học", "bàn ăn", "tủ sách"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_010", word = "ghế học", classifier = null,
            imageResId = R.drawable.school_010,
            distractors = listOf("bàn học", "ghế sofa", "giường"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_011", word = "bảng đen", classifier = null,
            imageResId = R.drawable.school_011,
            distractors = listOf("bảng trắng", "màn hình", "tranh"), category = "school", difficulty = 1
        ),
        VocabularyItem(
            id = "school_012", word = "hộp bút", classifier = null,
            imageResId = R.drawable.school_012,
            distractors = listOf("cặp", "túi", "bao"), category = "school", difficulty = 1
        ),

        // ==================== HOUSEHOLD ITEMS (13 words) ====================
        VocabularyItem(
            id = "household_001", word = "bàn", classifier = "cái",
            imageResId = R.drawable.household_001,
            distractors = listOf("ghế", "tủ", "giường"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_002", word = "ghế", classifier = "cái",
            imageResId = R.drawable.household_002,
            distractors = listOf("bàn", "giường", "tủ"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_003", word = "giường", classifier = "cái",
            imageResId = R.drawable.household_003,
            distractors = listOf("bàn", "ghế", "sofa"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_004", word = "tủ", classifier = "cái",
            imageResId = R.drawable.household_004,
            distractors = listOf("bàn", "ghế", "kệ"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_005", word = "gối", classifier = "cái",
            imageResId = R.drawable.household_005,
            distractors = listOf("chăn", "nệm", "mền"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_006", word = "chăn", classifier = "cái",
            imageResId = R.drawable.household_006,
            distractors = listOf("gối", "nệm", "ga"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_007", word = "đèn", classifier = "cái",
            imageResId = R.drawable.household_007,
            distractors = listOf("quạt", "máy lạnh", "tivi"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_008", word = "quạt", classifier = "cái",
            imageResId = R.drawable.household_008,
            distractors = listOf("đèn", "máy lạnh", "tivi"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_009", word = "tủ lạnh", classifier = null,
            imageResId = R.drawable.household_009,
            distractors = listOf("máy giặt", "điều hòa", "tivi"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_010", word = "máy giặt", classifier = null,
            imageResId = R.drawable.household_010,
            distractors = listOf("tủ lạnh", "máy sấy", "điều hòa"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_011", word = "điều hòa", classifier = null,
            imageResId = R.drawable.household_011,
            distractors = listOf("quạt", "tủ lạnh", "máy sưởi"), category = "household", difficulty = 2
        ),
        VocabularyItem(
            id = "household_012", word = "khóa", classifier = "cái",
            imageResId = R.drawable.household_012,
            distractors = listOf("chìa khóa", "cửa", "then"), category = "household", difficulty = 1
        ),
        VocabularyItem(
            id = "household_013", word = "chìa khóa", classifier = null,
            imageResId = R.drawable.household_013,
            distractors = listOf("khóa", "cửa", "móc"), category = "household", difficulty = 1
        ),

        // ==================== KITCHEN (12 words) ====================
        VocabularyItem(
            id = "kitchen_001", word = "nồi", classifier = "cái",
            imageResId = R.drawable.kitchen_001,
            distractors = listOf("chảo", "ấm", "xoong"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_002", word = "chảo", classifier = "cái",
            imageResId = R.drawable.kitchen_002,
            distractors = listOf("nồi", "ấm", "xoong"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_003", word = "dao", classifier = "con",
            imageResId = R.drawable.kitchen_003,
            distractors = listOf("thớt", "kéo", "muỗng"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_004", word = "thớt", classifier = "cái",
            imageResId = R.drawable.kitchen_004,
            distractors = listOf("dao", "bàn", "kệ"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_005", word = "muỗng", classifier = "cái",
            imageResId = R.drawable.kitchen_005,
            distractors = listOf("nĩa", "đũa", "dao"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_006", word = "nĩa", classifier = "cái",
            imageResId = R.drawable.kitchen_006,
            distractors = listOf("muỗng", "đũa", "dao"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_007", word = "đũa", classifier = "cái",
            imageResId = R.drawable.kitchen_007,
            distractors = listOf("muỗng", "nĩa", "que"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_008", word = "bát", classifier = "cái",
            imageResId = R.drawable.kitchen_008,
            distractors = listOf("tô", "chén", "đĩa"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_009", word = "tô", classifier = "cái",
            imageResId = R.drawable.kitchen_009,
            distractors = listOf("bát", "chén", "đĩa"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_010", word = "ly", classifier = "cái",
            imageResId = R.drawable.kitchen_010,
            distractors = listOf("cốc", "chén", "bình"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_011", word = "chén", classifier = "cái",
            imageResId = R.drawable.kitchen_011,
            distractors = listOf("bát", "tô", "ly"), category = "kitchen", difficulty = 1
        ),
        VocabularyItem(
            id = "kitchen_012", word = "ấm", classifier = "cái",
            imageResId = R.drawable.kitchen_012,
            distractors = listOf("nồi", "bình", "chảo"), category = "kitchen", difficulty = 1
        ),

        // ==================== FOOD & DRINKS (20 words) ====================
        VocabularyItem(
            id = "food_001", word = "bánh mì", classifier = null,
            imageResId = R.drawable.food_001,
            distractors = listOf("bánh bao", "bánh cuốn", "xôi"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_002", word = "phở bò", classifier = null,
            imageResId = R.drawable.food_002,
            distractors = listOf("bún bò", "phở gà", "hủ tiếu"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_003", word = "bún bò", classifier = null,
            imageResId = R.drawable.food_003,
            distractors = listOf("phở bò", "bún riêu", "bún chả"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_004", word = "cơm gà", classifier = null,
            imageResId = R.drawable.food_004,
            distractors = listOf("cơm sườn", "cơm chiên", "cháo gà"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_005", word = "cháo gà", classifier = null,
            imageResId = R.drawable.food_005,
            distractors = listOf("cháo lòng", "cơm gà", "súp"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_006", word = "trứng rán", classifier = null,
            imageResId = R.drawable.food_006,
            distractors = listOf("trứng luộc", "trứng ốp", "trứng hấp"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_007", word = "trứng luộc", classifier = null,
            imageResId = R.drawable.food_007,
            distractors = listOf("trứng rán", "trứng ốp", "trứng hấp"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_008", word = "cá kho", classifier = null,
            imageResId = R.drawable.food_008,
            distractors = listOf("cá chiên", "cá hấp", "cá nướng"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_009", word = "thịt nướng", classifier = null,
            imageResId = R.drawable.food_009,
            distractors = listOf("thịt kho", "thịt luộc", "thịt chiên"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_010", word = "rau muống", classifier = null,
            imageResId = R.drawable.food_010,
            distractors = listOf("rau cải", "rau xà lách", "rau mồng tơi"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_011", word = "cà chua", classifier = null,
            imageResId = R.drawable.food_011,
            distractors = listOf("củ hành", "ớt", "tỏi"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_012", word = "củ hành", classifier = null,
            imageResId = R.drawable.food_012,
            distractors = listOf("cà chua", "tỏi", "gừng"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_013", word = "táo", classifier = "trái",
            imageResId = R.drawable.food_013,
            distractors = listOf("cam", "chuối", "xoài"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_014", word = "cam", classifier = "trái",
            imageResId = R.drawable.food_014,
            distractors = listOf("táo", "chuối", "bưởi"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_015", word = "chuối", classifier = "trái",
            imageResId = R.drawable.food_015,
            distractors = listOf("táo", "cam", "xoài"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_016", word = "nước lọc", classifier = null,
            imageResId = R.drawable.food_016,
            distractors = listOf("nước cam", "nước mía", "trà"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_017", word = "cà phê", classifier = null,
            imageResId = R.drawable.food_017,
            distractors = listOf("trà", "nước cam", "sữa"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_018", word = "trà sữa", classifier = null,
            imageResId = R.drawable.food_018,
            distractors = listOf("cà phê sữa", "nước cam", "sinh tố"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_019", word = "nước cam", classifier = null,
            imageResId = R.drawable.food_019,
            distractors = listOf("nước mía", "nước chanh", "nước dừa"), category = "food", difficulty = 1
        ),
        VocabularyItem(
            id = "food_020", word = "nước mía", classifier = null,
            imageResId = R.drawable.food_020,
            distractors = listOf("nước cam", "nước dừa", "nước chanh"), category = "food", difficulty = 1
        ),

        // ==================== PLACES (15 words) ====================
        VocabularyItem(
            id = "place_001", word = "ngôi nhà", classifier = null,
            imageResId = R.drawable.place_001,
            distractors = listOf("căn hộ", "biệt thự", "chung cư"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_002", word = "trường học", classifier = null,
            imageResId = R.drawable.place_002,
            distractors = listOf("lớp học", "thư viện", "bệnh viện"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_003", word = "lớp học", classifier = null,
            imageResId = R.drawable.place_003,
            distractors = listOf("trường học", "phòng họp", "thư viện"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_004", word = "thư viện", classifier = null,
            imageResId = R.drawable.place_004,
            distractors = listOf("nhà sách", "trường học", "bảo tàng"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_005", word = "bệnh viện", classifier = null,
            imageResId = R.drawable.place_005,
            distractors = listOf("tiệm thuốc", "phòng khám", "trường học"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_006", word = "siêu thị", classifier = null,
            imageResId = R.drawable.place_006,
            distractors = listOf("chợ", "cửa hàng", "nhà hàng"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_007", word = "nhà hàng", classifier = null,
            imageResId = R.drawable.place_007,
            distractors = listOf("quán ăn", "siêu thị", "quán cà phê"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_008", word = "quán ăn", classifier = null,
            imageResId = R.drawable.place_008,
            distractors = listOf("nhà hàng", "căng tin", "quán cà phê"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_009", word = "quán cà phê", classifier = null,
            imageResId = R.drawable.place_009,
            distractors = listOf("quán trà", "nhà hàng", "quán ăn"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_010", word = "công viên", classifier = null,
            imageResId = R.drawable.place_010,
            distractors = listOf("sân vận động", "bãi biển", "hồ bơi"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_011", word = "sân bay", classifier = null,
            imageResId = R.drawable.place_011,
            distractors = listOf("nhà ga", "bến xe", "bến cảng"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_012", word = "nhà ga", classifier = null,
            imageResId = R.drawable.place_012,
            distractors = listOf("sân bay", "bến xe", "bến tàu"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_013", word = "bến xe", classifier = null,
            imageResId = R.drawable.place_013,
            distractors = listOf("nhà ga", "sân bay", "bãi đỗ xe"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_014", word = "bưu điện", classifier = null,
            imageResId = R.drawable.place_014,
            distractors = listOf("ngân hàng", "cửa hàng", "tiệm thuốc"), category = "places", difficulty = 1
        ),
        VocabularyItem(
            id = "place_015", word = "tiệm thuốc", classifier = null,
            imageResId = R.drawable.place_015,
            distractors = listOf("bệnh viện", "phòng khám", "bưu điện"), category = "places", difficulty = 1
        ),

        // ==================== VEHICLES (6 words) ====================
        VocabularyItem(
            id = "vehicle_001", word = "xe máy", classifier = null,
            imageResId = R.drawable.vehicle_001,
            distractors = listOf("xe đạp", "ô tô", "xe buýt"), category = "vehicles", difficulty = 1
        ),
        VocabularyItem(
            id = "vehicle_002", word = "xe đạp", classifier = null,
            imageResId = R.drawable.vehicle_002,
            distractors = listOf("xe máy", "ô tô", "xe ba bánh"), category = "vehicles", difficulty = 1
        ),
        VocabularyItem(
            id = "vehicle_003", word = "ô tô", classifier = null,
            imageResId = R.drawable.vehicle_003,
            distractors = listOf("xe máy", "xe buýt", "taxi"), category = "vehicles", difficulty = 1
        ),
        VocabularyItem(
            id = "vehicle_004", word = "xe buýt", classifier = null,
            imageResId = R.drawable.vehicle_004,
            distractors = listOf("ô tô", "tàu hỏa", "xe khách"), category = "vehicles", difficulty = 1
        ),
        VocabularyItem(
            id = "vehicle_005", word = "tàu hỏa", classifier = null,
            imageResId = R.drawable.vehicle_005,
            distractors = listOf("xe buýt", "máy bay", "tàu thủy"), category = "vehicles", difficulty = 1
        ),
        VocabularyItem(
            id = "vehicle_006", word = "máy bay", classifier = null,
            imageResId = R.drawable.vehicle_006,
            distractors = listOf("tàu hỏa", "trực thăng", "tàu thủy"), category = "vehicles", difficulty = 1
        ),

        // ==================== BODY & CLOTHING (11 words) ====================
        VocabularyItem(
            id = "body_001", word = "mái tóc", classifier = null,
            imageResId = R.drawable.body_001,
            distractors = listOf("đôi mắt", "cái mũi", "cái miệng"), category = "body", difficulty = 1
        ),
        VocabularyItem(
            id = "body_002", word = "đôi mắt", classifier = null,
            imageResId = R.drawable.body_002,
            distractors = listOf("mái tóc", "cái mũi", "đôi tai"), category = "body", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_001", word = "mũ", classifier = "cái",
            imageResId = R.drawable.clothing_001,
            distractors = listOf("nón", "khăn", "kính"), category = "clothing", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_002", word = "áo thun", classifier = null,
            imageResId = R.drawable.clothing_002,
            distractors = listOf("áo sơ mi", "áo khoác", "áo len"), category = "clothing", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_003", word = "áo sơ mi", classifier = null,
            imageResId = R.drawable.clothing_003,
            distractors = listOf("áo thun", "áo khoác", "áo vest"), category = "clothing", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_004", word = "áo khoác", classifier = null,
            imageResId = R.drawable.clothing_004,
            distractors = listOf("áo thun", "áo sơ mi", "áo len"), category = "clothing", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_005", word = "quần jean", classifier = null,
            imageResId = R.drawable.clothing_005,
            distractors = listOf("quần tây", "quần short", "quần kaki"), category = "clothing", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_006", word = "giày", classifier = "đôi",
            imageResId = R.drawable.clothing_006,
            distractors = listOf("dép", "sandal", "bốt"), category = "clothing", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_007", word = "dép lê", classifier = null,
            imageResId = R.drawable.clothing_007,
            distractors = listOf("dép xỏ ngón", "giày", "sandal"), category = "clothing", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_008", word = "túi xách", classifier = null,
            imageResId = R.drawable.clothing_008,
            distractors = listOf("ba lô", "ví", "cặp"), category = "clothing", difficulty = 1
        ),
        VocabularyItem(
            id = "clothing_009", word = "đồng hồ", classifier = null,
            imageResId = R.drawable.clothing_009,
            distractors = listOf("vòng tay", "nhẫn", "dây chuyền"), category = "clothing", difficulty = 1
        )
    )

    /**
     * Sentences for sentence-based exercises
     */
    val allSentences: List<SentenceItem> = listOf(
        SentenceItem(
            id = "sentence_001",
            fullSentence = "Tôi thích ăn cơm",
            words = listOf("Tôi", "thích", "ăn", "cơm"),
            blankWordIndex = 2,
            translation = "I like to eat rice",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_002",
            fullSentence = "Con mèo rất dễ thương",
            words = listOf("Con", "mèo", "rất", "dễ", "thương"),
            blankWordIndex = 1,
            translation = "The cat is very cute",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_003",
            fullSentence = "Cái bàn này màu trắng",
            words = listOf("Cái", "bàn", "này", "màu", "trắng"),
            blankWordIndex = 4,
            translation = "This table is white",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_004",
            fullSentence = "Tôi đi học mỗi ngày",
            words = listOf("Tôi", "đi", "học", "mỗi", "ngày"),
            blankWordIndex = 2,
            translation = "I go to school every day",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_005",
            fullSentence = "Trời hôm nay rất đẹp",
            words = listOf("Trời", "hôm", "nay", "rất", "đẹp"),
            blankWordIndex = 4,
            translation = "The weather today is very nice",
            difficulty = 2
        ),
        // New sentences (01-40)
        SentenceItem(
            id = "sentence_006",
            fullSentence = "Tôi uống cà phê vào buổi sáng",
            words = listOf("Tôi", "uống", "cà", "phê", "vào", "buổi", "sáng"),
            blankWordIndex = 1,
            translation = "I drink coffee in the morning",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_007",
            fullSentence = "Bạn giúp tôi được không",
            words = listOf("Bạn", "giúp", "tôi", "được", "không"),
            blankWordIndex = 1,
            translation = "Can you help me?",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_008",
            fullSentence = "Hôm nay trời rất nóng",
            words = listOf("Hôm", "nay", "trời", "rất", "nóng"),
            blankWordIndex = 4,
            translation = "The weather is very hot today",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_009",
            fullSentence = "Tôi sống ở thành phố này",
            words = listOf("Tôi", "sống", "ở", "thành", "phố", "này"),
            blankWordIndex = 1,
            translation = "I live in this city",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_010",
            fullSentence = "Anh ấy đi đến trường bằng xe buýt",
            words = listOf("Anh", "ấy", "đi", "đến", "trường", "bằng", "xe", "buýt"),
            blankWordIndex = 2,
            translation = "He goes to school by bus",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_011",
            fullSentence = "Em làm bài tập vào buổi tối",
            words = listOf("Em", "làm", "bài", "tập", "vào", "buổi", "tối"),
            blankWordIndex = 1,
            translation = "I do homework in the evening",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_012",
            fullSentence = "Chúng tôi ăn cơm ở nhà",
            words = listOf("Chúng", "tôi", "ăn", "cơm", "ở", "nhà"),
            blankWordIndex = 2,
            translation = "We eat at home",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_013",
            fullSentence = "Tôi đọc sách trước khi ngủ",
            words = listOf("Tôi", "đọc", "sách", "trước", "khi", "ngủ"),
            blankWordIndex = 1,
            translation = "I read books before sleeping",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_014",
            fullSentence = "Bạn tên là gì",
            words = listOf("Bạn", "tên", "là", "gì"),
            blankWordIndex = 1,
            translation = "What is your name?",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_015",
            fullSentence = "Tôi gặp bạn vào ngày mai",
            words = listOf("Tôi", "gặp", "bạn", "vào", "ngày", "mai"),
            blankWordIndex = 1,
            translation = "I will meet you tomorrow",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_016",
            fullSentence = "Cửa hàng này mở lúc 9 giờ",
            words = listOf("Cửa", "hàng", "này", "mở", "lúc", "9", "giờ"),
            blankWordIndex = 3,
            translation = "This shop opens at 9 o'clock",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_017",
            fullSentence = "Bạn hãy đóng cửa lại",
            words = listOf("Bạn", "hãy", "đóng", "cửa", "lại"),
            blankWordIndex = 2,
            translation = "Please close the door",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_018",
            fullSentence = "Tôi để điện thoại ở trên bàn",
            words = listOf("Tôi", "để", "điện", "thoại", "ở", "trên", "bàn"),
            blankWordIndex = 1,
            translation = "I left my phone on the table",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_019",
            fullSentence = "Bạn có thể nói chậm hơn không",
            words = listOf("Bạn", "có", "thể", "nói", "chậm", "hơn", "không"),
            blankWordIndex = 3,
            translation = "Can you speak more slowly?",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_020",
            fullSentence = "Tôi học tiếng Việt mỗi ngày",
            words = listOf("Tôi", "học", "tiếng", "Việt", "mỗi", "ngày"),
            blankWordIndex = 1,
            translation = "I learn Vietnamese every day",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_021",
            fullSentence = "Tôi mua một chiếc áo mới",
            words = listOf("Tôi", "mua", "một", "chiếc", "áo", "mới"),
            blankWordIndex = 1,
            translation = "I bought a new shirt",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_022",
            fullSentence = "Bạn trả tiền mặt hay thẻ",
            words = listOf("Bạn", "trả", "tiền", "mặt", "hay", "thẻ"),
            blankWordIndex = 1,
            translation = "Do you pay by cash or card?",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_023",
            fullSentence = "Tôi suýt quên chìa khóa",
            words = listOf("Tôi", "suýt", "quên", "chìa", "khóa"),
            blankWordIndex = 1,
            translation = "I almost forgot the key",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_024",
            fullSentence = "Xin lỗi cho tôi hỏi",
            words = listOf("Xin", "lỗi", "cho", "tôi", "hỏi"),
            blankWordIndex = 1,
            translation = "Excuse me, may I ask?",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_025",
            fullSentence = "Cảm ơn bạn rất nhiều",
            words = listOf("Cảm", "ơn", "bạn", "rất", "nhiều"),
            blankWordIndex = 1,
            translation = "Thank you very much",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_026",
            fullSentence = "Tôi cần nước vì tôi khát",
            words = listOf("Tôi", "cần", "nước", "vì", "tôi", "khát"),
            blankWordIndex = 1,
            translation = "I need water because I'm thirsty",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_027",
            fullSentence = "Bạn thích món này không",
            words = listOf("Bạn", "thích", "món", "này", "không"),
            blankWordIndex = 1,
            translation = "Do you like this dish?",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_028",
            fullSentence = "Tôi ở đây một tuần",
            words = listOf("Tôi", "ở", "đây", "một", "tuần"),
            blankWordIndex = 1,
            translation = "I stayed here for a week",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_029",
            fullSentence = "Chúng ta cùng bắt đầu nhé",
            words = listOf("Chúng", "ta", "cùng", "bắt", "đầu", "nhé"),
            blankWordIndex = 2,
            translation = "Let's start",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_030",
            fullSentence = "Bạn có nghe nhạc không",
            words = listOf("Bạn", "có", "nghe", "nhạc", "không"),
            blankWordIndex = 1,
            translation = "Do you listen to music?",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_031",
            fullSentence = "Tôi thấy rất mệt hôm nay",
            words = listOf("Tôi", "thấy", "rất", "mệt", "hôm", "nay"),
            blankWordIndex = 1,
            translation = "I feel very tired today",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_032",
            fullSentence = "Cô ấy hay cười rất tươi",
            words = listOf("Cô", "ấy", "hay", "cười", "rất", "tươi"),
            blankWordIndex = 2,
            translation = "She smiles very brightly",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_033",
            fullSentence = "Tôi muốn gọi một ly trà đá",
            words = listOf("Tôi", "muốn", "gọi", "một", "ly", "trà", "đá"),
            blankWordIndex = 2,
            translation = "I want to order a glass of iced tea",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_034",
            fullSentence = "Bạn nên rẽ trái ở ngã tư",
            words = listOf("Bạn", "nên", "rẽ", "trái", "ở", "ngã", "tư"),
            blankWordIndex = 1,
            translation = "You should turn left at the intersection",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_035",
            fullSentence = "Tôi biết đường về nhà",
            words = listOf("Tôi", "biết", "đường", "về", "nhà"),
            blankWordIndex = 1,
            translation = "I know the way home",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_036",
            fullSentence = "Bạn hãy chụp ảnh giúp tôi nhé",
            words = listOf("Bạn", "hãy", "chụp", "ảnh", "giúp", "tôi", "nhé"),
            blankWordIndex = 1,
            translation = "Please take a photo for me",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_037",
            fullSentence = "Tôi thường làm việc đến khuya",
            words = listOf("Tôi", "thường", "làm", "việc", "đến", "khuya"),
            blankWordIndex = 1,
            translation = "I often work late",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_038",
            fullSentence = "Bạn phải đến đúng giờ",
            words = listOf("Bạn", "phải", "đến", "đúng", "giờ"),
            blankWordIndex = 1,
            translation = "You need to arrive on time",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_039",
            fullSentence = "Tôi có một câu hỏi",
            words = listOf("Tôi", "có", "một", "câu", "hỏi"),
            blankWordIndex = 1,
            translation = "I have a question",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_040",
            fullSentence = "Hôm nay tôi định đi siêu thị",
            words = listOf("Hôm", "nay", "tôi", "định", "đi", "siêu", "thị"),
            blankWordIndex = 3,
            translation = "Today I plan to go to the supermarket",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_041",
            fullSentence = "Tôi đã làm mất ví rồi",
            words = listOf("Tôi", "đã", "làm", "mất", "ví", "rồi"),
            blankWordIndex = 1,
            translation = "I lost my wallet",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_042",
            fullSentence = "Bạn cho tôi mượn bút được không",
            words = listOf("Bạn", "cho", "tôi", "mượn", "bút", "được", "không"),
            blankWordIndex = 1,
            translation = "Can you lend me a pen?",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_043",
            fullSentence = "Tôi sẽ ngủ sớm tối nay",
            words = listOf("Tôi", "sẽ", "ngủ", "sớm", "tối", "nay"),
            blankWordIndex = 1,
            translation = "I will sleep early tonight",
            difficulty = 1
        ),
        SentenceItem(
            id = "sentence_044",
            fullSentence = "Chúng tôi đi dạo sau bữa tối",
            words = listOf("Chúng", "tôi", "đi", "dạo", "sau", "bữa", "tối"),
            blankWordIndex = 2,
            translation = "We go for a walk after dinner",
            difficulty = 2
        ),
        SentenceItem(
            id = "sentence_045",
            fullSentence = "Tôi gửi bạn một tin nhắn",
            words = listOf("Tôi", "gửi", "bạn", "một", "tin", "nhắn"),
            blankWordIndex = 1,
            translation = "I sent you a message",
            difficulty = 1
        )
    )

    /**
     * Word item with Vietnamese word, English translation, and non-diacritic version
     */
    data class WordItem(
        val vietnamese: String,
        val english: String,
        val noAccent: String, // Non-diacritic version for matching
        val category: String
    )

    /**
     * Full vocabulary dataset for word chain and word search games
     * Format: vietnamese|english|noAccent (grid_word)
     */
    val wordDataset: List<WordItem> = listOf(
        // ==================== OBJECTS & HOUSEHOLD ====================
        WordItem("cái bàn", "table", "caiban", "household"),
        WordItem("cái ghế", "chair", "caighe", "household"),
        WordItem("cái giường", "bed", "caigiuong", "household"),
        WordItem("cái tủ", "cabinet", "caitu", "household"),
        WordItem("cái gối", "pillow", "caigoi", "household"),
        WordItem("cái chăn", "blanket", "caichan", "household"),
        WordItem("cái đèn", "lamp", "caiden", "household"),
        WordItem("cái quạt", "fan", "caiquat", "household"),
        WordItem("tủ lạnh", "fridge", "tulanh", "household"),
        WordItem("máy giặt", "washing machine", "maygiat", "household"),
        WordItem("điều hòa", "air conditioner", "dieuhoa", "household"),
        WordItem("cái khóa", "lock", "caikhoa", "household"),
        WordItem("chìa khóa", "key", "chiakhoa", "household"),
        WordItem("cái cửa", "door", "caicua", "household"),
        WordItem("cửa sổ", "window", "cuaso", "household"),
        WordItem("bức tường", "wall", "buctuong", "household"),
        WordItem("cái kệ", "shelf", "caike", "household"),
        WordItem("cái thảm", "carpet", "caitham", "household"),
        WordItem("cái gương", "mirror", "caiguong", "household"),
        WordItem("cái đồng hồ", "clock", "caidongho", "household"),
        WordItem("đồng hồ đeo tay", "watch", "donghodeotay", "household"),
        WordItem("cái túi", "bag", "caitui", "household"),
        WordItem("túi xách", "handbag", "tuixach", "household"),
        WordItem("ba lô", "backpack", "balo", "household"),
        WordItem("cái ô", "umbrella", "caio", "household"),
        WordItem("áo mưa", "raincoat", "aomua", "clothing"),
        WordItem("cái nón", "hat", "cainon", "clothing"),
        WordItem("cái mũ", "cap", "caimu", "clothing"),
        WordItem("đôi giày", "shoes", "doigiay", "clothing"),
        WordItem("dép lê", "slippers", "deple", "clothing"),
        WordItem("áo thun", "t-shirt", "aothun", "clothing"),
        WordItem("áo sơ mi", "shirt", "aosomi", "clothing"),
        WordItem("áo khoác", "jacket", "aokhoac", "clothing"),
        WordItem("quần jean", "jeans", "quanjean", "clothing"),
        WordItem("váy", "dress", "vay", "clothing"),
        WordItem("khăn quàng", "scarf", "khanquang", "clothing"),
        WordItem("kính mắt", "glasses", "kinhmat", "clothing"),
        WordItem("cái ví", "wallet", "caivi", "household"),
        WordItem("cái hộp", "box", "caihop", "household"),
        WordItem("cái chai", "bottle", "caichai", "household"),
        WordItem("cái bình", "jug", "caibinh", "household"),

        // ==================== SCHOOL & OFFICE SUPPLIES ====================
        WordItem("cây bút", "pen", "caybut", "school"),
        WordItem("bút chì", "pencil", "butchi", "school"),
        WordItem("cục tẩy", "eraser", "cuctay", "school"),
        WordItem("quyển sách", "book", "quyensach", "school"),
        WordItem("cuốn vở", "notebook", "cuonvo", "school"),
        WordItem("tập giấy", "paper pad", "tapgiay", "school"),
        WordItem("cái thước", "ruler", "caithuoc", "school"),
        WordItem("cái kéo", "scissors", "caikeo", "school"),
        WordItem("hồ dán", "glue", "hodan", "school"),
        WordItem("băng keo", "tape", "bangkeo", "school"),
        WordItem("cái ghim", "staple", "caighim", "school"),
        WordItem("kẹp giấy", "paper clip", "kepgiay", "school"),
        WordItem("cái cặp", "school bag", "caicap", "school"),
        WordItem("hộp bút", "pencil case", "hopbut", "school"),
        WordItem("bảng đen", "blackboard", "bangden", "school"),
        WordItem("bảng trắng", "whiteboard", "bangtrang", "school"),
        WordItem("cục phấn", "chalk", "cucphan", "school"),
        WordItem("bút lông", "marker", "butlong", "school"),
        WordItem("máy tính", "computer", "maytinh", "school"),
        WordItem("điện thoại", "phone", "dienthoai", "school"),
        WordItem("tai nghe", "headphones", "tainghe", "school"),
        WordItem("bàn phím", "keyboard", "banphim", "school"),
        WordItem("chuột máy tính", "mouse", "chuotmaytinh", "school"),
        WordItem("máy in", "printer", "mayin", "school"),
        WordItem("máy ảnh", "camera", "mayanh", "school"),

        // ==================== KITCHEN ====================
        WordItem("cái nồi", "pot", "cainoi", "kitchen"),
        WordItem("cái chảo", "pan", "caichao", "kitchen"),
        WordItem("cái thớt", "cutting board", "caithot", "kitchen"),
        WordItem("con dao", "knife", "condao", "kitchen"),
        WordItem("cái muỗng", "spoon", "caimuong", "kitchen"),
        WordItem("cái nĩa", "fork", "cainia", "kitchen"),
        WordItem("đôi đũa", "chopsticks", "doidua", "kitchen"),
        WordItem("cái chén", "bowl", "caichen", "kitchen"),
        WordItem("cái bát", "bowl", "caibat", "kitchen"),
        WordItem("cái tô", "bowl", "caito", "kitchen"),
        WordItem("cái ly", "glass", "caily", "kitchen"),
        WordItem("cái cốc", "cup", "caicoc", "kitchen"),
        WordItem("cái ấm", "kettle", "caiam", "kitchen"),
        WordItem("bếp gas", "gas stove", "bepgas", "kitchen"),
        WordItem("lò vi sóng", "microwave", "lovisong", "kitchen"),
        WordItem("nồi cơm điện", "rice cooker", "noicomdien", "kitchen"),
        WordItem("chảo chống dính", "non-stick pan", "chaochongdinh", "kitchen"),
        WordItem("khăn lau", "cleaning cloth", "khanlau", "kitchen"),

        // ==================== FOOD & DRINKS ====================
        WordItem("bánh mì", "bread", "banhmi", "food"),
        WordItem("cơm", "rice", "com", "food"),
        WordItem("mì", "noodles", "mi", "food"),
        WordItem("phở", "pho", "pho", "food"),
        WordItem("bún", "rice vermicelli", "bun", "food"),
        WordItem("cháo", "porridge", "chao", "food"),
        WordItem("trứng", "egg", "trung", "food"),
        WordItem("trứng rán", "fried egg", "trungran", "food"),
        WordItem("trứng luộc", "boiled egg", "trungluoc", "food"),
        WordItem("gà", "chicken", "ga", "food"),
        WordItem("cá", "fish", "ca", "food"),
        WordItem("thịt", "meat", "thit", "food"),
        WordItem("thịt heo", "pork", "thitheo", "food"),
        WordItem("thịt bò", "beef", "thitbo", "food"),
        WordItem("rau", "vegetables", "rau", "food"),
        WordItem("cà chua", "tomato", "cachua", "food"),
        WordItem("củ hành", "onion", "cuhanh", "food"),
        WordItem("tỏi", "garlic", "toi", "food"),
        WordItem("gừng", "ginger", "gung", "food"),
        WordItem("ớt", "chili", "ot", "food"),
        WordItem("đường", "sugar", "duong", "food"),
        WordItem("muối", "salt", "muoi", "food"),
        WordItem("nước mắm", "fish sauce", "nuocmam", "food"),
        WordItem("dầu ăn", "cooking oil", "dauan", "food"),
        WordItem("sữa", "milk", "sua", "food"),
        WordItem("sữa chua", "yogurt", "suachua", "food"),
        WordItem("nước", "water", "nuoc", "food"),
        WordItem("nước lọc", "water", "nuocloc", "food"),
        WordItem("nước cam", "orange juice", "nuoccam", "food"),
        WordItem("nước mía", "sugarcane juice", "nuocmia", "food"),
        WordItem("cà phê", "coffee", "caphe", "food"),
        WordItem("trà", "tea", "tra", "food"),
        WordItem("trà sữa", "milk tea", "trasua", "food"),
        WordItem("nước ngọt", "soft drink", "nuocngot", "food"),
        WordItem("bánh ngọt", "cake", "banhngot", "food"),
        WordItem("kẹo", "candy", "keo", "food"),
        WordItem("kem", "ice cream", "kem", "food"),
        WordItem("trái táo", "apple", "traitao", "food"),
        WordItem("trái cam", "orange", "traicam", "food"),
        WordItem("trái chuối", "banana", "traichuoi", "food"),
        WordItem("trái nho", "grapes", "trainho", "food"),
        WordItem("dưa hấu", "watermelon", "duahau", "food"),
        WordItem("xoài", "mango", "xoai", "food"),
        WordItem("dứa", "pineapple", "dua", "food"),

        // ==================== ANIMALS ====================
        WordItem("con mèo", "cat", "conmeo", "animals"),
        WordItem("con chó", "dog", "concho", "animals"),
        WordItem("con gà", "chicken", "conga", "animals"),
        WordItem("con vịt", "duck", "convit", "animals"),
        WordItem("con cá", "fish", "conca", "animals"),
        WordItem("con heo", "pig", "conheo", "animals"),
        WordItem("con bò", "cow", "conbo", "animals"),
        WordItem("con dê", "goat", "conde", "animals"),
        WordItem("con thỏ", "rabbit", "contho", "animals"),
        WordItem("con voi", "elephant", "convoi", "animals"),
        WordItem("con hổ", "tiger", "conho", "animals"),
        WordItem("con ngựa", "horse", "conngua", "animals"),
        WordItem("con chim", "bird", "conchim", "animals"),
        WordItem("con ong", "bee", "conong", "animals"),
        WordItem("con cua", "crab", "concua", "animals"),
        WordItem("con tôm", "shrimp", "contom", "animals"),
        WordItem("con ếch", "frog", "conech", "animals"),
        WordItem("con rắn", "snake", "conran", "animals"),
        WordItem("con sóc", "squirrel", "consoc", "animals"),
        WordItem("con khỉ", "monkey", "conkhi", "animals"),
        WordItem("con cá mập", "shark", "concamap", "animals"),

        // ==================== PLACES ====================
        WordItem("ngôi nhà", "house", "ngoinha", "places"),
        WordItem("phòng khách", "living room", "phongkhach", "places"),
        WordItem("phòng ngủ", "bedroom", "phongngu", "places"),
        WordItem("nhà bếp", "kitchen", "nhabep", "places"),
        WordItem("nhà tắm", "bathroom", "nhatam", "places"),
        WordItem("trường học", "school", "truonghoc", "places"),
        WordItem("lớp học", "classroom", "lophoc", "places"),
        WordItem("thư viện", "library", "thuvien", "places"),
        WordItem("bệnh viện", "hospital", "benhvien", "places"),
        WordItem("siêu thị", "supermarket", "sieuthi", "places"),
        WordItem("chợ", "market", "cho", "places"),
        WordItem("công viên", "park", "congvien", "places"),
        WordItem("nhà hàng", "restaurant", "nhahang", "places"),
        WordItem("quán ăn", "eatery", "quanan", "places"),
        WordItem("quán cà phê", "cafe", "quancaphe", "places"),
        WordItem("tiệm bánh", "bakery", "tiembanh", "places"),
        WordItem("tiệm thuốc", "pharmacy", "tiemthuoc", "places"),
        WordItem("bưu điện", "post office", "buudien", "places"),
        WordItem("ngân hàng", "bank", "nganhang", "places"),
        WordItem("trạm xe buýt", "bus stop", "tramxebuyt", "places"),
        WordItem("bến xe", "bus station", "benxe", "places"),
        WordItem("nhà ga", "train station", "nhaga", "places"),
        WordItem("sân bay", "airport", "sanbay", "places"),
        WordItem("cửa hàng", "shop", "cuahang", "places"),
        WordItem("trung tâm", "center", "trungtam", "places"),
        WordItem("rạp phim", "cinema", "rapphim", "places"),
        WordItem("sân vận động", "stadium", "sanvandong", "places"),

        // ==================== VEHICLES ====================
        WordItem("xe máy", "motorbike", "xemay", "vehicles"),
        WordItem("xe đạp", "bicycle", "xedap", "vehicles"),
        WordItem("ô tô", "car", "oto", "vehicles"),
        WordItem("xe buýt", "bus", "xebuyt", "vehicles"),
        WordItem("tàu hỏa", "train", "tauhoa", "vehicles"),
        WordItem("máy bay", "airplane", "maybay", "vehicles"),
        WordItem("tàu điện", "metro", "taudien", "vehicles"),
        WordItem("thuyền", "boat", "thuyen", "vehicles"),

        // ==================== COLORS ====================
        WordItem("màu đỏ", "red", "maudo", "colors"),
        WordItem("màu xanh", "blue/green", "mauxanh", "colors"),
        WordItem("màu vàng", "yellow", "mauvang", "colors"),
        WordItem("màu đen", "black", "mauden", "colors"),
        WordItem("màu trắng", "white", "mautrang", "colors"),
        WordItem("màu hồng", "pink", "mauhong", "colors"),
        WordItem("màu tím", "purple", "mautim", "colors"),
        WordItem("màu cam", "orange", "maucam", "colors"),
        WordItem("màu nâu", "brown", "maunau", "colors"),
        WordItem("màu xám", "gray", "mauxam", "colors"),

        // ==================== THỜI GIAN & THỜI TIẾT ====================
        WordItem("hôm nay", "today", "homnay", "time"),
        WordItem("ngày mai", "tomorrow", "ngaymai", "time"),
        WordItem("hôm qua", "yesterday", "homqua", "time"),
        WordItem("buổi sáng", "morning", "buoisang", "time"),
        WordItem("buổi trưa", "noon", "buoitrua", "time"),
        WordItem("buổi tối", "evening", "buoitoi", "time"),
        WordItem("ban đêm", "night", "bandem", "time"),
        WordItem("bây giờ", "now", "baygio", "time"),
        WordItem("tuần này", "this week", "tuannay", "time"),
        WordItem("tháng này", "this month", "thangnay", "time"),
        WordItem("mùa hè", "summer", "muahe", "time"),
        WordItem("mùa đông", "winter", "muadong", "time"),
        WordItem("trời nắng", "sunny", "troinang", "weather"),
        WordItem("trời mưa", "rainy", "troimua", "weather"),
        WordItem("trời gió", "windy", "troigio", "weather"),
        WordItem("trời lạnh", "cold", "troilanh", "weather"),
        WordItem("trời nóng", "hot", "troinong", "weather"),
        WordItem("mây mù", "foggy", "maymu", "weather"),

        // ==================== BASIC VERBS ====================
        WordItem("ăn", "eat", "an", "verbs"),
        WordItem("uống", "drink", "uong", "verbs"),
        WordItem("đi", "go", "di", "verbs"),
        WordItem("đến", "arrive", "den", "verbs"),
        WordItem("về", "return", "ve", "verbs"),
        WordItem("ngủ", "sleep", "ngu", "verbs"),
        WordItem("đọc", "read", "doc", "verbs"),
        WordItem("viết", "write", "viet", "verbs"),
        WordItem("học", "study", "hoc", "verbs"),
        WordItem("làm", "do/work", "lam", "verbs"),
        WordItem("chơi", "play", "choi", "verbs"),
        WordItem("nghe", "listen", "nghe", "verbs"),
        WordItem("nói", "speak", "noi", "verbs"),
        WordItem("gặp", "meet", "gap", "verbs"),
        WordItem("mua", "buy", "mua", "verbs"),
        WordItem("bán", "sell", "ban", "verbs"),
        WordItem("mở", "open", "mo", "verbs"),
        WordItem("đóng", "close", "dong", "verbs"),
        WordItem("đợi", "wait", "doi", "verbs"),
        WordItem("chạy", "run", "chay", "verbs"),
        WordItem("đứng", "stand", "dung", "verbs"),
        WordItem("ngồi", "sit", "ngoi", "verbs"),
        WordItem("nhìn", "look", "nhin", "verbs"),
        WordItem("tìm", "find", "tim", "verbs"),
        WordItem("giúp", "help", "giup", "verbs"),
        WordItem("gửi", "send", "gui", "verbs"),
        WordItem("nhận", "receive", "nhan", "verbs"),
        WordItem("cười", "smile", "cuoi", "verbs"),
        WordItem("khóc", "cry", "khoc", "verbs"),
        WordItem("nấu", "cook", "nau", "verbs"),

        // ==================== BASIC ADJECTIVES ====================
        WordItem("đẹp", "beautiful", "dep", "adjectives"),
        WordItem("xấu", "ugly", "xau", "adjectives"),
        WordItem("cao", "tall/high", "cao", "adjectives"),
        WordItem("thấp", "low/short", "thap", "adjectives"),
        WordItem("to", "big", "to", "adjectives"),
        WordItem("nhỏ", "small", "nho", "adjectives"),
        WordItem("dài", "long", "dai", "adjectives"),
        WordItem("ngắn", "short", "ngan", "adjectives"),
        WordItem("nhanh", "fast", "nhanh", "adjectives"),
        WordItem("chậm", "slow", "cham", "adjectives"),
        WordItem("mới", "new", "moi", "adjectives"),
        WordItem("cũ", "old", "cu", "adjectives"),
        WordItem("nóng", "hot", "nong", "adjectives"),
        WordItem("lạnh", "cold", "lanh", "adjectives"),
        WordItem("vui", "happy", "vui", "adjectives"),
        WordItem("buồn", "sad", "buon", "adjectives"),
        WordItem("mệt", "tired", "met", "adjectives"),
        WordItem("khỏe", "healthy", "khoe", "adjectives"),
        WordItem("no", "full", "no", "adjectives"),
        WordItem("đói", "hungry", "doi", "adjectives"),
        WordItem("rẻ", "cheap", "re", "adjectives"),
        WordItem("đắt", "expensive", "dat", "adjectives"),
        WordItem("dễ", "easy", "de", "adjectives"),
        WordItem("khó", "difficult", "kho", "adjectives"),
        WordItem("sạch", "clean", "sach", "adjectives"),
        WordItem("bẩn", "dirty", "ban", "adjectives"),
        WordItem("ồn", "noisy", "on", "adjectives"),
        WordItem("yên tĩnh", "quiet", "yentinh", "adjectives")
    )

    /**
     * Words for word chain game - all words from dataset
     */
    val wordChainWords: List<String> = wordDataset.map { it.vietnamese }

    /**
     * Map from non-diacritic to Vietnamese word for matching
     */
    val noAccentToVietnamese: Map<String, String> = wordDataset.associate { it.noAccent to it.vietnamese }

    /**
     * Map from Vietnamese to non-diacritic for reverse lookup
     */
    val vietnameseToNoAccent: Map<String, String> = wordDataset.associate { it.vietnamese to it.noAccent }

    /**
     * Get Vietnamese word from input (can be Vietnamese or non-diacritic)
     */
    fun findVietnameseWord(input: String): String? {
        val normalized = input.lowercase().replace(" ", "")
        // First check if it's already a Vietnamese word
        if (wordDataset.any { it.vietnamese == input.lowercase() }) {
            return input.lowercase()
        }
        // Check non-diacritic version
        return noAccentToVietnamese[normalized]
    }

    /**
     * Check if an input matches a word (Vietnamese or non-diacritic)
     */
    fun matchesWord(input: String, targetWord: String): Boolean {
        val inputLower = input.lowercase().replace(" ", "")
        val targetLower = targetWord.lowercase()
        val targetNoAccent = vietnameseToNoAccent[targetLower] ?: ""

        return inputLower == targetLower.replace(" ", "") || inputLower == targetNoAccent
    }

    // Helper functions

    /**
     * Get vocabulary items that have classifiers (for syllable matching game)
     */
    fun getVocabularyWithClassifiers(): List<VocabularyItem> =
        allVocabulary.filter { it.classifier != null }

    /**
     * Get vocabulary by category
     */
    fun getVocabularyByCategory(category: String): List<VocabularyItem> =
        allVocabulary.filter { it.category == category }

    /**
     * Get vocabulary by difficulty
     */
    fun getVocabularyByDifficulty(difficulty: Int): List<VocabularyItem> =
        allVocabulary.filter { it.difficulty <= difficulty }

    /**
     * Get random vocabulary items
     */
    fun getRandomVocabulary(count: Int): List<VocabularyItem> =
        allVocabulary.shuffled().take(count)

    /**
     * Get all available categories
     */
    fun getAllCategories(): List<String> =
        allVocabulary.map { it.category }.distinct()
}

