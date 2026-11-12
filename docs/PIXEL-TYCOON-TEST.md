# ncLEAN Pixel Tycoon — eksperyment wizualny

**Wersja do oceny, bez akceptacji jako domyślny wygląd CRM.** Osobna gałąź `experiment/pixel-tycoon`, oparta na naprawionej wersji `fix/whole-crm-consistency` (`44effb9`). Nie scalano do main i nie wdrażano na produkcję.

## Jak włączyć i wrócić

Normalne uruchomienie zachowuje klasyczny interfejs. Motyw jest domyślnie wyłączony przez `crm.ui.pixel-tycoon=false`. Profil `tycoon` ustawia wyłącznie tę opcję; nie nadpisuje bazy, logowania ani zabezpieczeń.

Windows, po wcześniejszej lokalnej inicjalizacji:

```powershell
.\scripts\Start-Local.ps1 -PixelTycoon
```

Powrót: zatrzymaj aplikację przez Ctrl+C i uruchom:

```powershell
.\scripts\Start-Local.ps1
```

Przy ręcznym uruchomieniu użyj profili `local,tycoon`, zamiast samego `local`. Nie nadpisuj JAR działającej JVM. Nie trzeba zmieniać bazy, usuwać wolumenu, cofać migracji ani ponownie wpisywać danych. Wszystkie dane, rachunki i zlecenia są współdzielone przez oba warianty, bo motyw zmienia tylko prezentację.

Można również wrócić do gałęzi `fix/whole-crm-consistency`. Gałąź eksperymentalna zachowuje jej historię i funkcje.

## Wygląd i ergonomia

- Granatowy pasek nawigacji, kremowa plansza, niebieskie i miętowe akcenty.
- Kafelki z bocznym cieniem, schodkowe detale, wciskane przyciski i kompaktowe statusy.
- Lokalna czcionka **Pixelify Sans** tylko do krótkich nagłówków/brandu; dane, telefony, ceny i formularze pozostają w Montserrat. Sprawdzono polskie znaki. Licencja SIL OFL jest dołączona przy pliku fontu.
- Własna lekka grafika SVG: izometryczna baza, samochód, kanapa, ekstraktor, chemia i rośliny. Ikony w ustawieniach pochodzą z tej samej grafiki. Bez silnika 3D, WebGL i zewnętrznych bibliotek.
- Scenka bazy jest dekoracyjna i znajduje się **po podstawowych liczbach** dashboardu. Na telefonie zajmuje mniej miejsca. Nie przechwytuje kliknięć i nie tworzy fikcyjnych wskaźników.
- Kalendarz zachowuje miesiąc z siedmioma kolumnami i panel dnia na komputerze, pasek tygodnia/listę na telefonie oraz własne kolory obszarów. Karty zleceń pozostają czytelne na osobnej powierzchni.
- Suma, zapis, kompletność dnia i statusy nadal pochodzą z istniejących danych. Nie dodano punktów, rankingów, walut ani nowych kroków pracy.
- CSS respektuje `prefers-reduced-motion`, utrzymuje widoczny fokus i duże kontrolki. JavaScript motywu dodaje jedynie krótkie potwierdzenie do istniejących komunikatów sukcesu. Nie przejmuje zapisu formularzy.

## Co zmieniono technicznie

`ThemeModel` udostępnia opcjonalną flagę szablonom. Pełne strony dołączają warunkowo fragment `theme :: assets`, zawierający CSS i niewielki skrypt. Przy wyłączonej fladze zasoby motywu, odznaka testowa i scenka nie występują w HTML. Fragmenty formularzy nadal obsługuje dotychczasowa aplikacja.

Nie zmieniono modeli biznesowych, obliczeń, endpointów zapisu, migracji V1–V13 ani reguł OWNER/CSRF. Font i grafiki są serwowane lokalnie; aplikacja nie pobiera ich z CDN.

## Weryfikacja — 12.11.2026

**Maven Wrapper `clean verify`: 97 testów — 26 jednostkowych/MVC i 71 integracyjnych PostgreSQL/Testcontainers. Zero błędów, niepowodzeń i pominiętych.**

Nowy test uruchamia klasyczny wariant, następnie profil `tycoon`, sprawdza strony głównych modułów i wymaganie CSRF, a następnie restartuje wariant klasyczny. Pełny rekord istniejącego zlecenia pozostaje identyczny przez wszystkie przełączenia. Domyślne uruchomienie nie dołącza motywu.

Rzeczywista aplikacja z profilem `local,tycoon`, osobnymi tymczasowymi schematami PostgreSQL i kopią JAR poza target:

- nowy i powracający klient, usługi z dwóch kategorii, cena/adres wizyty, osiem zleceń i zmiana terminu;
- tworzenie/edycja kategorii, usługi, chemii i standardu; zachowanie historycznej ceny/prognozy po zmianie katalogu;
- obszary, nazwy/HEX, kilka oznaczeń, plan tygodnia i historia po dezaktywacji;
- zdjęcia/czas, wyszukiwanie/ponowny kontakt, wykonanie, rzeczywista chemia, zamknięcie dnia i notatki/cele;
- paliwo, rachunki, wydatki i sprzęt, anulowanie, powiązanie miesięczne bez podwójnego kosztu;
- błędy HTTP503 z ponowieniem, konflikty dwóch kart i trwałość danych po restarcie;
- szerokości **360, 390, 768, 1440 px** bez poziomego przewijania; dostępność zapisu przy mniejszej wysokości formularza. Brak błędów JS w wykonanych scenariuszach.

To próby Chromium na Linux. Windows/PowerShell, fizyczna klawiatura/aparat telefonu i wydajność na rzeczywistym słabszym urządzeniu wymagają osobnej oceny. Nie uruchamiano produkcyjnej bazy nclean.pl.

## Podgląd do oceny

Osobny plik `ncLEAN-PixelTycoon-TEST.html` bazuje na widokach rzeczywistej aplikacji z motywem i jednym zestawie syntetycznych danych. Zawiera przycisk **„Wygląd klasyczny” / „Pixel Tycoon”** do porównania bez kasowania formularza. Wybór wyglądu trwa tylko przez otwarcie pliku.

Osobna próba HTML potwierdziła przełączanie wyglądu bez utraty formularza i zachowanie wyboru przy nawigacji, dodanie/edycję zlecenia, obszary, klientów, koszty, sprzęt i notatki. Sprawdzono cztery szerokości i zapis w zagnieżdżonym iframe z `allow-scripts`, bez błędów JS. Dowody: [audit/pixel-tycoon-evidence.json](audit/pixel-tycoon-evidence.json).

Zapis w HTML jest symulowany w pamięci; podsumowania są przykładowe. Nie wpisuj tam prawdziwych danych klientów. Właściwy backend i PostgreSQL sprawdzono osobno. Wcześniejszy plik klasycznego podglądu pozostaje zachowany. Zatwierdzenie tego stylu jest osobną decyzją użytkownika.
