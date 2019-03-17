
# Упрощенный чек-лист для получения результатов под виндой максимально быстро

## Установка SonarQube

SonarQube это сайт, на котором видно ошибки по проекту.

https://docs.sonarqube.org/latest/setup/get-started-2-minutes/

* [Скачать](https://www.sonarqube.org/downloads/) 
* Распаковать в папку `C:\Sonar\sonarqube`
* В файл `C:\Sonar\sonarqube\conf\sonar.properties` вставить 

```ini
sonar.web.javaOpts=-server -Xmx2g -Xms1g -XX:+HeapDumpOnOutOfMemoryError
sonar.ce.javaOpts=-Xmx6g -Xms4g -XX:+HeapDumpOnOutOfMemoryError
sonar.search.javaOpts=-Xms5g -Xmx6g -XX:+HeapDumpOnOutOfMemoryError
```
* Запустить `C:\Sonar\sonarqube\bin\windows-x86-64\StartSonar.bat`
* В консоле через некоторое время должно появится `SonarQube is up`
* Зайти на [http://localhost:9000/](http://localhost:9000/), авторизоваться admin/admin
* Для остановки сервера в консоле нажать `Ctrl+C`

## Настройка SonarQube

* Руссификация - `http://localhost:9000/admin/marketplace`, найти плагин `Russian Pack` и установить
* Плагин для bsl
  * Скачать jar-файл с https://github.com/1c-syntax/sonar-bsl-plugin-community/releases
  * Скопировать его в `C:\Sonar\sonarqube\extensions\plugins`
* Перезапустить сервер SonarQube

## Установка Scanner

Он нужен для передачи результатов анализа репортерами в сонар.

https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner

* Скачать 
* Распаковать в `C:\Sonar\sonar-scanner`
* Добавить `C:\Sonar\sonar-scanner\bin` в PATH
* В консоле проверить, что `sonar-scanner.bat -h` выдает справку

## Установка BSL Language Server

* Скачать jar-файл с https://github.com/1c-syntax/bsl-language-server/releases
* Скопировать и переименовать его в `C:\Sonar\bin\bsl-language-server.jar`

## Настройка проекта

* Создать папку `C:\Sonar\<ИмяПроекта>`
* В папке `C:\Sonar\<ИмяПроекта>\conf` создать файлы:
  * `C:\Sonar\<ИмяПроекта>\conf\bsl-language-server.conf`
    ```json
    {
    "diagnosticLanguage": "ru",
    "diagnostics": {
        "LineLength": {
        "maxLineLength": 140
        }
    }
    }
    ```
  * `C:\Sonar\<ИмяПроекта>\conf\sonar-project.properties`
    ```ini
    sonar.host.url=http://localhost:9000
    sonar.projectKey=<ИмяПроекта>
    sonar.projectVersion=<ВерсияПроекта>
    sonar.sources=src
    sonar.sourceEncoding=UTF-8
    sonar.inclusions=**/*.bsl
    sonar.externalIssuesReportPaths=bsl-generic-json.json
    ```
  * Выгрузить конфу в файлы в `C:\Sonar\<ИмяПроекта>\src`

## Запуск проверки

```cmd
cd C:\Sonar\<ИмяПроекта>

java -Xmx8g -jar ../bin/bsl-language-server.jar -a -s ./src -r generic -c ./conf/bsl-language-server.conf

@set SONAR_SCANNER_OPTS=-Xmx6g

sonar-scanner -D project.settings=./conf/sonar-project.properties
```

Через некоторое время результаты появятся на странице http://localhost:9000/dashboard?id=<ИмяПроекта>