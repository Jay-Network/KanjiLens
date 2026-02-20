package com.jworks.kanjilens.domain.models

data class ScanChallenge(
    val targetKanji: String,
    val reading: String,
    val meaning: String,
    val isCompleted: Boolean = false
)

object ScanChallengeKanji {
    /** Common kanji likely found in everyday life — signs, menus, labels, books */
    val CHALLENGE_POOL = listOf(
        ScanChallenge("食", "しょく", "eat/food"),
        ScanChallenge("日", "にち", "day/sun"),
        ScanChallenge("本", "ほん", "book/origin"),
        ScanChallenge("大", "だい", "big"),
        ScanChallenge("中", "ちゅう", "middle"),
        ScanChallenge("人", "じん", "person"),
        ScanChallenge("出", "しゅつ", "exit"),
        ScanChallenge("入", "にゅう", "enter"),
        ScanChallenge("水", "すい", "water"),
        ScanChallenge("火", "か", "fire"),
        ScanChallenge("山", "さん", "mountain"),
        ScanChallenge("川", "かわ", "river"),
        ScanChallenge("学", "がく", "learn"),
        ScanChallenge("生", "せい", "life"),
        ScanChallenge("東", "とう", "east"),
        ScanChallenge("西", "せい", "west"),
        ScanChallenge("南", "なん", "south"),
        ScanChallenge("北", "ほく", "north"),
        ScanChallenge("店", "てん", "store"),
        ScanChallenge("駅", "えき", "station"),
        ScanChallenge("電", "でん", "electricity"),
        ScanChallenge("車", "しゃ", "car"),
        ScanChallenge("道", "どう", "road"),
        ScanChallenge("時", "じ", "time"),
        ScanChallenge("金", "きん", "gold/money"),
        ScanChallenge("魚", "ぎょ", "fish"),
        ScanChallenge("肉", "にく", "meat"),
        ScanChallenge("茶", "ちゃ", "tea"),
        ScanChallenge("花", "か", "flower"),
        ScanChallenge("空", "くう", "sky"),
        ScanChallenge("雨", "あめ", "rain"),
        ScanChallenge("風", "ふう", "wind"),
        ScanChallenge("天", "てん", "heaven"),
        ScanChallenge("月", "つき", "moon/month"),
        ScanChallenge("年", "ねん", "year"),
        ScanChallenge("新", "しん", "new"),
        ScanChallenge("名", "めい", "name"),
        ScanChallenge("白", "しろ", "white"),
        ScanChallenge("黒", "くろ", "black"),
        ScanChallenge("赤", "あか", "red"),
        ScanChallenge("青", "あお", "blue"),
        ScanChallenge("高", "たか", "tall/expensive"),
        ScanChallenge("安", "やす", "cheap/safe"),
        ScanChallenge("国", "こく", "country"),
        ScanChallenge("語", "ご", "language"),
        ScanChallenge("読", "どく", "read"),
        ScanChallenge("書", "しょ", "write"),
        ScanChallenge("話", "わ", "talk"),
        ScanChallenge("見", "けん", "see"),
        ScanChallenge("手", "て", "hand")
    )

    fun getRandomChallenge(): ScanChallenge {
        return CHALLENGE_POOL.random()
    }
}
