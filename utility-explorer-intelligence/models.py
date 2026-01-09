from pydantic import BaseModel, Field
from typing import List, Optional, Any

class IntentRequest(BaseModel):
    query: str
    user_id: Optional[str] = None

class IntentResponse(BaseModel):
    intent_type: str = Field(..., description="The classification of the user's intent (e.g., 'fact_retrieval', 'comparison', 'explanation', 'general')")
    confidence: float = Field(..., description="Confidence score of the classification (0.0 to 1.0)")
    entities: dict = Field(default_factory=dict, description="Extracted entities like dates, locations, metrics")
    response_text: str = Field(..., description="The generated natural language response")
    data: Optional[Any] = Field(None, description="Structured data payload if applicable")
