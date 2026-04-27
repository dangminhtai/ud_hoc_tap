"""Language detection service for flashcard content."""

from langdetect import detect, LangDetectException
from functools import lru_cache


@lru_cache(maxsize=1000)
def detect_language(text: str) -> str:
    """
    Detect the language of the given text.

    Returns language code: 'en', 'vi', 'fr', 'de', 'es', 'zh-cn', etc.
    Defaults to 'en' if detection fails.
    """
    if not text or not text.strip():
        return "en"

    try:
        # Use minimum 5 characters for reliable detection
        sample = text.strip()[:500]  # Use first 500 chars
        lang = detect(sample)
        return lang
    except LangDetectException:
        return "en"


def get_language_name(lang_code: str) -> str:
    """Get human-readable language name."""
    LANGUAGES = {
        "en": "English",
        "vi": "Tiếng Việt",
        "fr": "Français",
        "de": "Deutsch",
        "es": "Español",
        "zh-cn": "中文 (简体)",
        "zh-tw": "中文 (繁體)",
        "ja": "日本語",
        "ko": "한국어",
        "ru": "Русский",
        "pt": "Português",
        "it": "Italiano",
    }
    return LANGUAGES.get(lang_code, lang_code.upper())


def is_supported_language(lang_code: str) -> bool:
    """Check if language has specialized prompt support."""
    SUPPORTED = {"en", "vi", "fr", "de", "es", "zh-cn", "zh-tw", "ja"}
    return lang_code in SUPPORTED
