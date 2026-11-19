# TEST: sprzęt i pojazdy — 19.11.2026

Gałąź `feat/equipment-maintenance-test`, nowy nieprzesuwany tag `nclean-v0.1-test.2`.
Punkt startowy: zaakceptowany podgląd `6077b0f`, oparty na FINAL `55ce4a1`.
Bez scalania, PR i wdrożenia do instalacji właściciela.

- `Więcej → Sprzęt i pojazdy`: istniejąca kartoteka, metadane, archiwizacja,
  kompletność historii i opcjonalna granica filtrowania.
- Niezależne liczniki urządzeń/podzespołów: narastające odczyty i oddzielne szacunki.
  Nowszy szacunek może wyznaczać przypomnienie, z jawnym oznaczeniem. Nie jest
  dodawany do pomiaru. Rzeczywiste odczyty muszą zachować kolejność wartości.
- Samochód/agregat mogą wskazywać istniejący licznik Paliwa: bez kopiowania
  odczytów i bez sumowania dziennych czasów agregatu jako stanu narastającego.
- Plany według daty/licznika, ostrzeżenia, odroczenie bez zmiany terminu,
  zakończenie planu, historia wykonania i kolejne progi od daty/stanu wykonania.
- Opcjonalny koszt serwisu zapisuje się transakcyjnie w `cost_entry`, raz.
  Jeśli wydatek już istnieje, pozostaw koszt wykonania pusty. Pozostałe rachunki
  nadal obsługuje istniejący rejestr kosztów. Zasady finansowe i tankowania bez zmian.
- DZISIAJ i ncLEANEK czytają te same plany; archiwizacja, wykonanie i odroczenie
  nie tworzą niezależnych zadań. Mechanizm pomocy pozostaje tylko do odczytu.
- Migracja V21 dodaje pola/tabele; poprzednie migracje i historyczne ceny bez zmian.

Poza etapem: automatyczne naliczanie ze zleceń, pełny magazyn/montaż części,
zdjęcia/dokumenty sprzętu oraz obliczenia i raportowanie amortyzacji.
Dotychczasowe zdjęcia i dokumenty pozostają w swoich modułach.

## Weryfikacja

Java 17, izolowane kontenery PostgreSQL; bez połączenia z bazą nclean.pl.
`./mvnw verify`: 199 testów (68 jednostkowych/HTTP/architektury oraz 131 integracyjnych),
0 błędów, 0 niepowodzeń, 0 pominięć. Dodatkowo powtórzono 5 testów
EquipmentPostgresIT po doprecyzowaniu połączenia ze schematem V20.
Chromium na działającym CRM pod `/` i `/crm`: tworzenie kartoteki/licznika/planu,
brak połączenia i zachowanie pól, utrata odpowiedzi po zatwierdzonym zapisie
oraz ponowienie bez drugiego kosztu/historii, zgodność DZISIAJ/pomocy,
archiwizacja/przywrócenie, CSRF, Escape i 24 widoki w 360/390/768/1440 px: PASS.
Bez poziomego przewijania i błędów JavaScript. Składnia zmienionego JS: PASS.

Powtórzenie testów przeglądarkowych na nowej syntetycznej instalacji:
`python scripts/verify-equipment-live.py --work-dir /tmp/nclean-equipment-qa --java /path/to/java17`.
Wymaga zbudowanego JAR, Docker, Playwright/Chromium i Python bcrypt.
Runner tworzy wyłącznie własny kontener QA na tmpfs; nie korzysta z Compose,
`.local`, istniejących wolumenów ani haseł instalacji.

Docker PC: wymagana przebudowa obrazu przy aktualizacji aplikacji, nie sam restart.
Konfiguracja Compose i podłączenie bazy nie były zmieniane.
