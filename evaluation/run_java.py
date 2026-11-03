"""Compile the actual dependency-free product engine and a small Java CLI."""
from __future__ import annotations

import os
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "evaluation" / "build"
ENGINE = ROOT / "backend/src/main/java/com/campus/recommend/recommendation"


def java_tool(name: str) -> str:
    home = os.environ.get("JAVA_HOME")
    if home:
        tool = Path(home) / "bin" / (name + (".exe" if os.name == "nt" else ""))
        if tool.is_file():
            return str(tool)
    tool = shutil.which(name)
    if not tool:
        raise RuntimeError("Java 17+ JDK is required; set JAVA_HOME or add javac/java to PATH")
    return tool


def compile_cli(*sources: Path) -> Path:
    BUILD.mkdir(parents=True, exist_ok=True)
    files = sorted(ENGINE.glob("*.java")) + list(sources)
    subprocess.run([java_tool("javac"), "--release", "17", "-encoding", "UTF-8", "-d", str(BUILD),
                    *map(str, files)], cwd=ROOT, check=True)
    return BUILD


def run_cli(main: str, *args: object) -> str:
    result = subprocess.run([java_tool("java"), "-cp", str(BUILD), main, *map(str, args)],
                            cwd=ROOT, check=True, text=True, encoding="utf-8", capture_output=True)
    return result.stdout
