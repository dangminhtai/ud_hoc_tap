from collections.abc import Callable

_provider_registry: dict[str, type] = {}


def register_provider(name: str) -> Callable[[type], type]:

    def decorator(cls: type) -> type:
        if name in _provider_registry:
            raise ValueError(f"Provider '{name}' is already registered")
        _provider_registry[name] = cls
        setattr(cls, "__provider_name__", name)
        return cls

    return decorator


def get_provider_class(name: str) -> type:
    return _provider_registry[name]


def list_providers() -> list[str]:
    return list(_provider_registry.keys())


def is_provider_registered(name: str) -> bool:
    return name in _provider_registry
