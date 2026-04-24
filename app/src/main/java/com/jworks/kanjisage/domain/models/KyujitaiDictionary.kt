package com.jworks.kanjisage.domain.models

object KyujitaiDictionary {

    private val kyujitaiToShinjitai = mapOf(
        // Education / Knowledge
        "學" to "学", "國" to "国", "語" to "語", "讀" to "読", "書" to "書",
        "數" to "数", "圖" to "図", "會" to "会", "體" to "体", "聲" to "声",
        "藝" to "芸", "點" to "点", "畫" to "画", "晝" to "昼", "區" to "区",

        // People / Society
        "兒" to "児", "單" to "単", "齡" to "齢", "齊" to "斉", "黨" to "党",
        "當" to "当", "權" to "権", "勞" to "労", "團" to "団", "關" to "関",
        "醫" to "医", "應" to "応", "從" to "従", "價" to "価", "佛" to "仏",

        // Action / Movement
        "來" to "来", "發" to "発", "轉" to "転", "歸" to "帰", "歲" to "歳",
        "戰" to "戦", "賣" to "売", "營" to "営", "驛" to "駅", "傳" to "伝",
        "遲" to "遅", "歡" to "歓", "觀" to "観", "對" to "対", "變" to "変",

        // Nature / Geography
        "氣" to "気", "海" to "海", "邊" to "辺", "灣" to "湾", "島" to "島",
        "鐵" to "鉄", "龍" to "竜", "櫻" to "桜", "澤" to "沢", "瀧" to "滝",

        // Communication / Abstract
        "經" to "経", "濟" to "済", "證" to "証", "辯" to "弁", "辨" to "弁",
        "辮" to "弁", "說" to "説", "譯" to "訳", "聽" to "聴", "號" to "号",
        "實" to "実", "寶" to "宝", "廣" to "広", "繩" to "縄", "總" to "総",

        // Structures / Objects
        "樓" to "楼", "橋" to "橋", "藏" to "蔵", "鹽" to "塩", "齒" to "歯",
        "絲" to "糸", "萬" to "万", "疊" to "畳", "爐" to "炉", "燈" to "灯",

        // Descriptors
        "舊" to "旧", "惡" to "悪", "豐" to "豊", "亂" to "乱", "獨" to "独",
        "險" to "険", "嚴" to "厳", "雜" to "雑", "淺" to "浅", "樂" to "楽",
        "輕" to "軽", "靜" to "静", "榮" to "栄", "殘" to "残", "濕" to "湿",

        // Food / Daily
        "飲" to "飲", "麥" to "麦", "餘" to "余", "穗" to "穂", "禮" to "礼",
        "隨" to "随", "雙" to "双", "覺" to "覚", "屬" to "属", "續" to "続",

        // Additional common
        "假" to "仮", "佛" to "仏", "據" to "拠", "擔" to "担", "擴" to "拡",
        "攝" to "摂", "晚" to "晩", "條" to "条", "樣" to "様", "歷" to "歴",
        "氷" to "氷", "淨" to "浄", "獻" to "献", "稱" to "称", "節" to "節",
        "經" to "経", "縣" to "県", "與" to "与", "舉" to "挙", "薰" to "薫",
        "處" to "処", "譽" to "誉", "豫" to "予", "遞" to "逓", "鑄" to "鋳",
    )

    private val shinjitaiToKyujitai: Map<String, List<String>> by lazy {
        val result = mutableMapOf<String, MutableList<String>>()
        kyujitaiToShinjitai.forEach { (old, new) ->
            result.getOrPut(new) { mutableListOf() }.add(old)
        }
        result
    }

    fun getShinjitai(kyujitai: String): String? =
        kyujitaiToShinjitai[kyujitai]

    fun getKyujitai(shinjitai: String): List<String> =
        shinjitaiToKyujitai[shinjitai] ?: emptyList()

    fun isKyujitai(kanji: String): Boolean =
        kanji in kyujitaiToShinjitai

    fun isShinjitaiWithVariant(kanji: String): Boolean =
        kanji in shinjitaiToKyujitai

    fun hasVariant(kanji: String): Boolean =
        isKyujitai(kanji) || isShinjitaiWithVariant(kanji)
}
