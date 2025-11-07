'use client'

import React from 'react'
import { Editor } from '@tiptap/react'
import { Bold, Italic, Underline, Strikethrough, Code, Highlighter } from 'lucide-react'

interface SimpleBubbleMenuProps {
  editor: Editor
  isVisible: boolean
  position: { top: number; left: number }
}

const SimpleBubbleMenu: React.FC<SimpleBubbleMenuProps> = ({ editor, isVisible, position }) => {
  if (!isVisible) {
    return null
  }

  return (
    <div
      className="absolute z-50 bg-gray-800 dark:bg-gray-900 text-white rounded-lg shadow-lg p-1 flex gap-1"
      style={{
        top: `${position.top}px`,
        left: `${position.left}px`,
      }}
    >
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBold().run()}
        className={`p-2 rounded hover:bg-gray-700 ${
          editor.isActive('bold') ? 'bg-gray-600' : ''
        }`}
        title="굵게"
      >
        <Bold size={16} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleItalic().run()}
        className={`p-2 rounded hover:bg-gray-700 ${
          editor.isActive('italic') ? 'bg-gray-600' : ''
        }`}
        title="기울임"
      >
        <Italic size={16} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleUnderline().run()}
        className={`p-2 rounded hover:bg-gray-700 ${
          editor.isActive('underline') ? 'bg-gray-600' : ''
        }`}
        title="밑줄"
      >
        <Underline size={16} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleStrike().run()}
        className={`p-2 rounded hover:bg-gray-700 ${
          editor.isActive('strike') ? 'bg-gray-600' : ''
        }`}
        title="취소선"
      >
        <Strikethrough size={16} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleCode().run()}
        className={`p-2 rounded hover:bg-gray-700 ${
          editor.isActive('code') ? 'bg-gray-600' : ''
        }`}
        title="인라인 코드"
      >
        <Code size={16} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHighlight().run()}
        className={`p-2 rounded hover:bg-gray-700 ${
          editor.isActive('highlight') ? 'bg-gray-600' : ''
        }`}
        title="형광펜"
      >
        <Highlighter size={16} />
      </button>
    </div>
  )
}

export default SimpleBubbleMenu
