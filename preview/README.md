# Aktualny podgląd: audyt pracy i zapisu

[Otwórz HTML](nclean-daily-audit.html) · [Pobierz ZIP](nclean-daily-audit.zip) · [Raport audytu](../docs/daily-reliability/REPORT.md).

Szkice edytora notatek i kosztów można przywrócić i jawnie odrzucić; dotychczasowy układ LEAN pozostaje. Bieżąca gałąź: `feat/nclean-daily-reliability-audit`.

HTML jest syntetycznym DEMO bez połączenia z bazą. Stan próby i szkice są pamięcią strony; JSON nie jest backupem bazy ani zdjęć. Pełną aplikację, rzeczywiste zapisy, sesję, ponowienia i odtwarzanie PostgreSQL sprawdzono osobno — wyniki w raporcie. Nie wpisuj danych prawdziwych klientów do DEMO.

# Archiwum: LEAN — opcje na żądanie

[Otwórz HTML](nclean-lean.html) · [Pobierz ZIP](nclean-lean.zip) · [Obsługa i testy](../docs/lean-disclosure/REPORT.md).

Plan dnia przed analizami, zwijane filtry firmy i kosztów, kompaktowy edytor notatki, przypomnienia z liczbą w nagłówku oraz opcjonalne analizy paliwa. Bieżący kod: `feat/lean-progressive-disclosure`.

Pełna aplikacja zapisuje w PostgreSQL, migracje V1–V19 pozostają niezmienione. DEMO HTML zapisuje tylko w pamięci karty; odświeżenie resetuje próbę. Eksport JSON nie jest backupem bazy ani zdjęć. Wyłącznie syntetyczne dane. Natychmiastowe powiadomienia systemowe Android i SMS nie są częścią demonstracji.

# Archiwum: Dzisiaj, kalendarz, tankowania i przypomnienia

[Otwórz HTML](nclean-today-workspace.html) · [Pobierz ZIP](nclean-today-workspace.zip) · [Raport i obsługa](../docs/today-workspace/REPORT.md).

W prawdziwej aplikacji dane są w PostgreSQL (migracje V1–V18). HTML jest
syntetyczną demonstracją w pamięci karty: odświeżenie resetuje stan. Menu `⋯`
pozwala wyeksportować/wczytać próbę, również tankowania i stan przypomnień,
bez zdjęć oraz szkiców. Nie wpisuj danych klientów. Wykresy są przykładowym
scenariuszem, nie pełnym silnikiem rozliczeń DEMO.

Bieżący kod: `feat/today-planning-fuel-reminders`. Poniższe pliki to archiwum
wcześniejszych etapów; stara paczka administratora nie zawiera migracji V18.

# Archiwum: pixelowy interfejs i główny cel

[Otwórz HTML](nclean-pixel-main-goal.html) · [Pobierz ZIP](nclean-pixel-main-goal.zip) · [Raport i obsługa](../docs/pixel-main-goal/REPORT.md).

W prawdziwej aplikacji główny cel jest zapisywany w PostgreSQL (nowa migracja V17).
HTML jest demonstracją z danymi syntetycznymi, w pamięci karty. Odświeżenie resetuje
stan; eksport/import ćwiczenia znajduje się w menu `⋯`. Nie wpisuj danych klientów.

Poniższe podglądy i paczki źródłowe to archiwum wcześniejszych etapów. Paczka
administratora z V1–V16 nie zawiera jeszcze tej aktualizacji; aktualny kod jest
na gałęzi `feat/pixel-motion-main-goal`.

# Archiwum: audyt użytkowy 15.11.2026

Aktualny [HTML](nclean-user-journey.html), [ZIP](nclean-user-journey.zip), [raport](../docs/user-journey-audit/REPORT.md).

Poniższe paczki pochodzą z wcześniejszego etapu 14.11.2026.

# Archiwum: podgląd i paczka dla administratora

- [Klikalny HTML](nclean-final.html)
- [Podgląd ZIP: index.html + instrukcja](nclean-final-demo.zip)
- [Paczka źródłowa dla administratora](nclean-administrator-source.zip)
- [Wersja źródeł i sumy plików](final-build.json)
- [Audyt i poprawki](../docs/final-audit/REPORT.md)
- [Uruchomienie, aktualizacja i odzyskanie danych](../deploy/ops/RUNBOOK.md)

Na Androidzie pobierz ZIP, rozpakuj i otwórz index.html w Chrome z JavaScript.
Podgląd systemowego menedżera plików może nie wykonywać skryptów. To samodzielny
HTML z prawdziwych widoków aplikacji i syntetycznych danych. Nie jest hostingiem
ani zapisem do PostgreSQL. Odświeżenie resetuje stan demonstracji.

Menu `⋯` umożliwia eksport/import ćwiczenia JSON: dodane zlecenia, notatki,
oznaczenia dni i koszty, bez zdjęć oraz szkiców. Nie wpisuj danych klientów.
Historyczne zlecenie można dodać bez wymyślonego kontaktu; to samo działa
w prawdziwej aplikacji, gdzie dane i zdjęcia zapisują się w PostgreSQL.

Paczka źródłowa zawiera kod, Maven Wrapper, migracje V1–V16, dokumentację,
narzędzia backup/update/restore i ten podgląd. Nie zawiera JAR-a ani haseł.
Administrator buduje aplikację przez `./mvnw verify` (JDK17, Docker dla testów).
Pełna paczka z gotowym JAR-em jest osobnym plikiem przekazanym w czacie.

Sprawdzono cztery szerokości, zagnieżdżony sandbox, zapis/edycję, usuwanie/
przywracanie i ograniczenie animacji. Osobny test potwierdza eksport i ponowne
wczytanie ćwiczenia oraz rozdzielenie pól i stopki zapisu w pięciu rozmiarach.
Starsze pliki podglądu w tym katalogu są archiwalne.
