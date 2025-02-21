
# GitHub Reader 📖

![Java](https://img.shields.io/badge/Java-23-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green.svg)
![Maven](https://img.shields.io/badge/Maven-Plugin-orange.svg)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

GitHub Reader — это Spring Boot приложение для работы с содержимым репозиториев GitHub через API.   
Оно позволяет получать содержимое файлов, скачивать репозитории и сохранять данные как в отдельные файлы,   
так и в один общий файл.

---

## 🚀 Основные возможности

- Получение содержимого конкретного файла из репозитория.
- Загрузка списка содержимого репозитория с поддержкой HTML-ссылок.
- Сохранение содержимого репозитория в локальные файлы.
- Объединение всех данных репозитория в один текстовый файл.

---

## 📋 Требования проекта

- **JDK**: 23 или выше
- **Maven**: Для сборки и управления зависимостями
- **GitHub Token**: Личный токен доступа GitHub с правами `repo` (указывается в `application.yml`)
- **Spring Boot**: Версия 3.4.3

---

## 🛠 Установка и настройка

1. **Клонируйте репозиторий:**
```bash
   git clone https://github.com/yourusername/github-reader.git
   cd github-reader
```

2. **Настройте GitHub токен:**

Добавьте ваш GitHub токен в переменную окружения GITHUB_TOKEN. Например в Intellij IDEA

```yaml
github:
  token: ${GITHUB_TOKEN}
```

3. **Соберите проект:**  

```bash
mvn clean install
```

4. **Запустите приложение:**
```bash
mvn spring-boot:run
```

## 🎯 API Эндпоинты
| Метод | URL | Описание |
|--------|-----------------|----------------|
| **GET** |`/api/github/content`|Получить содержимое файла|
| **GET** |`/api/github/repo-contents`|Получить список содержимого репозитория|
| **POST** |`/api/github/save-contents`|Сохранить содержимое в файлы|
| **POST** |`/api/github/save-all-to-single-file`|Сохранить всё содержимое в один файл|

### Примеры запросов
Получить содержимое файла:
```bash
curl "http://localhost:8080/api/github/content?repoUrl=https://github.com/user/repo&filePath=src/main/java/Test.java"
```
Сохранить всё в один файл:
```bash
curl -X POST "http://localhost:8080/api/github/save-all-to-single-file?repoUrl=https://github.com/user/repo"
```
## 🏗 Структура проекта
```plaintext
src/main/java/com/example/githubreader
├── config/                   
│   ├── GithubConfig.java     # Конфигурация GitHub API
├── controller/               
│   ├── GithubController.java # REST API эндпоинты
├── service/                  
│   ├── GithubContentService.java # Логика работы с GitHub
├── GithubReaderApplication.java  # Основной класс приложения
```
* pom.xml: Конфигурация Maven с зависимостями Spring Boot, Lombok и тестовыми библиотеками.
* src/main/resources/application.yml: Настройки приложения и фильтры файлов.
## ⚙ Конфигурация
Файл `application.yml` поддерживает настройку:
* github.token: Ваш GitHub токен.
* github.includePatterns: Шаблоны для включения файлов (например, **/*.java).
* github.excludePatterns: Шаблоны для исключения (например, .gitignore).
* singleFilePath: Путь для сохранения единого файла (по умолчанию output/all_contents.txt).

### Пример:
```yaml
github:
token: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
includePatterns:
- "**/*.java"
- "**/*.md"
excludePatterns:
- ".idea/**"
- "target/**"
```
## 🧪 Тестирование
Проект включает юнит-тесты с использованием:
Mockito: Для моков сервисов.
Spring Boot Test: Для тестирования контроллеров.
Запустите тесты:
```bash
mvn test
```
## 📦 Зависимости
* Spring Boot Starter Web: Для REST API.
* Lombok: Для упрощения кода.
* Mockito & Hamcrest: Для тестирования.  
Полный список зависимостей в pom.xml.
## 🤝 Вклад в проект
1. Форкните репозиторий.  
2. Создайте ветку для вашей фичи: git checkout -b feature/ваша-идея.  
3. Сделайте изменения и закоммитьте: git commit -m "Добавлена новая фича".  
4. Отправьте в ваш форк: git push origin feature/ваша-идея.  
5. Создайте Pull Request.

## 📜 Лицензия
Проект распространяется под лицензией MIT (LICENSE).
📧 Контакты
Если у вас есть вопросы, пишите на: your.email@example.com (mailto:your.email@example.com).
Спасибо за использование GitHub Reader! 🌟

### Объяснение структуры и подходов:

1. **Заголовок и бейджи**: Использованы эмодзи и shields.io для визуальной привлекательности и быстрого понимания технологий.
2. **Краткое описание**: Четко указано, что делает проект.
3. **Разделы**: Логически разделены с эмодзи для читаемости (`🚀`, `📋`, `🎯` и т.д.).
4. **Инструкции**: Подробные шаги установки и запуска, примеры запросов.
5. **API**: Таблица с эндпоинтами для удобства восприятия.
6. **Структура проекта**: Простая схема с пояснениями.
7. **Конфигурация и тесты**: Указаны ключевые настройки и как запустить тесты.
8. **Призыв к действию**: Добавлены разделы для контрибьюторов и контактов.
