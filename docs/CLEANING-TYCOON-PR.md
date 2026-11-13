Eksperymentalny motyw Cleaning Tycoon otrzymuje izometryczną scenę firmy sprzątającej z delikatnymi animacjami oraz postęp oparty na faktycznym zapisie pracy. Klasyczny wygląd pozostaje domyślny; nowe elementy UI są dostępne w profilu `tycoon`.

- Postęp dokumentacji wybranego dnia i jeden istniejący przypięty cel z krokami. Pięć odznak: 1/10/50 wykonanych usług, kompletny dzień i świadome ukończenie celu.
- V14 zapisuje odznaki w transakcji operacji biznesowej; bez zmian V1–V13 i historycznych cen. Migruje historyczne etapy bez serii powiadomień. Atomowe potwierdzenie zapobiega powtarzaniu komunikatu w wielu kartach/restartach; historia pozostaje dostępna.
- Własne lokalne SVG, ograniczenie ruchu, pierwszeństwo preferencji systemowej i zatrzymywanie sceny poza widokiem/tłem. Powiadomienie nie przerywa otwartego formularza.
- OWNER i CSRF zachowane. Testy i próbki korzystają wyłącznie z odizolowanych schematów/syntetycznych danych.

Weryfikacja: Maven Wrapper `clean verify` — 27 jednostkowych/MVC i 76 integracyjnych PostgreSQL/Testcontainers, łącznie 103; zero porażek, błędów i pominięć. Chromium: 360/390/768/1440 px, osiem wizyt, zdjęcia/chemia/czas,503 i ponowienie, konflikty kart, cel, zamknięcie/korekta dnia, restart, ustawienia ruchu i odroczenie powiadomienia. Dodano `--pixel-tycoon` do powtarzalnego `scripts/verify-browser.py`.

Pobieralny `preview/nclean-pixel-tycoon-test.zip` zawiera 60 rzeczywistych widoków z tymi samymi modułami UI; zapis HTML jest jawnie symulowany w pamięci karty. Sprawdzono nawigację, formularze, odznaki, ruch, klasyczny wygląd i zagnieżdżoną ramkę. Sesja nie ma natywnego przekierowania podglądu. Instrukcja i ograniczenia: `docs/CLEANING-TYCOON-PROGRESS.md`, wyniki/sumy: `docs/audit/cleaning-tycoon-evidence.json`, tożsamość paczki: `preview/build.json`.

Gałąź bazowa to istniejący `experiment/pixel-tycoon`, aby ten PR przedstawiał przyrost eksperymentu, wraz z zależnością od wcześniejszych zmian CRM. Nie wdrożono na produkcję ani nie scalono do main.
