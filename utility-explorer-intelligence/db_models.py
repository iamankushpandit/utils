from sqlalchemy import Column, Integer, String, Float, DateTime, Text, Boolean, ARRAY, Uuid
from sqlalchemy.sql import func
from database import Base

class IntelligenceQueryLog(Base):
    __tablename__ = "intelligence_query_log"

    id = Column(Integer, primary_key=True, index=True)
    query_text = Column(Text, nullable=False)
    detected_intent = Column(String, nullable=True)
    confidence_score = Column(Float, nullable=True)
    status = Column(String, nullable=False)  # 'answered', 'out_of_scope', 'unsupported', 'error'
    created_at = Column(DateTime(timezone=True), server_default=func.now())

class KnowledgeChunk(Base):
    __tablename__ = "knowledge_chunk"

    id = Column(Integer, primary_key=True, index=True)
    content = Column(Text, nullable=False)
    source_id = Column(String, nullable=False)
    embedding = Column(ARRAY(Float), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

class FactValue(Base):
    __tablename__ = "fact_value"

    # Composite Primary Key matching the actual definition in Postgres (V1__Create_core_tables.sql was different, actual DB is what matters)
    # "fact_value_pkey" PRIMARY KEY, btree (metric_id, source_id, geo_level, geo_id, period_start, period_end)
    
    metric_id = Column(String, primary_key=True)
    source_id = Column(String, primary_key=True)
    geo_level = Column(String, primary_key=True)
    geo_id = Column(String, primary_key=True)
    period_start = Column(DateTime, primary_key=True)
    period_end = Column(DateTime, primary_key=True)
    
    value_numeric = Column(Float, nullable=False)
