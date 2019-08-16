package exception

open class ShortcutThrowable(open val msg: String) : Exception() {

    open fun json(): String {
        return Message(Shortcut.OTHER, msg, null).json()
    }

    class OK(
        override val msg: String = "success",
        val data: Any? = null
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.OK, msg, data).json()
        }
    }

    class AE(
        override val msg: String = "argument mismatch"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.AE, msg, null).json()
        }
    }

    class UR(
        override val msg: String = "user has been registered"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.UR, msg, null).json()
        }
    }

    class UNE(
        override val msg: String = "user not found"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.UNE, msg, null).json()
        }
    }

    class UPE(
        override val msg: String = "wrong password"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.UPE, msg, null).json()
        }
    }

    class TE(
        override val msg: String = "invalid token"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.TE, msg, null).json()
        }
    }

    class BNE(
        override val msg: String = "blog not found"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.BNE, msg, null).json()
        }
    }

    class AIF(
        override val msg: String = "argument format incorrect"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.AIF, msg, null).json()
        }
    }

    class TNE(
        override val msg: String = "topic not found"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.TNE, msg, null).json()
        }
    }

    class PME(
        override val msg: String = "permission dined"
    ) : ShortcutThrowable(msg) {
        override fun json(): String {
            return Message(Shortcut.PME, msg, null).json()
        }
    }

    class OTHER(
        override val msg: String = "UNKNOWN ERROR"
    ) : ShortcutThrowable(msg)

    /**
     *     AE, // argument mismatch
     *     UR, // user have been registered
     *     OK, // success
     *     UNE, // user not found
     *     UPE, // password error
     *     TE, // invalid token
     *     BNE, // blog not found
     *     AIF, // argument format mismatch
     *     TNE, // topic/tag not found
     *     PME, // permission not allowed
     *     OTHER
     */
}