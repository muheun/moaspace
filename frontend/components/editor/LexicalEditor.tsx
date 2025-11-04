/**
 * Lexical 마크다운 에디터 컴포넌트
 *
 * Constitution Principle VIII: content (HTML) + plainContent (Plain Text) 동시 추출
 * Constitution Principle VI: shadcn/ui 기반 컴포넌트 우선 아키텍처
 */

'use client';

import { $getRoot } from 'lexical'
import { useEffect, useState } from 'react'
import './styles/editor.css'

import { LexicalComposer } from '@lexical/react/LexicalComposer'
import { RichTextPlugin } from '@lexical/react/LexicalRichTextPlugin'
import { ContentEditable } from '@lexical/react/LexicalContentEditable'
import { HistoryPlugin } from '@lexical/react/LexicalHistoryPlugin'
import { AutoFocusPlugin } from '@lexical/react/LexicalAutoFocusPlugin'
import { LexicalErrorBoundary } from '@lexical/react/LexicalErrorBoundary'
import { HeadingNode, QuoteNode } from '@lexical/rich-text'
import { TableCellNode, TableNode, TableRowNode } from '@lexical/table'
import { ListItemNode, ListNode } from '@lexical/list'
import { CodeHighlightNode, CodeNode } from '@lexical/code'
import { AutoLinkNode, LinkNode } from '@lexical/link'
import { LinkPlugin } from '@lexical/react/LexicalLinkPlugin'
import { ListPlugin } from '@lexical/react/LexicalListPlugin'
import { MarkdownShortcutPlugin } from '@lexical/react/LexicalMarkdownShortcutPlugin'
import { TRANSFORMERS, $convertToMarkdownString } from '@lexical/markdown'

import ToolbarPlugin from './plugins/ToolbarPlugin'
import AutoLinkPlugin from './plugins/AutoLinkPlugin'
import CodeHighlightPlugin from './plugins/CodeHighlightPlugin'
import ListMaxIndentLevelPlugin from './plugins/ListMaxIndentLevelPlugin'
import ExampleTheme from './themes/ExampleTheme'
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext'
import { $generateHtmlFromNodes, $generateNodesFromDOM } from '@lexical/html'
import { $insertNodes, $createParagraphNode, $createTextNode } from 'lexical'

function Placeholder() {
  return (
    <div className="absolute top-4 left-4 text-gray-400 pointer-events-none select-none">
      게시글을 작성하세요...
    </div>
  )
}

interface LexicalEditorProps {
  /**
   * 초기 HTML 콘텐츠 (수정 시 사용)
   */
  initialContent?: string

  /**
   * 에디터 내용 변경 시 호출
   * @param html - HTML 포맷 (contentHtml 필드에 저장)
   * @param text - Plain Text (contentText 필드에 저장, 벡터화 대상)
   */
  onChange: (html: string, text: string) => void

  /**
   * placeholder 텍스트
   */
  placeholder?: string

  /**
   * 에디터 비활성화 여부
   */
  disabled?: boolean
}

// 에디터 설정
function createEditorConfig() {
  return {
    namespace: 'PostEditor',
    theme: ExampleTheme,
    onError(error: Error) {
      console.error('[LexicalEditor] Error:', error)
    },
    nodes: [
      HeadingNode,
      ListNode,
      ListItemNode,
      QuoteNode,
      CodeNode,
      CodeHighlightNode,
      TableNode,
      TableCellNode,
      TableRowNode,
      AutoLinkNode,
      LinkNode
    ],
    editorState: undefined
  }
}

// 초기값 설정 플러그인
function InitialValuePlugin({ initialContent }: { initialContent?: string }) {
  const [editor] = useLexicalComposerContext()
  const [isFirst, setIsFirst] = useState(true)

  useEffect(() => {
    if (!initialContent || !isFirst) return

    editor.update(() => {
      const root = $getRoot()
      root.clear()

      if (initialContent.trim().startsWith('<')) {
        try {
          const parser = new DOMParser()
          const dom = parser.parseFromString(initialContent, 'text/html')
          const nodes = $generateNodesFromDOM(editor, dom)

          if (nodes.length > 0) {
            $insertNodes(nodes)
          } else {
            const paragraph = $createParagraphNode()
            paragraph.append($createTextNode(initialContent))
            root.append(paragraph)
          }
        } catch (error) {
          console.error('[LexicalEditor] Failed to parse HTML:', error)
          const paragraph = $createParagraphNode()
          paragraph.append($createTextNode(initialContent))
          root.append(paragraph)
        }
      } else {
        const paragraph = $createParagraphNode()
        paragraph.append($createTextNode(initialContent))
        root.append(paragraph)
      }
    })

    setIsFirst(false)
  }, [editor, initialContent, isFirst])

  return null
}

// 에디터 상태 변경 핸들러 컴포넌트
function EditorChangeHandler({ onChange }: { onChange?: (html: string, text: string) => void }) {
  const [editor] = useLexicalComposerContext()

  useEffect(() => {
    if (!onChange) return

    return editor.registerUpdateListener(({ editorState }) => {
      editorState.read(() => {
        // HTML 생성
        const html = $generateHtmlFromNodes(editor)

        // Plain Text 추출
        const root = $getRoot()
        const text = root.getTextContent()

        onChange(html, text)
      })
    })
  }, [editor, onChange])

  return null
}

export function LexicalEditor({
  initialContent,
  onChange,
  placeholder,
  disabled = false
}: LexicalEditorProps) {
  const editorConfig = createEditorConfig()

  return (
    <LexicalComposer initialConfig={editorConfig}>
      <div className="relative w-full">
        {/* 툴바 */}
        <ToolbarPlugin />

        {/* 에디터 본문 */}
        <div className="relative min-h-[400px] bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 rounded-b-lg">
          <RichTextPlugin
            contentEditable={
              <ContentEditable
                className="min-h-[400px] px-4 py-4 outline-none resize-none text-gray-900 dark:text-white bg-white dark:bg-gray-800"
                aria-label="게시글 에디터"
                disabled={disabled}
              />
            }
            placeholder={placeholder ? <div className="absolute top-4 left-4 text-gray-400 pointer-events-none select-none">{placeholder}</div> : <Placeholder />}
            ErrorBoundary={LexicalErrorBoundary}
          />

          {/* 플러그인들 */}
          <HistoryPlugin />
          <AutoFocusPlugin />
          <CodeHighlightPlugin />
          <ListPlugin />
          <LinkPlugin />
          <AutoLinkPlugin />
          <ListMaxIndentLevelPlugin maxDepth={7} />
          <MarkdownShortcutPlugin transformers={TRANSFORMERS} />

          {/* 초기값 설정 */}
          {initialContent && <InitialValuePlugin initialContent={initialContent} />}

          {/* 변경사항 핸들러 */}
          {onChange && <EditorChangeHandler onChange={onChange} />}
        </div>
      </div>
    </LexicalComposer>
  )
}
