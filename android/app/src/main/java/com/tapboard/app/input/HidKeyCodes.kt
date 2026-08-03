package com.tapboard.app.input

/**
 * HID usage IDs (Keyboard/Keypad page 0x07) for Bluetooth HID reports.
 */
object HidKeyCodes {
    const val A = 4
    const val B = 5
    const val C = 6
    const val D = 7
    const val E = 8
    const val F = 9
    const val G = 10
    const val H = 11
    const val I = 12
    const val J = 13
    const val K = 14
    const val L = 15
    const val M = 16
    const val N = 17
    const val O = 18
    const val P = 19
    const val Q = 20
    const val R = 21
    const val S = 22
    const val T = 23
    const val U = 24
    const val V = 25
    const val W = 26
    const val X = 27
    const val Y = 28
    const val Z = 29
    const val NUM_1 = 30
    const val NUM_2 = 31
    const val NUM_3 = 32
    const val NUM_4 = 33
    const val NUM_5 = 34
    const val NUM_6 = 35
    const val NUM_7 = 36
    const val NUM_8 = 37
    const val NUM_9 = 38
    const val NUM_0 = 39
    const val ENTER = 40
    const val ESCAPE = 41
    const val BACKSPACE = 42
    const val TAB = 43
    const val SPACE = 44
    const val MINUS = 45
    const val EQUAL = 46
    const val LEFT_BRACKET = 47
    const val RIGHT_BRACKET = 48
    const val BACKSLASH = 49
    const val SEMICOLON = 51
    const val APOSTROPHE = 52
    const val GRAVE = 53
    const val COMMA = 54
    const val PERIOD = 55
    const val SLASH = 56
    const val CAPS_LOCK = 57
    const val F1 = 58
    const val F2 = 59
    const val F3 = 60
    const val F4 = 61
    const val F5 = 62
    const val F6 = 63
    const val F7 = 64
    const val F8 = 65
    const val F9 = 66
    const val F10 = 67
    const val F11 = 68
    const val F12 = 69
    const val PRINT_SCREEN = 70
    const val SCROLL_LOCK = 71
    const val PAUSE = 72
    const val INSERT = 73
    const val HOME = 74
    const val PAGE_UP = 75
    const val DELETE = 76
    const val END = 77
    const val PAGE_DOWN = 78
    const val RIGHT = 79
    const val LEFT = 80
    const val DOWN = 81
    const val UP = 82
    const val MUTE = 127
    const val VOLUME_UP = 128
    const val VOLUME_DOWN = 129

    // Consumer page helpers for media keys
    const val MEDIA_PLAY_PAUSE = 0xCD
    const val MEDIA_NEXT = 0xB5
    const val MEDIA_PREV = 0xB6

    const val MOD_LCTRL = 0x01
    const val MOD_LSHIFT = 0x02
    const val MOD_LALT = 0x04
    const val MOD_LGUI = 0x08
    const val MOD_RCTRL = 0x10
    const val MOD_RSHIFT = 0x20
    const val MOD_RALT = 0x40
    const val MOD_RGUI = 0x80

    private val letterMap = ('a'..'z').mapIndexed { i, c -> c to (A + i) }.toMap()
    private val digitMap = mapOf(
        '1' to NUM_1, '2' to NUM_2, '3' to NUM_3, '4' to NUM_4, '5' to NUM_5,
        '6' to NUM_6, '7' to NUM_7, '8' to NUM_8, '9' to NUM_9, '0' to NUM_0
    )

    fun fromChar(ch: Char): Pair<Int, Int>? {
        when (ch) {
            ' ' -> return SPACE to 0
            '\n' -> return ENTER to 0
            '\t' -> return TAB to 0
        }
        val lower = ch.lowercaseChar()
        letterMap[lower]?.let { return it to if (ch.isUpperCase()) MOD_LSHIFT else 0 }
        digitMap[ch]?.let { return it to 0 }
        val shifted = mapOf(
            '!' to (NUM_1 to MOD_LSHIFT),
            '@' to (NUM_2 to MOD_LSHIFT),
            '#' to (NUM_3 to MOD_LSHIFT),
            '$' to (NUM_4 to MOD_LSHIFT),
            '%' to (NUM_5 to MOD_LSHIFT),
            '^' to (NUM_6 to MOD_LSHIFT),
            '&' to (NUM_7 to MOD_LSHIFT),
            '*' to (NUM_8 to MOD_LSHIFT),
            '(' to (NUM_9 to MOD_LSHIFT),
            ')' to (NUM_0 to MOD_LSHIFT),
            '-' to (MINUS to 0),
            '_' to (MINUS to MOD_LSHIFT),
            '=' to (EQUAL to 0),
            '+' to (EQUAL to MOD_LSHIFT),
            '[' to (LEFT_BRACKET to 0),
            '{' to (LEFT_BRACKET to MOD_LSHIFT),
            ']' to (RIGHT_BRACKET to 0),
            '}' to (RIGHT_BRACKET to MOD_LSHIFT),
            '\\' to (BACKSLASH to 0),
            '|' to (BACKSLASH to MOD_LSHIFT),
            ';' to (SEMICOLON to 0),
            ':' to (SEMICOLON to MOD_LSHIFT),
            '\'' to (APOSTROPHE to 0),
            '"' to (APOSTROPHE to MOD_LSHIFT),
            '`' to (GRAVE to 0),
            '~' to (GRAVE to MOD_LSHIFT),
            ',' to (COMMA to 0),
            '<' to (COMMA to MOD_LSHIFT),
            '.' to (PERIOD to 0),
            '>' to (PERIOD to MOD_LSHIFT),
            '/' to (SLASH to 0),
            '?' to (SLASH to MOD_LSHIFT)
        )
        return shifted[ch]
    }
}
