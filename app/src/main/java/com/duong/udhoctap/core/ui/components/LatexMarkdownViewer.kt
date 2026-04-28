package com.duong.udhoctap.core.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.MaterialTheme

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LatexMarkdownViewer(
    content: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    
    // Chuyển đổi màu từ ARGB sang Hex để dùng trong CSS
    val textColorHex = String.format("#%06X", 0xFFFFFF and textColor)

    // Mã hóa Base64 để truyền dữ liệu an toàn vào JS (tránh lỗi ký tự đặc biệt)
    val base64Content = android.util.Base64.encodeToString(content.toByteArray(), android.util.Base64.NO_WRAP)

    // HTML template tích hợp KaTeX và Marked.js
    val htmlData = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"></script>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    font-size: 15px;
                    line-height: 1.6;
                    color: $textColorHex;
                    margin: 0;
                    padding: 0;
                    background-color: transparent;
                    -webkit-user-select: none;
                }
                .content {
                    word-wrap: break-word;
                }
                p { margin: 8px 0; }
                p:first-child { margin-top: 0; }
                p:last-child { margin-bottom: 0; }
                ul, ol { padding-left: 20px; margin: 8px 0; }
                code { background: rgba(0,0,0,0.05); padding: 2px 4px; border-radius: 4px; }
            </style>
        </head>
        <body>
            <div id="content" class="content"></div>
            <script>
                function decodeUnicodeBase64(s) {
                    return decodeURIComponent(atob(s).split('').map(function(c) {
                        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                    }).join(''));
                }

                document.addEventListener("DOMContentLoaded", function() {
                    try {
                        const rawContent = decodeUnicodeBase64('$base64Content');
                        
                        // Parse Markdown
                        document.getElementById('content').innerHTML = marked.parse(rawContent);
                        
                        // Render LaTeX
                        renderMathInElement(document.body, {
                            delimiters: [
                                {left: '$$', right: '$$', display: true},
                                {left: '$', right: '$', display: false},
                                {left: '\\(', right: '\\)', display: false},
                                {left: '\\[', right: '\\]', display: true}
                            ],
                            throwOnError : false
                        });
                    } catch (e) {
                        document.getElementById('content').innerText = "Lỗi hiển thị nội dung";
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(0) // Transparent
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
        }
    )
}
