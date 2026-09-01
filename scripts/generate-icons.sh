#!/usr/bin/env bash

set -euo pipefail

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "Icon generation requires macOS tools: sips and iconutil" >&2
  exit 2
fi
if ! command -v sips >/dev/null 2>&1 || ! command -v iconutil >/dev/null 2>&1; then
  echo "sips and iconutil are required" >&2
  exit 2
fi

project_dir="$(cd "$(dirname "$0")/.." && pwd)"
source_dir="$project_dir/packaging/icons/src"
macos_dir="$project_dir/packaging/icons/macos"
windows_dir="$project_dir/packaging/icons/windows"
linux_dir="$project_dir/packaging/icons/linux"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

mkdir -p "$macos_dir" "$windows_dir" "$linux_dir"

generate_icon() {
  local app_name="$1"
  local source_svg="$source_dir/$app_name.svg"
  local master_png="$work_dir/$app_name-1024.png"
  local iconset="$work_dir/$app_name.iconset"

  sips -s format png "$source_svg" --out "$master_png" >/dev/null
  mkdir -p "$iconset"

  while read -r filename size; do
    sips -z "$size" "$size" "$master_png" --out "$iconset/$filename" >/dev/null
  done <<EOF
icon_16x16.png 16
icon_16x16@2x.png 32
icon_32x32.png 32
icon_32x32@2x.png 64
icon_128x128.png 128
icon_128x128@2x.png 256
icon_256x256.png 256
icon_256x256@2x.png 512
icon_512x512.png 512
icon_512x512@2x.png 1024
EOF

  iconutil -c icns "$iconset" -o "$macos_dir/$app_name.icns"
  cp "$iconset/icon_512x512.png" "$linux_dir/$app_name.png"
  sips -s format ico "$iconset/icon_256x256.png" --out "$windows_dir/$app_name.ico" >/dev/null
}

generate_icon "yandex-mcp-tracker"
generate_icon "yandex-mcp-wiki"

echo "Generated application icons in packaging/icons"
