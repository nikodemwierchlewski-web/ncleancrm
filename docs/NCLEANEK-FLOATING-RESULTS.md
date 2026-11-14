# Testy i podgląd ruchomego ncLEANKA — 14.11.2026

Zamiast elementu w nagłówku ncLEANEK jest samodzielną postacią nad interfejsem. Kliknięcie od razu uruchamia podpowiedzi aktualnego ekranu lub otwartego formularza. Przeciąganie podnosi go za rękę; poruszają się nogi i zmienia przechylenie, po odłożeniu następuje krótkie lądowanie. W trybie ograniczonego ruchu poza jest statyczna, a sterowanie nadal działa.

W panelu **Opcje** pozostają przypomnienia, wyciszenie, ukrywanie, reset położenia i przeniesienie na drugą stronę. Ustawienia zawierają te same polecenia. Strzałki przesuwają postać, Home przywraca pozycję; Enter/Spacja otwierają pomoc. Przy krawędzi działa lekkie przyciąganie. Położenie zapisujemy jako dwie liczby 0–1, bez danych klientów i bez zapisywania szkiców.

Automatyczne dopasowanie nie nadpisuje preferencji położenia. Ludzik omija pola, przyciski i panel pomocy. Przy małej ilości miejsca zmniejsza się; przy braku bezpiecznego miejsca czasowo znika, żeby pozostawić dostępny zapis. Nie dodano migracji ani modułów biznesowych, nie zmieniono OWNER/CSRF, nie połączono produkcji.

## Weryfikacja

- Pełne Maven Wrapper `verify`: **36 unit/MVC + 85 integracyjnych PostgreSQL/Testcontainers = 121**, zero failures/errors/skipped. Ponowione 14.11.2026 po wznowieniu środowiska.
- Prawdziwa aplikacja na osobnym schemacie PostgreSQL: przeciąganie myszą, próg ruchu 8px, podniesiona ręka, upuszczenie, anulowanie wskaźnika, obsługa klawiaturą, reset i pamięć położenia między ekranami. Kliknięcie jest rozdzielone od przeciągania, a dotknięcie po upuszczeniu otwiera pomoc dokładnie raz.
- 28 kombinacji siedmiu ekranów i szerokości 360/390/768/1440 px: jedna postać, granice widocznego ekranu, brak poziomego przewijania. Dodatkowo krótki viewport 390×430, dostępność przycisku zapisu oraz formularz z zachowanym szkicem.
- Chromium z emulacją dotyku przez CDP: przeciągnięcie, brak samoczynnego otwarcia pomocy i następne świadome tapnięcie. To test emulacji, nie fizycznego telefonu.
- Osiem zleceń w teście pozostało bez zmian. Kliknięcia, ruch, samouczek i zmiana preferencji nie tworzyły zapisów biznesowych.
- Dotychczasowy scenariusz rzeczywistej aplikacji przeszedł: rezerwacja nowa/powracająca, dwie kategorie i uzgodniona cena, historia, zdjęcia/chemia, koszty/notatki, zamknięcie dnia i restart.
- axe-core 4.10.3: 16 kombinacji postaci, samouczka, opcji i ustawień, zero zgłoszonych naruszeń. Raport zachowuje incomplete; nie oznacza pełnego odbioru WCAG.
- Podgląd: przeciąganie i pomoc, pamięć położenia między ekranami, reset, formularz z zachowanymi polami i próbny zapis, Klienci/Ustawienia oraz zagnieżdżony sandbox. Bez błędów JavaScript w zaliczonych próbach.

Wykryte i poprawione podczas testów: opóźnione przeniesienie postaci do otwartego dialogu, blokowanie następnego kliknięcia po dragu oraz brak syntetycznego click w dotykowym Chromium po użyciu pointer capture. Dotyk ma teraz własną, pojedynczą aktywację; następujący po niej click jest pomijany. Dopasowanie pozycji uwzględnia dokładne krawędzie przeszkód i rozmiar rzeczywistego tekstu nagłówka.

Wyniki maszynowe: `docs/audit/ncleanek-floating-*.json`. Brak prób na fizycznym Androidzie/iOS i rzeczywistej klawiaturze ekranowej; krótki viewport nie zastępuje takiego testu. Nie wykonywano pełnego ręcznego testu czytnika ekranu ani odbioru starszych przeglądarek bez Popover API.

## Podgląd i powtórzenie testu

Aktualne pliki: `preview/ncleanek-floating.html` (jeden plik) oraz `preview/ncleanek-floating-demo.zip` (wypakuj i otwórz index.html). Dane syntetyczne, zapis symulowany w pamięci; nie wpisuj prawdziwych danych klientów. To eksport 62 widoków aplikacji z jej stylami i skryptami, z lokalnymi adapterami zapisu. Preferencje położenia przechodzą między ekranami demonstracji; odświeżenie resetuje sesję.

Podgląd pliku zależy od klienta. Pobrany HTML należy otworzyć w aktualnej przeglądarce obsługującej JavaScript; sam podgląd pliku w systemowym menedżerze telefonu może nie uruchamiać skryptów. Nie uruchomiono nowego hostingu. Dostępność panelu Codexa nie została potwierdzona — środowisko zgłasza capabilities: [].

Test eksportu można powtórzyć lokalnie (Python, Playwright i Chromium):

```bash
python -m http.server 8174 --bind 127.0.0.1 --directory preview
# W drugim terminalu:
python scripts/verify-floating-preview.py \
  --url http://127.0.0.1:8174/ncleanek-floating.html \
  --html preview/ncleanek-floating.html \
  --output /tmp/ncleanek-floating-preview-test
```

Gałąź: `feature/ncleanek-floating`, baza: `feature/ncleanek-guide@d99ea42`. Na prośbę właściciela nie tworzono Pull Requesta. Nie scalano do main i nie wdrażano aplikacji.

## Potwierdzenie dostarczenia

Gałąź i kod wysłano do GitHuba. Pobrano ponownie ZIP i pojedynczy HTML; ich SHA-256 są zgodne z `preview/build.json`, a index.html w ZIP jest identyczny z pojedynczym plikiem. Pobrany HTML przeszedł ponowne testy przeglądarkowe: przeciąganie, samouczek, pamięć/reset położenia, formularz i próbny zapis oraz zagnieżdżony sandbox, bez błędów JS. [Dowód](audit/ncleanek-floating-delivery.json).

[ZIP z GitHuba](https://github.com/nikodemwierchlewski-web/ncleancrm/raw/refs/heads/feature/ncleanek-floating/preview/ncleanek-floating-demo.zip). [Pojedynczy HTML](../preview/ncleanek-floating.html). Pull Request nie był tworzony.
