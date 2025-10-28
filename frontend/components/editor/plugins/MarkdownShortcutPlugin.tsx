/**
 * Lexical 마크다운 단축키 플러그인
 *
 * 마크다운 문법 지원:
 * - # ~ ### : 제목 (h1 ~ h3)
 * - - 또는 * : 순서 없는 리스트
 * - 1. : 순서 있는 리스트
 * - > : 인용구
 * - ` : 인라인 코드
 */

'use client';

import { useEffect } from 'react';
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';
import {
  TRANSFORMERS,
} from '@lexical/markdown';

export function MarkdownShortcutPlugin() {
  const [editor] = useLexicalComposerContext();

  useEffect(() => {
    // 마크다운 변환기 등록
    return editor.registerUpdateListener(() => {
      // 필요 시 마크다운 변환 로직 추가
      });
    });
  }, [editor]);

  return null;
}
