#!/usr/bin/env python
"""
ChatAgent - Lightweight conversational AI with multi-turn support.

This agent provides:
- Multi-turn conversation with history management
- Token-based context truncation
- Optional RAG and Web Search augmentation
- Streaming response generation

Uses the unified LLM factory from BaseAgent for both cloud and local LLM support.
"""

from typing import Any, AsyncGenerator

from backend.agents.base_agent import BaseAgent
from backend.runtime.registry.tool_registry import get_tool_registry


class ChatAgent(BaseAgent):
    """
    Lightweight conversational agent with multi-turn support.

    Features:
    - Conversation history management with token limits
    - RAG (Retrieval-Augmented Generation) support
    - Web search integration
    - Streaming response generation via BaseAgent.stream_llm()
    """

    # Default token limit for conversation history
    DEFAULT_MAX_HISTORY_TOKENS = 4000

    def __init__(
        self,
        language: str = "zh",
        config: dict[str, Any] | None = None,
        max_history_tokens: int | None = None,
        **kwargs,
    ):
        super().__init__(
            module_name="chat",
            agent_name="chat_agent",
            language=language,
            config=config,
            **kwargs,
        )

        # Configure history token limit
        self.max_history_tokens = max_history_tokens or self.agent_config.get(
            "max_history_tokens", self.DEFAULT_MAX_HISTORY_TOKENS
        )
        self._tool_registry = get_tool_registry()

        self.logger.info(f"ChatAgent initialized: model={self.model}, base_url={self.base_url}")

    def count_tokens(self, text: str) -> int:
        """
        Count tokens in text using tiktoken.

        Falls back to character-based estimation if tiktoken unavailable.

        Args:
            text: Text to count tokens for

        Returns:
            Estimated token count
        """
        try:
            import tiktoken

            # Use cl100k_base encoding (GPT-4, GPT-3.5-turbo)
            encoding = tiktoken.get_encoding("cl100k_base")
            return len(encoding.encode(text))
        except ImportError:
            # Fallback: rough estimate of 4 characters per token
            return len(text) // 4

    def truncate_history(
        self,
        history: list[dict[str, str]],
        max_tokens: int | None = None,
    ) -> list[dict[str, str]]:
        """
        Truncate conversation history to fit within token limit.

        Keeps the most recent messages, discarding older ones first.

        Args:
            history: List of message dicts with 'role' and 'content'
            max_tokens: Maximum tokens allowed (uses default if None)

        Returns:
            Truncated history list
        """
        max_tokens = max_tokens or self.max_history_tokens

        if not history:
            return []

        # Calculate tokens for each message
        message_tokens = []
        for msg in history:
            content = msg.get("content", "")
            tokens = self.count_tokens(content)
            message_tokens.append((msg, tokens))

        # Build history from newest to oldest, stop when limit reached
        truncated = []
        total_tokens = 0

        for msg, tokens in reversed(message_tokens):
            if total_tokens + tokens > max_tokens:
                break
            truncated.insert(0, msg)
            total_tokens += tokens

        if len(truncated) < len(history):
            self.logger.info(
                f"Truncated history from {len(history)} to {len(truncated)} messages "
                f"({total_tokens} tokens)"
            )

        return truncated

    def format_history_for_prompt(self, history: list[dict[str, str]]) -> str:
        """
        Format conversation history as a string for the prompt.

        Args:
            history: List of message dicts

        Returns:
            Formatted history string
        """
        if not history:
            return ""

        lines = []
        for msg in history:
            role = msg.get("role", "user")
            content = msg.get("content", "")
            prefix = "User" if role == "user" else "Assistant"
            lines.append(f"{prefix}: {content}")

        return "\n\n".join(lines)

    async def retrieve_context(
        self,
        message: str,
        kb_name: str | None = None,
        enable_rag: bool = False,
        enable_web_search: bool = False,
    ) -> tuple[str, dict[str, Any]]:
        """
        Retrieve context from RAG and/or Web Search.

        Args:
            message: User message to search for
            kb_name: Knowledge base name for RAG
            enable_rag: Whether to use RAG
            enable_web_search: Whether to use Web Search

        Returns:
            Tuple of (context_string, sources_dict)
        """
        context_parts = []
        sources = {"rag": [], "web": []}

        # RAG retrieval
        if enable_rag and kb_name:
            try:
                self.logger.info(f"RAG search: {message[:50]}...")
                rag_result = await self._tool_registry.execute(
                    "rag",
                    query=message,
                    kb_name=kb_name,
                    mode="hybrid",
                )
                rag_answer = rag_result.content
                if rag_answer:
                    context_parts.append(f"[Knowledge Base: {kb_name}]\n{rag_answer}")

                    if rag_result.sources:
                        sources["rag"] = rag_result.sources
                    else:
                        sources["rag"].append(
                            {
                                "kb_name": kb_name,
                                "content": rag_answer[:500] + "..."
                                if len(rag_answer) > 500
                                else rag_answer,
                            }
                        )
                    self.logger.info(
                        f"RAG retrieved {len(rag_answer)} chars, "
                        f"{len(sources['rag'])} sources"
                    )
            except Exception as e:
                self.logger.warning(f"RAG search failed: {e}")

        # Web search
        if enable_web_search:
            try:
                self.logger.info(f"Web search: {message[:50]}...")
                web_result = await self._tool_registry.execute(
                    "web_search",
                    query=message,
                    verbose=False,
                )
                web_answer = web_result.content
                web_citations = web_result.sources

                if web_answer:
                    context_parts.append(f"[Web Search Results]\n{web_answer}")
                    sources["web"] = web_citations[:5]
                    self.logger.info(
                        f"Web search returned {len(web_answer)} chars, "
                        f"{len(web_citations)} citations"
                    )
            except Exception as e:
                self.logger.warning(f"Web search failed: {e}")

        context = "\n\n".join(context_parts)

        # Debug logging
        self.logger.info(f"Retrieved context: {len(context)} chars, RAG sources: {len(sources.get('rag', []))}, Web sources: {len(sources.get('web', []))}")
        if sources.get("rag"):
            for src in sources["rag"]:
                self.logger.debug(f"RAG source: {src.get('title', 'unknown')} (score: {src.get('score', 'N/A')})")

        return context, sources

    def build_messages(
        self,
        message: str,
        history: list[dict[str, str]],
        context: str = "",
    ) -> list[dict[str, str]]:
        """
        Build the messages array for the LLM API call.

        Args:
            message: Current user message
            history: Truncated conversation history
            context: Retrieved context (RAG/Web)

        Returns:
            List of message dicts for OpenAI API
        """
        messages = []

        # System prompt
        system_prompt = self.get_prompt("system", "You are a helpful AI assistant.")
        messages.append({"role": "system", "content": system_prompt})

        # Add context if available
        if context:
            context_template = self.get_prompt("context_template", "Reference context:\n{context}")
            context_msg = context_template.format(context=context)
            messages.append({"role": "system", "content": context_msg})

        # Add conversation history
        for msg in history:
            role = msg.get("role", "user")
            content = msg.get("content", "")
            if role in ("user", "assistant"):
                messages.append({"role": role, "content": content})

        # Add current message
        messages.append({"role": "user", "content": message})

        return messages

    async def generate_stream(
        self,
        messages: list[dict[str, Any]],
        attachments: list[Any] | None = None,
    ) -> AsyncGenerator[str, None]:
        """
        Generate streaming response from LLM.

        Uses BaseAgent.stream_llm() which routes to the appropriate provider
        (cloud or local) based on configuration.

        Args:
            messages: Messages array for OpenAI API
            attachments: Image/file attachments for multimodal input

        Yields:
            Response chunks as strings
        """
        system_prompt = ""
        user_prompt = ""
        for msg in messages:
            if msg.get("role") == "system":
                system_prompt = msg.get("content", "")
                break

        for msg in reversed(messages):
            if msg.get("role") == "user":
                content = msg.get("content", "")
                user_prompt = content if isinstance(content, str) else str(content)
                break

        async for chunk in self.stream_llm(
            user_prompt=user_prompt,
            system_prompt=system_prompt,
            messages=messages,
            stage="chat_stream",
            attachments=attachments,
        ):
            yield chunk

    async def generate(self, messages: list[dict[str, str]]) -> str:
        """
        Generate complete response from LLM (non-streaming).

        Uses BaseAgent.call_llm() which routes to the appropriate provider
        (cloud or local) based on configuration.

        Args:
            messages: Messages array for OpenAI API

        Returns:
            Complete response string
        """
        # Extract system prompt from messages
        system_prompt = ""
        user_prompt = ""
        for msg in messages:
            if msg.get("role") == "system":
                system_prompt = msg.get("content", "")
                break

        # Get the last user message
        for msg in reversed(messages):
            if msg.get("role") == "user":
                user_prompt = msg.get("content", "")
                break

        from backend.services.llm import stream as llm_stream

        _chunks: list[str] = []
        async for _c in llm_stream(
            prompt=user_prompt,
            system_prompt=system_prompt,
            model=self.get_model(),
            api_key=self.api_key,
            base_url=self.base_url,
            messages=messages,
            temperature=self.get_temperature(),
        ):
            _chunks.append(_c)
        response = "".join(_chunks)

        # Track token usage
        self._track_tokens(
            model=self.get_model(),
            system_prompt=system_prompt,
            user_prompt=user_prompt,
            response=response,
            stage="chat",
        )

        return response

    def detect_question_intent(self, message: str) -> bool:
        """
        Detect if user is asking to create questions/quiz.

        Keywords: tạo question, làm quiz, tạo bài tập, etc.
        """
        keywords = [
            "tạo question", "tạo quiz", "tạo bài tập", "làm quiz",
            "create question", "generate quiz", "make questions",
            "tạo đề", "làm đề", "create exam", "tạo test",
        ]
        msg_lower = message.lower().strip()
        return any(kw in msg_lower for kw in keywords)

    def build_question_menu(self) -> dict[str, Any]:
        """Build interactive menu for question generation."""
        return {
            "type": "question_intent_detected",
            "message": "Bạn muốn tạo question như thế nào?",
            "options": [
                {
                    "id": "from_topic",
                    "label": "📚 Tạo từ chủ đề",
                    "description": "Nhập chủ đề, tôi sẽ tạo question cho bạn",
                },
                {
                    "id": "from_pdf",
                    "label": "📄 Tạo từ PDF",
                    "description": "Upload file PDF, tôi sẽ phân tích và tạo question",
                },
                {
                    "id": "exam_mimic",
                    "label": "📋 Exam Mimic",
                    "description": "Tạo question tương tự đề thi",
                },
            ],
            "confirm_message": "Bạn chắc chắn muốn tiếp tục không?",
        }

    async def generate_questions(
        self,
        option: str,
        details: dict[str, Any],
        kb_name: str | None = None,
    ) -> dict[str, Any]:
        """
        Generate questions based on user choice.

        Args:
            option: "from_topic", "from_pdf", "exam_mimic"
            details: {
                "topic": str,
                "count": int,
                "difficulty": str,
                "question_type": str,
                "pdf_path": str,
                etc.
            }
            kb_name: Knowledge base name

        Returns:
            Generated questions data
        """
        try:
            from backend.agents.question import AgentCoordinator

            coordinator = AgentCoordinator(
                api_key=self.api_key,
                base_url=self.base_url,
                api_version=self.api_version,
                kb_name=kb_name,
                language=self.language,
            )

            if option == "from_topic":
                result = await coordinator.generate_from_topic(
                    user_topic=details.get("topic", ""),
                    preference=details.get("preference", ""),
                    num_questions=details.get("count", 5),
                    difficulty=details.get("difficulty", ""),
                    question_type=details.get("question_type", ""),
                )
                return result

            elif option == "from_pdf":
                result = await coordinator.generate_from_exam(
                    exam_paper_path=details.get("pdf_path", ""),
                    max_questions=details.get("count", 5),
                    paper_mode="upload",
                )
                return result

            elif option == "exam_mimic":
                result = await coordinator.generate_from_exam(
                    exam_paper_path=details.get("paper_path", ""),
                    max_questions=details.get("count", 5),
                    paper_mode="parsed",
                )
                return result

            else:
                return {
                    "success": False,
                    "error": f"Unknown option: {option}",
                }

        except Exception as e:
            self.logger.error(f"Question generation failed: {e}")
            return {
                "success": False,
                "error": str(e),
            }

    async def process(
        self,
        message: str,
        history: list[dict[str, str]] | None = None,
        kb_name: str | None = None,
        enable_rag: bool = False,
        enable_web_search: bool = False,
        stream: bool = False,
        attachments: list[Any] | None = None,
    ) -> dict[str, Any] | AsyncGenerator[dict[str, Any], None]:
        """
        Process a chat message with optional context retrieval.

        Args:
            message: User message
            history: Conversation history (will be truncated if needed)
            kb_name: Knowledge base name for RAG
            enable_rag: Whether to enable RAG retrieval
            enable_web_search: Whether to enable web search
            stream: Whether to stream the response
            attachments: Image/file attachments for multimodal input

        Returns:
            If stream=False: Dict with 'response', 'sources', 'truncated_history'
            If stream=True: AsyncGenerator yielding chunks
        """
        history = history or []

        # Check for question generation intent first
        if self.detect_question_intent(message):
            menu = self.build_question_menu()
            if stream:
                async def menu_generator():
                    yield {"type": "menu", "data": menu}
                return menu_generator()
            else:
                return {"type": "menu", "data": menu}

        truncated_history = self.truncate_history(history)

        context, sources = await self.retrieve_context(
            message=message,
            kb_name=kb_name,
            enable_rag=enable_rag,
            enable_web_search=enable_web_search,
        )

        messages = self.build_messages(
            message=message,
            history=truncated_history,
            context=context,
        )

        if stream:
            async def stream_generator():
                """Stream generator asynchronously."""
                full_response = ""
                async for chunk in self.generate_stream(messages, attachments=attachments):
                    full_response += chunk
                    yield {"type": "chunk", "content": chunk}

                yield {
                    "type": "complete",
                    "response": full_response,
                    "sources": sources,
                    "truncated_history": truncated_history,
                }

            return stream_generator()
        else:
            response = await self.generate(messages)

            return {
                "response": response,
                "sources": sources,
                "truncated_history": truncated_history,
            }


__all__ = ["ChatAgent"]
