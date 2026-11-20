# TEST: amortyzacja ekonomiczna — 20.11.2026

Gałąź `feat/economic-depreciation`, tag `nclean-v0.1-test.3`.
Punkt startowy: poprzedni TEST `f0b5393`. Bez PR, scalania i wdrożenia instalacji właściciela.

- Kartoteka sprzętu: wartość początkowa/końcowa, pierwszy miesiąc i okres liniowy;
  kwota bieżącego miesiąca, plan roku, umorzenie do bieżącego dnia i pozostała wartość.
- Plan zaczyna się pierwszego dnia wybranego miesiąca. Pierwsze ustawienie może
  obejmować wcześniejsze miesiące. Korekty i wyłączenie dotyczą początku bieżącego
  miesiąca (lub przyszłego początku planu); wcześniejsze miesiące pozostają bez zmian.
- Rewizje i ich kwoty są zachowywane. Przy korekcie pozostała kwota jest rozkładana
  do końca nowego okresu, bez powtórnego uwzględnienia wcześniej rozliczonej wartości.
- Archiwizacja kartoteki nie wyłącza planu. Służy do tego opcja w formularzu amortyzacji.
- Podsumowanie i DZISIAJ: domyślnie wyłączony przełącznik. Włączenie zmienia
  prezentowany wynik i jego wartości dzienne; wyłączenie przywraca wynik bazowy.
  Wybór jest parametrem raportu, nie stałym ustawieniem finansowym.
- Dni i niepełne miesiące otrzymują proporcjonalny udział. Zaokrąglenie narastające
  zapewnia dokładną sumę groszy. Pełny przyszły okres zawiera zaplanowane kwoty.
- Brak planu oznacza brak informacji, nie potwierdzone zero. Raport ostrzega
  o aktywnych kartotekach bez ustawień. Wyłączenie lub plan zerowy to jawna decyzja.
- Amortyzacja nie jest podatkowa i nie tworzy `cost_entry`, wpłat ani tankowań.
  Historyczne ceny usług, zasady finansowe i stare migracje pozostają bez zmian.
- Migracja V22 jest addytywna, nie tworzy automatycznych planów dla istniejących danych.
  Wspólny podział miesięcznych kwot na dni zachowuje dotychczasowe obliczenia kosztów.

## Weryfikacja

Java 17 i izolowany PostgreSQL, bez produkcji nclean.pl i danych właściciela.
`./mvnw verify`: 210 testów (73 jednostkowe/HTTP/architektury, 137 integracyjnych),
0 niepowodzeń, błędów i pominięć. Testy obejmują grosze, wartość końcową, korekty
z zachowaniem historii, wyłączenie, brak planu, jednoczesne ponowienia, rollback
całego planu i migrację V21 → V22 bez zmiany historycznego zakupu.

Chromium na rzeczywistym CRM pod `/` i `/crm`: 3600/36 = 100 zł miesięcznie,
utrata odpowiedzi i ponowienie bez drugiej rewizji, przełącznik i nawigacja okresów,
niepełny miesiąc, zgodność sum dziennych z wynikiem, korekta i wyłączenie planu:
PASS. 24 widoki w 360/390/768/1440 px: bez poziomego przewijania i błędów JS.

Powtórzenie na nowym, syntetycznym kontenerze QA:
`python scripts/verify-equipment-live.py --work-dir /tmp/nclean-depreciation-qa --java /path/to/java17 --depreciation-only`
Opcja `--context-path ''` sprawdza główną ścieżkę zamiast domyślnego `/crm`.
Wymagania runnera pozostają takie jak w poprzednim wydaniu TEST.

Docker PC: przy aktualizacji wymagana przebudowa obrazu; sama zmiana taga lub
restart nie aktualizuje kodu. Konfiguracja Compose i podłączenie bazy nie były zmieniane.
Automatyczne motogodziny ze zleceń pozostają osobnym etapem.
