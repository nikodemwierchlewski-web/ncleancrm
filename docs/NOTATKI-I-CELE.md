# Notatki i cele

Gałąź bazowa: `audit/release-readiness` (`ebd3c9b`), zawierająca ostatnie niescalone funkcje i poprawki audytu. To część istniejącego CRM, nie osobna aplikacja.

## Szybka obsługa

- **Ustawienia → Notatki i cele** prowadzi do jednego ekranu. Nie dodano przycisku do dolnego paska telefonu.
- **+ Wpis**: domyślnie notatka. Sam tytuł wystarcza. Treść, termin i przypięcie są opcjonalne.
- **Zadanie** ma checkbox do szybkiego ukończenia. W „Ukończonych” można je odznaczyć i przywrócić do aktywnych.
- **Cel** może mieć do 30 kroków. Licznik pokazuje np. „1 z 2 kroków”. Odhaczenie wszystkich nie kończy celu; robi to świadoma akcja „Cel osiągnięty”. Cel bez kroków nie pokazuje sztucznego procentu.
- Wpis otwiera się po kliknięciu tytułu. Pełna treść dostępna jest w edycji; na liście pokazujemy krótki podgląd.
- **Przypnij / Odepnij** zmienia widoczność w dashboardzie, nie usuwa wpisu.
- **Archiwizuj** zachowuje treść, kroki i ukończenie. Przywrócenie ukończonego zadania z archiwum pozostawia je w „Ukończonych”, dopóki nie zostanie świadomie wznowione.
- Dostępne są „Aktywne”, „Ukończone”, „Archiwum”, wyszukiwanie po tytule/treści i filtr przypiętych. Lista ma strony po 50 wpisów.

## Dashboard

Zwarta sekcja „Przypięte” znajduje się po podstawowych liczbach, przed strukturą kosztów. Pokazuje najwyżej trzy aktywne wpisy i link do pełnej listy. Najpierw sortuje po terminie, następnie od ostatnio przypiętych. Nie przypina niczego automatycznie.

„+ Notatka” otwiera krótki formularz bez opuszczania dashboardu, z zaznaczonym przypięciem, które można odznaczyć. Zadanie można odhaczyć bez otwierania szczegółów. Ukończone i zarchiwizowane wpisy znikają z tej sekcji, ale pozostają w bazie.

Przypięcia są wspólne dla dnia, tygodnia i miesiąca, niezależnie od oglądanej daty. Termin jest datą lokalną; „Po terminie” liczymy względem Europe/Warsaw tylko dla aktywnych zadań i celów. Data notatki nie oznacza zaległego zadania.

**Żaden wpis ani cel nie wpływa na kompletność zlecenia, finanse ani zamknięcie dnia.**

## Dane i bezpieczeństwo

Flyway V12 dodaje `memo_entry`, `memo_step` i osobny znacznik serializacji zapisów. V1–V11 pozostają bez zmian. Wszystkie wpisy należą do obecnego, prywatnego CRM z rolą OWNER; nie dodano modelu wielu organizacji.

Tytuł: do 160 znaków, treść: do 5000, kroki: maksymalnie 30, po 300 znaków. Dane są zwykłym tekstem renderowanym przez escapowane Thymeleaf, bez HTML, edytora rich text i załączników. Nie ma trwałego usuwania wpisów; kroki można świadomie zmieniać/usuwać w edycji celu.

Zapis wymaga OWNER i CSRF. Token tworzenia chroni przed powtórzeniem żądania, także równoczesnym. Każda edycja, odhaczenie, przypięcie i archiwizacja zmienia wersję. Stary edytor nie nadpisuje czynności wykonanej w innej karcie. Konflikt pozostawia pola do sprawdzenia przed ponowną edycją.

Potwierdzenie „Zapisano” pojawia się po odpowiedzi serwera i odświeżeniu listy. Przy błędzie zachowane są pola; checkbox po nieudanej czynności wraca do poprzedniego stanu. Brak localStorage i publicznego cache prywatnych notatek. Nie dodano powiadomień, SMS, AI, Kanban ani automatyzacji.

## Wyniki weryfikacji

**Maven Wrapper `verify`: 86 testów, w tym 25 jednostkowych/MVC i 61 integracyjnych PostgreSQL/Testcontainers. Zero błędów, niepowodzeń i pominiętych.**

Nowe testy obejmują:

- zapis i ponowienie tego samego tokenu, również w dwóch wątkach;
- trzy widoczne przypięcia z większej liczby oraz identyczną sekcję w dniu/tygodniu/miesiącu;
- trwałość po restarcie;
- odhaczenie, archiwum, przywrócenie i zachowanie ukończenia;
- ręczne ukończenie celu, kroki i cel bez kroków;
- datę notatki bez oznaczania jej jako zaległej;
- walidację długości, liczby kroków, wyszukiwanie i escapowanie skryptu w tytule;
- konflikt wersji po edycji lub czynności w innej karcie;
- OWNER, CSRF i niezależność od zamkniętego dnia pracy.

Prawdziwa aplikacja w Chromium, na tymczasowym schemacie PostgreSQL:

- dodanie memo, zadania po terminie, celu z krokami i bez nich oraz długiej treści;
- dodanie z dashboardu, pięć przypięć / trzy widoczne, odhaczenie i archiwizacja/przywrócenie;
- odhaczenie kroków bez automatycznego zamknięcia celu;
- wyszukanie fragmentu treści;
- kontrolowany HTTP503 i skuteczne ponowienie bez utraty tekstu;
- dwie karty: czynność zmienia wersję, a stary formularz zachowuje roboczy tekst i odrzuca nadpisanie;
- widoki listy, dashboardu i edytora przy 360/390/768/1440 px bez poziomego przewijania;
- zapis dostępny w zmniejszonym viewporcie 390×430;
- restart zachowuje sześć wpisów i postęp celu. Przeszła również istniejąca próba zamknięcia dnia ze zdjęciami i pomiarami.

Nie testowano fizycznej klawiatury iOS/Android. Próba zmniejszonego viewportu nie zastępuje testu na urządzeniu.

## Podgląd HTML

Wygenerowany z aktualnych odpowiedzi szablonów i tych samych modułów JS/CSS. Umożliwia dodanie memo, przypięcie, odhaczenie, kroki celu, edycję i archiwum z zachowaniem stanu podczas nawigacji w obrębie pliku.

To symulacja transportu i przechowywania w pamięci. Odświeżenie pliku resetuje dane; nie ma PostgreSQL, logowania ani trwałości między sesjami. Podsumowania finansowe pozostają przykładami. Weryfikację trwałości i bezpieczeństwa przeprowadzono w prawdziwej aplikacji.

Podgląd sprawdzono zarówno bezpośrednio w przeglądarce, jak i w zagnieżdżonym `iframe sandbox="allow-scripts"`: utworzenie/przypięcie memo, ukończenie zadania, archiwizacja/przywrócenie, kroki i edycja celu oraz zachowanie stanu po przejściu do ustawień i dashboardu. Brak błędów JavaScript. Funkcje przechowywania w tej demonstracji są odrębne od rzeczywistego zapisu.
