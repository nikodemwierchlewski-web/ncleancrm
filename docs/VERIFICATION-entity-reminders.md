# Weryfikacja: przypomnienia przy rekordach i koło Dzisiaj

Data: 15.11.2026. Gałąź: `feat/entity-reminders-pixel-summary`, oparta na ostatniej pełnej implementacji `feat/today-planning-fuel-reminders` (`26c5732`). Bez wdrożenia i bez dostępu do produkcji.

## Wyniki prawdziwej aplikacji

- Maven Wrapper `verify`: **185 testów, 0 błędów, 0 pominiętych**: 66 testów jednostkowych/MVC/architektury oraz 119 integracyjnych na PostgreSQL 17 przez Testcontainers. Wynik po poprawkach zapytania SQL, precyzji czasu i dostosowaniu testów migracji do nowej pustej kolumny `due_time`.
- Po poprawce semantyki nagłówka ponowne `package`: 66 testów przeszło, JAR zbudowany. Ostatnia poprawka CSS rozmiaru checkboxów również została spakowana i sprawdzona w przeglądarce.
- Prawdziwy Spring Boot + izolowane schematy PostgreSQL: UI zlecenia (30 min / 3 godz. / 2 dni, kilka przypomnień), zachowanie pól po odpowiedzi 503 i ponowienie, cel z datą/godziną i konkretnym przypomnieniem, odrzucenie braku terminu bez utraty formularza, edycja/dezaktywacja, bezpośrednie otwarcie notatki, wykres i zakresy własne.
- Regresje przeglądarki: 8 wizyt, nawigacja, miesięczny kalendarz i mobilny panel, dwa obszary, zmiana rozmiaru/fokus, notatki, cele, cennik, zdjęcia, koszty, tankowanie i ponowienie po błędach. **Bez błędów JavaScript**. Widoki **360, 390, 768 i 1440 px**, bez poziomego przewijania.
- Pod `/crm`: zapis zlecenia i notatki, odczyt i potwierdzenie powiadomienia, odrzucenie POST bez CSRF (403), własny zakres i cztery szerokości. **Restart zachował definicję i stan READ**; przyszłe przypomnienie notatki pozostało osobnym PENDING.
- axe: **73 skany, 0 automatycznie wykrytych naruszeń** po poprawieniu kolejności nagłówków i rozmiaru checkboxów. Automatyczny skan nie jest pełną certyfikacją dostępności.
- `node scripts/test-web-boundaries.cjs`: PASS (ścieżki główne i `/crm`, linki zewnętrzne, zakres service workera). Sprawdzenie składni nowych skryptów JS i `git diff --check`: PASS.

Integracje obejmują zmianę terminu i anulowanie wizyty, oddzielenie odczytu od zakończenia zlecenia, idempotentny zapis, dwa równoległe potwierdzenia DISPLAYED, odroczenie i odrzucenie starej wersji, atomowość wpisu/terminu/przypomnień, restart, konflikt edycji, próbę użycia UUID innego rekordu, migrację V18→V19 bez utraty poprzednich cykli i historii oraz uzgodnienie sum częściowego okresu z wartościami dziennymi.

## Granice

Powiadomienia są na razie wewnątrz otwartej aplikacji. Nie uruchomiono SMS, push ani aplikacji Android; kontrakt i potrzebny dalszy etap opisano w `REMINDERS-ANDROID-CONTRACT.md`. Testy telefonu wykonano w Chromium przy odpowiednich szerokościach, bez fizycznego urządzenia.

Aktualna sesja nie udostępnia działającego mechanizmu otwarcia serwera jako podglądu Codexa. Osobny HTML jest demonstracją z syntetycznymi danymi opartą na przechwyconych widokach serwera oraz tych samych modułach UI. Jego zapisy są symulowane w karcie; eksport JSON próby nie zastępuje PostgreSQL ani backupu zdjęć.

## Podgląd HTML

`preview/nclean-contextual-reminders.html` zawiera 81 przechwyconych widoków prawdziwego serwera, te same moduły UI i jawnie oddzielony transport DEMO. `scripts/verify-entity-preview.py` przeszedł: 360/390/768/1440 px, koło do dnia/tydzień/własne daty, zapis i ponowne otwarcie przypomnień zlecenia oraz nowej notatki z terminem, kalendarz 7 kolumn i dwa zagnieżdżone sandboxy o nieprzezroczystym pochodzeniu. Brak błędów JavaScript. ZIP sprawdzono przez `ZipFile.testzip()`; plik HTML pobiera się z lokalnego serwera HTTP (200). Nie zweryfikowano działania natywnego panelu podglądu Codexa — brak dostępnego narzędzia przeglądarki/otwarcia tego panelu w tej sesji.
