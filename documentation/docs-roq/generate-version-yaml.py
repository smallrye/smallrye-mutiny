#!/usr/bin/env python3
"""Generate a version YAML file from extracted content."""

import os
import re
import sys


TUTORIAL_ORDER = [
    'getting-mutiny', 'hello-mutiny', 'creating-uni-pipelines',
    'creating-multi-pipelines', 'observing-events', 'transforming-items',
    'transforming-items-asynchronously', 'handling-failures', 'retrying',
    'mutiny-workshop'
]

TUTORIAL_ICONS = {
    'getting-mutiny': 'fa-solid fa-download',
    'hello-mutiny': 'fa-solid fa-hand-wave',
    'creating-uni-pipelines': 'fa-solid fa-code',
    'creating-multi-pipelines': 'fa-solid fa-code',
    'observing-events': 'fa-solid fa-eye',
    'transforming-items': 'fa-solid fa-shuffle',
    'transforming-items-asynchronously': 'fa-solid fa-shuffle',
    'handling-failures': 'fa-solid fa-triangle-exclamation',
    'retrying': 'fa-solid fa-rotate',
    'mutiny-workshop': 'fa-solid fa-flask',
}


def get_title(filepath):
    """Extract title from first H1 in a markdown file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            for line in f:
                if line.startswith('# '):
                    return line[2:].strip()
    except Exception:
        pass
    slug = os.path.splitext(os.path.basename(filepath))[0]
    return slug.replace('-', ' ').title()


def escape_yaml(s):
    """Escape a string for YAML double-quoted value."""
    return s.replace('\\', '\\\\').replace('"', '\\"')


def generate(version, version_dir, sort_order):
    lines = []
    lines.append(f'label: "{version}"')
    lines.append(f'path: "{version}"')
    lines.append(f'sortOrder: {sort_order}')
    lines.append('defaultVersion: false')
    lines.append('devVersion: false')
    lines.append('sections:')

    # Tutorials
    tut_dir = os.path.join(version_dir, 'tutorials')
    if os.path.isdir(tut_dir):
        tut_files = [f for f in os.listdir(tut_dir) if f.endswith('.md')]
        if tut_files:
            lines.append('  - name: "Tutorials"')
            lines.append('    items:')
            # Ordered tutorials first
            seen = set()
            for slug in TUTORIAL_ORDER:
                fname = slug + '.md'
                if fname in tut_files:
                    seen.add(fname)
                    title = escape_yaml(get_title(os.path.join(tut_dir, fname)))
                    icon = TUTORIAL_ICONS.get(slug, 'fa-solid fa-file')
                    lines.append(f'      - title: "{title}"')
                    lines.append(f'        path: "/tutorials/{slug}"')
                    lines.append(f'        icon: "{icon}"')
            # Any remaining tutorials not in the order list
            for fname in sorted(tut_files):
                if fname not in seen:
                    slug = fname[:-3]
                    title = escape_yaml(get_title(os.path.join(tut_dir, fname)))
                    lines.append(f'      - title: "{title}"')
                    lines.append(f'        path: "/tutorials/{slug}"')
                    lines.append(f'        icon: "fa-solid fa-file"')

    # Guides
    guides_dir = os.path.join(version_dir, 'guides')
    if os.path.isdir(guides_dir):
        guide_files = sorted(f for f in os.listdir(guides_dir) if f.endswith('.md'))
        if guide_files:
            lines.append('  - name: "Guides"')
            lines.append('    items:')
            for fname in guide_files:
                slug = fname[:-3]
                title = escape_yaml(get_title(os.path.join(guides_dir, fname)))
                lines.append(f'      - title: "{title}"')
                lines.append(f'        path: "/guides/{slug}"')
                lines.append(f'        icon: "fa-solid fa-book"')

    # Reference
    ref_dir = os.path.join(version_dir, 'reference')
    if os.path.isdir(ref_dir):
        ref_files = sorted(f for f in os.listdir(ref_dir) if f.endswith('.md'))
        if ref_files:
            lines.append('  - name: "Reference"')
            lines.append('    items:')
            for fname in ref_files:
                slug = fname[:-3]
                title = escape_yaml(get_title(os.path.join(ref_dir, fname)))
                lines.append(f'      - title: "{title}"')
                lines.append(f'        path: "/reference/{slug}"')
                lines.append(f'        icon: "fa-solid fa-file"')
            lines.append('      - title: "API (Javadoc)"')
            lines.append('        path: "https://javadoc.io/doc/io.smallrye.reactive/mutiny/latest/index.html"')
            lines.append('        icon: "fa-solid fa-file-code"')
            lines.append('        target: "_blank"')

    return '\n'.join(lines) + '\n'


if __name__ == '__main__':
    version = sys.argv[1]
    version_dir = sys.argv[2]
    sort_order = int(sys.argv[3])
    print(generate(version, version_dir, sort_order), end='')
