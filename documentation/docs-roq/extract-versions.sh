#!/bin/bash
# Extract real documentation from git for each version and convert to Roq format
set -e

REPO_ROOT="/home/ehugonne/tmp/smallrye-mutiny"
ROQ_DIR="$REPO_ROOT/documentation/docs-roq"
CONTENT_DIR="$ROQ_DIR/content"
DATA_DIR="$ROQ_DIR/data/versions"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

MKDOCS_VERSIONS=(
  2.0.0 2.1.0 2.2.0 2.3.0 2.3.1 2.4.0
  2.5.0 2.5.1 2.5.2 2.5.3 2.5.4 2.5.5 2.5.6
  2.6.0 2.6.1 2.6.2 2.7.0 2.8.0
  2.9.0 2.9.1 2.9.2 2.9.3 2.9.4 2.9.5
  3.0.0 3.0.1 3.0.2 3.0.3
  3.1.0 3.1.1
  3.2.0 3.2.1
)

cd "$REPO_ROOT"

echo "=== Extracting versioned docs ==="

declare -A SORT_ORDERS
order=2
for v in 3.2.1 3.2.0 3.1.1 3.1.0 3.0.3 3.0.2 3.0.1 3.0.0 2.9.5 2.9.4 2.9.3 2.9.2 2.9.1 2.9.0 2.8.0 2.7.0 2.6.2 2.6.1 2.6.0 2.5.6 2.5.5 2.5.4 2.5.3 2.5.2 2.5.1 2.5.0 2.4.0 2.3.1 2.3.0 2.2.0 2.1.0 2.0.0; do
  SORT_ORDERS[$v]=$order
  ((order++))
done

for VERSION in "${MKDOCS_VERSIONS[@]}"; do
  echo "--- Processing $VERSION ---"

  VERSION_DIR="$CONTENT_DIR/$VERSION"

  if ! git show "$VERSION:documentation/docs/tutorials/" >/dev/null 2>&1; then
    echo "  SKIP: no MkDocs docs at tag $VERSION"
    continue
  fi

  rm -rf "$VERSION_DIR"
  mkdir -p "$VERSION_DIR/tutorials" "$VERSION_DIR/guides" "$VERSION_DIR/reference"

  TMPDIR=$(mktemp -d)

  for subdir in tutorials guides reference; do
    git ls-tree --name-only "$VERSION" "documentation/docs/$subdir/" 2>/dev/null | while read filepath; do
      filename=$(basename "$filepath")
      if [[ "$filename" == *.md ]]; then
        git show "$VERSION:$filepath" > "$TMPDIR/$filename"
        python3 "$SCRIPT_DIR/convert-mkdocs.py" "$TMPDIR/$filename" "$VERSION_DIR/$subdir/$filename"
        rm "$TMPDIR/$filename"
      fi
    done
  done

  if git show "$VERSION:documentation/docs/tags-index.md" >/dev/null 2>&1; then
    git show "$VERSION:documentation/docs/tags-index.md" > "$TMPDIR/tags-index.md"
    python3 "$SCRIPT_DIR/convert-mkdocs.py" "$TMPDIR/tags-index.md" "$VERSION_DIR/tags-index.md"
    rm "$TMPDIR/tags-index.md"
  fi

  rmdir "$TMPDIR" 2>/dev/null || true

  so=${SORT_ORDERS[$VERSION]:-50}
  python3 "$SCRIPT_DIR/generate-version-yaml.py" "$VERSION" "$VERSION_DIR" "$so" > "$DATA_DIR/$VERSION.yml"

  echo "  Done: $(find "$VERSION_DIR" -name '*.md' | wc -l) pages"
done

for VERSION in 1.6.0 1.7.0; do
  echo "--- Removing $VERSION (Jekyll era) ---"
  rm -rf "$CONTENT_DIR/$VERSION"
  rm -f "$DATA_DIR/$VERSION.yml"
done

echo ""
echo "=== Done ==="
echo "Versions extracted: ${#MKDOCS_VERSIONS[@]}"
echo "Versions removed (Jekyll): 1.6.0, 1.7.0"
