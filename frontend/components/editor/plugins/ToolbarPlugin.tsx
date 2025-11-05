/**
 * 툴바 플러그인
 * 텍스트 포맷팅 및 블록 타입 변경 기능 제공
 */

'use client'

import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  CAN_REDO_COMMAND,
  CAN_UNDO_COMMAND,
  REDO_COMMAND,
  UNDO_COMMAND,
  SELECTION_CHANGE_COMMAND,
  FORMAT_TEXT_COMMAND,
  FORMAT_ELEMENT_COMMAND,
  $getSelection,
  $isRangeSelection,
  $createParagraphNode,
  COMMAND_PRIORITY_LOW,
  RangeSelection,
  LexicalEditor,
  EditorState,
} from 'lexical'
import { $isLinkNode, TOGGLE_LINK_COMMAND } from '@lexical/link'
import {
  $wrapNodes,
  $isAtNodeEnd,
} from '@lexical/selection'
import { $getNearestNodeOfType, mergeRegister } from '@lexical/utils'
import {
  INSERT_ORDERED_LIST_COMMAND,
  INSERT_UNORDERED_LIST_COMMAND,
  REMOVE_LIST_COMMAND,
  $isListNode,
  ListNode,
} from '@lexical/list'
import {
  $createHeadingNode,
  $createQuoteNode,
  $isHeadingNode,
  HeadingTagType,
} from '@lexical/rich-text'
import { $createCodeNode } from '@lexical/code'
import Icon from '../ui/Icon'
import { createPortal } from 'react-dom'

const LowPriority = 1

function getSelectedNode(selection: RangeSelection) {
  const anchor = selection.anchor
  const focus = selection.focus
  const anchorNode = selection.anchor.getNode()
  const focusNode = selection.focus.getNode()
  if (anchorNode === focusNode) {
    return anchorNode
  }
  const isBackward = selection.isBackward()
  if (isBackward) {
    return $isAtNodeEnd(focus) ? anchorNode : focusNode
  } else {
    return $isAtNodeEnd(anchor) ? focusNode : anchorNode
  }
}

function positionEditorElement(editor: HTMLElement, rect: DOMRect | null) {
  if (rect === null) {
    editor.style.opacity = '0'
    editor.style.top = '-1000px'
    editor.style.left = '-1000px'
  } else {
    editor.style.opacity = '1'
    editor.style.top = `${rect.top + rect.height + window.pageYOffset + 10}px`
    editor.style.left = `${rect.left + window.pageXOffset}px`
  }
}

function FloatingLinkEditor({ editor }: { editor: LexicalEditor }) {
  const editorRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const mouseDownRef = useRef(false)
  const [linkUrl, setLinkUrl] = useState('')
  const [isEditMode, setEditMode] = useState(false)
  const [lastSelection, setLastSelection] = useState<RangeSelection | null>(null)

  const updateLinkEditor = useCallback(() => {
    const selection = $getSelection()
    if ($isRangeSelection(selection)) {
      const node = getSelectedNode(selection)
      const parent = node.getParent()
      if ($isLinkNode(parent)) {
        setLinkUrl(parent.getURL())
      } else if ($isLinkNode(node)) {
        setLinkUrl(node.getURL())
      } else {
        setLinkUrl('')
      }
    }
    const editorElem = editorRef.current
    const nativeSelection = window.getSelection()
    const activeElement = document.activeElement

    if (editorElem === null) {
      return
    }

    const rootElement = editor.getRootElement()
    if (
      selection !== null &&
      nativeSelection &&
      !nativeSelection.isCollapsed &&
      rootElement !== null &&
      rootElement.contains(nativeSelection.anchorNode)
    ) {
      const domRange = nativeSelection.getRangeAt(0)
      let rect
      if (nativeSelection.anchorNode === rootElement) {
        let inner = rootElement as HTMLElement
        while (inner.firstElementChild != null) {
          inner = inner.firstElementChild as HTMLElement
        }
        rect = inner.getBoundingClientRect()
      } else {
        rect = domRange.getBoundingClientRect()
      }

      if (!mouseDownRef.current) {
        positionEditorElement(editorElem, rect)
      }
      setLastSelection($isRangeSelection(selection) ? selection : null)
    } else if (!activeElement || activeElement.className !== 'link-input') {
      positionEditorElement(editorElem, null)
      setLastSelection(null)
      setEditMode(false)
      setLinkUrl('')
    }

    return true
  }, [editor])

  useEffect(() => {
    return mergeRegister(
      editor.registerUpdateListener(({ editorState }: { editorState: EditorState }) => {
        editorState.read(() => {
          updateLinkEditor()
        })
      }),

      editor.registerCommand(
        SELECTION_CHANGE_COMMAND,
        () => {
          updateLinkEditor()
          return true
        },
        COMMAND_PRIORITY_LOW
      )
    )
  }, [editor, updateLinkEditor])

  useEffect(() => {
    editor.getEditorState().read(() => {
      updateLinkEditor()
    })
  }, [editor, updateLinkEditor])

  useEffect(() => {
    if (isEditMode && inputRef.current) {
      inputRef.current.focus()
    }
  }, [isEditMode])

  return (
    <div ref={editorRef} className="link-editor">
      {isEditMode ? (
        <input
          ref={inputRef}
          className="link-input"
          value={linkUrl}
          onChange={(event) => {
            setLinkUrl(event.target.value)
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              if (lastSelection !== null) {
                if (linkUrl !== '') {
                  editor.dispatchCommand(TOGGLE_LINK_COMMAND, linkUrl)
                }
                setEditMode(false)
              }
            } else if (event.key === 'Escape') {
              event.preventDefault()
              setEditMode(false)
            }
          }}
        />
      ) : (
        <>
          <div className="link-input">
            <a href={linkUrl} target="_blank" rel="noopener noreferrer">
              {linkUrl}
            </a>
            <div
              className="link-edit"
              role="button"
              tabIndex={0}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => {
                setEditMode(true)
              }}
            />
          </div>
        </>
      )}
    </div>
  )
}


const blockTypeToBlockName = {
  code: '코드',
  h1: '제목 1',
  h2: '제목 2',
  h3: '제목 3',
  h4: '제목 4',
  h5: '제목 5',
  ol: '순서 목록',
  paragraph: '본문',
  quote: '인용',
  ul: '글머리 기호'
}

const blockTypeIconNames = {
  paragraph: 'paragraph',
  h1: 'h1',
  h2: 'h2',
  h3: 'h3',
  h4: 'h4',
  h5: 'h5',
  quote: 'quote',
  ul: 'ul',
  ol: 'ol',
  code: 'code-block'
}

function Divider() {
  return <div className="divider" />
}

function BlockOptionsDropdownList({
  editor,
  blockType,
  toolbarRef,
  setShowBlockOptionsDropDown
}: {
  editor: LexicalEditor
  blockType: keyof typeof blockTypeToBlockName
  toolbarRef: React.MutableRefObject<HTMLDivElement | null>
  setShowBlockOptionsDropDown: (show: boolean) => void
}) {
  const dropDownRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const dropDown = dropDownRef.current
    const button = document.getElementById('block-type-button')

    if (button !== null && dropDown !== null) {
      const { bottom, left } = button.getBoundingClientRect()
      dropDown.style.top = `${bottom + 4}px`
      dropDown.style.left = `${left}px`
    }
  }, [dropDownRef])

  useEffect(() => {
    const dropDown = dropDownRef.current
    const toolbar = toolbarRef.current

    if (dropDown !== null && toolbar !== null) {
      const handle = (event: MouseEvent) => {
        const target = event.target as Node
        if (!dropDown.contains(target) && !toolbar.contains(target)) {
          setShowBlockOptionsDropDown(false)
        }
      }
      document.addEventListener('click', handle)

      return () => {
        document.removeEventListener('click', handle)
      }
    }
  }, [dropDownRef, toolbarRef, setShowBlockOptionsDropDown])

  const formatParagraph = () => {
    editor.update(() => {
      const selection = $getSelection()
      if ($isRangeSelection(selection)) {
        $wrapNodes(selection, () => $createParagraphNode())
      }
    })
    setShowBlockOptionsDropDown(false)
  }

  const formatHeading = (headingSize: HeadingTagType) => {
    if (blockType !== headingSize) {
      editor.update(() => {
        const selection = $getSelection()
        if ($isRangeSelection(selection)) {
          $wrapNodes(selection, () => $createHeadingNode(headingSize))
        }
      })
    }
    setShowBlockOptionsDropDown(false)
  }

  const formatBulletList = () => {
    if (blockType !== 'ul') {
      editor.dispatchCommand(INSERT_UNORDERED_LIST_COMMAND, undefined)
    } else {
      editor.dispatchCommand(REMOVE_LIST_COMMAND, undefined)
    }
    setShowBlockOptionsDropDown(false)
  }

  const formatNumberedList = () => {
    if (blockType !== 'ol') {
      editor.dispatchCommand(INSERT_ORDERED_LIST_COMMAND, undefined)
    } else {
      editor.dispatchCommand(REMOVE_LIST_COMMAND, undefined)
    }
    setShowBlockOptionsDropDown(false)
  }

  const formatQuote = () => {
    if (blockType !== 'quote') {
      editor.update(() => {
        const selection = $getSelection()
        if ($isRangeSelection(selection)) {
          $wrapNodes(selection, () => $createQuoteNode())
        }
      })
    }
    setShowBlockOptionsDropDown(false)
  }

  const formatCode = () => {
    if (blockType !== 'code') {
      editor.update(() => {
        const selection = $getSelection()
        if ($isRangeSelection(selection)) {
          $wrapNodes(selection, () => $createCodeNode())
        }
      })
    }
    setShowBlockOptionsDropDown(false)
  }

  return (
    <div
      className="fixed z-10 bg-white dark:bg-gray-800 shadow-lg rounded-lg py-2 min-w-[180px] border border-gray-200 dark:border-gray-700"
      ref={dropDownRef}
      style={{ position: 'fixed' }}
    >
      <button type="button"
        className="w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
        onClick={formatParagraph}
      >
        <Icon name="paragraph" size={16} />
        <span className="text-sm text-gray-700 dark:text-gray-200">본문</span>
      </button>
      <button type="button"
        className="w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
        onClick={() => formatHeading('h1')}
      >
        <Icon name="h1" size={16} />
        <span className="text-sm text-gray-700 dark:text-gray-200">제목 1</span>
      </button>
      <button type="button"
        className="w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
        onClick={() => formatHeading('h2')}
      >
        <Icon name="h2" size={16} />
        <span className="text-sm text-gray-700 dark:text-gray-200">제목 2</span>
      </button>
      <button type="button"
        className="w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
        onClick={formatBulletList}
      >
        <Icon name="ul" size={16} />
        <span className="text-sm text-gray-700 dark:text-gray-200">글머리 기호</span>
      </button>
      <button type="button"
        className="w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
        onClick={formatNumberedList}
      >
        <Icon name="ol" size={16} />
        <span className="text-sm text-gray-700 dark:text-gray-200">순서 목록</span>
      </button>
      <button type="button"
        className="w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
        onClick={formatQuote}
      >
        <Icon name="quote" size={16} />
        <span className="text-sm text-gray-700 dark:text-gray-200">인용</span>
      </button>
      <button type="button"
        className="w-full px-4 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
        onClick={formatCode}
      >
        <Icon name="code-block" size={16} />
        <span className="text-sm text-gray-700 dark:text-gray-200">코드</span>
      </button>
    </div>
  )
}

export default function ToolbarPlugin() {
  const [editor] = useLexicalComposerContext()
  const toolbarRef = useRef<HTMLDivElement | null>(null)
  const [canUndo, setCanUndo] = useState(false)
  const [canRedo, setCanRedo] = useState(false)
  const [blockType, setBlockType] = useState<keyof typeof blockTypeToBlockName>('paragraph')
  const [showBlockOptionsDropDown, setShowBlockOptionsDropDown] = useState(false)
  const [isLink, setIsLink] = useState(false)
  const [isBold, setIsBold] = useState(false)
  const [isItalic, setIsItalic] = useState(false)
  const [isUnderline, setIsUnderline] = useState(false)
  const [isStrikethrough, setIsStrikethrough] = useState(false)
  const [isCode, setIsCode] = useState(false)

  const $updateToolbar = useCallback(() => {
    const selection = $getSelection()
    if ($isRangeSelection(selection)) {
      const anchorNode = selection.anchor.getNode()
      const element =
        anchorNode.getKey() === 'root'
          ? anchorNode
          : anchorNode.getTopLevelElementOrThrow()
      const elementKey = element.getKey()
      const elementDOM = editor.getElementByKey(elementKey)
      if (elementDOM !== null) {
        if ($isListNode(element)) {
          const parentList = $getNearestNodeOfType(anchorNode, ListNode)
          const type = parentList ? parentList.getTag() : element.getTag()
          setBlockType(type)
        } else {
          const type = $isHeadingNode(element)
            ? element.getTag()
            : element.getType()
          setBlockType(type as keyof typeof blockTypeToBlockName)
        }
      }
      // 텍스트 포맷 상태 업데이트
      setIsBold(selection.hasFormat('bold'))
      setIsItalic(selection.hasFormat('italic'))
      setIsUnderline(selection.hasFormat('underline'))
      setIsStrikethrough(selection.hasFormat('strikethrough'))
      setIsCode(selection.hasFormat('code'))

      // 링크 상태 업데이트
      const node = anchorNode.getParent()
      if ($isLinkNode(node)) {
        setIsLink(true)
      } else {
        setIsLink(false)
      }
    }
  }, [editor])

  useEffect(() => {
    return mergeRegister(
      editor.registerUpdateListener(({ editorState }) => {
        editorState.read(() => {
          $updateToolbar()
        })
      }),
      editor.registerCommand(
        SELECTION_CHANGE_COMMAND,
        () => {
          $updateToolbar()
          return false
        },
        LowPriority
      ),
      editor.registerCommand(
        CAN_UNDO_COMMAND,
        (payload) => {
          setCanUndo(payload)
          return false
        },
        LowPriority
      ),
      editor.registerCommand(
        CAN_REDO_COMMAND,
        (payload) => {
          setCanRedo(payload)
          return false
        },
        LowPriority
      )
    )
  }, [editor, $updateToolbar])

  const insertLink = useCallback(() => {
    if (!isLink) {
      editor.dispatchCommand(TOGGLE_LINK_COMMAND, 'https://')
    } else {
      editor.dispatchCommand(TOGGLE_LINK_COMMAND, null)
    }
  }, [editor, isLink])

  const blockIconName = blockTypeIconNames[blockType] || 'paragraph'

  return (
    <div
      className="toolbar"
      ref={toolbarRef}
    >
      {/* 실행 취소/다시 실행 */}
      <button type="button"
        disabled={!canUndo}
        onClick={() => {
          editor.dispatchCommand(UNDO_COMMAND, undefined)
        }}
        className="toolbar-item p-2"
        aria-label="실행 취소"
      >
        <Icon name="undo" />
      </button>
      <button type="button"
        disabled={!canRedo}
        onClick={() => {
          editor.dispatchCommand(REDO_COMMAND, undefined)
        }}
        className="toolbar-item p-2"
        aria-label="다시 실행"
      >
        <Icon name="redo" />
      </button>
      <Divider />

      {/* 블록 타입 선택 */}
      <div className="relative">
        <button type="button"
          id="block-type-button"
          className="toolbar-item px-3 py-2 rounded hover:bg-gray-200 dark:hover:bg-gray-700 flex items-center gap-2 min-w-[120px] justify-between"
          onClick={() => setShowBlockOptionsDropDown(!showBlockOptionsDropDown)}
          aria-label="블록 타입"
        >
          <div className="flex items-center gap-2">
            <Icon name={blockIconName} />
            <span className="text-sm text-gray-700 dark:text-gray-200">{blockTypeToBlockName[blockType]}</span>
          </div>
          <Icon name="chevron-down" size={12} />
        </button>
        {showBlockOptionsDropDown && (
          <BlockOptionsDropdownList
            editor={editor}
            blockType={blockType}
            toolbarRef={toolbarRef}
            setShowBlockOptionsDropDown={setShowBlockOptionsDropDown}
          />
        )}
      </div>
      <Divider />

      {/* 텍스트 포맷팅 */}
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'bold')
        }}
        className={`toolbar-item p-2 ${
          isBold ? 'active' : ''
        }`}
        aria-label="굵게"
      >
        <Icon name="bold" />
      </button>
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'italic')
        }}
        className={`toolbar-item p-2 ${
          isItalic ? 'active' : ''
        }`}
        aria-label="기울임"
      >
        <Icon name="italic" />
      </button>
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'underline')
        }}
        className={`toolbar-item p-2 ${
          isUnderline ? 'active' : ''
        }`}
        aria-label="밑줄"
      >
        <Icon name="underline" />
      </button>
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'strikethrough')
        }}
        className={`toolbar-item p-2 ${
          isStrikethrough ? 'active' : ''
        }`}
        aria-label="취소선"
      >
        <Icon name="strikethrough" />
      </button>
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_TEXT_COMMAND, 'code')
        }}
        className={`toolbar-item p-2 ${
          isCode ? 'active' : ''
        }`}
        aria-label="코드"
      >
        <Icon name="code" />
      </button>
      <button type="button"
        onClick={insertLink}
        className={`toolbar-item p-2 ${
          isLink ? 'active' : ''
        }`}
        aria-label="링크"
      >
        <Icon name="link" />
      </button>
      {isLink && createPortal(<FloatingLinkEditor editor={editor} />, document.body)}
      <Divider />

      {/* 정렬 */}
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_ELEMENT_COMMAND, 'left')
        }}
        className="toolbar-item p-2"
        aria-label="왼쪽 정렬"
      >
        <Icon name="align-left" />
      </button>
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_ELEMENT_COMMAND, 'center')
        }}
        className="toolbar-item p-2"
        aria-label="가운데 정렬"
      >
        <Icon name="align-center" />
      </button>
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_ELEMENT_COMMAND, 'right')
        }}
        className="toolbar-item p-2"
        aria-label="오른쪽 정렬"
      >
        <Icon name="align-right" />
      </button>
      <button type="button"
        onClick={() => {
          editor.dispatchCommand(FORMAT_ELEMENT_COMMAND, 'justify')
        }}
        className="toolbar-item p-2"
        aria-label="양쪽 정렬"
      >
        <Icon name="align-justify" />
      </button>
    </div>
  )
}
