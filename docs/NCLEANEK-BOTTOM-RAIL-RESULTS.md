# ncLEANEK: dolna krawędź i zamknięcie dnia — 14.11.2026

[Zastosowany prompt](NCLEANEK-BOTTOM-RAIL-PROMPT.md).

## Poprawka

Odtworzono zgłoszenie w dotychczasowym podglądzie: po upuszczeniu postać zmieniła
pozycję w pionie o ok. 20 px, a po przewijaniu rozmiar z 76×112 na 44×60 px.
Przyczyną był mechanizm wyszukiwania wolnych miejsc, kompaktowania i przyciągania
oraz zmiana punktu złapania na współrzędne podniesionej ręki.

Usunięto automatyczny wybór miejsca i rozmiaru. Domyślnie prawy dolny róg,
stałe 44×60 px, przeciąganie tylko lewo/prawo, nad dolną nawigacją. Pionowy ruch
palca nie przesuwa ludzika. Wskaźnik zachowuje punkt złapania; po zwolnieniu nie
ma przyciągania. Przewijanie i zmiany treści nie przestawiają postaci.

Zachowano normalizowaną preferencję `nclean.puppet.position.v1`; używamy `rx`,
a `ry=1` oznacza dolną krawędź. Poprzednie preferencje nie są usuwane. Lewo/prawo
przesuwa, Home resetuje do prawego rogu, Enter/Spacja otwiera pomoc. W kolizji
z zapisem lub aktywnym polem postać blednie i przepuszcza dotyk bez zmiany miejsca.

Mała chmurka „Dokończ dzień” prowadzi do istniejącego podsumowania i pierwszej
wizyty z brakami. × odkłada na 20 minut; Opcje pozwalają wyciszyć na dziś.
Chmurka nie otwiera panelu ani nie zabiera fokusu; ustępuje w formularzu,
podczas przeciągania i pomocy. Reguły `GuideService`/`DayReviewService` pozostają:
dzisiejszy dzień od 18:00 Europe/Warsaw lub po rozstrzygnięciu wszystkich wizyt,
wcześniejsze niezakończone dni; bez pustych/przyszłych/zamkniętych dni.
Nie ma push/SMS, automatycznego zapisu ani zamykania dnia.

## Wyniki

- Pełny Maven Wrapper `verify`: 36 unit/MVC + 85 integracyjnych PostgreSQL/
  Testcontainers = **121**, zero failures/errors/skipped. Po korekcie semantyki
  HTML chmurki wykonano ponowne pakowanie i testy przeglądarkowe.
- Rzeczywisty Spring Boot na izolowanym schemacie lokalnego PostgreSQL:
  **27 gestów** z kontrolą dokładnego położenia/stałego rozmiaru, ruchy pionowe,
  przewijanie, zmiany treści, anulowanie i obcy pointer; strzałki lewo/prawo,
  brak ruchu po góra/dół, Home i powrót po zmianie rozmiaru ekranu; **28 kombinacji**
  siedmiu ekranów i szerokości 360/390/768/1440 px, bez poziomego przewijania.
- Formularz zachowuje szkic; przycisk zapisu otrzymuje kliknięcie. Dialog nie
  zmienia wybranej pozycji. Trusted CDP touch: przeciąganie i kolejne tapnięcie,
  brak niechcianej pomocy; reduced-motion respektowane.
- Pusta rzeczywista baza: brak chmurki. Osiem nieuzupełnionych wizyt z wczoraj:
  rzeczywisty endpoint wskazuje osiem braków i właściwy dzień. Przetestowano
  odłożenie/wygaśnięcie, wyciszenie na dziś i trwałość preferencji między trasami,
  ukrywanie chmurki podczas edycji i pomocy, otwarcie właściwego podsumowania.
  Wszystkie osiem rekordów zleceń pozostało bez zmian.
- Te same scenariusze przeszły w syntetycznym HTML. Dodatkowy test podglądu
  obejmuje próbny zapis zlecenia, Klientów/Ustawienia i zagnieżdżony sandbox.
- axe-core 4.10.3 przy czterech szerokościach: zero naruszeń w samym pomocniku.
  Poprawiono zagnieżdżony landmark chmurki. Pełny widok ma istniejący problem
  kontrastu tekstu kart wykonanych/anulowanych wizyt, odtworzony również w
  poprzednim pobranym HTML. Nie ogłaszamy zgodności całego ekranu z WCAG.
  Raport zachowuje pozycje incomplete.

Nie testowano fizycznego Androida/iOS, rzeczywistej klawiatury telefonu ani pełnej
obsługi czytnikiem ekranu. Emulacja dotyku nie zastępuje odbioru na urządzeniu.

Raporty: `docs/audit/ncleanek-bottom-rail-*.json`. Skrypty regresji:
`scripts/verify-ncleanek-drag.py` i `scripts/verify-ncleanek-reminders.py`.
Przykład z podglądem serwowanym lokalnie:

```sh
python scripts/verify-ncleanek-drag.py --url http://127.0.0.1:8174 --embedded
python scripts/verify-ncleanek-reminders.py --url http://127.0.0.1:8174 --embedded
```

## Podgląd i zapis

`preview/ncleanek-floating.html` i ZIP zawierają poprawione moduły tej aplikacji,
62 syntetyczne widoki oraz zapis symulowany w pamięci. Nie łączą się z bazą.
Pobrany HTML sprawdzamy przez HTTP i w ramce sandbox; bez gwarancji wykonywania
JavaScript w telefonowym menedżerze plików. Panel Codexa nie zgłasza możliwości
udostępnienia przeglądarki; cloud Chromium blokuje bezpośrednie `file://`.
Nie wystawiono publicznego hostingu. Nie zmieniono modeli biznesowych, migracji,
OWNER ani CSRF, nie podłączono produkcji. Kontynuacja gałęzi
`feature/ncleanek-floating`; bez Pull Requesta.

Pobrano obie paczki z GitHuba z commitu `560c5f6`, porównano SHA-256 i zawartość
ZIP. Pobrany HTML przeszedł test przeciągania/pomocy/zapisu próbnego oraz
zagnieżdżonego sandboxu, a także przypomnień/odłożenia/wyciszenia i odnośnika.
Próba otwarcia nowego pliku przez `open_in_codex` nie zwróciła potwierdzenia;
nie uznajemy tego za działający panel. [Dowód dostarczenia](audit/ncleanek-bottom-rail-delivery.json).
