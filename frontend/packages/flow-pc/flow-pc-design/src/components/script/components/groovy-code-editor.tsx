import React, { useEffect, useRef } from 'react';
import { EditorState } from '@codemirror/state';
import { EditorView, keymap, placeholder as cmPlaceholder, lineNumbers, highlightActiveLineGutter, highlightSpecialChars, drawSelection, dropCursor, rectangularSelection, crosshairCursor, highlightActiveLine } from '@codemirror/view';
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
import { indentOnInput, bracketMatching, foldGutter, foldKeymap } from '@codemirror/language';
import { java } from '@codemirror/lang-java';
import { oneDark } from '@codemirror/theme-one-dark';
import { syntaxHighlighting, defaultHighlightStyle, HighlightStyle } from '@codemirror/language';
import { tags } from '@lezer/highlight';

interface GroovyCodeEditorProps {
  value: string;
  readonly?: boolean;
  onChange?: (value: string) => void;
  placeholder?: string;
  theme?: 'dark' | 'light';
  options?: {
    fontSize?: number;
    minHeight?: number;
    maxHeight?: number;
  };
}

const darkHighlightStyle = HighlightStyle.define([
  { tag: tags.keyword, color: '#c678dd' },
  { tag: tags.operator, color: '#56b6c2' },
  { tag: tags.variableName, color: '#e5c07b' },
  { tag: tags.string, color: '#98c379' },
  { tag: tags.comment, color: '#5c6370', fontStyle: 'italic' },
  { tag: tags.number, color: '#d19a66' },
  { tag: tags.bool, color: '#d19a66' },
  { tag: tags.null, color: '#d19a66' },
  { tag: tags.propertyName, color: '#e06c75' },
  { tag: tags.function(tags.variableName), color: '#61afef' },
  { tag: tags.definition(tags.variableName), color: '#e5c07b' },
  { tag: tags.typeName, color: '#e5c07b' },
  { tag: tags.className, color: '#e5c07b' },
  { tag: tags.annotation, color: '#d19a66' },
]);

export const GroovyCodeEditor: React.FC<GroovyCodeEditorProps> = (props) => {
  const {
    value,
    readonly = false,
    onChange,
    placeholder = '请输入 Groovy 脚本...',
    theme = 'dark',
    options = {}
  } = props;

  const editorRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);

  const { fontSize = 14, minHeight = 300, maxHeight = 300 } = options;

  useEffect(() => {
    if (!editorRef.current) return;

    const extensions = [
      lineNumbers(),
      highlightActiveLineGutter(),
      highlightSpecialChars(),
      history(),
      foldGutter(),
      drawSelection(),
      dropCursor(),
      EditorState.allowMultipleSelections.of(true),
      indentOnInput(),
      bracketMatching(),
      rectangularSelection(),
      crosshairCursor(),
      highlightActiveLine(),
      keymap.of([
        ...defaultKeymap,
        ...historyKeymap,
        ...foldKeymap,
        indentWithTab,
      ]),
      java(),
      cmPlaceholder(placeholder),
      EditorView.updateListener.of((update) => {
        if (update.docChanged && onChange) {
          onChange(update.state.doc.toString());
        }
      }),
      EditorView.theme({
        '&': {
          fontSize: `${fontSize}px`,
          minHeight: `${minHeight}px`,
          maxHeight: `${maxHeight}px`,
        },
        '.cm-scroller': {
          overflow: 'auto',
        },
        '.cm-content': {
          fontFamily: 'monospace',
        },
      }),
    ];

    if (theme === 'dark') {
      extensions.push(oneDark);
      extensions.push(syntaxHighlighting(darkHighlightStyle));
    } else {
      extensions.push(syntaxHighlighting(defaultHighlightStyle));
    }

    if (readonly) {
      extensions.push(EditorState.readOnly.of(true));
    }

    const state = EditorState.create({
      doc: value,
      extensions,
    });

    const view = new EditorView({
      state,
      parent: editorRef.current,
    });

    viewRef.current = view;

    return () => {
      view.destroy();
    };
  }, [theme, fontSize, minHeight, maxHeight, placeholder, readonly]);

  useEffect(() => {
    if (viewRef.current && value !== viewRef.current.state.doc.toString()) {
      viewRef.current.dispatch({
        changes: {
          from: 0,
          to: viewRef.current.state.doc.length,
          insert: value,
        },
      });
    }
  }, [value]);

  return <div ref={editorRef} style={{ border: '1px solid #434343', borderRadius: 6, overflow: 'hidden' }} />;
};
