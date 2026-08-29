# Релизы и дистрибуция

Документ предназначен для пользователей, которые скачивают готовое приложение, и для разработчиков, выпускающих новую версию. Пользователь может ограничиться разделом проверки скачанного файла; технические детали workflow нужны сопровождающим проекта.

## Каналы доставки

| Канал | Для кого | Содержимое |
|---|---|---|
| GitHub Releases | конечные пользователи | переносимые пакеты для ОС, JAR и `SHA256SUMS` |
| GitHub Container Registry (`ghcr.io`) | Docker/серверный запуск | Tracker и Wiki multi-arch образы |
| GitHub Actions artifacts | разработчики и диагностика CI | промежуточные результаты workflow |

GitHub Maven Packages не используется как пользовательский загрузчик: он предназначен для Maven-зависимостей и требует настройки аутентификации. Для скачиваемого приложения GitHub Release проще.

## Версионирование

Релиз запускается тегом SemVer:

```bash
git tag 1.0.0
git push origin 1.0.0
```

Поддерживаются теги как без префикса (`1.0.0`), так и с ним (`v1.0.0`). Допустимы prerelease-теги, например `1.1.0-rc.1`. Такой выпуск отмечается как prerelease и не обновляет Docker-тег `latest`.

Workflow также поддерживает ручной запуск `workflow_dispatch` с версией без префикса `v`. Maven-версия устанавливается только внутри CI и не изменяет POM в репозитории.

## Что делает release workflow

1. Проверяет формат версии.
2. Выполняет полный `mvn verify`.
3. Собирает JAR с версией релиза для Tracker и Wiki.
4. На виртуальных машинах GitHub создаёт `app-image` с собственной Java Runtime.
5. Запускает `doctor` для обоих исполняемых файлов до упаковки.
6. Формирует архивы Linux x64/ARM64, macOS Intel/Apple Silicon и Windows x64.
7. Публикует multi-arch Docker-образы в GHCR.
8. Генерирует `SHA256SUMS` и build provenance attestations.
9. Создаёт GitHub Release с автоматически сформированными release notes.

## Локальная проверка упаковки

После `mvn package` на Linux или macOS:

```bash
scripts/package-release.sh \
  0.1.0 \
  macos-arm64 \
  yandex-mcp-workspace-tracker/target/yandex-mcp-workspace-tracker-0.1.0-SNAPSHOT.jar \
  yandex-mcp-workspace-wiki/target/yandex-mcp-workspace-wiki-0.1.0-SNAPSHOT.jar \
  target/release
```

На Windows используйте `scripts/package-release.ps1` с параметрами `Version`, `Classifier`, `TrackerJar`, `WikiJar` и `OutputDir`.

## Проверка скачанного файла

SHA-256:

```bash
# Linux
sha256sum -c SHA256SUMS --ignore-missing

# macOS: проверка одного скачанного архива
grep "<имя-архива>" SHA256SUMS | shasum -a 256 -c -
```

GitHub attestation:

```bash
gh attestation verify yandex-mcp-workspace-<version>-<os>-<arch>.<archive> \
  -R developerdevpav/yandex-mcp-workspace
```

Аттестация происхождения подтверждает репозиторий, commit и workflow сборки, но не заменяет аудит кода или системную подпись приложения.

Официальные материалы GitHub: [управление Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository), [artifact attestations](https://docs.github.com/en/actions/concepts/security/artifact-attestations), [GitHub-hosted runner’ы](https://docs.github.com/en/actions/reference/runners/github-hosted-runners).
