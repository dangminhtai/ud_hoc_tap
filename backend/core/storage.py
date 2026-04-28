import os
import sqlite3
import json
from datetime import datetime

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB_PATH = os.path.join(BASE_DIR, "ud_hoc_tap.db")

def init_db():
    """Khởi tạo cấu trúc Database SQLite."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # --- Chat Tables ---
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS chat_sessions (
        id TEXT PRIMARY KEY,
        title TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    ''')
    
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS chat_messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        session_id TEXT,
        role TEXT,
        text TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
    )
    ''')
    
    # --- Notebook & Flashcard Tables ---
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS notebooks (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        tags TEXT, -- JSON string
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    ''')
    
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS notebook_records (
        id TEXT PRIMARY KEY,
        record_type TEXT, -- solve, question, etc.
        title TEXT,
        summary TEXT,
        user_query TEXT,
        output TEXT,
        kb_name TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    ''')
    
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS notebook_record_map (
        notebook_id TEXT,
        record_id TEXT,
        PRIMARY KEY (notebook_id, record_id),
        FOREIGN KEY (notebook_id) REFERENCES notebooks(id),
        FOREIGN KEY (record_id) REFERENCES notebook_records(id)
    )
    ''')

    # --- Knowledge Base Files (Gemini File API) ---
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS kb_files (
        id TEXT PRIMARY KEY,
        kb_name TEXT,
        file_name TEXT,
        file_uri TEXT,
        file_type TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    ''')
    
    conn.commit()

    # --- Migration ---
    cursor.execute("PRAGMA table_info(chat_sessions)")
    columns = [col[1] for col in cursor.fetchall()]
    if "title" not in columns:
        cursor.execute("ALTER TABLE chat_sessions ADD COLUMN title TEXT")
    if "updated_at" not in columns:
        cursor.execute("ALTER TABLE chat_sessions ADD COLUMN updated_at TIMESTAMP")
    
    conn.commit()
    conn.close()

# --- Functions ---

def save_chat_message(session_id, role, text):
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("INSERT OR IGNORE INTO chat_sessions (id, title) VALUES (?, ?)", (session_id, "Cuộc trò chuyện mới"))
    if role == "user":
        cursor.execute("SELECT COUNT(*) FROM chat_messages WHERE session_id = ?", (session_id,))
        if cursor.fetchone()[0] == 0:
            title = text[:50] + "..." if len(text) > 50 else text
            cursor.execute("UPDATE chat_sessions SET title = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", (title, session_id))
        else:
            cursor.execute("UPDATE chat_sessions SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", (session_id,))
    cursor.execute("INSERT INTO chat_messages (session_id, role, text) VALUES (?, ?, ?)", (session_id, role, text))
    conn.commit(); conn.close()

def get_chat_history(session_id):
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("SELECT role, text FROM chat_messages WHERE session_id = ? ORDER BY created_at ASC", (session_id,))
    rows = cursor.fetchall(); conn.close()
    return [{"role": row[0], "text": row[1]} for row in rows]

def list_chat_sessions(limit=30):
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("SELECT id, title, created_at, updated_at FROM chat_sessions ORDER BY updated_at DESC LIMIT ?", (limit,))
    rows = cursor.fetchall()
    sessions = []
    for row in rows:
        cursor.execute("SELECT COUNT(*) FROM chat_messages WHERE session_id = ?", (row[0],))
        sessions.append({"session_id": row[0], "title": row[1], "created_at": row[2], "updated_at": row[3], "message_count": cursor.fetchone()[0]})
    conn.close(); return sessions

def delete_chat_session(session_id):
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("DELETE FROM chat_messages WHERE session_id = ?", (session_id,))
    cursor.execute("DELETE FROM chat_sessions WHERE id = ?", (session_id,))
    conn.commit(); conn.close(); return True

# Notebooks
def create_notebook(name, description="", tags=[]):
    import uuid as uuid_lib
    nb_id = str(uuid_lib.uuid4())
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("INSERT INTO notebooks (id, name, description, tags) VALUES (?, ?, ?, ?)", (nb_id, name, description, json.dumps(tags)))
    conn.commit(); conn.close(); return nb_id

def get_notebooks():
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("SELECT id, name, description, tags, created_at FROM notebooks ORDER BY updated_at DESC")
    rows = cursor.fetchall()
    notebooks = []
    for r in rows:
        cursor.execute("SELECT COUNT(*) FROM notebook_record_map WHERE notebook_id = ?", (r[0],))
        notebooks.append({"id": r[0], "name": r[1], "description": r[2], "tags": json.loads(r[3] or "[]"), "created_at": r[4], "record_count": cursor.fetchone()[0]})
    conn.close(); return notebooks

def get_notebook_detail(nb_id):
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("SELECT id, name, description, tags, created_at FROM notebooks WHERE id = ?", (nb_id,))
    nb = cursor.fetchone()
    if not nb: conn.close(); return None
    cursor.execute("SELECT r.id, r.record_type, r.title, r.summary, r.user_query, r.output, r.kb_name, r.created_at FROM notebook_records r JOIN notebook_record_map m ON r.id = m.record_id WHERE m.notebook_id = ? ORDER BY r.created_at DESC", (nb_id,))
    records = [{"id": r[0], "record_type": r[1], "title": r[2], "summary": r[3], "user_query": r[4], "output": r[5], "kb_name": r[6], "created_at": r[7]} for r in cursor.fetchall()]
    conn.close()
    return {"id": nb[0], "name": nb[1], "description": nb[2], "tags": json.loads(nb[3] or "[]"), "created_at": nb[4], "records": records}

def add_notebook_record(notebook_ids, record_type, title, summary, user_query, output, kb_name):
    import uuid as uuid_lib
    rec_id = str(uuid_lib.uuid4())
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("INSERT INTO notebook_records (id, record_type, title, summary, user_query, output, kb_name) VALUES (?, ?, ?, ?, ?, ?, ?)", (rec_id, record_type, title, summary, user_query, output, kb_name))
    for nb_id in notebook_ids:
        cursor.execute("INSERT INTO notebook_record_map (notebook_id, record_id) VALUES (?, ?)", (nb_id, rec_id))
        cursor.execute("UPDATE notebooks SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", (nb_id,))
    conn.commit(); conn.close(); return rec_id

# Knowledge Base Files
def save_kb_file(kb_name, file_name, file_uri, file_type):
    import uuid as uuid_lib
    f_id = str(uuid_lib.uuid4())
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("INSERT INTO kb_files (id, kb_name, file_name, file_uri, file_type) VALUES (?, ?, ?, ?, ?)", (f_id, kb_name, file_name, file_uri, file_type))
    conn.commit(); conn.close(); return f_id

def get_kb_files(kb_name):
    conn = sqlite3.connect(DB_PATH); cursor = conn.cursor()
    cursor.execute("SELECT file_name, file_uri, file_type FROM kb_files WHERE kb_name = ?", (kb_name,))
    rows = cursor.fetchall(); conn.close()
    return [{"file_name": r[0], "file_uri": r[1], "file_type": r[2]} for r in rows]

# Init
init_db()
