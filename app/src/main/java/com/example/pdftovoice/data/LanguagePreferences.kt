package com.example.pdftovoice.data

import android.content.Context
import android.content.SharedPreferences
import com.example.pdftovoice.tts.Language
import java.util.*

class LanguagePreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_LANGUAGE_CODE = "selected_language_code"
        private const val KEY_LANGUAGE_NAME = "selected_language_name"
    }
    
    fun saveSelectedLanguage(language: Language) {
        sharedPreferences.edit()
            .putString(KEY_LANGUAGE_CODE, language.code)
            .putString(KEY_LANGUAGE_NAME, language.name)
            .apply()
    }
    
    fun getSelectedLanguage(): Language? {
        val code = sharedPreferences.getString(KEY_LANGUAGE_CODE, null)
        val name = sharedPreferences.getString(KEY_LANGUAGE_NAME, null)
        
        return if (code != null && name != null) {
            val locale = when (code) {
                "en" -> Locale.ENGLISH
                "es" -> Locale.forLanguageTag("es")
                "fr" -> Locale.FRENCH
                "de" -> Locale.GERMAN
                "it" -> Locale.ITALIAN
                "pt" -> Locale.forLanguageTag("pt")
                "ru" -> Locale.forLanguageTag("ru")
                "zh" -> Locale.CHINESE
                "ja" -> Locale.JAPANESE
                "ko" -> Locale.KOREAN
                "ar" -> Locale.forLanguageTag("ar")
                "hi" -> Locale.forLanguageTag("hi")
                "mr" -> Locale.forLanguageTag("mr")
                "ta" -> Locale.forLanguageTag("ta")
                "te" -> Locale.forLanguageTag("te")
                "bn" -> Locale.forLanguageTag("bn")
                "gu" -> Locale.forLanguageTag("gu")
                "kn" -> Locale.forLanguageTag("kn")
                "ml" -> Locale.forLanguageTag("ml")
                "pa" -> Locale.forLanguageTag("pa")
                "nl" -> Locale.forLanguageTag("nl")
                "sv" -> Locale.forLanguageTag("sv")
                "da" -> Locale.forLanguageTag("da")
                "no" -> Locale.forLanguageTag("no")
                "fi" -> Locale.forLanguageTag("fi")
                "pl" -> Locale.forLanguageTag("pl")
                "tr" -> Locale.forLanguageTag("tr")
                "th" -> Locale.forLanguageTag("th")
                else -> Locale.getDefault()
            }
            Language(code, name, locale)
        } else {
            null
        }
    }
    
    fun hasSelectedLanguage(): Boolean {
        return sharedPreferences.contains(KEY_LANGUAGE_CODE)
    }
    
    fun clearSelectedLanguage() {
        sharedPreferences.edit()
            .remove(KEY_LANGUAGE_CODE)
            .remove(KEY_LANGUAGE_NAME)
            .apply()
    }
}
