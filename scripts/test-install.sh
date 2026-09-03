#!/usr/bin/env bash

set -euo pipefail

readonly PROJECT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
test_directory="$(mktemp -d)"
trap 'rm -rf -- "${test_directory}"' EXIT

fail() {
  printf 'Installer test failed: %s\n' "$1" >&2
  exit 1
}

classifier() {
  local operating_system architecture
  case "$(uname -s)" in
    Darwin) operating_system="macos" ;;
    Linux) operating_system="linux" ;;
    *) fail "unsupported test host" ;;
  esac
  case "$(uname -m)" in
    x86_64|amd64) architecture="x64" ;;
    arm64|aarch64) architecture="arm64" ;;
    *) fail "unsupported test architecture" ;;
  esac
  printf '%s-%s' "${operating_system}" "${architecture}"
}

make_release() {
  local version="$1"
  local behavior="$2"
  local release_directory="${test_directory}/release-${version}"
  local bundle="yandex-mcp-tracker-${version}-$(classifier)"
  local bundle_directory="${test_directory}/${bundle}"
  local launcher
  mkdir -p "${release_directory}" "${bundle_directory}"
  if [[ "$(classifier)" == macos-* ]]; then
    launcher="${bundle_directory}/yandex-mcp-tracker.app/Contents/MacOS/yandex-mcp-tracker"
  else
    launcher="${bundle_directory}/app/bin/yandex-mcp-tracker"
  fi
  mkdir -p "$(dirname "${launcher}")"
  sed \
    -e "s|@STATE@|${test_directory}/state-${version}|g" \
    -e "s|@BEHAVIOR@|${behavior}|g" \
    "${PROJECT_DIRECTORY}/scripts/testdata/fake-launcher.sh" >"${launcher}"
  chmod +x "${launcher}"
  tar -C "${test_directory}" -czf "${release_directory}/${bundle}.tar.gz" "${bundle}"
  if command -v sha256sum >/dev/null 2>&1; then
    (cd "${release_directory}" && sha256sum "${bundle}.tar.gz" >SHA256SUMS)
  else
    local checksum
    checksum="$(shasum -a 256 "${release_directory}/${bundle}.tar.gz" | awk '{print $1}')"
    printf '%s  %s\n' "${checksum}" "${bundle}.tar.gz" >"${release_directory}/SHA256SUMS"
  fi
  rm -rf -- "${bundle_directory}"
  printf '%s' "${release_directory}"
}

run_installer() {
  local version="$1"
  local release_directory="$2"
  : >"${test_directory}/input"
  : >"${test_directory}/output"
  HOME="${test_directory}/home" \
  YANDEX_MCP_INSTALL_ROOT="${test_directory}/home/.local/share/yandex-mcp" \
  YANDEX_MCP_BIN_DIR="${test_directory}/home/.local/bin" \
  YANDEX_MCP_INPUT_PATH="${test_directory}/input" \
  YANDEX_MCP_OUTPUT_PATH="${test_directory}/output" \
  YANDEX_MCP_COMPONENTS=tracker \
  YANDEX_MCP_VERSION="${version}" \
  YANDEX_MCP_RELEASE_BASE_URL="file://${release_directory}" \
    bash "${PROJECT_DIRECTORY}/scripts/install.sh"
}

mkdir -p "${test_directory}/home"
release_one="$(make_release "9.8.7" success)"
run_installer "9.8.7" "${release_one}"
[[ -L "${test_directory}/home/.local/bin/yandex-mcp-tracker" ]] || fail "stable launcher was not created"
[[ -f "${test_directory}/state-9.8.7/setup" ]] || fail "OAuth setup was not called"
[[ -f "${test_directory}/state-9.8.7/connect" ]] || fail "client connection was not called"

old_target="$(readlink "${test_directory}/home/.local/bin/yandex-mcp-tracker")"
release_two="$(make_release "9.8.8" broken-doctor)"
if run_installer "9.8.8" "${release_two}"; then
  fail "broken doctor unexpectedly succeeded"
fi
[[ "$(readlink "${test_directory}/home/.local/bin/yandex-mcp-tracker")" == "${old_target}" ]] \
  || fail "failed update changed the stable launcher"

cp "${release_one}/SHA256SUMS" "${release_one}/SHA256SUMS.original"
printf '%064d  %s\n' 0 "yandex-mcp-tracker-9.8.7-$(classifier).tar.gz" >"${release_one}/SHA256SUMS"
if run_installer "9.8.7" "${release_one}"; then
  fail "checksum mismatch unexpectedly succeeded"
fi
mv "${release_one}/SHA256SUMS.original" "${release_one}/SHA256SUMS"

release_three="$(make_release "9.8.9" setup-fail)"
if run_installer "9.8.9" "${release_three}"; then
  fail "failed OAuth setup unexpectedly succeeded"
fi
[[ ! -f "${test_directory}/state-9.8.9/connect" ]] || fail "clients changed after failed OAuth setup"

if run_installer "9.9.0" "${test_directory}/missing-release"; then
  fail "missing release unexpectedly succeeded"
fi

if HOME="${test_directory}/home" \
  YANDEX_MCP_INPUT_PATH="${test_directory}/input" \
  YANDEX_MCP_OUTPUT_PATH="${test_directory}/output" \
  YANDEX_MCP_UNAME_S=UnsupportedOS \
    bash "${PROJECT_DIRECTORY}/scripts/install.sh"; then
  fail "unsupported platform unexpectedly succeeded"
fi

printf 'Installer smoke tests passed\n'
