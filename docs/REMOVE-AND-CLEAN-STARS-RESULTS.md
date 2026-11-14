# Usuwanie pomyłek i niebieskie gwiazdki — 14.11.2026

[Zastosowany prompt](REMOVE-AND-CLEAN-STARS-PROMPT.md).

## Obsługa

- Kalendarz → wpis zlecenia → **Usuń**. Przycisk jest też w szczegółach wykonania.
  Potwierdzenie pokazuje nazwę. „Zostaw” i Escape rezygnują bez zapisu.
- Po sukcesie pojawia się **Cofnij usunięcie**. Później zlecenie przywrócisz
  w **Zlecenia → Usunięte zlecenia**; link jest w panelu dnia kalendarza.
  Lista obejmuje ostatnie 200 usuniętych zleceń; baza zachowuje także starsze.
- **Cennik**: po rozwinięciu kategorii są oddzielne przyciski przy kategorii
  i usługach; przy środkach chemicznych jest analogiczny przycisk. Na dole
  „Usunięte elementy cennika” pozwalają przywrócić wpis.
- **Koszty i inwestycje**: widoczne **Usuń** w wierszu. Korzysta z istniejącego
  anulowania, więc wydatek przestaje być naliczany, a rachunek i audyt pozostają.
  „Pokaż usunięte / anulowane” udostępnia **Przywróć**. Dotyczy także tankowań;
  te same przyciski są w historii dziennika paliwa.
- **Sprzęt**: **Archiwizuj sprzęt** usuwa go z aktywnych kart, zachowując historię
  i koszt zakupu. Zarchiwizowany sprzęt można przywrócić. Usuwanie pozycji kosztów
  miesięcznych działa nadal w dotychczasowym formularzu z zapisem miesiąca.

## Zachowanie danych

Usuwanie jest odwracalne. V15 dodaje techniczną datę usunięcia i audyt; nie edytuje
V1–V14, nie kasuje cen, klientów, pozycji, zdjęć, chemii, płatności ani relacji.
Usunięte zlecenia znikają z kalendarza, list, wyszukiwania, przychodu, pomiarów
w statystykach, sugestii zużycia i przypomnień. Niezależne rachunki, koszty stałe
oraz fizyczne przepływy wpłat/korekt pozostają w swojej ewidencji.

Przed usunięciem zlecenia z wpłatą trzeba ją skorygować; z aktywnym timerem —
zatrzymać pomiar. Kontrola wersji i wspólne blokady zapisu chronią przed konfliktem
z innym oknem. Ponowne żądanie nie dubluje audytu ani nie odtwarza usuniętej
rezerwacji przez stary token dodawania. Błąd pozostawia otwarte potwierdzenie
oraz oryginalny formularz. Przywracanie tankowania powtarza kontrolę licznika,
kwoty i rachunku; niespójne przywrócenie wycofuje się transakcyjnie.

Usunięcie kategorii ukrywa ofertę jej usług w nowej rezerwacji; przywrócenie nie
przestawia aktywności jej usług. Backend również odrzuca ukrytą usługę/kategorię.
Środka używanego w standardzie lub prognozie zaplanowanej wizyty nie usuwamy
bez zmiany powiązań. Wycofany środek nadal jest dostępny w rozliczeniu wizyty,
która już go używała; korekta ilości zachowuje historyczną cenę.

Dodanie pustej kolumny `removed_at` nie unieważnia dawnych zamknięć dnia.
Usunięcie/przywrócenie rzeczywistego zlecenia zmienia zawartość dnia i wymaga
ponownego sprawdzenia. OWNER, logowanie i CSRF pozostają włączone.

## Animacje i kolizje

Po udanym zapisie wykonania pojawia się maksymalnie osiem niebieskich/błękitnych
pixelowych gwiazdek (700–840 ms, sprzątanie warstwy po 900 ms). Nie przejmują
kliknięć, nie zmieniają układu, nie mają dźwięku. Nowy efekt zastępuje poprzedni,
zamiast tworzyć kolejne warstwy. Błąd zapisu, usuwanie i anulowanie nie świętują.
Osiągnięcia czekają na zakończenie efektu, a chmurka ncLEANKA ustępuje komunikatom.
Otwarty dialog/pomoc, ukryta karta i ograniczenie ruchu hamują efekty.

Odtworzono i poprawiono kolizję chmurki z „Zapisz wykonanie”: po opuszczeniu pola
chmurka mogła wrócić przed zakończeniem kliknięcia. Teraz ustępuje przyciskom,
linkom i polom na podstawie stałego prostokąta, bez migania i przestawiania
ludzika. Przypomnienie pozostaje dostępne w jego panelu. ncLEANEK ma nadal
stały rozmiar i przesuwa się wyłącznie lewo/prawo przy dolnej krawędzi.

## Rzeczywiste wyniki

- Maven Wrapper `verify`: **38 unit/MVC + 93 PostgreSQL/Testcontainers = 131**;
  0 failures, 0 errors, 0 skipped. `BUILD SUCCESS`.
- Nowe testy: usunięcie/przywrócenie i restart, spójność list/statystyk/szukania,
  historyczne ceny, wpłaty i timer, wersje/duplikaty, cennik i blokada ukrytej
  oferty, chemia historyczna, koszty/tankowania, archiwizacja sprzętu bez zmiany
  inwestycji, podpis dnia po migracji V14→V15, OWNER/CSRF i potwierdzenie.
- Prawdziwa aplikacja na izolowanym schemacie lokalnego PostgreSQL, Chromium:
  360/390/768/1440 px; rezygnacja, usunięcie i cofnięcie zlecenia, usługi,
  kategorii, chemii i kosztu, zachowane pozycje zlecenia, konflikt wersji,
  wymuszone 503 i udane ponowienie; bez błędów JavaScript.
- Sprawdzono maksymalnie jedną warstwę ośmiu gwiazdek, przepuszczanie kliknięć,
  sprzątanie, ponowne wyzwolenie, reduced-motion i blokadę podczas dialogu.
- Pełny scenariusz prawdziwej aplikacji przeszedł: nowy/powracający klient,
  dwie kategorie, własna cena i adres, chemia, zdjęcia/czas, notatki/cele,
  koszty, retry/konflikty, zamknięcie dnia, restart i ponowne otwarcie po korekcie.
  Przechwycono **63 widoki** do demonstracji. Zapis wykonania po kolizji chmurki
  potwierdzono rzeczywistym żądaniem i przekierowaniem.
- HTML: cztery szerokości, usuwanie/cofanie także nowego zlecenia, ukryta oferta
  kategorii w formularzu, koszty, gwiazdki/reduced-motion oraz zagnieżdżony sandbox.
  Dodatkowo przeszły wcześniejsze scenariusze podglądu i przypomnień ncLEANKA.
- axe-core 4.10.3 po zakończeniu przejścia dialogu: cztery szerokości, zero
  zgłoszonych naruszeń w samym potwierdzeniu. To nie audyt WCAG całej aplikacji;
  raport zachowuje incomplete. Nie badano fizycznego Androida/iOS ani czytnika.

Raporty: `docs/audit/removal-clean-stars-*.json`. Powtarzalny test demonstracji:

```sh
python scripts/verify-removal-preview.py --url http://127.0.0.1:8184 --html preview/nclean-removal-stars.html
```

Adres powyżej służy testowi po lokalnym zaserwowaniu pliku, nie udostępnieniu
chmurowego localhost na telefonie użytkownika. Faktyczne linki do pobrania są
w `preview/README.md`. DEMO ma syntetyczne dane i zapis w pamięci; odświeżenie
resetuje sesję. Nie łączy się z bazą. Prawdziwe operacje zweryfikowano oddzielnie
na PostgreSQL. Panel Codexa nie jest potwierdzonym hostingiem aplikacji.

Zmiany na `feature/ncleanek-floating`, bez Pull Requesta, scalenia i produkcji.
