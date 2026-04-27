"""Vision Solver Agent for analyzing math problems from images."""

from typing import Any, AsyncGenerator

from backend.agents.base_agent import BaseAgent


class VisionSolverAgent(BaseAgent):
    """Agent for analyzing mathematical images and generating GeoGebra visualizations."""

    def __init__(
        self,
        api_key: str | None = None,
        base_url: str | None = None,
        language: str = "zh",
    ):
        """Initialize VisionSolverAgent.

        Args:
            api_key: LLM API key
            base_url: LLM base URL
            language: Language setting ('zh' or 'en')
        """
        super().__init__(
            module_name="vision_solver",
            agent_name="vision_solver_agent",
            api_key=api_key,
            base_url=base_url,
            language=language,
        )

    async def process(
        self,
        question_text: str,
        image_base64: str,
        session_id: str,
    ) -> dict[str, Any]:
        """Process an image analysis request.

        Args:
            question_text: The question about the image
            image_base64: Base64 encoded image
            session_id: Session identifier

        Returns:
            Analysis result with GeoGebra commands
        """
        result = {
            "session_id": session_id,
            "has_image": bool(image_base64),
            "final_ggb_commands": [],
            "bbox_output": {"elements": []},
            "image_is_reference": False,
        }
        return result

    async def stream_process_with_tutor(
        self,
        question_text: str,
        image_base64: str,
        session_id: str,
    ) -> AsyncGenerator[dict[str, Any], None]:
        """Stream analysis with tutor response.

        Args:
            question_text: The question about the image
            image_base64: Base64 encoded image
            session_id: Session identifier

        Yields:
            Event dictionaries with type and data
        """
        yield {
            "event": "analysis_start",
            "data": {"question": question_text[:50]},
        }
        yield {
            "event": "analysis_complete",
            "data": {},
        }

    def format_ggb_block(
        self,
        commands: list[dict[str, Any]],
        page_id: str = "analysis",
        title: str = "Analysis",
    ) -> str:
        """Format GeoGebra commands into a block.

        Args:
            commands: List of GeoGebra commands
            page_id: Page identifier
            title: Block title

        Returns:
            Formatted GeoGebra script
        """
        return ""
