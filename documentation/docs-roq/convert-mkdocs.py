#!/usr/bin/env python3
"""Convert a MkDocs markdown file to Roq format."""

import re
import sys
import os


def strip_frontmatter(lines):
    """Remove YAML frontmatter and return (tags_list, body_lines)."""
    tags = []
    if not lines or lines[0].strip() != '---':
        return tags, lines

    end = -1
    for i in range(1, len(lines)):
        if lines[i].strip() == '---':
            end = i
            break
    if end == -1:
        return tags, lines

    fm_lines = lines[1:end]
    in_tags = False
    for line in fm_lines:
        if line.strip().startswith('tags:'):
            in_tags = True
            continue
        if in_tags:
            m = re.match(r'^\s*-\s+(.+)', line)
            if m:
                tags.append(m.group(1).strip())
            else:
                in_tags = False
    return tags, lines[end + 1:]


def extract_title(lines):
    """Extract title from first H1 heading."""
    for line in lines:
        if line.startswith('# '):
            return line[2:].strip()
    return None


def convert_macros(text):
    """Convert MkDocs macros to Qute template expressions."""
    # {{ insert('file', 'tag') }} or {{ insert("file", "tag") }}
    text = re.sub(
        r"\{\{\s*insert\(['\"]([^'\"]+)['\"],\s*['\"]([^'\"]+)['\"]\)\s*\}\}",
        r'{=snippet:insert("\1", "\2")}',
        text
    )
    # {{ insert('file') }} or {{ insert("file") }}
    text = re.sub(
        r"\{\{\s*insert\(['\"]([^'\"]+)['\"]\)\s*\}\}",
        r'{=snippet:insert("\1")}',
        text
    )
    # {{ attributes.xxx }}
    text = re.sub(
        r"\{\{\s*attributes\.([a-zA-Z0-9_.]+)\s*\}\}",
        r'{=cdi:attributes.\1}',
        text
    )
    # Fix snake_case attribute names to match Java record accessors
    text = text.replace('cdi:attributes.versions.vertx_bindings',
                        'cdi:attributes.versions.vertxBindings')
    return text


def convert_tabs(lines):
    """Convert MkDocs tabbed content === 'Tab' to #### Tab with dedented body."""
    result = []
    in_tab = False
    for line in lines:
        m = re.match(r'^=== "(.+)"', line)
        if m:
            in_tab = True
            result.append(f'#### {m.group(1)}\n')
            continue
        if in_tab:
            if line.startswith('    '):
                result.append(line[4:])
                continue
            elif line.strip() == '':
                result.append(line)
                continue
            else:
                in_tab = False
        result.append(line)
    return result


def convert_admonitions(lines):
    """Convert MkDocs admonitions to GFM alerts or <details>."""
    type_map = {
        'INFO': 'NOTE', 'SUCCESS': 'TIP', 'EXAMPLE': 'NOTE', 'QUOTE': 'NOTE',
        'DANGER': 'CAUTION', 'BUG': 'WARNING', 'ABSTRACT': 'NOTE',
        'QUESTION': 'NOTE', 'FAILURE': 'WARNING'
    }
    valid_types = {'NOTE', 'TIP', 'IMPORTANT', 'WARNING', 'CAUTION'}

    result = []
    i = 0
    while i < len(lines):
        line = lines[i]
        m = re.match(r'^(!!!|\?\?\?) +(\w+)( +"([^"]+)")?\s*$', line)
        if m:
            marker = m.group(1)
            adm_type = m.group(2).upper()
            title = m.group(4) or ''

            mapped = type_map.get(adm_type, adm_type)
            if mapped not in valid_types:
                mapped = 'NOTE'

            i += 1
            # Collect indented content
            content_lines = []
            while i < len(lines):
                if lines[i].startswith('    '):
                    content_lines.append(lines[i][4:])
                elif lines[i].strip() == '':
                    # Blank line: include only if next line is still indented
                    if i + 1 < len(lines) and lines[i + 1].startswith('    '):
                        content_lines.append('\n')
                    else:
                        break
                else:
                    break
                i += 1

            if marker == '???':
                result.append('<details>\n')
                summary = title if title else adm_type.title()
                result.append(f'<summary>{summary}</summary>\n')
                result.append('\n')
                result.extend(content_lines)
                result.append('\n')
                result.append('</details>\n')
            else:
                result.append(f'> [!{mapped}]\n')
                if title and title.lower() != adm_type.lower():
                    result.append(f'> **{title}**\n')
                for cl in content_lines:
                    if cl.strip() == '':
                        result.append('>\n')
                    else:
                        result.append(f'> {cl}')
            continue

        result.append(line)
        i += 1
    return result


def convert_file(src_path, dst_path):
    with open(src_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    tags, body_lines = strip_frontmatter(lines)
    title = extract_title(body_lines)
    if not title:
        title = os.path.splitext(os.path.basename(dst_path))[0]

    # Escape double quotes in title for YAML
    title = title.replace('"', '\\"')

    # Build body text
    body = ''.join(body_lines)

    # Convert macros
    body = convert_macros(body)

    # Convert tabs
    body_lines = convert_tabs(body.splitlines(keepends=True))

    # Convert admonitions
    body_lines = convert_admonitions(body_lines)

    # Write output
    with open(dst_path, 'w', encoding='utf-8') as f:
        f.write('---\n')
        f.write(f'title: "{title}"\n')
        f.write('layout: page\n')
        if tags:
            f.write('tags:\n')
            for t in tags:
                f.write(f'- {t}\n')
        f.write('---\n')
        f.writelines(body_lines)


if __name__ == '__main__':
    convert_file(sys.argv[1], sys.argv[2])
