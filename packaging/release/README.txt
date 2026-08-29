Yandex MCP Workspace
====================

This portable package contains Yandex Tracker and Yandex Wiki MCP servers
with a private Java runtime. Java and Docker do not need to be installed.

First setup
-----------

Run the Tracker launcher with the "setup" argument once. Tracker and Wiki
share the saved OAuth settings and tokens.

Linux:
  app/bin/yandex-mcp-tracker setup

macOS:
  app/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker setup

Windows PowerShell:
  .\app\yandex-mcp-tracker\yandex-mcp-tracker.exe setup

MCP launchers
-------------

Tracker:
  Linux:  app/bin/yandex-mcp-tracker
  macOS:  app/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker
  Windows: app\yandex-mcp-tracker\yandex-mcp-tracker.exe

Wiki:
  Linux:  app/bin/yandex-mcp-wiki
  macOS:  app/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-wiki
  Windows: app\yandex-mcp-tracker\yandex-mcp-wiki.exe

Documentation:
  https://github.com/developerdevpav/yandex-mcp-workspace/tree/master/docs
