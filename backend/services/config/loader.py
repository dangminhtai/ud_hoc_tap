import asyncio
from pathlib import Path
from typing import Any

import yaml

from backend.services.path_service import get_path_service
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent.parent


def get_runtime_settings_dir(project_root: Path | None = None) -> Path:
    root = project_root or PROJECT_ROOT
    return root / "data" / "user" / "settings"

def _deep_merge(base: dict[str, Any], override: dict[str, Any]) -> dict[str, Any]:
    result = base.copy()

    for key, value in override.items():
        if key in result and isinstance(result[key], dict) and isinstance(value, dict):
            # Recursively merge dictionaries
            result[key] = _deep_merge(result[key], value)
        else:
            # Direct override
            result[key] = value

    return result


def _load_yaml_file(file_path: Path) -> dict[str, Any]:
    with open(file_path, encoding="utf-8") as f:
        return yaml.safe_load(f) or {}


def _inject_runtime_paths(config: dict[str, Any]) -> dict[str, Any]:
    path_service = get_path_service()
    normalized = dict(config or {})
    tools = dict(normalized.get("tools", {}) or {})
    run_code = dict(tools.get("run_code", {}) or {})
    run_code["workspace"] = str(path_service.get_chat_feature_dir("_detached_code_execution"))
    tools["run_code"] = run_code
    normalized["tools"] = tools
    normalized["paths"] = {
        "user_data_dir": str(path_service.get_user_root()),
        "knowledge_bases_dir": str(path_service.project_root / "data" / "knowledge_bases"),
        "user_log_dir": str(path_service.get_logs_dir()),
        "performance_log_dir": str(path_service.get_logs_dir() / "performance"),
        "guide_output_dir": str(path_service.get_guide_dir()),
        "question_output_dir": str(path_service.get_chat_feature_dir("deep_question")),
        "research_output_dir": str(path_service.get_research_dir()),
        "research_reports_dir": str(path_service.get_research_reports_dir()),
        "solve_output_dir": str(path_service.get_chat_feature_dir("deep_solve")),
    }
    return normalized


async def _load_yaml_file_async(file_path: Path) -> dict[str, Any]:
    return await asyncio.to_thread(_load_yaml_file, file_path)


def resolve_config_path(
    config_file: str,
    project_root: Path | None = None,
) -> tuple[Path, bool]:
    if project_root is None:
        project_root = PROJECT_ROOT

    settings_dir = get_runtime_settings_dir(project_root)
    config_path = settings_dir / config_file
    if config_path.exists():
        return config_path, False
    raise FileNotFoundError(
        f"Configuration file not found: {config_file} "
        f"(expected under {settings_dir})"
    )


def load_config_with_main(config_file: str, project_root: Path | None = None) -> dict[str, Any]:
    if project_root is None:
        project_root = PROJECT_ROOT

    config_path, _ = resolve_config_path(config_file, project_root)
    return _inject_runtime_paths(_load_yaml_file(config_path))


async def load_config_with_main_async(
    config_file: str, project_root: Path | None = None
) -> dict[str, Any]:
    if project_root is None:
        project_root = PROJECT_ROOT

    config_path, _ = resolve_config_path(config_file, project_root)
    return _inject_runtime_paths(await _load_yaml_file_async(config_path))


def get_path_from_config(config: dict[str, Any], path_key: str, default: str = None) -> str:
    injected = _inject_runtime_paths(config)
    if "paths" in injected and path_key in injected["paths"]:
        return injected["paths"][path_key]
    if path_key == "workspace":
        return injected.get("tools", {}).get("run_code", {}).get("workspace", default)
    return default


def parse_language(language: Any) -> str:
    if not language:
        return "en"

    if isinstance(language, str):
        lang_lower = language.lower()
        if lang_lower in ["en", "english"]:
            return "en"

    return "en"


def get_agent_params(module_name: str) -> dict:
    defaults = {
        "temperature": 0.5,
        "max_tokens": 4096,
    }
    section_map = {
        "solve": ("capabilities", "solve"),
        "research": ("capabilities", "research"),
        "question": ("capabilities", "question"),
        "guide": ("capabilities", "guide"),
        "co_writer": ("capabilities", "co_writer"),
        "brainstorm": ("tools", "brainstorm"),
        
        "math_animator": ("plugins", "math_animator"),
    }
    path = get_runtime_settings_dir(PROJECT_ROOT) / "agents.yaml"
    if not path.exists():
        raise FileNotFoundError(f"Missing required configuration file: {path}")
    section = section_map.get(module_name)
    if section is None:
        return defaults
    with open(path, encoding="utf-8") as f:
        agents_config = yaml.safe_load(f) or {}
    module_config: dict[str, Any] = agents_config
    for key in section:
        module_config = module_config.get(key, {})
    return {
        "temperature": module_config.get("temperature", defaults["temperature"]),
        "max_tokens": module_config.get("max_tokens", defaults["max_tokens"]),
    }


__all__ = [
    "PROJECT_ROOT",
    "get_runtime_settings_dir",
    "load_config_with_main",
    "get_path_from_config",
    "parse_language",
    "get_agent_params",
    "_deep_merge",
]
