Yandex MCP Tracker
==================

This package contains only the Yandex Tracker MCP server and its private
Java runtime. Java and Docker do not need to be installed.
This is a stdio server without a graphical window. The MCP client starts it.

First setup
-----------

Linux:
  app/bin/yandex-mcp-tracker setup

macOS:
  yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker setup

Windows PowerShell:
  .\app\yandex-mcp-tracker.exe setup

MCP executable
--------------

Linux:  app/bin/yandex-mcp-tracker
macOS:  yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker
Windows: app\yandex-mcp-tracker.exe

Tracker and Wiki packages share the saved OAuth settings and tokens when
both are installed for the same user.

Documentation:
  https://github.com/developerdevpav/yandex-mcp-workspace/tree/master/docs
