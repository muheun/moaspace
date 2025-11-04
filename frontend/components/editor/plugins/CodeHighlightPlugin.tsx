/**
 * 코드 하이라이팅 플러그인
 * 코드 블록의 구문 강조 기능 제공
 */

'use client'

import { useEffect } from 'react'
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext'
import { registerCodeHighlighting } from '@lexical/code'

export default function CodeHighlightPlugin() {
  const [editor] = useLexicalComposerContext()

  useEffect(() => {
    return registerCodeHighlighting(editor)
  }, [editor])

  return null
}
