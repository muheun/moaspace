/**
 * 에디터 아이콘 컴포넌트
 * Lucide React 아이콘 사용
 */

import {
  Bold,
  Italic,
  Underline,
  Strikethrough,
  Code,
  Link,
  Type,
  Heading1,
  Heading2,
  Heading3,
  Quote,
  List,
  ListOrdered,
  FileCode,
  AlignLeft,
  AlignCenter,
  AlignRight,
  AlignJustify,
  Undo,
  Redo,
  ChevronDown,
  Pencil,
} from 'lucide-react'

interface IconProps {
  name: string
  className?: string
  size?: number
}

// 아이콘 이름과 Lucide 컴포넌트 매핑
const iconMap = {
  // 툴바 아이콘
  'bold': Bold,
  'italic': Italic,
  'underline': Underline,
  'strikethrough': Strikethrough,
  'code': Code,
  'link': Link,

  // 블록 타입 아이콘
  'paragraph': Type,
  'h1': Heading1,
  'h2': Heading2,
  'h3': Heading3,
  'quote': Quote,
  'ul': List,
  'ol': ListOrdered,
  'code-block': FileCode,

  // 정렬 아이콘
  'align-left': AlignLeft,
  'align-center': AlignCenter,
  'align-right': AlignRight,
  'align-justify': AlignJustify,

  // 기능 아이콘
  'undo': Undo,
  'redo': Redo,
  'chevron-down': ChevronDown,
  'pencil': Pencil,
}

export default function Icon({ name, className = '', size = 18 }: IconProps) {
  const IconComponent = iconMap[name as keyof typeof iconMap]

  if (!IconComponent) {
    return null
  }

  return (
    <IconComponent
      size={size}
      className={`${className}`}
      strokeWidth={2}
    />
  )
}
