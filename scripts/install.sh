#!/usr/bin/env bash

set -euo pipefail

readonly REPOSITORY="developerdevpav/yandex-mcp-workspace"
readonly INSTALL_ROOT="${YANDEX_MCP_INSTALL_ROOT:-${HOME}/.local/share/yandex-mcp}"
readonly BIN_DIR="${YANDEX_MCP_BIN_DIR:-${HOME}/.local/bin}"
readonly INPUT_PATH="${YANDEX_MCP_INPUT_PATH:-/dev/tty}"
readonly OUTPUT_PATH="${YANDEX_MCP_OUTPUT_PATH:-/dev/tty}"

temporary_directory=""

cleanup() {
  if [[ -n "${temporary_directory}" && -d "${temporary_directory}" ]]; then
    rm -rf -- "${temporary_directory}"
  fi
}
trap cleanup EXIT

fail() {
  printf 'Ошибка: %s\n' "$1" >&2
  exit 1
}

if [[ ! -r "${INPUT_PATH}" || ! -w "${OUTPUT_PATH}" ]]; then
  fail "нужен интерактивный терминал (${INPUT_PATH} или ${OUTPUT_PATH} недоступен)"
fi
exec 3<"${INPUT_PATH}" 4>"${OUTPUT_PATH}"

say() {
  printf '%s\n' "$*" >&4
}

prompt() {
  local message="$1"
  local answer
  printf '%s' "${message}" >&4
  IFS= read -r answer <&3 || fail "ввод отменён"
  printf '%s' "${answer}"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "не найдена обязательная команда: $1"
}

detect_classifier() {
  local operating_system architecture system_name machine_name
  system_name="${YANDEX_MCP_UNAME_S:-$(uname -s)}"
  machine_name="${YANDEX_MCP_UNAME_M:-$(uname -m)}"
  case "${system_name}" in
    Darwin) operating_system="macos" ;;
    Linux) operating_system="linux" ;;
    *) fail "поддерживаются только macOS и Linux" ;;
  esac

  case "${machine_name}" in
    x86_64|amd64) architecture="x64" ;;
    arm64|aarch64) architecture="arm64" ;;
    *) fail "неподдерживаемая архитектура: ${machine_name}" ;;
  esac
  printf '%s-%s' "${operating_system}" "${architecture}"
}

choose_components() {
  local answer="${YANDEX_MCP_COMPONENTS:-}"
  local normalized
  if [[ -z "${answer}" ]]; then
    say ""
    say "Что установить?"
    say "  1) Tracker и Wiki (по умолчанию)"
    say "  2) Только Tracker"
    say "  3) Только Wiki"
    answer="$(prompt "Выбор [1]: ")"
  fi

  normalized="$(printf '%s' "${answer}" | tr '[:upper:]' '[:lower:]')"
  case "${normalized}" in
    ""|1|all|both|tracker,wiki|wiki,tracker) printf '%s\n' tracker wiki ;;
    2|tracker) printf '%s\n' tracker ;;
    3|wiki) printf '%s\n' wiki ;;
    *) fail "неизвестный выбор компонентов: ${answer}" ;;
  esac
}

resolve_release() {
  local latest_url effective_url
  if [[ -n "${YANDEX_MCP_VERSION:-}" ]]; then
    RELEASE_TAG="${YANDEX_MCP_VERSION}"
  else
    latest_url="https://github.com/${REPOSITORY}/releases/latest"
    effective_url="$(curl -fsSL -o /dev/null -w '%{url_effective}' "${latest_url}")" \
      || fail "не удалось определить последний стабильный релиз"
    RELEASE_TAG="${effective_url##*/}"
  fi
  RELEASE_VERSION="${RELEASE_TAG#v}"
  [[ "${RELEASE_VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] \
    || fail "GitHub вернул некорректную версию: ${RELEASE_TAG}"
  RELEASE_URL="${YANDEX_MCP_RELEASE_BASE_URL:-https://github.com/${REPOSITORY}/releases/download/${RELEASE_TAG}}"
}

verify_checksum() {
  local archive_name="$1"
  local expected actual
  expected="$(awk -v file="${archive_name}" '$2 == file || $2 == "*" file { print $1; exit }' "${temporary_directory}/SHA256SUMS")"
  [[ -n "${expected}" ]] || fail "в SHA256SUMS отсутствует ${archive_name}"

  if command -v sha256sum >/dev/null 2>&1; then
    actual="$(sha256sum "${temporary_directory}/${archive_name}" | awk '{print $1}')"
  elif command -v shasum >/dev/null 2>&1; then
    actual="$(shasum -a 256 "${temporary_directory}/${archive_name}" | awk '{print $1}')"
  else
    fail "для проверки архива нужна команда sha256sum или shasum"
  fi
  [[ "${actual}" == "${expected}" ]] || fail "контрольная сумма ${archive_name} не совпала"
}

component_launcher() {
  local component="$1"
  local component_directory="$2"
  if [[ "${CLASSIFIER}" == macos-* ]]; then
    printf '%s/yandex-mcp-%s.app/Contents/MacOS/yandex-mcp-%s' \
      "${component_directory}" "${component}" "${component}"
  else
    printf '%s/app/bin/yandex-mcp-%s' "${component_directory}" "${component}"
  fi
}

install_component() {
  local component="$1"
  local archive_name="yandex-mcp-${component}-${RELEASE_VERSION}-${CLASSIFIER}.tar.gz"
  local extracted_directory="${temporary_directory}/unpacked-${component}"
  local bundle_directory="${extracted_directory}/yandex-mcp-${component}-${RELEASE_VERSION}-${CLASSIFIER}"
  local destination="${INSTALL_ROOT}/releases/${RELEASE_VERSION}/${component}"
  local staged_launcher stable_launcher temporary_link

  say "Скачивание ${archive_name}..."
  curl -fsSL --retry 3 --retry-delay 1 -o "${temporary_directory}/${archive_name}" \
    "${RELEASE_URL}/${archive_name}" || fail "не удалось скачать ${archive_name}"
  verify_checksum "${archive_name}"

  mkdir -p "${extracted_directory}"
  tar -xzf "${temporary_directory}/${archive_name}" -C "${extracted_directory}"
  [[ -d "${bundle_directory}" ]] || fail "в архиве ${archive_name} нет ожидаемого каталога"
  staged_launcher="$(component_launcher "${component}" "${bundle_directory}")"
  [[ -x "${staged_launcher}" ]] || fail "в архиве отсутствует исполняемый файл ${component}"

  say "Проверка ${component}..."
  "${staged_launcher}" doctor --logging.level.root=ERROR >&4 2>&4 \
    || fail "проверка ${component} завершилась ошибкой; действующая версия не изменена"

  if [[ ! -d "${destination}" ]]; then
    mkdir -p "$(dirname "${destination}")"
    mv "${bundle_directory}" "${destination}"
  fi

  stable_launcher="$(component_launcher "${component}" "${destination}")"
  mkdir -p "${BIN_DIR}"
  temporary_link="${BIN_DIR}/.yandex-mcp-${component}.$$"
  ln -s "${stable_launcher}" "${temporary_link}"
  mv -f "${temporary_link}" "${BIN_DIR}/yandex-mcp-${component}"
  printf '%s=%s\n' "${component}" "${BIN_DIR}/yandex-mcp-${component}" >>"${temporary_directory}/launchers"
}

launcher_for() {
  local component="$1"
  awk -F= -v component="${component}" '$1 == component { print substr($0, index($0, "=") + 1); exit }' \
    "${temporary_directory}/launchers"
}

authorize_if_needed() {
  local launcher diagnostics
  launcher="$(launcher_for tracker)"
  if [[ -z "${launcher}" ]]; then
    launcher="$(launcher_for wiki)"
  fi

  diagnostics="$("${launcher}" doctor --verify-token --logging.level.root=ERROR 2>&1 || true)"
  if printf '%s\n' "${diagnostics}" | grep -Eq '^авторизован: да$'; then
    say "OAuth уже настроен, сохранённая авторизация будет использована."
    return
  fi

  say ""
  say "Переходим к настройке OAuth. Пароль аккаунта Яндекса вводится только на сайте Яндекса."
  if ! "${launcher}" setup --logging.level.root=ERROR <&3 >&4 2>&4; then
    say ""
    say "Авторизация не завершена. Настройки агентов не изменялись."
    say "Повторите установку той же командой или выполните: ${launcher} setup"
    exit 1
  fi
}

connect_clients() {
  local launcher tracker_launcher wiki_launcher
  tracker_launcher="$(launcher_for tracker)"
  wiki_launcher="$(launcher_for wiki)"
  launcher="${tracker_launcher:-${wiki_launcher}}"

  say ""
  say "Подключение MCP к агентам..."
  local arguments=(connect --logging.level.root=ERROR)
  [[ -z "${tracker_launcher}" ]] || arguments+=("--tracker-command=${tracker_launcher}")
  [[ -z "${wiki_launcher}" ]] || arguments+=("--wiki-command=${wiki_launcher}")
  "${launcher}" "${arguments[@]}" <&3 >&4 2>&4
}

main() {
  local -a components
  local component
  require_command curl
  require_command tar
  CLASSIFIER="$(detect_classifier)"
  readonly CLASSIFIER
  temporary_directory="$(mktemp -d)"
  : >"${temporary_directory}/launchers"

  say "Yandex MCP Workspace — установка без sudo"
  say "Платформа: ${CLASSIFIER}"
  components=()
  while IFS= read -r component; do
    components+=("${component}")
  done < <(choose_components)
  resolve_release
  say "Версия: ${RELEASE_VERSION}"
  curl -fsSL --retry 3 --retry-delay 1 -o "${temporary_directory}/SHA256SUMS" \
    "${RELEASE_URL}/SHA256SUMS" || fail "не удалось скачать SHA256SUMS"

  for component in "${components[@]}"; do
    install_component "${component}"
  done

  authorize_if_needed
  connect_clients

  say ""
  say "Установка завершена. Исполняемые файлы находятся в ${BIN_DIR}."
  case ":${PATH}:" in
    *":${BIN_DIR}:"*) ;;
    *) say "Добавьте ${BIN_DIR} в PATH, если хотите запускать команды по короткому имени." ;;
  esac
}

main "$@"
