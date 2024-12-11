# Documentation

This module contains the documentation and website.

## Requirements

Install the [uv](https://docs.astral.sh/uv/) tool, then get a working virtual environment:

```shell
uv python install
uv venv
source .venv/bin/activate
uv sync --all-extras --dev
```

## MkDocs development mode

From this directory (`documentation/`):

```shell
uv run mkdocs serve
```

## Upgrade the dependencies

```shell
uv sync --upgrade
uv lock
```