# -*- coding: utf-8 -*-
"""列出本地 config.yaml 中已配置的模型（不打印任何密钥）。"""
import importlib.util
import json
import pathlib
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

SCRIPT = pathlib.Path(r"C:\Users\jw\.minimax\.builtin-skills\llm-call\scripts\llm_call.py")

import yaml

spec = importlib.util.spec_from_file_location("llm_call", SCRIPT)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)


def load_config(path=None):
    p = pathlib.Path(path) if path else mod.DEFAULT_CONFIG_PATH
    with open(p, encoding="utf-8") as f:
        return yaml.safe_load(f)


mod.load_config = load_config
caller = mod.LLMCaller()

print("config:", mod.DEFAULT_CONFIG_PATH)
print("defaultModel:", caller.config.get("defaultModel"))
print("\n可用模型：")
for m in caller.list_models():
    print(" -", m.get("ref"), "| id:", m.get("id"), "| protocol:", m.get("protocol"))
print("\nprovider 协议：")
for pid, pconf in caller.providers.items():
    print(f" - {pid}: npm={pconf.get('npm')} baseURL={(pconf.get('options') or {}).get('baseURL','')[:60]} key={'有' if (pconf.get('options') or {}).get('apiKey') else '无'}")
