#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 5 ]]; then
  echo "Usage: $0 <version> <classifier> <tracker-jar> <wiki-jar> <output-dir>" >&2
  exit 2
fi

version="$1"
classifier="$2"
tracker_jar="$3"
wiki_jar="$4"
output_dir="$5"

if [[ ! "$version" =~ ^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$ ]]; then
  echo "Invalid release version: $version" >&2
  exit 2
fi
if [[ ! "$classifier" =~ ^(linux|macos)-(x64|arm64)$ ]]; then
  echo "Invalid Unix classifier: $classifier" >&2
  exit 2
fi
if [[ ! -f "$tracker_jar" || ! -f "$wiki_jar" ]]; then
  echo "Tracker and Wiki JAR files must exist" >&2
  exit 2
fi
if ! command -v jpackage >/dev/null 2>&1; then
  echo "jpackage is required (JDK 21)" >&2
  exit 2
fi

case "$(uname -s)" in
  Darwin) host_os="macos" ;;
  Linux) host_os="linux" ;;
  *) echo "Unsupported Unix host: $(uname -s)" >&2; exit 2 ;;
esac
case "$(uname -m)" in
  x86_64|amd64) host_arch="x64" ;;
  arm64|aarch64) host_arch="arm64" ;;
  *) echo "Unsupported architecture: $(uname -m)" >&2; exit 2 ;;
esac
if [[ "$classifier" != "${host_os}-${host_arch}" ]]; then
  echo "Classifier $classifier does not match host ${host_os}-${host_arch}" >&2
  exit 2
fi

work_dir="$(mktemp -d)"
smoke_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir" "$smoke_dir"' EXIT

input_dir="$work_dir/input"
image_dir="$work_dir/image"
bundle_name="yandex-mcp-workspace-${version}-${classifier}"
bundle_dir="$work_dir/$bundle_name"

mkdir -p "$input_dir" "$image_dir" "$bundle_dir"
cp "$tracker_jar" "$input_dir/yandex-mcp-tracker.jar"
cp "$wiki_jar" "$input_dir/yandex-mcp-wiki.jar"

jpackage \
  --type app-image \
  --name yandex-mcp-tracker \
  --dest "$image_dir" \
  --input "$input_dir" \
  --main-jar yandex-mcp-tracker.jar \
  --main-class org.springframework.boot.loader.launch.JarLauncher \
  --add-launcher yandex-mcp-wiki=packaging/jpackage/wiki.properties \
  --vendor Sorface \
  --description "Yandex Tracker and Wiki MCP servers"

if [[ "$(uname -s)" == "Darwin" ]]; then
  mkdir -p "$bundle_dir/app"
  mv "$image_dir/yandex-mcp-tracker.app" "$bundle_dir/app/"
  tracker_launcher="$bundle_dir/app/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker"
  wiki_launcher="$bundle_dir/app/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-wiki"
else
  mv "$image_dir/yandex-mcp-tracker" "$bundle_dir/app"
  tracker_launcher="$bundle_dir/app/bin/yandex-mcp-tracker"
  wiki_launcher="$bundle_dir/app/bin/yandex-mcp-wiki"
fi

cp packaging/release/README.txt "$bundle_dir/README.txt"
echo "$version" > "$bundle_dir/VERSION"

YANDEX_CONFIG_PATH="$smoke_dir/config.properties" \
YANDEX_TOKEN_STORE_PATH="$smoke_dir/tokens.json" \
  "$tracker_launcher" doctor --logging.level.root=ERROR
YANDEX_CONFIG_PATH="$smoke_dir/config.properties" \
YANDEX_TOKEN_STORE_PATH="$smoke_dir/tokens.json" \
  "$wiki_launcher" doctor --logging.level.root=ERROR

mkdir -p "$output_dir"
tar -C "$work_dir" -czf "$output_dir/$bundle_name.tar.gz" "$bundle_name"
echo "Created $output_dir/$bundle_name.tar.gz"
