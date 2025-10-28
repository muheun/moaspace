/**
 * Lexical 마크다운 에디터 컴포넌트
 *
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 동시 추출
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트 우선 아키텍처
 */

'use client';

import { useCallback, useEffect } from 'react';
import { LexicalComposer } from '@lexical/react/LexicalComposer';
import { RichTextPlugin } from '@lexical/react/LexicalRichTextPlugin';
import { ContentEditable } from '@lexical/react/LexicalContentEditable';
import { HistoryPlugin } from '@lexical/react/LexicalHistoryPlugin';
import { OnChangePlugin } from '@lexical/react/LexicalOnChangePlugin';
import LexicalErrorBoundary from '@lexical/react/LexicalErrorBoundary';
import { HeadingNode, QuoteNode } from '@lexical/rich-text';
import { ListNode, ListItemNode } from '@lexical/list';
import { CodeNode } from '@lexical/code';
import { LinkNode } from '@lexical/link';
import { EditorState, $getRoot } from 'lexical';
import { $generateHtmlFromNodes } from '@lexical/html';
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';
import { MarkdownShortcutPlugin } from './plugins/MarkdownShortcutPlugin';

interface LexicalEditorProps {
  /**
   * 초기 HTML 콘텐츠 (수정 시 사용)
   */
  initialContent?: string;

  /**
   * 에디터 내용 변경 시 호출
   * @param html - HTML 포맷 (content 필드에 저장)
   * @param plainText - Plain Text (plainContent 필드에 저장, 벡터화 대상)
   */
  onChange: (html: string, plainText: string) => void;

  /**
   * placeholder 텍스트
   */
  placeholder?: string;

  /**
   * 에디터 비활성화 여부
   */
  disabled?: boolean;
}

/**
 * Lexical 에디터 설정
 */
const editorConfig = {
  namespace: 'PostEditor',
  theme: {
    paragraph: 'mb-2',
    heading: {
      h1: 'text-3xl font-bold mb-4',
      h2: 'text-2xl font-bold mb-3',
      h3: 'text-xl font-bold mb-2',
    },
    list: {
      ul: 'list-disc ml-4',
      ol: 'list-decimal ml-4',
      listitem: 'mb-1',
    },
    quote: 'border-l-4 border-gray-300 pl-4 italic my-4',
    code: 'bg-gray-100 px-1 py-0.5 rounded font-mono text-sm',
  },
  nodes: [
    HeadingNode,
    QuoteNode,
    ListNode,
    ListItemNode,
    CodeNode,
    LinkNode,
  ],
  onError: (error: Error) => {
    console.error('[LexicalEditor] Error:', error);
  },
};

export function LexicalEditor({
  initialContent,
  onChange,
  placeholder = '게시글을 작성하세요...',
  disabled = false,
}: LexicalEditorProps) {
  /**
   * 에디터 상태 변경 시 HTML + Plain Text 추출
   */
  const handleChange = useCallback(
    (editorState: EditorState) => {
      editorState.read(() => {
        // HTML 추출 (content 필드)
        const html = $generateHtmlFromNodes(null);

        // Plain Text 추출 (plainContent 필드, 벡터화 대상)
        const root = $getRoot();
        const plainText = root.getTextContent();

        onChange(html, plainText);
      });
    },
    [onChange]
  );

  return (
    <LexicalComposer initialConfig={editorConfig}>
      <div className="relative rounded-md border border-input bg-background">
        <RichTextPlugin
          contentEditable={
            <ContentEditable
              className="min-h-[200px] px-4 py-3 outline-none prose prose-sm max-w-none"
              aria-label="게시글 에디터"
              disabled={disabled}
            />
          }
          placeholder={
            <div className="absolute top-3 left-4 text-muted-foreground pointer-events-none">
              {placeholder}
            </div>
          }
          ErrorBoundary={LexicalErrorBoundary}
        />
        <HistoryPlugin />
        <OnChangePlugin onChange={handleChange} />
        <MarkdownShortcutPlugin />
        {initialContent && <InitialContentPlugin initialContent={initialContent} />}
      </div>
    </LexicalComposer>
  );
}

/**
 * 초기 콘텐츠 로드 플러그인
 */
function InitialContentPlugin({ initialContent }: { initialContent: string }) {
  const [editor] = useLexicalComposerContext();

  useEffect(() => {
    if (initialContent && editor) {
      editor.update(() => {
        const root = $getRoot();
        root.clear();

        const tempDiv = document.createElement('div');
        tempDiv.innerHTML = initialContent;

        root.append(...Array.from(tempDiv.childNodes).map(node => {
          const textNode = node.textContent || '';
          return $getRoot().createTextNode(textNode);
        }));
      });
    }
  }, [editor, initialContent]);

  return null;
}
