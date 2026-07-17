#!/usr/bin/env bash

# Khai báo các ID và file tương ứng
ID_DEBUG="ses_090e49f8dffeI9ymSHFVFOK0np"
FILE_DEBUG="ses-debug.json"

ID_CODING="ses_090e4b0cbffesnrSrdr0YvUYHL"
FILE_CODING="ses-coding.json"

echo "======================================="
echo "   OPENCODE SESSION AUTO SYNC"
echo "======================================="
echo "1. Xuất dữ liệu (Export từ OpenCode ra file)"
echo "2. Nhập dữ liệu (Import từ file vào OpenCode)"
read -p "Nhập lựa chọn của bạn (1 hoặc 2): " choice

case "$choice" in
    1)
        echo "--> Đang tự động export cả 2 session..."
        
        # Export debug session
        echo "Đang xuất: $ID_DEBUG -> $FILE_DEBUG"
        opencode export "$ID_DEBUG" > "$FILE_DEBUG" 2>/dev/null || opencode export "$ID_DEBUG" -o "$FILE_DEBUG"
        
        # Export coding session
        echo "Đang xuất: $ID_CODING -> $FILE_CODING"
        opencode export "$ID_CODING" > "$FILE_CODING" 2>/dev/null || opencode export "$ID_CODING" -o "$FILE_CODING"
        
        echo "=> Hoàn thành Export cả 2 file!"
        ;;
        
    2)
        echo "--> Đang tự động import cả 2 session..."
        
        # Import debug session
        if [ -f "$FILE_DEBUG" ]; then
            echo "Đang nhập: $FILE_DEBUG"
            opencode import "$FILE_DEBUG"
        else
            echo "[Lỗi] Không tìm thấy file $FILE_DEBUG"
        fi
        
        # Import coding session
        if [ -f "$FILE_CODING" ]; then
            echo "Đang nhập: $FILE_CODING"
            opencode import "$FILE_CODING"
        else
            echo "[Lỗi] Không tìm thấy file $FILE_CODING"
        fi
        
        echo "=> Hoàn thành Import cả 2 file!"
        ;;
        
    *)
        echo "Lựa chọn không hợp lệ! Vui lòng chỉ chọn 1 hoặc 2."
        exit 1
        ;;
esac
