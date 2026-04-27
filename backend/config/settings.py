from pydantic import BaseModel, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class LLMRetryConfig(BaseModel):
    """L l m retry config class."""
    max_retries: int = Field(default=8, description="Maximum retry attempts for LLM calls")
    base_delay: float = Field(default=5.0, description="Base delay between retries in seconds")
    exponential_backoff: bool = Field(
        default=True, description="Whether to use exponential backoff"
    )


class Settings(BaseSettings):
    # LLM retry configuration
    """Settings class."""
    retry: LLMRetryConfig = Field(default_factory=LLMRetryConfig)

    # Deprecated: use retry instead
    @property
    def llm_retry(self):
        """Llm retry."""
        import warnings

        warnings.warn(
            "settings.llm_retry is deprecated, use settings.retry instead",
            DeprecationWarning,
            stacklevel=2,
        )
        return self.retry

    model_config = SettingsConfigDict(
        env_prefix="LLM_",
        env_nested_delimiter="__",
    )


# Global settings instance
settings = Settings()
