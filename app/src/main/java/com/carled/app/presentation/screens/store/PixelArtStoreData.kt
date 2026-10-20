package com.carled.app.presentation.screens.store

import androidx.compose.ui.graphics.Color

data class PixelArtStoreItem(
    val id: String,
    val title: String,
    val author: String,
    val category: String, // "MỚI", "Nhân vật", "Ngày lễ", "Anime", "Đề xuất", "Haunted2026", "Xe hơi"
    val likes: Int,
    val comments: Int,
    val hasAudio: Boolean = false,
    val isLiked: Boolean = false,
    val width: Int = 16,
    val height: Int = 16,
    val colorPalette: Map<Char, Color>,
    val pattern: List<String>
) {
    /**
     * Chuyển đổi pixel art thành mảng nhị phân RGB565 kích thước 64×32 Little Endian
     * tương thích 100% với ESP32 Binary Projection Protocol (0x10 -> 0x11 -> 0x12).
     */
    fun toRgb565Data(): ByteArray {
        val data = ByteArray(64 * 32 * 2)
        var idx = 0
        val scaleX = 64 / width
        val scaleY = 32 / height

        for (y in 0 until 32) {
            val srcY = (y / scaleY).coerceIn(0, height - 1)
            val rowStr = pattern.getOrElse(srcY) { "".padEnd(width, '.') }
            for (x in 0 until 64) {
                val srcX = (x / scaleX).coerceIn(0, width - 1)
                val char = rowStr.getOrElse(srcX) { '.' }
                val color = colorPalette[char] ?: Color.Black
                val r = (color.red * 255).toInt()
                val g = (color.green * 255).toInt()
                val b = (color.blue * 255).toInt()

                val r5 = (r shr 3) and 0x1F
                val g6 = (g shr 2) and 0x3F
                val b5 = (b shr 3) and 0x1F
                val col565 = (r5 shl 11) or (g6 shl 5) or b5

                data[idx++] = (col565 and 0xFF).toByte()
                data[idx++] = ((col565 shr 8) and 0xFF).toByte()
            }
        }
        return data
    }
}

object PixelArtStoreData {
    val items: List<PixelArtStoreItem> = listOf(
        // 1. Cô gái Anime (Anime Girl mắt sao)
        PixelArtStoreItem(
            id = "anime_1",
            title = "Cô Bé Mắt Sao",
            author = "KiraPixel",
            category = "Anime",
            likes = 36,
            comments = 2,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF1B2C24),
                'H' to Color(0xFF2E6B4F),
                'h' to Color(0xFF1F4E38),
                'S' to Color(0xFFFFDAB9),
                'E' to Color(0xFFFFE082),
                'e' to Color(0xFF333333),
                'W' to Color(0xFFFFFFFF),
                'C' to Color(0xFF1A3D2E)
            ),
            pattern = listOf(
                "...HHHHHHHHH....",
                "..HHHHHHHHHHH...",
                ".HHHHHHHHHHHHH..",
                ".HHHHHHHHHHHHH..",
                "HHHHHSSSSSHHHHH.",
                "HHHSSSSSSSSSHHH.",
                "HHSEeSSSEeSSSHH.",
                "HHSEESSSEESSSHH.",
                "HHSSSSSSSSSSSHH.",
                "HHHSSSSWSSSSHHH.",
                ".HHHSSSWSSSHHH..",
                "..HHHSSSHHHHH...",
                "...CCCC.CCCC....",
                "...CCCCCCCC.....",
                "...CCCC.CCCC....",
                "................"
            )
        ),

        // 2. Cờ Pixel Art
        PixelArtStoreItem(
            id = "flag_1",
            title = "Lá Cờ Vinh Quang",
            author = "Vexillo_Art",
            category = "MỚI",
            likes = 13,
            comments = 0,
            hasAudio = false,
            colorPalette = mapOf(
                'B' to Color(0xFF0055A5),
                'W' to Color(0xFFFFFFFF),
                'R' to Color(0xFFD91023),
                'G' to Color(0xFF009639),
                'Y' to Color(0xFFFFD100),
                '.' to Color(0xFF151922)
            ),
            pattern = listOf(
                "BBBBBBBBBBBBBBBB",
                "BBBBBBBBBBBBBBBB",
                "WWWWWWWWWWWWWWWW",
                "WWWWWWWWWWWWWWWW",
                "RRRRRRRRRRRRRRRR",
                "RRRRRRRRRRRRRRRR",
                "RRRRRGGYYGGRRRRR",
                "RRRRRGGYYGGRRRRR",
                "RRRRRRRRRRRRRRRR",
                "RRRRRRRRRRRRRRRR",
                "WWWWWWWWWWWWWWWW",
                "WWWWWWWWWWWWWWWW",
                "BBBBBBBBBBBBBBBB",
                "BBBBBBBBBBBBBBBB",
                "................",
                "................"
            )
        ),

        // 3. Đền cổ Nhật Bản (Torii Shrine)
        PixelArtStoreItem(
            id = "shrine_1",
            title = "Cổng Đền Cổ Kính",
            author = "Kyoto_8Bit",
            category = "Đề xuất",
            likes = 30,
            comments = 1,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF6B8E99),
                'T' to Color(0xFF2C5530),
                't' to Color(0xFF183B1C),
                'R' to Color(0xFFC0392B),
                'W' to Color(0xFFFDFEFE),
                'W' to Color(0xFFBDC3C7),
                'G' to Color(0xFFD4AC0D),
                'B' to Color(0xFF4A235A)
            ),
            pattern = listOf(
                "....TTT.........",
                "...TTTTT..RRRRR.",
                "..TTtTTTT.R...R.",
                "..TTTTTTT.R...R.",
                "...t.TT.t.RRRRR.",
                ".....TT...R...R.",
                "....W..W..R...R.",
                "...WWWWWW.R...R.",
                "..WWG..GWW......",
                "..WWGGGGWW......",
                "..WWG..GWW......",
                "...WWWWWW.......",
                ".....WW.........",
                "....W..W........",
                "...W....W.......",
                "................"
            )
        ),

        // 4. Lâu đài ma Halloween (Haunted Castle)
        PixelArtStoreItem(
            id = "haunt_1",
            title = "Lâu Đài Ma Quái",
            author = "SpookyNights",
            category = "Haunted2026",
            likes = 111,
            comments = 2,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF0F0A1E),
                'S' to Color(0xFF1E1435),
                'B' to Color(0xFF0A0713),
                'O' to Color(0xFFFF6F00),
                'Y' to Color(0xFFFFB300),
                'P' to Color(0xFF6A1B9A),
                'G' to Color(0xFF00E676)
            ),
            pattern = listOf(
                "......B.........",
                ".....BBB...B....",
                "..B..BBB..BBB...",
                ".BBB.BBB.BBBB...",
                ".BBB.BOB.BBBB...",
                ".BBB.BOB.BOBB...",
                ".BBB.BBB.BOBB...",
                "BBBBBBBBBBBBB...",
                "BBBOBBBOBBBBB...",
                "BBBBBBBBBBBBB...",
                "BBBBBOOOBBBBB...",
                "B.B.BOOOB.B.B...",
                "....BOOOB..OO...",
                "..P.BBBBB.OOOO..",
                ".PPP.BBB.OGGGGO.",
                "................"
            )
        ),

        // 5. Mặt trăng và hồn ma (Moon & Ghost)
        PixelArtStoreItem(
            id = "haunt_2",
            title = "Trăng Tròn & Hồn Ma",
            author = "LunaPixel",
            category = "Haunted2026",
            likes = 76,
            comments = 2,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF4A148C),
                'Y' to Color(0xFFFFD54F),
                'y' to Color(0xFFFFCA28),
                'W' to Color(0xFFFFFFFF),
                'B' to Color(0xFF000000),
                'H' to Color(0xFF212121)
            ),
            pattern = listOf(
                "....YYYYYY......",
                "..YYYYYYYYYY....",
                ".YYYYYYYYYYYY...",
                ".YYYYWWWWYYYY...",
                "YYYYWWWWWWYYY...",
                "YYYYWBWBWWYYY...",
                "YYYYWWWWWWYYY...",
                "YYYYWWWWWWYYY...",
                "YYYYWW.W.WYYY...",
                "YYYY..HH..YYY...",
                ".YYY.HHHH.YYY...",
                ".YY.HHHHHH.YY...",
                "..Y.HHH.HH..Y...",
                "....HHHHHH......",
                ".....HHHH.......",
                "................"
            )
        ),

        // 6. Ngôi nhà phù thủy (Witch Cottage)
        PixelArtStoreItem(
            id = "haunt_3",
            title = "Căn Lều Phù Thủy",
            author = "WitchCraft",
            category = "Haunted2026",
            likes = 46,
            comments = 0,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF0B1325),
                'M' to Color(0xFFFFD700),
                'P' to Color(0xFF6A1B9A),
                'p' to Color(0xFF4A148C),
                'W' to Color(0xFF5D4037),
                'O' to Color(0xFFFF7043),
                'G' to Color(0xFF1B5E20)
            ),
            pattern = listOf(
                "..............M.",
                ".............MMM",
                "......P......M..",
                ".....PPP........",
                "....PPPPP.......",
                "...PPPPPPP......",
                "..ppppppppp.....",
                "...WWWWWWW......",
                "...W.O.O.W......",
                "...W.OOO.W......",
                "...WWWWWWW......",
                "..GWW.O.WWG..O..",
                ".GGWW.O.WWGG.OO.",
                "GGGGWWWWWWGGGGGG",
                "GGGGGGGGGGGGGGGG",
                "................"
            )
        ),

        // 7. Băng dán Halloween (Cat & Spider Badges)
        PixelArtStoreItem(
            id = "haunt_4",
            title = "Băng Dán Halloween",
            author = "CuteSpook",
            category = "Ngày lễ",
            likes = 143,
            comments = 3,
            hasAudio = true,
            colorPalette = mapOf(
                '.' to Color(0xFF1E112A),
                'P' to Color(0xFF9C27B0),
                'O' to Color(0xFFFF9800),
                'G' to Color(0xFF8BC34A),
                'B' to Color(0xFF000000),
                'W' to Color(0xFFFFFFFF),
                'Y' to Color(0xFFFFEB3B)
            ),
            pattern = listOf(
                ".PPPPPPPPPPPPPP.",
                "P.Y..B.B..Y...P.",
                "P...BBBBB.....P.",
                ".PPPPPPPPPPPPPP.",
                "................",
                ".OOOOOOOOOOOOOO.",
                "O.B.HALLOWEEN.BO",
                "O..BBBBBBBBBB..O",
                ".OOOOOOOOOOOOOO.",
                "................",
                ".GGGGGGGGGGGGGG.",
                "G..W...B...W...G",
                "G.WWW.BBB.WWW..G",
                ".GGGGGGGGGGGGGG.",
                "................",
                "................"
            )
        ),

        // 8. Cửa sổ ma mị (Mystic Eye)
        PixelArtStoreItem(
            id = "haunt_5",
            title = "Con Mắt Bóng Đêm",
            author = "DarkGaze",
            category = "Haunted2026",
            likes = 82,
            comments = 1,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF080D1A),
                'F' to Color(0xFF37474F),
                'W' to Color(0xFFECEFF1),
                'R' to Color(0xFFE53935),
                'B' to Color(0xFF00E5FF),
                'P' to Color(0xFF000000),
                'L' to Color(0xFFFFD54F)
            ),
            pattern = listOf(
                "FFFFFFFFFFFFFFFF",
                "F......FF......F",
                "F......FF......F",
                "F......FF......F",
                "F...RRRRRRRR...F",
                "F.RRWWWWWWWWRR.F",
                "FRWWWWBBBBWWWWRF",
                "FRWWWBBPPBBWWWRF",
                "FRWWWBBPPBBWWWRF",
                "FRWWWWBBBBWWWWRF",
                "F.RRWWWWWWWWRR.F",
                "F...RRRRRRRR...F",
                "F......FF......F",
                "F......FF......F",
                "FFFFFFFFFFFFFFFF",
                "................"
            )
        ),

        // 9. Bia mộ và ma nhỏ (Tombstone & Ghost)
        PixelArtStoreItem(
            id = "haunt_6",
            title = "Bia Mộ Huyền Bí",
            author = "GhostlyGrave",
            category = "Haunted2026",
            likes = 287,
            comments = 14,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF281C3E),
                'T' to Color(0xFF78909C),
                't' to Color(0xFF546E7A),
                'W' to Color(0xFFFFFFFF),
                'B' to Color(0xFF1A1A1A),
                'G' to Color(0xFF2E7D32)
            ),
            pattern = listOf(
                "................",
                ".....TTTT.......",
                "....TTTTTT......",
                "....TTtTTT......",
                "...TTttttTT.....",
                "...TTtTTtTT.WWW.",
                "...TTttttTTWWWW.",
                "...TTtTTtTTWWBW.",
                "...TTTTTTTTWWWW.",
                "...TTT..TTT.W.W.",
                "...TTTTTTTT.....",
                "...TTTTTTTT.....",
                "..GGGGGGGGGG....",
                ".GGGGGGGGGGGG...",
                "GGGGGGGGGGGGGG..",
                "................"
            )
        ),

        // 10. Quái thú núi đồi (Monster Mountain)
        PixelArtStoreItem(
            id = "nature_1",
            title = "Rừng Núi Hoang Dã",
            author = "NordicPixel",
            category = "Đề xuất",
            likes = 192,
            comments = 5,
            hasAudio = true,
            colorPalette = mapOf(
                '.' to Color(0xFF78909C),
                'M' to Color(0xFF455A64),
                'm' to Color(0xFF37474F),
                'G' to Color(0xFF263238),
                'W' to Color(0xFF90A4AE),
                'L' to Color(0xFF546E7A)
            ),
            pattern = listOf(
                "................",
                ".....M..........",
                "....MMM...M.....",
                "...MMMMM.MMM....",
                "..MMMMMMMMMMM...",
                ".MMMMMMMMMMMMM..",
                "MMMMmMMMMMmMMMM.",
                "GGGGGGGGGGGGGGGG",
                "GGGGGGGGGGGGGGGG",
                "LLLLLLLLLLLLLLLL",
                "LWWLLWWLLWWLLWWL",
                "LLLLLLLLLLLLLLLL",
                "LWWLLWWLLWWLLWWL",
                "LLLLLLLLLLLLLLLL",
                "................",
                "................"
            )
        ),

        // 11. Cậu bé ăn mì (Boy Eating)
        PixelArtStoreItem(
            id = "anime_2",
            title = "Thưởng Thức Ramen",
            author = "TokyoBite",
            category = "Anime",
            likes = 450,
            comments = 12,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF1B2838),
                'H' to Color(0xFF1E88E5),
                'h' to Color(0xFF0D47A1),
                'S' to Color(0xFFFFCC80),
                'R' to Color(0xFFE53935),
                'B' to Color(0xFF26C6DA),
                'N' to Color(0xFFFFF59D),
                'E' to Color(0xFF212121)
            ),
            pattern = listOf(
                "....hhhhh.......",
                "...hHHHHHh......",
                "..hHHHHHHHh.....",
                "..hHSSSSHh......",
                "..hHSEESHh......",
                "...HSSSSSh......",
                "....RRRR........",
                "...RRRRRR.......",
                "..RRRRRRRR......",
                ".RRRR..BBBBB....",
                ".RRR..BNNNNB....",
                ".....BNNNNNB....",
                "......BBBBB.....",
                "................",
                "................",
                "................"
            )
        ),

        // 12. Hoàng hôn biển (Sunset Beach)
        PixelArtStoreItem(
            id = "nature_2",
            title = "Hoàng Hôn Trên Biển",
            author = "OceanBreeze",
            category = "Đề xuất",
            likes = 620,
            comments = 18,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF3949AB),
                'S' to Color(0xFFFFD54F),
                'O' to Color(0xFFFF7043),
                'B' to Color(0xFF1E88E5),
                'b' to Color(0xFF0D47A1),
                'A' to Color(0xFFFFB74D),
                'H' to Color(0xFF4E342E)
            ),
            pattern = listOf(
                ".......SS.......",
                "......SSSS......",
                ".......SS.......",
                "................",
                "OOOOOOOOOOOOOOOO",
                "BBBBBBBBBBBBBBBB",
                "BBBBbBBBBbBBBBbB",
                "BBBBBBBBBBBBBBBB",
                "bbbbbbbbbbbbbbbb",
                "AAAAAAAAAAAAAAAA",
                "AAAAAAAA.H.AAAAA",
                "AAAAAAAAHHHAAAAA",
                "AAAAAAA.HHH.AAAA",
                "AAAAAAAAAAAAAAAA",
                "................",
                "................"
            )
        ),

        // 13. Xe đua Cyberpunk (Sportscar)
        PixelArtStoreItem(
            id = "car_1",
            title = "Siêu Xe Cyber Cyan",
            author = "SpeedDemon",
            category = "Xe hơi",
            likes = 540,
            comments = 22,
            hasAudio = false,
            colorPalette = mapOf(
                '.' to Color(0xFF0B0E14),
                'C' to Color(0xFF00E5FF),
                'c' to Color(0xFF0097A7),
                'W' to Color(0xFFFFFFFF),
                'R' to Color(0xFFFF1744),
                'B' to Color(0xFF212121),
                'Y' to Color(0xFFFFEA00)
            ),
            pattern = listOf(
                "................",
                "......CCCC......",
                "....CCCCCCCC....",
                "...CCWWWWWWCC...",
                "..CCCCCCCCCCCC..",
                ".CCCCCCCCCCCCCC.",
                "CCCCCCCCCCCCCCCC",
                "CCCCCCCCCCCCCCCC",
                "CCCCCCCCCCCCCCCC",
                ".CC...CCCC...CC.",
                ".BB...BBBB...BB.",
                "..B....BB....B..",
                "................",
                "................",
                "................",
                "................"
            )
        )
    )
}
