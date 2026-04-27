BUILTIN_CAPABILITY_CLASSES: dict[str, str] = {
    "chat": "backend.capabilities.chat:ChatCapability",
    "deep_solve": "backend.capabilities.deep_solve:DeepSolveCapability",
    "deep_question": "backend.capabilities.deep_question:DeepQuestionCapability",
    "deep_research": "backend.capabilities.deep_research:DeepResearchCapability",
    "math_animator": "backend.capabilities.math_animator:MathAnimatorCapability",
}
