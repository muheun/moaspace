'use client'

import React from 'react'
import { Editor } from '@tiptap/react'
import {
  Bold,
  Italic,
  Underline,
  Strikethrough,
  Code,
  Heading1,
  Heading2,
  Heading3,
  List,
  ListOrdered,
  Quote,
  Link,
  Image,
  Youtube,
  Table,
  AlignLeft,
  AlignCenter,
  AlignRight,
  Highlighter,
  Maximize
} from 'lucide-react'

interface MenuBarProps {
  editor: Editor
  onFullscreen: () => void
}

const MenuBar: React.FC<MenuBarProps> = ({ editor, onFullscreen }) => {
  const setLink = () => {
    const previousUrl = editor.getAttributes('link').href
    const url = window.prompt('URL', previousUrl)

    if (url === null) {
      return
    }

    if (url === '') {
      editor.chain().focus().extendMarkRange('link').unsetLink().run()
      return
    }

    editor.chain().focus().extendMarkRange('link').setLink({ href: url }).run()
  }

  const addImage = () => {
    const url = window.prompt('이미지 URL')
    if (url) {
      editor.chain().focus().setImage({ src: url }).run()
    }
  }

  const addYoutube = () => {
    const url = window.prompt('YouTube URL')
    if (url) {
      editor.commands.setYoutubeVideo({ src: url })
    }
  }

  const addTable = () => {
    editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
  }

  return (
    <div className="border-b border-gray-300 dark:border-gray-600 p-2 flex flex-wrap gap-1 bg-gray-50 dark:bg-gray-800 overflow-x-auto">
      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBold().run()}
        disabled={!editor.can().chain().focus().toggleBold().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('bold') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="굵게 (Ctrl+B)"
      >
        <Bold size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleItalic().run()}
        disabled={!editor.can().chain().focus().toggleItalic().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('italic') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="기울임 (Ctrl+I)"
      >
        <Italic size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleUnderline().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('underline') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="밑줄 (Ctrl+U)"
      >
        <Underline size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleStrike().run()}
        disabled={!editor.can().chain().focus().toggleStrike().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('strike') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="취소선"
      >
        <Strikethrough size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleCode().run()}
        disabled={!editor.can().chain().focus().toggleCode().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('code') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="인라인 코드"
      >
        <Code size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHighlight().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('highlight') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="형광펜"
      >
        <Highlighter size={18} />
      </button>

      <div className="w-px h-6 bg-gray-300 dark:bg-gray-600 mx-1 self-center" />

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('heading', { level: 1 }) ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="제목 1"
      >
        <Heading1 size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('heading', { level: 2 }) ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="제목 2"
      >
        <Heading2 size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('heading', { level: 3 }) ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="제목 3"
      >
        <Heading3 size={18} />
      </button>

      <div className="w-px h-6 bg-gray-300 dark:bg-gray-600 mx-1 self-center" />

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBulletList().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('bulletList') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="목록"
      >
        <List size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleOrderedList().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('orderedList') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="번호 목록"
      >
        <ListOrdered size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().toggleBlockquote().run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('blockquote') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="인용"
      >
        <Quote size={18} />
      </button>

      <div className="w-px h-6 bg-gray-300 dark:bg-gray-600 mx-1 self-center" />

      <button
        type="button"
        onClick={() => editor.chain().focus().setTextAlign('left').run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive({ textAlign: 'left' }) ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="왼쪽 정렬"
      >
        <AlignLeft size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().setTextAlign('center').run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive({ textAlign: 'center' }) ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="가운데 정렬"
      >
        <AlignCenter size={18} />
      </button>

      <button
        type="button"
        onClick={() => editor.chain().focus().setTextAlign('right').run()}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive({ textAlign: 'right' }) ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="오른쪽 정렬"
      >
        <AlignRight size={18} />
      </button>

      <div className="w-px h-6 bg-gray-300 dark:bg-gray-600 mx-1 self-center" />

      <button
        type="button"
        onClick={setLink}
        className={`p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 ${
          editor.isActive('link') ? 'bg-gray-300 dark:bg-gray-600' : ''
        }`}
        title="링크"
      >
        <Link size={18} />
      </button>

      <button
        type="button"
        onClick={addImage}
        className="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700"
        title="이미지"
      >
        <Image size={18} />
      </button>

      <button
        type="button"
        onClick={addYoutube}
        className="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700"
        title="YouTube"
      >
        <Youtube size={18} />
      </button>

      <button
        type="button"
        onClick={addTable}
        className="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700"
        title="표"
      >
        <Table size={18} />
      </button>

      <div className="w-px h-6 bg-gray-300 dark:bg-gray-600 mx-1 self-center" />

      <button
        type="button"
        onClick={onFullscreen}
        className="p-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700"
        title="전체 화면"
      >
        <Maximize size={18} />
      </button>
    </div>
  )
}

export default MenuBar
