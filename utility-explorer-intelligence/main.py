from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel
from sqlalchemy import text, desc, asc
from sqlalchemy.orm import Session
from database import get_db, engine, Base
from models import IntentRequest, IntentResponse
from db_models import IntelligenceQueryLog, KnowledgeChunk, FactValue, MetricMetadata
from embeddings import generate_embedding, compute_similarity
import joblib
import os
from contextlib import asynccontextmanager
from typing import Optional, List, Dict
from sklearn.linear_model import LinearRegression
import numpy as np
import datetime
import spacy
import asyncio
from metadata_consumer import consume_metadata
from llm_client import LLMClient

# Helper: US State Name to Code Mapping (FIPS Codes as stored in DB)
US_STATES = {
    'alabama': '01', 'alaska': '02', 'arizona': '04', 'arkansas': '05', 'california': '06',
    'colorado': '08', 'connecticut': '09', 'delaware': '10', 'florida': '12', 'georgia': '13',
    'hawaii': '15', 'idaho': '16', 'illinois': '17', 'indiana': '18', 'iowa': '19',
    'kansas': '20', 'kentucky': '21', 'louisiana': '22', 'maine': '23', 'maryland': '24',
    'massachusetts': '25', 'michigan': '26', 'minnesota': '27', 'mississippi': '28', 'missouri': '29',
    'montana': '30', 'nebraska': '31', 'nevada': '32', 'new hampshire': '33', 'new jersey': '34',
    'new mexico': '35', 'new york': '36', 'north carolina': '37', 'north dakota': '38', 'ohio': '39',
    'oklahoma': '40', 'oregon': '41', 'pennsylvania': '42', 'rhode island': '44', 'south carolina': '45',
    'south dakota': '46', 'tennessee': '47', 'texas': '48', 'utah': '49', 'vermont': '50',
    'virginia': '51', 'washington': '53', 'west virginia': '54', 'wisconsin': '55', 'wyoming': '56',
    # Reverse helper for standard abbreviations people might type
    'al': '01', 'ak': '02', 'az': '04', 'ar': '05', 'ca': '06', 'co': '08', 'ct': '09', 'de': '10',
    'fl': '12', 'ga': '13', 'hi': '15', 'id': '16', 'il': '17', 'in': '18', 'ia': '19', 'ks': '20',
    'ky': '21', 'la': '22', 'me': '23', 'md': '24', 'ma': '25', 'mi': '26', 'mn': '27', 'ms': '28',
    'mo': '29', 'mt': '30', 'ne': '31', 'nv': '32', 'nh': '33', 'nj': '34', 'nm': '35', 'ny': '36',
    'nc': '37', 'nd': '38', 'oh': '39', 'ok': '40', 'or': '41', 'pa': '42', 'ri': '44', 'sc': '45',
    'sd': '46', 'tn': '47', 'tx': '48', 'ut': '49', 'vt': '50', 'va': '51', 'wa': '53', 'wv': '54',
    'wi': '55', 'wy': '56'
}

# Reverse mapping for display (FIPS -> State Name)
FIPS_TO_NAME = {v: k.title() for k, v in US_STATES.items() if len(k) > 2}

# Global ML Model
intent_model = None
nlp = None
llm_client = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Create tables if they don't exist
    Base.metadata.create_all(bind=engine)
    
    # Start Metadata Listener (Background Task)
    asyncio.create_task(consume_metadata())
    
    # Load ML Model
    global intent_model
    global nlp
    global llm_client
    
    # Initialize Local LLM Client (Story 2 - GenAI Client)
    llm_client = LLMClient()
    is_llm_up = await llm_client.check_health()
    if is_llm_up:
        print("INFO: Connected to Local LLM Mesh (Ollama).")
    else:
        print("WARN: Could not connect to Local LLM Mesh. Check llm-mesh container.")

    # Load SPACY Model (For Entity Extraction)
    # This allows generic entity recognition (Locations, etc.) without manual rules
    try:
        print("INFO: Loading Spacy NLP model (en_core_web_sm)...")
        nlp = spacy.load("en_core_web_sm")
        print("INFO: Spacy NLP model loaded.")
    except Exception as e:
        print(f"WARN: Could not load Spacy model: {e}")

    # Use absolute path for Docker container (/app/ml/intent_model.pkl)
    model_path = os.path.join(os.getcwd(), "ml", "intent_model.pkl") 
    
    print(f"INFO: Attempting to load ML model from {model_path}...")
    if os.path.exists(model_path):
        try:
            intent_model = joblib.load(model_path)
            print("INFO: ML Intent Model loaded successfully.")
        except Exception as e:
            print(f"WARN: Failed to load ML model: {e}")
    else:
        print(f"WARN: ML Intent Model not found at {model_path}. Using fallback rules.")
        # Listing dir to debug
        try:
            print(f"DEBUG: Contents of ml dir: {os.listdir('ml')}")
        except:
            pass
        
    yield

app = FastAPI(title="Utility Intelligence Service", version="0.1.0", lifespan=lifespan)

class QueryRequest(BaseModel):
    question: str


class AgentResponse(BaseModel):
    answer: str
    sources: Optional[List] = None
    visualization: Optional[Dict] = None
    debug_meta: Optional[Dict] = None

@app.get("/health")
def health_check(db: Session = Depends(get_db)):
    db_status = "unknown"
    try:
        # Simple query to verify connection
        db.execute(text("SELECT 1"))
        db_status = "connected"
    except Exception as e:
        db_status = f"error: {str(e)}"

    return {
        "status": "ok", 
        "service": "utility-intelligence-service",
        "env": os.getenv("APP_ENV", "dev"),
        "database": db_status
    }

@app.get("/llm/test")
async def test_llm_connection():
    """
    Validation endpoint for Story 2 (Client Integration).
    """
    if not llm_client:
        raise HTTPException(status_code=503, detail="LLM Client not initialized")
    
    response = await llm_client.generate_response("Say 'Systems Online'")
    return {"status": "success", "response": response}

@app.post("/query", response_model=AgentResponse)
async def query_agent(request: QueryRequest, db: Session = Depends(get_db)):
    """
    Handles the user query.
    [DEPRECATION NOTICE]: Rule-based logic matches first; if low confidence, 
    falls back to Generative AI (LLM) Text-to-SQL.
    """
    # 1. Identify Intent (Rule Based)
    intent = identify_intent(request.question)
    
    # 2. Logic Router
    # If the rule engine is confident, try it. 
    # BUT if it returns "no data", we should still fall back to GenAI.
    if intent.intent_type != "unknown" and intent.intent_type != "out_of_scope" and intent.confidence >= 0.6:
        rule_response = handle_rule_based_query(request, db, intent)
        
        # Check if the rule response was actually useful
        is_useful = True
        if "found no matching data" in rule_response.answer: is_useful = False
        if "not enough historical data" in rule_response.answer.lower(): is_useful = False
        
        if is_useful:
            return rule_response
        else:
            print(f"INFO: Rule Engine failed to find data. Falling back to GenAI.")
    
    # 3. Generative Fallback (Story 3)
    if llm_client:
        print(f"INFO: Triggering GenAI Fallback for: '{request.question}'")
        try:
            # FLEXIBILITY: Fetch current metrics from DB to inform the LLM
            metrics_list = db.query(MetricMetadata).all()
            metrics_context = "\n".join([f"- {m.metric_id} ({m.display_name})" for m in metrics_list])
            
            generated_sql = await llm_client.generate_sql(request.question, metrics_context)
            print(f"INFO: Generated SQL: {generated_sql}")
            
            if generated_sql and "SELECT" in generated_sql.upper():
                # SAFETY: Execute Read-Only? In real PROD use a read-only DB user.
                # using text() for raw sql
                result_rows = db.execute(text(generated_sql)).fetchall()
                
                # Format response blindly
                if result_rows:
                    formatted_rows = "\n".join([str(row) for row in result_rows])
                    return AgentResponse(
                        answer=f"I generated a custom query for you.\nSQL executed: `{generated_sql}`\n\nResults:\n{formatted_rows}",
                        sources=["GEN_AI_SQL"],
                        visualization=None,
                        debug_meta={
                            "generated_sql": generated_sql,
                            "sql_result": [str(row) for row in result_rows]
                        }
                    )
                else:
                    return AgentResponse(
                        answer=f"Query executed but returned no data.\nSQL executed: `{generated_sql}`", 
                        sources=["GEN_AI_SQL"], 
                        visualization=None,
                        debug_meta={
                            "generated_sql": generated_sql,
                            "sql_result": []
                        }
                    )
        
        except Exception as e:
            print(f"GenAI Error: {e}")
            return AgentResponse(answer=f"I tried to analyze this with AI but failed: {str(e)}", sources=[], visualization=None)

    # 4. Default Fallback
    return AgentResponse(
        answer="I'm sorry, I couldn't understand your specific request with my standard rules, and AI fallback is unavailable.",
        sources=[],
        visualization=None
    )


def handle_rule_based_query(request, db, intent):
    """
    Refactored legacy logic into a helper function to clean up the main handler.
    """
    answer_text = intent.response_text
    fact_result = {"sources": [], "visualization": None}

    # ... (Keep existing If/Else logic for fact_retrieval, forecast, etc.)
    
    if intent.intent_type == "fact_retrieval":
         result_data = execute_fact_retrieval(db, intent, request.question)
         if result_data:
             answer_text = result_data["text"]
             fact_result = result_data
         else:
             answer_text += f"\n[System Note: Identified as Fact Retrieval with confidence {intent.confidence}]"
    
    elif intent.intent_type == "forecast":
        db_answer = execute_forecast(db, intent, request.question)
        if db_answer:
             answer_text = db_answer
        else:
             answer_text += f"\n[System Note: Identified as Forecast with confidence {intent.confidence}]"

    # Log to Database
    try:
        log_entry = IntelligenceQueryLog(
            query_text=request.question,
            detected_intent=intent.intent_type,
            confidence_score=intent.confidence,
            status="answered"
        )
        db.add(log_entry)
        db.commit()
    except Exception as e:
        print(f"Failed to log query: {e}")

    return AgentResponse(
        answer=answer_text,
        sources=fact_result.get("sources", []),
        visualization=fact_result.get("visualization")
    )


@app.post("/ingest-knowledge")
def ingest_knowledge(content: str, source: str, db: Session = Depends(get_db)):
    """
    Simulated endpoint to add knowledge chunks (since we don't have a crawler yet).
    This creates embedding and saves to DB.
    """
    try:
        embedding = generate_embedding(content)
        chunk = KnowledgeChunk(
            content=content,
            source_id=source,
            embedding=embedding
        )
        db.add(chunk)
        db.commit()
        return {"status": "success", "message": "Knowledge ingested and vectorized."}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


def identify_intent(question: str) -> IntentResponse:
    """
    Advanced Intent Classification using Semantic Similarity (Story 5.5).
    Replaces brittle keyword matching with vector distance checks against 'anchors'.
    """
    question_embedding = generate_embedding(question)
    
    # 0. ML Model Prediction (Scikit-Learn)
    # If the model is loaded, we use it to get the primary intent.
    # We still use the semantic guardrails (step 1) to double-check "out_of_scope".
    ml_intent = None
    if intent_model:
        try:
            ml_intent = intent_model.predict([question])[0]
            print(f"DEBUG: ML Model Prediction: {ml_intent}")
        except Exception as e:
            print(f"ML Prediction Error: {e}")

    # Define "Anchor" queries for known categories
    # In a real app, these clusters would be pre-calculated or stored in DB
    anchors = {
        "out_of_scope": [
            "how is the weather", "who won the game", "latest movie reviews", 
            "cooking recipes", "politics news", "baseball scores"
        ],
        "utility_domain": [
            "electricity price", "energy consumption", "utility rates",
            "compare state costs", "renewable energy trends", "kwh usage"
        ]
    }
    
    # 1. Check Semantic Guardrail
    # Measure max similarity to any "out_of_scope" anchor
    oos_scores = [compute_similarity(question_embedding, generate_embedding(a)) for a in anchors["out_of_scope"]]
    max_oos_score = max(oos_scores) if oos_scores else 0.0
    
    # Measure max similarity to "utility_domain"
    domain_scores = [compute_similarity(question_embedding, generate_embedding(a)) for a in anchors["utility_domain"]]
    max_domain_score = max(domain_scores) if domain_scores else 0.0
    
    print(f"DEBUG: Domain Score: {max_domain_score:.3f}, OOS Score: {max_oos_score:.3f}")

    # Decision Logic:
    # If it looks more like garbage than utility, OR if utility similarity is just too low.
    if max_oos_score > max_domain_score or max_domain_score < 0.25:
         return IntentResponse(
            intent_type="out_of_scope",
            confidence=1.0,
            response_text="I can only assist with utility, energy, and usage data questions. Please stick to the topic.",
            entities={}
        )

    # 2. ML Model Strict Policy (No Rules)
    if ml_intent:
         return IntentResponse(
            intent_type=ml_intent,
            confidence=0.95,
            response_text=f"Processed via ML Model ({ml_intent})...",
            entities={}
        )
    
    # Fallback only if model failed to load or predict
    return IntentResponse(
        intent_type="unknown",
        confidence=0.0,
        response_text="Model could not classify this request.",
        entities={}
    )

def execute_fact_retrieval(db: Session, intent: IntentResponse, question: str) -> Dict:
    """
    Executes a structured query against the FactValue table based on identified entities.
    Returns: Dict with keys 'text', 'sources'
    """
    try:
        # 0. NLP Processing
        doc = nlp(question) if nlp else None
        
        # 1. Geo Identification & Level
        geo_id = None
        geo_level = None # Strict filter if set
        found_loc_name = None
        
        # Keyword based Level Detection
        q_lower = question.lower()
        if "state" in q_lower: 
            geo_level = "STATE"
        elif "county" in q_lower or "counties" in q_lower: 
            geo_level = "COUNTY"
        elif "city" in q_lower or "cities" in q_lower or "place" in q_lower:
            geo_level = "PLACE"

        if doc:
            locations = [ent.text for ent in doc.ents if ent.label_ == "GPE"]
            if locations:
                found_loc_name = locations[0]
                lower_loc = found_loc_name.lower()
                geo_id = US_STATES.get(lower_loc, found_loc_name)
                # If we found a specific location, we usually don't want a "map of all states"
                # unless explicitly asked. But specific location implies specific ID.
                print(f"DEBUG: NLP Extracted Location: '{found_loc_name}' -> Mapped GeoID: '{geo_id}'")

        # 2. Metric Selection - Multi-Metric Support
        q_embedding = generate_embedding(question)
        relevant_metrics = [] # List of tuples (MetricMetadata, score)
        
        all_metrics = db.query(MetricMetadata).filter(MetricMetadata.embedding.isnot(None)).all()
        
        # Hybrid Search Logic
        q_lower = question.lower()
        scored_metrics = []
        
        if all_metrics:
            for m in all_metrics:
                score = compute_similarity(q_embedding, m.embedding)
                
                # Hybrid Boost
                m_id_lower = m.metric_id.lower()
                
                # Broader Synonym Matching for "Cost/Price"
                # If user asks for price, also boost cost, and vice versa.
                money_keywords = ["price", "cost", "rate", "bill"]
                usage_keywords = ["sales", "consumption", "usage", "kwh"]
                
                # Check Money related
                if any(k in q_lower for k in money_keywords) and any(k in m_id_lower for k in money_keywords):
                    score += 0.25
                
                # Check Usage related
                if any(k in q_lower for k in usage_keywords) and any(k in m_id_lower for k in usage_keywords):
                    score += 0.25

                scored_metrics.append((m, score))
        
        # Sort and Filter
        scored_metrics.sort(key=lambda x: x[1], reverse=True)
        
        # Take top metrics (threshold 0.4 or top 2 if close)
        if scored_metrics:
            top_score = scored_metrics[0][1]
            for m, score in scored_metrics:
                # Accept if score is high enough OR within 10% of top score (ambiguity handling)
                if score > 0.45 or (score > 0.3 and score >= top_score * 0.9):
                    relevant_metrics.append(m)
                
                if len(relevant_metrics) >= 3: break
        
        # Fallback
        if not relevant_metrics:
            # Default to Price if nothing matches relevantly
            fallback = next((m for m in all_metrics if "PRICE" in m.metric_id), None)
            if fallback: relevant_metrics.append(fallback)

        results_text_parts = []
        collected_sources = set()
        visualization_data = None

        # 3. Query for each relevant metric
        for metric in relevant_metrics:
            base_query = db.query(FactValue).filter(FactValue.metric_id == metric.metric_id)
            
            # Apply Geo Level Filter if Detected
            if geo_level:
                base_query = base_query.filter(FactValue.geo_level == geo_level)
            
            if geo_id:
                # Specific Location Requested
                result = base_query.filter(FactValue.geo_id == geo_id).order_by(desc(FactValue.period_start)).first()
                if result:
                    display_loc = FIPS_TO_NAME.get(result.geo_id, result.geo_id)
                    results_text_parts.append(
                        f"- **{metric.display_name or metric.metric_id}**: {result.value_numeric} {metric.unit_label} in {display_loc} ({str(result.period_start)[:7]})"
                    )
                    if metric.source_system: collected_sources.add(metric.source_system)
            else:
                # No specific location -> Check for List/Map intent or just Min/Max
                latest_entry = base_query.order_by(desc(FactValue.period_start)).first()
                if latest_entry:
                    latest_date = latest_entry.period_start
                    date_query = base_query.filter(FactValue.period_start == latest_date)
                    
                    # Visualization / List Intent Support
                    # If user asks for "across states", "map", "compare", we return full dataset for UI
                    viz_keywords = ["map", "chart", "graph", "compare", "across", "list", "distribution"]
                    is_viz_intent = any(k in question.lower() for k in viz_keywords)

                    if is_viz_intent:
                        all_vals = date_query.all()
                        if all_vals:
                            results_text_parts.append(
                                f"- **{metric.display_name or metric.metric_id}** ({str(latest_date)[:7]}): "
                                f"Retrieved data for {len(all_vals)} locations. See visualization below."
                            )
                            
                            # Construct Visualization Payload
                            points = []
                            for row in all_vals:
                                points.append({
                                    "geoId": row.geo_id,
                                    "value": row.value_numeric,
                                    "name": FIPS_TO_NAME.get(row.geo_id, row.geo_id), # Try resolve, else RAW ID
                                    "geoLevel": row.geo_level
                                })
                            
                            # Only set viz data if empty (priority to first metric)
                            if not visualization_data:
                                visualization_data = {
                                    "type": "choropleth_map" if geo_level == "STATE" else "bar_chart",
                                    "title": f"{metric.display_name} by {geo_level or 'Location'}",
                                    "metric": metric.display_name or metric.metric_id,
                                    "unit": metric.unit_label,
                                    "data": points
                                }
                            
                            if metric.source_system: collected_sources.add(metric.source_system)
                            continue # Skip Min/Max if we did viz

                    # Default: Find Min/Max for this date
                    min_val = date_query.order_by(asc(FactValue.value_numeric)).first()
                    max_val = date_query.order_by(desc(FactValue.value_numeric)).first()
                    
                    if min_val and max_val:
                         min_loc = FIPS_TO_NAME.get(min_val.geo_id, min_val.geo_id)
                         max_loc = FIPS_TO_NAME.get(max_val.geo_id, max_val.geo_id)
                         
                         results_text_parts.append(
                             f"- **{metric.display_name or metric.metric_id}** ({str(latest_date)[:7]}): "
                             f"Range: {min_val.value_numeric} - {max_val.value_numeric} {metric.unit_label}. "
                             f"(Lowest: {min_loc}, Highest: {max_loc})"
                         )
                         if metric.source_system: collected_sources.add(metric.source_system)

        if not results_text_parts:
            return {"text": "I checked the database but found no matching data for your request.", "sources": []}
            
        final_intro = "Here is the latest data I found:" if not geo_id else f"Here is the data for {found_loc_name}:"
        
        return {
            "text": final_intro + "\n" + "\n".join(results_text_parts),
            "sources": list(collected_sources),
            "visualization": visualization_data
        }
            
    except Exception as e:
        print(f"DB Query Error: {e}")
        return None
            
    except Exception as e:
        print(f"DB Query Error: {e}")
        return None

def execute_forecast(db: Session, intent: IntentResponse, question: str) -> Optional[str]:
    """
    Predicts future value using Linear Regression on historical data.
    Uses Spacy for location extraction to support any location dynamically.
    """
    try:
        # 1. Metric Mapping (Dynamic Registry)
        q_embedding = generate_embedding(question)
        
        best_metric_id = "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH" # Default Fallback
        best_score = -1.0
        
        all_metrics = db.query(MetricMetadata).filter(MetricMetadata.embedding.isnot(None)).all()
        if all_metrics:
            for m in all_metrics:
                score = compute_similarity(q_embedding, m.embedding)
                
                 # Hybrid Search
                q_lower_check = question.lower()
                m_id_lower = m.metric_id.lower()
                if "price" in q_lower_check and "price" in m_id_lower: score += 0.2
                if "cost" in q_lower_check and "cost" in m_id_lower: score += 0.2
                if "sales" in q_lower_check and "sales" in m_id_lower: score += 0.2
                
                if score > best_score:
                    best_score = score
                    best_metric_id = m.metric_id
        
        metric_id = best_metric_id

        # 2. Geo Identification (Automated via Spacy)
        geo_id = "48" # Default fallback (Texas)
        
        if nlp:
            doc = nlp(question)
            locations = [ent.text for ent in doc.ents if ent.label_ == "GPE"]
            if locations:
                found_loc = locations[0]
                # Map to FIPS
                lower_loc = found_loc.lower()
                geo_id = US_STATES.get(lower_loc, found_loc)
                print(f"DEBUG: Forecast Location: '{found_loc}' -> '{geo_id}'")
                    
        # 3. Fetch History
        history = db.query(FactValue).filter(
            FactValue.metric_id == metric_id,
            FactValue.geo_level == "STATE",
            FactValue.geo_id == geo_id
        ).order_by(asc(FactValue.period_start)).all()
        
        display_loc = FIPS_TO_NAME.get(geo_id, geo_id)

        if len(history) < 2:
            return f"Not enough historical data to generate a forecast for {display_loc} ({geo_id})."

        # 2. Prepare Data
        # Fix: Convert date to datetime for timestamp()
        dates = []
        values = []
        for h in history:
            # Ensure we have a date/datetime object
            if isinstance(h.period_start, datetime.date) and not isinstance(h.period_start, datetime.datetime):
                dt = datetime.datetime.combine(h.period_start, datetime.time.min)
            else:
                dt = h.period_start
            dates.append(dt.timestamp())
            values.append(h.value_numeric)
        
        X = np.array(dates).reshape(-1, 1)
        y = np.array(values)
        
        # 3. Train Model
        model = LinearRegression()
        model.fit(X, y)
        
        # 4. Predict Next Month
        last_date = history[-1].period_start
        # crude next month calc (handle date vs datetime)
        if isinstance(last_date, datetime.date) and not isinstance(last_date, datetime.datetime):
             last_date = datetime.datetime.combine(last_date, datetime.time.min)
             
        next_month_date = last_date + datetime.timedelta(days=30)
        X_next = np.array([[next_month_date.timestamp()]])
        
        predicted_value = model.predict(X_next)[0]
        
        metric_name = "Retail Price" if "PRICE" in metric_id else "Monthly Cost"
        return (f"🔮 **AI FORECAST**\n"
                f"Based on historical data for {display_loc}, the model predicts the {metric_name} "
                f"will be approximately **{predicted_value:.2f}** next month ({str(next_month_date)[:10]}).\n\n"
                f"_(Disclaimer: This is a generated prediction, not a historical fact.)_")

    except Exception as e:
        print(f"Forecast Error: {e}")
        return "An error occurred while generating the forecast."

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
