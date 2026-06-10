package com.example.vietforces.data.model

/**
 * A roleplay scenario for the AI conversation tutor.
 *
 * The learner chats in Vietnamese with an AI that plays [persona]. Scenarios are
 * grouped by [category] (food, transport, shopping, services, social) so the
 * learner can practise real-life situations.
 *
 * @param persona  who the AI plays + how it should behave (goes into the system prompt)
 * @param opening  the AI's first line, shown immediately without an API call
 * @param goal     a short hint of what the learner should try to accomplish
 * @param goalSteps the concrete sub-steps that make up [goal]; shown as a tick
 *        list in the chat header and tracked as the learner completes them.
 */
data class RoleplayScenario(
    val id: String,
    val title: String,
    val emoji: String,
    val category: String,
    val persona: String,
    val opening: String,
    val goal: String,
    val goalSteps: List<String> = emptyList()
)

/** Predefined roleplay scenarios, grouped by category. */
object RoleplayScenarios {

    val all: List<RoleplayScenario> = listOf(
        // ---------------- Ăn uống (Food & drink) ----------------
        RoleplayScenario(
            id = "pho",
            title = "Quán phở",
            emoji = "🍜",
            category = "Ăn uống",
            persona = "Bạn là cô bán phở ở Hà Nội, thân thiện và nhiệt tình. " +
                "Khách là người nước ngoài vừa vào quán ăn phở.",
            opening = "Em ơi vào ăn phở đi! Em muốn ăn phở bò hay phở gà?",
            goal = "Gọi một bát phở, hỏi giá và trả tiền.",
            goalSteps = listOf("Gọi một bát phở", "Hỏi giá", "Trả tiền")
        ),
        RoleplayScenario(
            id = "cafe",
            title = "Quán cà phê",
            emoji = "☕",
            category = "Ăn uống",
            persona = "Bạn là nhân viên quán cà phê trẻ trung, vui vẻ. Khách vừa bước vào quán.",
            opening = "Chào bạn, bạn muốn uống gì ạ?",
            goal = "Gọi một ly cà phê sữa đá và hỏi có wifi không.",
            goalSteps = listOf("Gọi cà phê sữa đá", "Hỏi có wifi không")
        ),
        RoleplayScenario(
            id = "restaurant",
            title = "Nhà hàng",
            emoji = "🍽️",
            category = "Ăn uống",
            persona = "Bạn là nhân viên phục vụ nhà hàng lịch sự, thân thiện. Khách vừa đến.",
            opening = "Chào anh/chị, anh/chị đi mấy người ạ? Mời ngồi bàn này nhé.",
            goal = "Đặt bàn cho 2 người và gọi vài món ăn.",
            goalSteps = listOf("Đặt bàn cho 2 người", "Gọi vài món ăn")
        ),

        // ---------------- Di chuyển (Getting around) ----------------
        RoleplayScenario(
            id = "grab",
            title = "Đi xe ôm / Grab",
            emoji = "🛵",
            category = "Di chuyển",
            persona = "Bạn là bác tài xế xe ôm Grab vui tính, hay nói chuyện. Khách vừa lên xe.",
            opening = "Chào em, em đi đâu để bác chở nào?",
            goal = "Nói điểm đến, hỏi giá và thời gian đi.",
            goalSteps = listOf("Nói điểm đến", "Hỏi giá", "Hỏi thời gian đi")
        ),
        RoleplayScenario(
            id = "direction",
            title = "Hỏi đường",
            emoji = "🗺️",
            category = "Di chuyển",
            persona = "Bạn là người dân địa phương tốt bụng đang đi bộ trên phố. " +
                "Một người nước ngoài tiến đến hỏi đường.",
            opening = "Em cần hỏi gì à? Em muốn đi đến đâu?",
            goal = "Hỏi đường đến Hồ Gươm và hỏi đi bộ có xa không.",
            goalSteps = listOf("Hỏi đường đến Hồ Gươm", "Hỏi đi bộ có xa không")
        ),

        // ---------------- Mua sắm (Shopping) ----------------
        RoleplayScenario(
            id = "market",
            title = "Mặc cả ở chợ",
            emoji = "🛒",
            category = "Mua sắm",
            persona = "Bạn là người bán hoa quả ở chợ, vui vẻ và hay nói thách giá. " +
                "Khách muốn mua trái cây.",
            opening = "Mua gì đi em ơi! Xoài ngon lắm, mới hái sáng nay đấy!",
            goal = "Hỏi giá xoài và mặc cả để mua được giá rẻ hơn.",
            goalSteps = listOf("Hỏi giá xoài", "Mặc cả giá rẻ hơn", "Chốt mua")
        ),
        RoleplayScenario(
            id = "shop",
            title = "Cửa hàng quần áo",
            emoji = "👕",
            category = "Mua sắm",
            persona = "Bạn là nhân viên cửa hàng quần áo nhiệt tình. Khách đang xem đồ.",
            opening = "Chào bạn, bạn cần tìm gì ạ? Bên mình mới về nhiều mẫu đẹp lắm!",
            goal = "Hỏi mua một chiếc áo, hỏi size và thử đồ.",
            goalSteps = listOf("Hỏi mua một chiếc áo", "Hỏi size", "Xin thử đồ")
        ),

        // ---------------- Dịch vụ (Services) ----------------
        RoleplayScenario(
            id = "hotel",
            title = "Nhận phòng khách sạn",
            emoji = "🏨",
            category = "Dịch vụ",
            persona = "Bạn là lễ tân khách sạn lịch sự, chuyên nghiệp. Khách đến nhận phòng.",
            opening = "Xin chào, em có thể giúp gì cho anh/chị ạ?",
            goal = "Nhận phòng đã đặt và hỏi giờ ăn sáng.",
            goalSteps = listOf("Nhận phòng đã đặt", "Hỏi giờ ăn sáng")
        ),
        RoleplayScenario(
            id = "pharmacy",
            title = "Mua thuốc",
            emoji = "💊",
            category = "Dịch vụ",
            persona = "Bạn là dược sĩ ân cần ở nhà thuốc. Khách trông có vẻ bị ốm nhẹ.",
            opening = "Chào em, em thấy trong người thế nào? Em cần mua thuốc gì?",
            goal = "Nói mình bị đau đầu, sổ mũi và mua thuốc.",
            goalSteps = listOf("Nói triệu chứng (đau đầu, sổ mũi)", "Mua thuốc")
        ),

        // ---------------- Xã giao (Social) ----------------
        RoleplayScenario(
            id = "friend",
            title = "Làm quen bạn mới",
            emoji = "👋",
            category = "Xã giao",
            persona = "Bạn là một bạn trẻ Việt Nam thân thiện, vừa gặp một người nước ngoài " +
                "ở quán cà phê và muốn làm quen.",
            opening = "Chào bạn! Mình tên Minh. Bạn tên gì? Bạn đến từ nước nào?",
            goal = "Giới thiệu bản thân và trò chuyện làm quen.",
            goalSteps = listOf("Giới thiệu tên và quê", "Hỏi tên và quê của bạn mới")
        )
    )

    /** Categories in display order. */
    val categories: List<String> = all.map { it.category }.distinct()

    fun byId(id: String): RoleplayScenario? = all.find { it.id == id }
}
