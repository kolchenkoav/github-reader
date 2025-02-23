
# GitHub Reader 📖

![Java](https://img.shields.io/badge/Java-23-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green.svg)
![Maven](https://img.shields.io/badge/Maven-Plugin-orange.svg)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

GitHub Reader — это Spring Boot приложение для работы с содержимым репозиториев GitHub через API, а также для обработки локальных директорий.  
Оно позволяет получать содержимое файлов из GitHub, скачивать репозитории, сохранять данные как в отдельные файлы, так и в один общий файл, а также объединять содержимое локальных директорий в единый текстовый файл с поддержкой многопоточности.

---

## 🚀 Основные возможности

- Получение содержимого конкретного файла из репозитория GitHub.
- Загрузка списка содержимого репозитория с поддержкой HTML-ссылок.
- Сохранение содержимого репозитория в локальные файлы.
- Объединение всех данных репозитория в один текстовый файл.
- Многопоточная обработка содержимого локальных директорий и сохранение в один файл с фильтрацией по паттернам.
- Веб-форма (Thymeleaf) для выбора источника (GitHub или директория) и сохранения содержимого в файл.

---

## 📋 Требования проекта

- **JDK**: 23 или выше
- **Maven**: Для сборки и управления зависимостями
- **GitHub Token**: Личный токен доступа GitHub с правами `repo` (указывается в `application.yml`)
- **Spring Boot**: Версия 3.4.3
- **Thymeleaf**: Версия 3.1.3.RELEASE (для веб-интерфейса)

---

## 🛠 Установка и настройка

1. **Клонируйте репозиторий:**
   ```bash
   git clone https://github.com/yourusername/github-reader.git
   cd github-reader
   ```
Настройте GitHub токен:
Добавьте ваш GitHub токен в переменную окружения GITHUB_TOKEN. Например, в IntelliJ IDEA:
```yaml
github:
token: ${GITHUB_TOKEN}
```
Настройте путь к локальной директории (опционально):
Укажите путь по умолчанию в application.yml под ключом directory.defaultPath,   
если хотите обрабатывать локальные файлы без указания пути в запросе.  
#### Соберите проект:
```bash
mvn clean install
```
#### Запустите приложение:
```bash
mvn spring-boot:run
```
## 🎯 API Эндпоинты
| Метод | URL | Описание |
|--------|-----------------|----------------|
| GET | /api/github/content | Получить содержимое файла из GitHub |
| GET | /api/github/repo-contents | Получить список содержимого репозитория |
| POST | /api/github/save-contents | Сохранить содержимое в файлы |
| POST | /api/github/save-all-to-single-file | Сохранить всё содержимое GitHub в один файл |
| POST | /api/directory/save-all-to-file | Сохранить содержимое локальной директории в один файл |

### Примеры запросов
#### Работа с GitHub
Получить содержимое файла:
```bash
curl "http://localhost:8080/api/github/content?repoUrl=https://github.com/user/repo&filePath=src/main/java/Test.java"
```
#### Сохранить всё в один файл:
```bash
curl -X POST "http://localhost:8080/api/github/save-all-to-single-file?repoUrl=https://github.com/user/repo"
```
#### Работа с локальной директорией
Сохранить содержимое локальной директории в файл (с использованием пути по умолчанию):
```bash
curl -X POST "http://localhost:8080/api/directory/save-all-to-file"
```
#### С указанием пути:
```bash
curl -X POST "http://localhost:8080/api/directory/save-all-to-file?directoryPath=E:%5Cprojects%5Cjava%5Cp3_tariff_calculator"
```
## 🏗 Структура проекта
```plaintext
src/main/java/com/example/githubreader
├── config/                   
│   ├── DirectoryConfig.java      # Конфигурация локальных директорий
│   ├── GithubConfig.java         # Конфигурация GitHub API
├── controller/               
│   ├── ContentFormController.java # Веб-форма для выбора источника
│   ├── DirectoryController.java  # REST API для локальных директорий
│   ├── GithubController.java     # REST API для GitHub
├── model/                   
│   ├── ContentSourceRequest.java # DTO для формы
├── service/                  
│   ├── DirectoryContentService.java # Логика работы с директориями
│   ├── GithubContentService.java    # Логика работы с GitHub
├── GithubReaderApplication.java     # Основной класс приложения

src/main/resources/
├── templates/                   
│   ├── content-form.html        # Thymeleaf-шаблон веб-формы
├── application.yml              # Настройки приложения

pom.xml: Конфигурация Maven с зависимостями.
```
## ⚙ Конфигурация
Файл application.yml поддерживает настройку:
* github.token: Ваш GitHub токен.
* github.includePatterns: Шаблоны для включения файлов (например, **/*.java).
* github.excludePatterns: Шаблоны для исключения (например, .gitignore, .git/**).
* github.singleFilePath: Путь для единого файла (по умолчанию output/all_contents.txt).
* directory.defaultPath: Путь по умолчанию для обработки локальной директории (например, E:\\projects\\java\\p3_tariff_calculator).
Пример:
```yaml
github:
  token: ${GITHUB_TOKEN}
  includePatterns:
  - "**/*.java"
  - "**/*.md"
  - "**/*.txt"
  excludePatterns:
  - ".idea/**"
  - "target/**"
  - ".git/**"
singleFilePath: "output/all_contents.txt"

directory:
  defaultPath: "E:\\projects\\java\\p3_tariff_calculator"
```
## 🧪 Тестирование
Проект включает юнит-тесты с использованием:
* Mockito: Для моков сервисов.
* Spring Boot Test: Для тестирования контроллеров.
Запустите тесты:
```bash

mvn test
```
Тесты покрывают:
1. Обработку содержимого GitHub репозиториев (GithubContentService).
2. Обработку локальных директорий с фильтрацией файлов (DirectoryContentService).
3. REST эндпоинты (GithubController и DirectoryController).
4. Веб-форму и редирект с корневого пути (ContentFormController).
## 📦 Зависимости
* Spring Boot Starter Web: Для REST API.
* Lombok: Для упрощения кода.
* Mockito & Hamcrest: Для тестирования.
* Полный список зависимостей в pom.xml.
## 🤝 Вклад в проект
1. Форкните репозиторий.
2. Создайте ветку для вашей фичи: git checkout -b feature/ваша-идея.
3. Сделайте изменения и закоммитьте: git commit -m "Добавлена новая фича".
4. Отправьте в ваш форк: git push origin feature/ваша-идея.
5. Создайте Pull Request.
## 📜 Лицензия
Проект распространяется под лицензией MIT (LICENSE).
## 📧 Контакты
Если у вас есть вопросы, пишите на: your.email@example.com (mailto:your.email@example.com).
Спасибо за использование GitHub Reader! 🌟
