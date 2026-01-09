from sentence_transformers import SentenceTransformer
import numpy as np

# Load a small, efficient model suitable for CPU
# 'all-MiniLM-L6-v2' is a very popular, fast, and lightweight model for embeddings
_model = None

def get_model():
    global _model
    if _model is None:
        print("Loading embedding model (all-MiniLM-L6-v2)...")
        _model = SentenceTransformer('all-MiniLM-L6-v2')
        print("Model loaded.")
    return _model

def generate_embedding(text: str) -> list[float]:
    """
    Generates a localized vector embedding for the given text using 'all-MiniLM-L6-v2'.
    
    Design Decision:
    We use a local CPU-friendly model rather than an external API (like OpenAI) to:
    1. Keep data local/private.
    2. Reduce latency for simple semantic checks.
    3. Avoid 3rd party costs during development.
    
    Args:
        text: The input string to embed.
        
    Returns:
        List[float]: A 384-dimensional vector.
    """
    model = get_model()
    return model.encode(text).tolist()

def compute_similarity(embedding1: list[float], embedding2: list[float]) -> float:
    """
    Computes Cosine Similarity between two vectors.
    
    Args:
        embedding1: First vector.
        embedding2: Second vector.
        
    Returns:
        float: Similarity score between -1.0 and 1.0.
    """
    # Cosine Similarity
    vec1 = np.array(embedding1)
    vec2 = np.array(embedding2)
    
    norm1 = np.linalg.norm(vec1)
    norm2 = np.linalg.norm(vec2)
    
    if norm1 == 0 or norm2 == 0:
        return 0.0
        
    return float(np.dot(vec1, vec2) / (norm1 * norm2))
