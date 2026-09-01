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

package_component() {
  local component="$1"
  local source_jar="$2"
  local description="$3"
  local app_name="yandex-mcp-$component"
  local bundle_name="${app_name}-${version}-${classifier}"
  local bundle_dir="$work_dir/$bundle_name"
  local input_dir="$work_dir/input-$component"
  local image_dir="$work_dir/image-$component"
  local launcher
  local icon_path="packaging/icons/$host_os/$app_name"

  if [[ "$host_os" == "macos" ]]; then
    icon_path="$icon_path.icns"
  else
    icon_path="$icon_path.png"
  fi
  if [[ ! -f "$icon_path" ]]; then
    echo "Application icon not found: $icon_path" >&2
    exit 2
  fi

  mkdir -p "$input_dir" "$image_dir" "$bundle_dir"
  cp "$source_jar" "$input_dir/$app_name.jar"

  jpackage \
    --type app-image \
    --name "$app_name" \
    --dest "$image_dir" \
    --input "$input_dir" \
    --main-jar "$app_name.jar" \
    --main-class org.springframework.boot.loader.launch.JarLauncher \
    --icon "$icon_path" \
    --vendor Sorface \
    --description "$description"

  if [[ "$host_os" == "macos" ]]; then
    mv "$image_dir/$app_name.app" "$bundle_dir/"
    launcher="$bundle_dir/$app_name.app/Contents/MacOS/$app_name"
  else
    mv "$image_dir/$app_name" "$bundle_dir/app"
    launcher="$bundle_dir/app/bin/$app_name"
  fi

  cp "packaging/release/$component-README.txt" "$bundle_dir/README.txt"
  echo "$version" > "$bundle_dir/VERSION"
  echo "$component" > "$bundle_dir/COMPONENT"

  YANDEX_CONFIG_PATH="$smoke_dir/config.properties" \
  YANDEX_TOKEN_STORE_PATH="$smoke_dir/tokens.json" \
    "$launcher" doctor --logging.level.root=ERROR

  mkdir -p "$output_dir"
  tar -C "$work_dir" -czf "$output_dir/$bundle_name.tar.gz" "$bundle_name"
  echo "Created $output_dir/$bundle_name.tar.gz"
}

package_component "tracker" "$tracker_jar" "Yandex Tracker MCP server"
package_component "wiki" "$wiki_jar" "Yandex Wiki MCP server"
