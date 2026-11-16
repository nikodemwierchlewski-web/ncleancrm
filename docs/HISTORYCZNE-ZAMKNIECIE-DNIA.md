# Wykonanie starego zlecenia i zamknięcie dnia

Zgłoszenie z 16.11.2026: zlecenie z 10.10.2026 nie pozwalało zamknąć dnia,
dopóki właściciel nie odnalazł pól powodów braku zdjęć. Anulowanie działało,
ponieważ anulowana wizyta nie wymaga dokumentacji wykonanej usługi.

Zasady zapisu pozostają bez zmian:

- Wykonanie można zapisać z niepełną dokumentacją i wrócić do niego później.
- Zamknięcie dnia wymaga kompletnych danych jego zleceń. Każde zdjęcie przed/po
  może zostać zastąpione jawnym, wpisanym przez właściciela powodem braku.
- Przy dawnej realizacji można podać rzeczywisty powód braku archiwalnych
  zdjęć. Formularz nie wymyśla zdjęć, powodów, pomiarów ani wpłat.
- Nieznane zużycie chemii pozostaje brakiem danych; zero oznacza rzeczywisty
  brak użycia. Nie trzeba zamykać niepełnego archiwum przez anulowanie usług.

Poprawki interfejsu:

- Pola powodów rozwijają się od razu, jeśli brakuje zdjęcia i powodu.
- Link w formularzu rozwija sekcję zdjęć i kieruje do wyboru pliku.
- Przy „Zamknij dzień” widać braki pierwszego niekompletnego zlecenia oraz
  link do jego formularza. Dalsze braki pojawiają się po zapisaniu poprzednich.
- Podane powody można poprawić; po ich zapisaniu sekcja jest zwinięta.

Weryfikacja obejmuje prawdziwe logowanie i HTTP na PostgreSQL, uzupełnienie
dawnej wizyty bez tworzenia fikcyjnego klienta, odrzucenie niepełnego dnia,
zamknięcie po podaniu obu powodów oraz trwałość po restarcie. Test Chromium
przechodzi przez ten sam scenariusz na widokach aplikacji przy 360, 390, 768
i 1440 px, sprawdzając też linki i zachowanie danych po odrzuconym zamknięciu.

Test przeglądarkowy na odizolowanym schemacie:

```sh
python3 scripts/verify-browser.py --owner-file /prywatna/sciezka/owner.json --historical-closeout-only
```

Wymaga zbudowanego JAR-a, lokalnego PostgreSQL Compose i prywatnych danych
logowania deweloperskiego. Nie korzysta z bazy produkcyjnej ani zwykłego schematu.
Pełny test aplikacji: `./mvnw verify`. Raporty XML są w `target/`.

Wynik pełnej weryfikacji z 16.11.2026: **186 testów (66 jednostkowych/MVC/
architektury i 120 integracyjnych PostgreSQL), 0 błędów i 0 pominięć**.
Kontrola adresów `node scripts/test-web-boundaries.cjs` również przeszła.
Próba Chromium opisana wyżej również zakończyła się **PASS**, bez błędów
JavaScript, w tym przejście linkiem do braków i pola dotykowe minimum 44 px.

Aktualizacja instalacji PC: [DOCKER-PC.md](DOCKER-PC.md). Brak nowej migracji;
istniejące dane i powody nie są automatycznie zmieniane. Poprawka pojawia się
na komputerze dopiero po aktualizacji i uruchomieniu nowego wydania.
