CREATE TABLE user_query (
  id BIGSERIAL PRIMARY KEY,
  question_text TEXT,
  timestamp TIMESTAMPTZ,
  metric_id TEXT,
  source_id TEXT,
  feedback TEXT,
  is_model_generated BOOLEAN,
  created_at TIMESTAMPTZ
);
