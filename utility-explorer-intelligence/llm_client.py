import os
import httpx
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class LLMClient:
    def __init__(self, base_url: str = None, model: str = None):
        # We accept overrides but default to environment variables
        self.base_url = base_url or os.getenv("LLM_HOST", "http://llm-mesh:11434")
        self.model = model or os.getenv("LLM_MODEL", "qwen2.5-coder:1.5b")
        logger.info(f"Initialized LLMClient with host={self.base_url}, model={self.model}")

    async def generate_response(self, prompt: str) -> str:
        """
        Generic generation method.
        """
        payload = {
            "model": self.model,
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": 0.1 # Low temperature for deterministic output
            }
        }

        try:
            async with httpx.AsyncClient() as client:
                # 30s timeout might be tight for CPU inference, boosting to 60s
                response = await client.post(f"{self.base_url}/api/generate", json=payload, timeout=60.0)
                response.raise_for_status()
                result = response.json()
                return result.get("response", "").strip()

        except Exception as e:
            logger.error(f"LLM Generation failed: {e}")
            return None

    async def generate_sql(self, question: str, metrics_list: str = "") -> str:
        """
        Story 3: Text-to-SQL Spec
        Generates a SQL query based on the fixed schema definition.
        """
        schema_context = f"""
        You are a Postgres SQL Expert. 
        Given the following database schema for a Utility Data Application:

        1. Table: fact_value
           - metric_id (VARCHAR): The type of data.
             Available Metrics:
             {metrics_list}
           - geo_id (VARCHAR): FIPS code for state (e.g. '48') or place (e.g. '2913600').
           - geo_level (VARCHAR): Either 'STATE' or 'PLACE' or 'COUNTY'.
           - period_start (TIMESTAMP): The date of the data point.
           - value_numeric (FLOAT): The actual value.
        
        2. Table: metric_metadata
           - metric_id, display_name, unit_label
           
        3. Table: region
           - geo_id (VARCHAR): The FIPS code (e.g. '06', '48'). Use this to join with fact_value.
           - name (VARCHAR): The readable name (e.g. 'California', 'Texas', 'Dallas').
           - geo_level (VARCHAR): 'STATE', 'COUNTY', 'PLACE'.
           
        Important Rules:
        - Return ONLY the raw SQL query. No markdown, no explanation.
        - Always use LIMIT 10 unless asked otherwise.
        - If asked for 'STATES', filter by geo_level='STATE'.
        - If asked for 'Prices', use metric_id LIKE '%PRICE%'.
        - ALWAYS JOIN 'region' table when looking up a specific Location Name (like 'Texas' or 'Dallas') to find the 'geo_id'. 
          Example: WHERE r.name = 'Texas' AND f.geo_id = r.geo_id.
        """

        prompt = f"{schema_context}\n\nQuestion: {question}\nSQL Query:"
        
        sql = await self.generate_response(prompt)
        
        # Cleanup markdown if model ignores instruction
        if sql:
            sql = sql.replace("```sql", "").replace("```", "").strip()
        
        return sql

    async def check_health(self) -> bool:
        """
        Verifies connection to Ollama.
        """
        try:
             async with httpx.AsyncClient() as client:
                response = await client.get(f"{self.base_url}/api/tags", timeout=5.0)
                return response.status_code == 200
        except Exception as e:
            logger.error(f"LLM Health check failed: {e}")
            return False
