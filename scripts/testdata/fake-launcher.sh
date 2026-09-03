#!/usr/bin/env bash

set -euo pipefail

state_directory="@STATE@"
behavior="@BEHAVIOR@"
mkdir -p "${state_directory}"

case "${1:-}" in
  doctor)
    if [[ "${behavior}" == "broken-doctor" ]]; then
      exit 1
    fi
    if [[ " $* " == *" --verify-token "* && -f "${state_directory}/setup" ]]; then
      printf 'авторизован: да\n' >&2
    else
      printf 'авторизован: нет\n' >&2
    fi
    ;;
  setup)
    if [[ "${behavior}" == "setup-fail" ]]; then
      exit 1
    fi
    touch "${state_directory}/setup"
    ;;
  connect)
    touch "${state_directory}/connect"
    ;;
  *)
    exit 64
    ;;
esac
