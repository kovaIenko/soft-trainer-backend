"""
Chat Init Service - Generate initial chat messages for simulations

Creates the first set of messages to start a simulation conversation,
returning a chat object with messages in the backend format.
"""

import json
import logging
import uuid
from typing import Any, Dict

from ..llm.llm_client import LLMClient
from ..schemas import (
    BackendChat,
    BackendMessage,
    CharacterInfo,
    GenerateChatMessagesRequest,
    GenerateChatMessagesResponse,
)

logger = logging.getLogger(__name__)


class ChatInitService:
    """
    Service for generating initial chat messages for simulation startup

    Creates a chat object with the first messages that introduce users
    to the simulation and set up the conversation flow.
    """

    def __init__(self, llm_client: LLMClient):
        self.llm_client = llm_client

    async def generate_initial_chat(
        self, request: GenerateChatMessagesRequest
    ) -> GenerateChatMessagesResponse:
        """
        Generate initial chat messages for simulation startup
        """
        logger.info(
            f"🎯 Generating initial chat messages for simulation: {request.simulation.name}"
        )

        try:
            # For now, return fallback messages - LLM integration can be added later
            fallback_messages = [
                BackendMessage(
                    id=f"msg_{uuid.uuid4().hex[:8]}",
                    message_type="Text",
                    author=CharacterInfo(id=1, name="AI Coordinator", avatar=None),
                    content=f"Welcome {request.user.name}! I'm your AI Coordinator, and I'm excited to guide you through today's '{request.simulation.name}' simulation. In this scenario, you'll have the opportunity to practice {', '.join(request.simulation.variables[:2])} in a realistic workplace setting.",
                    has_hint=False,
                    response_time_limit=None,
                ),
                BackendMessage(
                    id=f"msg_{uuid.uuid4().hex[:8]}",
                    message_type="Text",
                    author=CharacterInfo(id=1, name="AI Coordinator", avatar=None),
                    content=f"Here's your scenario: {request.simulation.description} Take your time to read through the situation and think about how you'd like to approach it.",
                    has_hint=False,
                    response_time_limit=None,
                ),
                BackendMessage(
                    id=f"msg_{uuid.uuid4().hex[:8]}",
                    message_type="Text",
                    author=CharacterInfo(id=1, name="AI Coordinator", avatar=None),
                    content="Are you ready to begin? Please let me know how you'd like to start approaching this situation.",
                    has_hint=False,
                    response_time_limit=None,
                ),
            ]

            chat = BackendChat(
                chat_id=None,
                skill_id=1,
                simulation_id=1,
                messages=fallback_messages,
                success=True,
            )

            return GenerateChatMessagesResponse(
                chat=chat,
                metadata={
                    "simulation": request.simulation.name,
                    "user": request.user.name,
                    "messages_count": len(fallback_messages),
                },
            )

        except Exception as e:
            logger.error(f"❌ Chat initialization failed: {e}")
            raise e
