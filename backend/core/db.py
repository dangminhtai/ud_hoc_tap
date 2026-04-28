import os
import chromadb

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHROMA_DATA_PATH = os.path.join(BASE_DIR, "chroma_db")

chroma_client = chromadb.PersistentClient(
    path=CHROMA_DATA_PATH,
    settings=chromadb.Settings(anonymized_telemetry=False)
)
collection = chroma_client.get_or_create_collection(name="ud_hoc_tap_knowledge")
