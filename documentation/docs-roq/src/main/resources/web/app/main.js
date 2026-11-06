import hljs from 'highlight.js';
import CopyButtonPlugin from 'highlightjs-copy';
import 'highlightjs-copy/dist/highlightjs-copy.min.css';
import 'highlight.js/styles/github.css';

hljs.addPlugin(new CopyButtonPlugin());
hljs.highlightAll();
