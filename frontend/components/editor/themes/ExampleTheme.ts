/**
 * Lexical 에디터 테마 설정
 * 에디터 내 각 요소의 CSS 클래스 정의
 */

import type { EditorThemeClasses } from 'lexical'

const ExampleTheme: EditorThemeClasses = {
  // 블록 레벨 요소들
  paragraph: 'mb-1 leading-tight',
  quote: 'my-2 ml-5 pl-4 border-l-4 border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 italic',
  heading: {
    h1: 'text-[2em] font-bold mb-4 pb-2 text-gray-900 dark:text-white border-b border-gray-200 dark:border-gray-700',
    h2: 'text-2xl font-semibold mb-3 pb-2 text-gray-800 dark:text-gray-100 border-b border-gray-200 dark:border-gray-700',
    h3: 'text-xl font-medium mb-2 text-gray-800 dark:text-gray-100',
    h4: 'text-base font-medium mb-2 text-gray-800 dark:text-gray-100',
    h5: 'text-sm font-medium mb-2 text-gray-800 dark:text-gray-100'
  },

  // 리스트
  list: {
    nested: {
      listitem: 'list-none'
    },
    ol: 'list-decimal ml-6 mb-2',
    ul: 'list-disc ml-6 mb-2',
    listitem: 'mb-1',
    listitemChecked: 'line-through opacity-50',
    listitemUnchecked: ''
  },

  // 테이블
  table: 'table-auto border-collapse border border-gray-300 dark:border-gray-600 my-4',
  tableCell: 'border border-gray-300 dark:border-gray-600 px-4 py-2',
  tableCellHeader: 'border border-gray-300 dark:border-gray-600 px-4 py-2 bg-gray-100 dark:bg-gray-700 font-semibold',
  tableRow: '',

  // 인라인 스타일
  text: {
    bold: 'font-bold',
    italic: 'italic',
    overflowed: 'truncate',
    hashtag: 'text-blue-600 dark:text-blue-400',
    underline: 'underline',
    strikethrough: 'line-through',
    underlineStrikethrough: 'underline line-through',
    code: 'px-1 py-0.5 bg-gray-100 dark:bg-gray-700 rounded text-sm font-mono text-red-600 dark:text-red-400'
  },

  // 코드 블록
  code: 'block bg-gray-900 dark:bg-gray-950 text-gray-100 rounded-md p-4 my-4 font-mono text-sm overflow-x-auto',
  codeHighlight: {
    atrule: 'text-purple-400',
    attr: 'text-green-400',
    boolean: 'text-orange-400',
    builtin: 'text-cyan-400',
    cdata: 'text-gray-400',
    char: 'text-green-400',
    class: 'text-yellow-400',
    'class-name': 'text-yellow-400',
    comment: 'text-gray-500 italic',
    constant: 'text-orange-400',
    deleted: 'text-red-400',
    doctype: 'text-gray-400',
    entity: 'text-pink-400',
    function: 'text-blue-400',
    important: 'text-orange-400 font-bold',
    inserted: 'text-green-400',
    keyword: 'text-purple-400',
    namespace: 'text-pink-400',
    number: 'text-orange-400',
    operator: 'text-gray-300',
    prolog: 'text-gray-400',
    property: 'text-cyan-400',
    punctuation: 'text-gray-400',
    regex: 'text-red-400',
    selector: 'text-green-400',
    string: 'text-green-400',
    symbol: 'text-orange-400',
    tag: 'text-pink-400',
    url: 'text-blue-400 underline',
    variable: 'text-orange-400'
  },

  // 링크
  link: 'text-blue-600 dark:text-blue-400 underline hover:text-blue-700 dark:hover:text-blue-300 cursor-pointer',

  // 해시태그
  hashtag: 'text-blue-600 dark:text-blue-400 hover:underline cursor-pointer',

  // 이미지
  image: 'max-w-full h-auto rounded-lg my-4',

  // 인라인 이미지
  inlineImage: 'inline max-h-6 align-text-bottom',

  // 기타
  mark: 'bg-yellow-200 dark:bg-yellow-800',
  markUnselected: 'bg-yellow-100 dark:bg-yellow-900',

  // 레이아웃
  layoutContainer: 'grid gap-4 my-4',
  layoutItem: ''
}

export default ExampleTheme
