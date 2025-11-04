/**
 * 리스트 들여쓰기 제한 플러그인
 * 리스트의 최대 들여쓰기 레벨 제한
 */

'use client'

import { useEffect } from 'react'
import type { ElementNode, RangeSelection } from 'lexical'

import { $getListDepth, $isListItemNode, $isListNode } from '@lexical/list'
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext'
import {
  $getSelection,
  $isElementNode,
  $isRangeSelection,
  COMMAND_PRIORITY_HIGH,
  INDENT_CONTENT_COMMAND,
} from 'lexical'

function getElementNodesInSelection(
  selection: RangeSelection,
): Set<ElementNode> {
  const nodesInSelection = selection.getNodes()
  const elements = new Set<ElementNode>()

  for (const node of nodesInSelection) {
    if ($isElementNode(node)) {
      elements.add(node)
    } else {
      const parent = node.getParentOrThrow()
      if ($isElementNode(parent) && !elements.has(parent)) {
        elements.add(parent)
      }
    }
  }

  return elements
}

function isIndentPermitted(maxDepth: number): boolean {
  const selection = $getSelection()

  if (!$isRangeSelection(selection)) {
    return false
  }

  const elementNodesInSelection = getElementNodesInSelection(selection)

  let totalDepth = 0

  for (const elementNode of elementNodesInSelection) {
    if ($isListNode(elementNode)) {
      totalDepth = Math.max($getListDepth(elementNode) + 1, totalDepth)
    } else if ($isListItemNode(elementNode)) {
      const parent = elementNode.getParent()
      if (!$isListNode(parent)) {
        throw new Error(
          'ListMaxIndentLevelPlugin: A ListItemNode must have a ListNode for a parent.',
        )
      }

      totalDepth = Math.max($getListDepth(parent) + 1, totalDepth)
    }
  }

  return totalDepth <= maxDepth
}

export default function ListMaxIndentLevelPlugin({
  maxDepth,
}: {
  maxDepth?: number
}): null {
  const [editor] = useLexicalComposerContext()

  useEffect(() => {
    return editor.registerCommand(
      INDENT_CONTENT_COMMAND,
      () => !isIndentPermitted(maxDepth ?? 7),
      COMMAND_PRIORITY_HIGH,
    )
  }, [editor, maxDepth])

  return null
}
