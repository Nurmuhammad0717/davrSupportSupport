package uz.davrmobile.support.bot.bot

import uz.davrmobile.support.enm.UserRole
import uz.davrmobile.support.util.roles
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.*

class Utils {
    companion object {
        fun isAdmin(): Boolean {
            val roles = roles()
            return (roles.contains(UserRole.ADMIN) || roles.contains(UserRole.DEV) || roles.contains(UserRole.MODERATOR))
        }

        fun randomHashId(): String {
            return UUID.randomUUID().toString().substringAfterLast("-") + UUID.randomUUID().toString()
                .substringAfterLast("-") + UUID.randomUUID().toString().substringAfterLast("-") + UUID.randomUUID()
                .toString().substringAfterLast("-")
        }

        fun String.prettyPhoneNumber(): String {
            return try {
                var phone = this.trim()
                if (phone.startsWith("+")) phone = phone.substring(1)
                if (phone.startsWith("998")) {
                    val regex = "(\\d{3})(\\d{2})(\\d{3})(\\d{2})(\\d{2})".toRegex()
                    phone.replace(regex, "+$1 $2 $3 $4 $5")
                } else "+$phone"
            } catch (e: Exception) {
                this
            }
        }

        fun String.clearPhone(): String {
            return try {
                this.replace(Regex("[^0-9]"), "")
            } catch (e: Exception) {
                this
            }
        }

        fun String.htmlBold(): String {
            return "<b>$this</b>"
        }

        fun String.htmlItalic(): String {
            return "<i>$this</i>"
        }

        fun String.htmlA(href: String): String {
            return "<a href=\"$href\">$this</a>"
        }

        fun String.htmlUnderline(): String {
            return "<u>$this</u>"
        }

        fun String.htmlStrikeThrough(): String {
            return "<s>$this</s>"
        }

        fun createFilesDirForToday(): String {
            return LocalDate.now().run {
                val month = if (monthValue in 0..9) "0$monthValue" else monthValue.toString()
                val day = if (dayOfMonth in 0..9) "0$dayOfMonth" else dayOfMonth.toString()
                createDir("${createDir("${createDir("./files/$year")}/$month")}/$day")
            }
        }

        private fun createDir(dirPath: String): String {
            Paths.get(dirPath).let { directoryPath ->
                if (!Files.exists(directoryPath)) Files.createDirectories(directoryPath)
            }
            return dirPath
        }
    }
}