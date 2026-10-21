package com.carled.app.domain.model

enum class TextProfilePlan(val limit: Int, val title: String) {
    BASIC(3, "Mặc định"), EXPANDED(10, "Mở rộng")
}

data class TextProfile(val id: String, val name: String, val text: String, val size: Int,
    val speed: Int, val mode: String, val r: Int, val g: Int, val b: Int, val rainbow: Boolean = false) {
    fun validate() {
        require(id.isNotBlank() && name.isNotBlank() && name.length <= 40) { "Tên profile cần từ 1 đến 40 ký tự." }
        require(text.isNotBlank() && text.all { it.code in 32..126 }) { "Nội dung cần là ký tự tiếng Anh không dấu." }
        require(size in 1..2 && speed in 1..100 && listOf(r,g,b).all { it in 0..255 }) { "Thông số chữ không hợp lệ." }
        require(mode in listOf("scroll_left", "static", "blink")) { "Hiệu ứng không hợp lệ." }
        require(mode == "scroll_left" || MarqueeLayout.fit(text,size).fits) { "Nội dung quá dài. Rút ngắn hoặc chọn Cuộn trái." }
    }
}

interface TextProfileRepository {
    fun plan(): TextProfilePlan
    fun list(): List<TextProfile>
    fun save(profile: TextProfile)
    fun delete(id: String)
}

/** Resolve this from a verified server entitlement when subscription APIs exist. */
fun interface TextProfilePlanProvider { fun currentPlan(): TextProfilePlan }
object PendingTextProfilePlans : TextProfilePlanProvider {
    override fun currentPlan() = TextProfilePlan.BASIC
}
