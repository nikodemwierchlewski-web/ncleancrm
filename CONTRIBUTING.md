# Rozbudowa ncLEAN CRM

Przed zmianą przeczytaj [architekturę i granice modułów](docs/architecture/ARCHITECTURE.md).
Aktualne ustalenia i ryzyka: [audyt](docs/architecture/AUDIT.md).

- Rozwijamy istniejący modularny monolit Spring Boot/Thymeleaf/PostgreSQL.
- HTTP nie zawiera SQL, repozytoriów ani transakcji. Logika nie zależy od kontrolerów.
- Nie dokładamy cykli między modułami ani zależności shared od domen.
- Zapis jest transakcyjny, wersjonowany i odporny na ponowienie; błąd zachowuje dane.
- Nie edytujemy wykonanych migracji i historycznych snapshotów.
- OWNER, CSRF i lokalne sekrety obowiązują na każdym nowym ekranie.
- Testy: `./mvnw verify` / `.\mvnw.cmd verify`; JDK 17, Docker dla PostgreSQL.
  `ArchitectureTest` działa również w zwykłym `./mvnw test` i w CI.
- Nie używamy danych klientów ani produkcyjnego CMS/bazy w testach i podglądach.
- Zmiany zachowania, formatowanie i migracje rozdzielamy na łatwe do oceny etapy.
