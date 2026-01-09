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

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Create tables if they don't exist
    Base.metadata.create_all(bind=engine)
    
    # Start Metadata Listener (Background Task)
    asyncio.create_task(consume_metadata())
    
    # Load ML Model
    global intent_model
    global nlp

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

@app.post("/query", response_model=AgentResponse)
def query_agent(request: QueryRequest, db: Session = Depends(get_db)):
    """
    Handles the user query by identifying intent, performing RAG, and generating a response.
    
    Flow:
    1. Identify Intent (Semantic Guardrails + Rules)
    2. If valid, perform RAG (Retrieval Augmented Generation) by searching KnowledgeChunks.
    3. Log the query and result to IntelligenceQueryLog for analytics.
    
    Returns:
        AgentResponse: The answer and any optional visualization data.
    """
    # 1. Identify Intent (Mock Implementation using Regex/Keywords)
    intent = identify_intent(request.question)
    
    # 2. Logic based on intent (Story 4 Placeholder)
    answer_text = intent.response_text
    
    # RAG Retrieval Logic (Story 5)
    retrieved_chunks = []
    if intent.intent_type != "out_of_scope":
        try:
            # 1. Embed query
            # Note: In production this would utilize a Vector DB. 
            # Here we do a linear scan in Python for demonstration/MVP.
            query_embedding = generate_embedding(request.question)
            
            # 2. Retrieve chunks from DB
            chunks = db.query(KnowledgeChunk).all()
            
            scored_chunks = []
            for chunk in chunks:
                if chunk.embedding:
                    score = compute_similarity(query_embedding, chunk.embedding)
                    if score > 0.3: # Threshold
                        scored_chunks.append((score, chunk.content))
            
            # 3. Sort and Select top 3
            scored_chunks.sort(key=lambda x: x[0], reverse=True)
            top_chunks = scored_chunks[:3]
            
            if top_chunks:
                retrieved_context = "\n".join([f"- {c[1]}" for c in top_chunks])
                retrieved_chunks = [c[1] for c in top_chunks]
                answer_text += f"\n\nContext found:\n{retrieved_context}"
                
        except Exception as e:
            print(f"RAG Retrieval failed: {e}")

    # 3. Handle Out of Scope / Low Confidence
    query_status = "answered"
    
    if intent.intent_type == "out_of_scope":
        query_status = "out_of_scope"
    elif intent.confidence < 0.6:
        query_status = "low_confidence"
        answer_text += " (Note: verified data for this specific query might be missing.)"
    elif intent.intent_type == "fact_retrieval":
        # Attempt to get real data
        db_answer = execute_fact_retrieval(db, intent, request.question)
        if db_answer:
             answer_text = db_answer
        else:
             answer_text += f"\n[System Note: Identified as Fact Retrieval with confidence {intent.confidence}]"
    elif intent.intent_type == "forecast":
        # Execute Forecast Logic
        db_answer = execute_forecast(db, intent, request.question)
        if db_answer:
             answer_text = db_answer
        else:
             answer_text += f"\n[System Note: Identified as Forecast with confidence {intent.confidence}]"

    # 4. Log to Database
    try:
        log_entry = IntelligenceQueryLog(
            query_text=request.question,
            detected_intent=intent.intent_type,
            confidence_score=intent.confidence,
            status=query_status
        )
        db.add(log_entry)
        db.commit()
    except Exception as e:
        print(f"Failed to log query: {e}")

    return AgentResponse(
        answer=answer_text,
        sources=[],
        visualization=intent.data if isinstance(intent.data, dict) else None
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

def execute_fact_retrieval(db: Session, intent: IntentResponse, question: str) -> Optional[str]:
    """
    Executes a structured query against the FactValue table based on identified entities.
    Uses Spacy for location extraction and Vectors for Metric mapping (No hardcoded rules).
    """
    try:
        query = db.query(FactValue)
        
        # 0. NLP Processing
        doc = nlp(question) if nlp else None
        
        # 1. Geo Identification (Auto-detected via Spacy)
        geo_id = None
        geo_level = "STATE" # Default fallback
        
        if doc:
            # Look for GPE (Geopolitical Entity)
            locations = [ent.text for ent in doc.ents if ent.label_ == "GPE"]
            if locations:
                # Naive: take the first one found
                found_loc = locations[0]
                
                # Attempt to map extracted location to FIPS code
                lower_loc = found_loc.lower()
                geo_id = US_STATES.get(lower_loc, found_loc)
                
                print(f"DEBUG: NLP Extracted Location: '{found_loc}' -> Mapped GeoID: '{geo_id}'")
                    
                # If we found a specific location, filter by it
                if geo_id:
                   query = query.filter(FactValue.geo_id == geo_id)

        # 2. Metric Identification (Dynamic Registry)
        # We don't use 'if "price" in question'. We use embedding distance.
        q_embedding = generate_embedding(question)
        
        best_metric_id = "ELECTRICITY_RETAIL_PRICE_CENTS_PER_KWH" # Default Fallback
        best_score = -1.0
        
        all_metrics = db.query(MetricMetadata).filter(MetricMetadata.embedding.isnot(None)).all()
        
        if all_metrics:
            for m in all_metrics:
                # Convert list to vector if needed, or rely on compute_similarity handling lists
                score = compute_similarity(q_embedding, m.embedding)
                if score > best_score:
                    best_score = score
                    best_metric_id = m.metric_id
        
        print(f"DEBUG: Semantic Metric Selection: '{best_metric_id}' (Score: {best_score:.3f})")
        query = query.filter(FactValue.metric_id == best_metric_id)
        
        # 3. Ordering
        order_asc = True
        q_lower = question.lower()
        if any(w in q_lower for w in ["most", "highest", "expensive", "max"]):
            order_asc = False
            
        if order_asc:
            query = query.order_by(asc(FactValue.value_numeric))
        else:
            query = query.order_by(desc(FactValue.value_numeric))
            
        # 4. Limit
        result = query.limit(1).first()
        
        if result:
            metric_name = "Retail Price" if "PRICE" in best_metric_id else "Monthly Cost"
            unit = "cents/kWh" if "PRICE" in best_metric_id else "USD"
            
            # Resolve State Name from FIPS for display
            display_loc = FIPS_TO_NAME.get(result.geo_id, result.geo_id)
            loc_str = f"in {display_loc}" if result.geo_id else ""
            
            return f"The {geo_level.lower()} {loc_str} with the {'lowest' if order_asc else 'highest'} {metric_name} is {display_loc} ({result.geo_id}) with a value of {result.value_numeric} {unit} (Period: {str(result.period_start)[:10]})."
        else:
            return "I couldn't find any data matching those criteria in the database."
            
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
