Yandex MCP Wiki
===============

This package contains only the Yandex Wiki MCP server and its private Java
runtime. Java and Docker do not need to be installed.
This is a stdio server without a graphical window. The MCP client starts it.

First setup
-----------

Linux:
  app/bin/yandex-mcp-wiki setup

macOS:
  yandex-mcp-wiki.app/Contents/MacOS/yandex-mcp-wiki setup

Windows PowerShell:
  .\app\yandex-mcp-wiki.exe setup

MCP executable
--------------

Linux:  app/bin/yandex-mcp-wiki
macOS:  yandex-mcp-wiki.app/Contents/MacOS/yandex-mcp-wiki
Windows: app\yandex-mcp-wiki.exe

Tracker and Wiki packages share the saved OAuth settings and tokens when
both are installed for the same user.

Documentation:
  https://github.com/developerdevpav/yandex-mcp-workspace/tree/master/docs
