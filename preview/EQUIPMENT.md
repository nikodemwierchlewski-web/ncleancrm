# Sprzęt i pojazdy — podgląd do akceptacji

Pobierz `nclean-equipment.html` i otwórz w przeglądarce. Wszystkie zasoby są
w pliku; nie wymaga uruchamiania Springa, bazy ani przebudowy Dockera.
Jeśli przeglądarka blokuje lokalne HTML, z katalogu repozytorium uruchom
`python -m http.server 8765 --bind 127.0.0.1` i otwórz
`http://127.0.0.1:8765/preview/nclean-equipment.html`.

Podstawa: FINAL `55ce4a1`. Wzorce: istniejąca kartoteka `costs/equipment`,
`CostService`, `EntityReminderService`, ekran DZISIAJ, `tycoon.css`,
`pixel-goal.css` oraz oryginalna grafika ncLEANKA i fonty aplikacji.
Podgląd nie rozszerza jeszcze tych usług ani modeli. Wszystkie dane są fikcyjne;
przykładowy dzień to 19.11.2026. Akcje działają w pamięci i znikają po odświeżeniu.
Nie dodano endpointów, migracji, skryptów CRM ani zapisów do storage/bazy.

Do oceny: lista i filtry, kartoteka, niepełna historia, osobne liczniki,
plan serwisu, wykonanie i odroczenie, DZISIAJ/ncLEANEK, opcjonalna amortyzacja.
Pełne naliczanie ze zleceń, montaż części i obliczenia amortyzacji są poza etapem.

Weryfikacja: `python scripts/verify-equipment-preview.py` — Chromium,
28 widoków w 360/390/768/1440 px bez poziomego przewijania; formularze,
filtry, granica historii, archiwizacja, odczyty i ich walidacja, cykl serwisu,
odroczenie i synchronizacja, amortyzacja, Escape, reduced motion,
reset po odświeżeniu oraz brak zewnętrznych żądań i zapisów do storage: PASS.
Skrypt wymaga Python Playwright i Chromium (domyślnie `/usr/bin/chromium`,
zmienna `CHROMIUM_PATH` pozwala wskazać inną lokalizację).
Sprawdzono również składnię JavaScript (`node --check`).
Testów Spring/PostgreSQL nie uruchamiano: zmiana zawiera wyłącznie podgląd i jego test.
