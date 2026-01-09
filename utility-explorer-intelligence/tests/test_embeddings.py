import pytest
import numpy as np
from embeddings import compute_similarity, generate_embedding

def test_compute_similarity_identical():
    """Test that similarity of identical vectors is 1.0"""
    vec1 = [1.0, 0.0, 0.0]
    vec2 = [1.0, 0.0, 0.0]
    assert compute_similarity(vec1, vec2) > 0.99

def test_compute_similarity_orthogonal():
    """Test that similarity of orthogonal vectors is 0.0"""
    vec1 = [1.0, 0.0, 0.0]
    vec2 = [0.0, 1.0, 0.0]
    assert abs(compute_similarity(vec1, vec2)) < 0.01

def test_compute_similarity_opposite():
    """Test that similarity of opposite vectors is -1.0"""
    vec1 = [1.0, 0.0]
    vec2 = [-1.0, 0.0]
    assert compute_similarity(vec1, vec2) < -0.99

def test_generate_embedding_returns_list(mocker):
    """Test that embedding generation returns a list of floats"""
    # Mock the actual model loading to speed up tests and avoid downloading models
    mock_model = mocker.Mock()
    mock_model.encode.return_value = np.array([0.1, 0.2, 0.3])
    
    mocker.patch('embeddings.get_model', return_value=mock_model)
    
    emb = generate_embedding("test query")
    assert isinstance(emb, list)
    assert len(emb) == 3
    assert isinstance(emb[0], float)
