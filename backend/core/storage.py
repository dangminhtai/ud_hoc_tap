import os
import sqlite3
from datetime import datetime

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DB_PATH = os.path.join(BASE_DIR, "ud_hoc_tap.db")

def init_db():
    """Khởi tạo cấu trúc Database SQLite."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # Bảng lưu trữ phiên chat
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS chat_sessions (
        id TEXT PRIMARY KEY,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    ''')
    
    # Bảng lưu trữ tin nhắn
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
    
    conn.commit()
    conn.close()

def save_chat_message(session_id, role, text):
    """Lưu một tin nhắn vào database."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # Đảm bảo session tồn tại
    cursor.execute("INSERT OR IGNORE INTO chat_sessions (id) VALUES (?)", (session_id,))
    
    # Lưu tin nhắn
    cursor.execute(
        "INSERT INTO chat_messages (session_id, role, text) VALUES (?, ?, ?)",
        (session_id, role, text)
    )
    
    conn.commit()
    conn.close()

def get_chat_history(session_id):
    """Lấy toàn bộ lịch sử tin nhắn của một session."""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    cursor.execute(
        "SELECT role, text FROM chat_messages WHERE session_id = ? ORDER BY created_at ASC",
        (session_id,)
    )
    
    rows = cursor.fetchall()
    conn.close()
    
    return [{"role": row[0], "text": row[1]} for row in rows]

# Khởi tạo DB khi module được load
init_db()
