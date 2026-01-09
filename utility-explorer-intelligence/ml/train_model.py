import json
import joblib
import os
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import SGDClassifier
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

def train():
    # Load Data
    data_path = os.path.join(os.path.dirname(__file__), 'training_data.json')
    with open(data_path, 'r') as f:
        data = json.load(f)

    texts = [item['text'] for item in data]
    labels = [item['intent'] for item in data]

    # Split (Mock split for small data, usually we need more)
    # X_train, X_test, y_train, y_test = train_test_split(texts, labels, test_size=0.2, random_state=42)
    # Using all data for this demo since dataset is tiny
    X_train, y_train = texts, labels

    # Create Pipeline
    # TfidfVectorizer: Converts text to vectors
    # SGDClassifier: A linear classifier (SVM/Logistic) optimized for text
    pipeline = Pipeline([
        ('tfidf', TfidfVectorizer(ngram_range=(1, 2), min_df=1)),
        ('clf', SGDClassifier(loss='hinge', penalty='l2', alpha=1e-3, random_state=42, max_iter=5, tol=None)),
    ])

    # Train
    print("Training model...")
    pipeline.fit(X_train, y_train)

    # Save
    model_path = os.path.join(os.path.dirname(__file__), 'intent_model.pkl')
    joblib.dump(pipeline, model_path)
    print(f"Model saved to {model_path}")

    # Test prediction
    test_phrase = "electricity cost in florida"
    prediction = pipeline.predict([test_phrase])[0]
    print(f"Test '{test_phrase}' -> {prediction}")

if __name__ == "__main__":
    train()
