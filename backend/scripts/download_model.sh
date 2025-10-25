#!/bin/bash

# MiniLM-L12-v2 ONNX 모델 다운로드 스크립트
# HuggingFace Hub에서 모델 파일과 토크나이저를 다운로드합니다.

set -e  # 오류 발생 시 즉시 종료

# 색상 코드 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 스크립트 위치 기준 경로 계산
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"

# 설정
MODEL_DIR="$BACKEND_DIR/models/models_minilm"
BASE_URL="https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main"
MAX_RETRIES=3
RETRY_DELAY=2
MIN_FILE_SIZE=100000000  # 100MB (바이트 단위)

# 다운로드할 파일 목록
FILES=(
    "onnx/model.onnx"
    "tokenizer.json"
    "config.json"
    "tokenizer_config.json"
)

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}MiniLM-L12-v2 모델 다운로드 시작${NC}"
echo -e "${GREEN}========================================${NC}"

# 모델 디렉토리 생성
mkdir -p "$MODEL_DIR"

# 각 파일 다운로드
for file in "${FILES[@]}"; do
    filename=$(basename "$file")
    output_path="$MODEL_DIR/$filename"

    # 파일이 이미 존재하는지 확인
    if [ -f "$output_path" ]; then
        file_size=$(stat -f%z "$output_path" 2>/dev/null || stat -c%s "$output_path" 2>/dev/null || echo "0")

        # model.onnx 파일은 크기 검증
        if [ "$filename" == "model.onnx" ] && [ "$file_size" -lt "$MIN_FILE_SIZE" ]; then
            echo -e "${YELLOW}⚠️  $filename 파일이 너무 작습니다 (${file_size} bytes). 재다운로드합니다.${NC}"
        else
            echo -e "${GREEN}✓ $filename 이미 존재합니다. 건너뜁니다.${NC}"
            continue
        fi
    fi

    # 다운로드 시도 (재시도 로직 포함)
    echo -e "${YELLOW}⬇️  $filename 다운로드 중...${NC}"

    retry_count=0
    success=false

    while [ $retry_count -lt $MAX_RETRIES ]; do
        if curl -L -o "$output_path" "$BASE_URL/$file" --progress-bar; then
            # 다운로드 성공 확인
            if [ -f "$output_path" ]; then
                file_size=$(stat -f%z "$output_path" 2>/dev/null || stat -c%s "$output_path" 2>/dev/null || echo "0")

                # model.onnx 파일은 크기 검증
                if [ "$filename" == "model.onnx" ]; then
                    if [ "$file_size" -lt "$MIN_FILE_SIZE" ]; then
                        echo -e "${RED}✗ 다운로드한 파일이 너무 작습니다 (${file_size} bytes).${NC}"
                        rm -f "$output_path"
                        retry_count=$((retry_count + 1))

                        if [ $retry_count -lt $MAX_RETRIES ]; then
                            echo -e "${YELLOW}⏳ ${RETRY_DELAY}초 후 재시도합니다... (${retry_count}/${MAX_RETRIES})${NC}"
                            sleep $RETRY_DELAY
                        fi
                        continue
                    fi
                fi

                echo -e "${GREEN}✓ $filename 다운로드 완료 (${file_size} bytes)${NC}"
                success=true
                break
            fi
        fi

        retry_count=$((retry_count + 1))

        if [ $retry_count -lt $MAX_RETRIES ]; then
            echo -e "${YELLOW}⏳ ${RETRY_DELAY}초 후 재시도합니다... (${retry_count}/${MAX_RETRIES})${NC}"
            sleep $RETRY_DELAY
        fi
    done

    if [ "$success" = false ]; then
        echo -e "${RED}✗ $filename 다운로드 실패 (최대 재시도 횟수 초과)${NC}"
        exit 1
    fi
done

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ 모든 모델 파일 다운로드 완료${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "모델 위치: ${MODEL_DIR}"
echo -e ""
echo -e "다운로드된 파일:"
ls -lh "$MODEL_DIR"
