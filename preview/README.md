# Audyt użytkowy 15.11.2026

Aktualny [HTML](nclean-user-journey.html), [ZIP](nclean-user-journey.zip), [raport](../docs/user-journey-audit/REPORT.md).

Poniższe paczki pochodzą z wcześniejszego etapu 14.11.2026.

# Aktualny podgląd i paczka dla administratora

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
