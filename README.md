# AIS Viewer

JavaFX desktopová aplikácia pre prezeranie údajov z AIS2 (Akademický informačný systém, UKF Nitra).

![AIS Viewer Screenshot](docs/screenshot.png)

## Popis

AIS Viewer umožňuje prihlásenie do AIS2 cez vaše študentské konto a zobrazuje:

- **Predmety** – povinné, povinne voliteľné a výberové predmety zo zápisného listu
- **Skúšky** – skúškové termíny
- **Kredity / Prehľady** – súhrn kreditov a priemer známok
- **Rozvrh** – rozvrh hodinový

## Technológie

- Java 17+ (OpenJDK 17)
- JavaFX 17 (OpenJFX)
- Maven 3.6+
- Jsoup 1.17 (HTML parsing)
- `java.net.http.HttpClient` (HTTP requesty, session management)

## Požiadavky

- JDK 17 alebo novší
- Apache Maven 3.6+
- Internetové pripojenie (pre prístup na ais2.ukf.sk)

## Štruktúra projektu

```
ais-viewer/
├── pom.xml
├── README.md
└── src/
    └── main/
        ├── java/
        │   └── sk/ukf/aisviewer/
        │       ├── App.java                    (hlavná JavaFX trieda)
        │       ├── controller/
        │       │   ├── LoginController.java    (prihlasovací formulár)
        │       │   └── MainController.java     (hlavné okno s tabmi)
        │       ├── model/
        │       │   ├── Subject.java            (predmet)
        │       │   ├── Exam.java               (skúška)
        │       │   └── StudentInfo.java        (info o študentovi)
        │       ├── service/
        │       │   ├── AisClient.java          (HTTP klient, login, session)
        │       │   └── AisParser.java          (Jsoup HTML parsing)
        │       └── util/
        │           └── CookieManager.java      (správa session cookies)
        └── resources/
            └── sk/ukf/aisviewer/
                ├── login.fxml                  (prihlasovacia obrazovka)
                ├── main.fxml                   (hlavné okno)
                └── styles.css                  (AIS2-inšpirovaný tmavý dizajn)
```

## Spustenie

### Pomocou Maven

```bash
# Zostaviť projekt
mvn clean compile

# Spustiť aplikáciu
mvn javafx:run
```

### Pomocou JAR súboru

```bash
# Zostaviť fat JAR
mvn clean package -DskipTests

# Spustiť
java -jar target/ais-viewer-1.0-SNAPSHOT.jar
```

> **Poznámka:** Keďže JavaFX nie je súčasťou štandardného JDK, pri spúšťaní JAR bez použitia Maven je potrebné pridať JavaFX moduly na classpath ručne, alebo použiť `mvn javafx:run`.

## Prihlásenie

1. Zadajte vaše **AIS2 prihlasovacie meno** (rovnaké ako na ais2.ukf.sk)
2. Zadajte vaše **heslo**
3. Kliknite na „Prihlásiť sa"

Aplikácia odošle POST request na `https://ais2.ukf.sk/ais/login.do` a udržuje session cookies pre ďalšie requesty.

## Poznámky k parsovaniu

AIS2 je Angular SPA aplikácia (ng-version 19.x). Niektoré dáta sa načítavajú dynamicky cez XHR/fetch requesty. Aplikácia používa kombináciu:

1. **HTML parsovanie cez Jsoup** – pre server-side renderované časti
2. **REST API endpointy** – aplikácia sa pokúša nájsť JSON API endpointy pod `/ais/api/...`
3. **Fallback stratégie** – ak hlavné parsovanie zlyhá, použijú sa alternatívne selektory

## Autor

Samuel Haluza – UKF Nitra, Aplikovaná informatika (2AI22b)
