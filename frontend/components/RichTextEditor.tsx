'use client'

import React, { useState, useEffect } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Link from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import TextAlign from '@tiptap/extension-text-align'
import Placeholder from '@tiptap/extension-placeholder'
import Underline from '@tiptap/extension-underline'
import Highlight from '@tiptap/extension-highlight'
import Color from '@tiptap/extension-color'
import { TextStyle } from '@tiptap/extension-text-style'
import Subscript from '@tiptap/extension-subscript'
import Superscript from '@tiptap/extension-superscript'
import { Table } from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import Youtube from '@tiptap/extension-youtube'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import { common, createLowlight } from 'lowlight'
import MenuBar from './editor/MenuBar'
import SimpleBubbleMenu from './editor/SimpleBubbleMenu'
import { FileText } from 'lucide-react'

const lowlight = createLowlight(common)

interface RichTextEditorProps {
  content: string
  onChange: (html: string) => void
  placeholder?: string
}

const RichTextEditor: React.FC<RichTextEditorProps> = ({ content, onChange, placeholder }) => {
  const [isFullscreen, setIsFullscreen] = useState(false)
  const [wordCount, setWordCount] = useState(0)
  const [charCount, setCharCount] = useState(0)
  const [bubbleMenuVisible, setBubbleMenuVisible] = useState(false)
  const [bubbleMenuPosition, setBubbleMenuPosition] = useState({ top: 0, left: 0 })

  const editor = useEditor({
    immediatelyRender: false,
    extensions: [
      StarterKit.configure({
        heading: {
          levels: [1, 2, 3]
        },
        codeBlock: false
      }),
      CodeBlockLowlight.configure({
        lowlight,
        HTMLAttributes: {
          class: 'bg-gray-900 text-gray-100 rounded-lg p-4 my-4'
        }
      }),
      Link.configure({
        openOnClick: false,
        HTMLAttributes: {
          class: 'text-primary-600 hover:underline cursor-pointer'
        }
      }),
      Image.configure({
        HTMLAttributes: {
          class: 'max-w-full h-auto rounded-lg my-4'
        }
      }),
      TextAlign.configure({
        types: ['heading', 'paragraph']
      }),
      Placeholder.configure({
        placeholder: placeholder || '내용을 입력하세요... (/ 를 입력하여 명령어 메뉴를 열 수 있습니다)'
      }),
      Underline,
      Highlight.configure({
        HTMLAttributes: {
          class: 'bg-yellow-200 dark:bg-yellow-800'
        }
      }),
      Color,
      TextStyle,
      Subscript,
      Superscript,
      Table.configure({
        resizable: true,
        HTMLAttributes: {
          class: 'border-collapse table-auto w-full my-4'
        }
      }),
      TableRow.configure({
        HTMLAttributes: {
          class: 'border border-gray-300 dark:border-gray-600'
        }
      }),
      TableCell.configure({
        HTMLAttributes: {
          class: 'border border-gray-300 dark:border-gray-600 px-3 py-2'
        }
      }),
      TableHeader.configure({
        HTMLAttributes: {
          class: 'border border-gray-300 dark:border-gray-600 px-3 py-2 bg-gray-100 dark:bg-gray-700 font-semibold'
        }
      }),
      Youtube.configure({
        width: 640,
        height: 360,
        HTMLAttributes: {
          class: 'rounded-lg my-4'
        }
      })
    ],
    content,
    onUpdate: ({ editor }) => {
      onChange(editor.getHTML())
      updateCounts(editor.getText())
    },
    onCreate: ({ editor }) => {
      updateCounts(editor.getText())
    },
    onSelectionUpdate: ({ editor }) => {
      const { from, to } = editor.state.selection
      const hasSelection = from !== to

      if (hasSelection) {
        const { view } = editor
        const coords = view.coordsAtPos(from)
        setBubbleMenuPosition({
          top: coords.top - 50,
          left: coords.left
        })
        setBubbleMenuVisible(true)
      } else {
        setBubbleMenuVisible(false)
      }
    }
  })

  const updateCounts = (text: string) => {
    setCharCount(text.length)
    setWordCount(text.split(/\s+/).filter(word => word.length > 0).length)
  }

  // content prop 변경 시 에디터 내용 업데이트
  useEffect(() => {
    if (editor && content !== editor.getHTML()) {
      editor.commands.setContent(content)
    }
  }, [content, editor])

  if (!editor) {
    return null
  }

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen)
  }

  return (
    <div className={`border border-gray-300 dark:border-gray-600 rounded-lg overflow-hidden transition-all ${
      isFullscreen ? 'fixed inset-0 z-50 bg-white dark:bg-gray-900' : ''
    }`}>
      <MenuBar editor={editor} onFullscreen={toggleFullscreen} />

      <SimpleBubbleMenu
        editor={editor}
        isVisible={bubbleMenuVisible}
        position={bubbleMenuPosition}
      />

      <EditorContent
        editor={editor}
        className={`ProseMirror-container focus:outline-none ${
          isFullscreen
            ? 'min-h-screen max-h-screen overflow-y-auto px-12 py-8'
            : 'min-h-[400px] max-h-[600px] overflow-y-auto px-8 py-6'
        }`}
      />

      <div className="bg-gray-50 dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 px-4 py-2 flex items-center justify-between text-sm text-gray-600 dark:text-gray-400">
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-1">
            <FileText size={14} />
            {wordCount} 단어
          </span>
          <span>{charCount} 문자</span>
        </div>
        <div className="text-xs">
          <kbd className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded">Ctrl</kbd>
          +
          <kbd className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded">B</kbd>
          굵게 ·
          <kbd className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded">Ctrl</kbd>
          +
          <kbd className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded">I</kbd>
          기울임 ·
          <kbd className="px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 rounded">/</kbd>
          명령어
        </div>
      </div>
    </div>
  )
}

export default RichTextEditor
