'use client'

import React, { useEffect } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'
import 'github-markdown-css/github-markdown.css'
import 'highlight.js/styles/github.css'
import 'highlight.js/styles/github-dark.css'

interface MarkdownViewerProps {
  content: string
  className?: string
}

export default function MarkdownViewer({ content, className = '' }: MarkdownViewerProps) {
  useEffect(() => {
    const isDark = document.documentElement.classList.contains('dark')
    const lightTheme = document.querySelector('link[href*="github.css"]') as HTMLLinkElement
    const darkTheme = document.querySelector('link[href*="github-dark.css"]') as HTMLLinkElement

    if (lightTheme && darkTheme) {
      lightTheme.disabled = isDark
      darkTheme.disabled = !isDark
    }
  }, [])

  return (
    <div
      className={`markdown-body ${className}`}
      style={{
        padding: '0',
        backgroundColor: 'transparent',
        color: 'inherit',
      }}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeHighlight]}
        components={{
          a: ({ ...props }) => (
            <a {...props} target="_blank" rel="noopener noreferrer" />
          ),
          img: ({ ...props }) => (
            // eslint-disable-next-line @next/next/no-img-element
            <img {...props} alt={props.alt || ""} style={{ maxWidth: '100%' }} />
          ),
          pre: ({ children, ...props }) => (
            <pre
              {...props}
              className="hljs"
              style={{
                padding: '16px',
                borderRadius: '6px',
                overflowX: 'auto',
              }}
            >
              {children}
            </pre>
          ),
          code: ({ className, children, ...props }) => {
            const inline = !className
            if (inline) {
              return (
                <code
                  {...props}
                  style={{
                    padding: '2px 4px',
                    borderRadius: '3px',
                    backgroundColor: 'rgba(175, 184, 193, 0.2)',
                    fontSize: '85%',
                  }}
                >
                  {children}
                </code>
              )
            }
            return <code className={className} {...props}>{children}</code>
          },
          input: ({ ...props }) => {
            if (props.type === 'checkbox') {
              return (
                <input
                  {...props}
                  disabled={false}
                  style={{
                    marginRight: '8px',
                  }}
                />
              )
            }
            return <input {...props} />
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  )
}
