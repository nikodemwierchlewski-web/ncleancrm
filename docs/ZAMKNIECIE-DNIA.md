# Podsumowanie i zamknięcie dnia

Baza gałęzi: `feat/lean-interface-polish`, która zawiera ostatnie niescalone funkcje CRM. Nowy ekran jest częścią aplikacji Spring Boot/Thymeleaf, pod `/day-closeout?date=RRRR-MM-DD`.

## Obsługa

1. Otwórz **Podsumowanie → Uzupełnij dzień** albo link **Zamknij dzień** pod planem dnia. Podsumowanie finansowe domyślnie pokazuje wykonanie; plan, tydzień i miesiąc nadal są dostępne.
2. Lista pokazuje godziny, klientów i konkretne braki. Domyślnie otwiera się pierwsze niekompletne zlecenie. Możesz wybrać inne bez opuszczania ekranu.
3. Potwierdź status: wykonana, przełożona, anulowana lub jeszcze nie wiem. Przełożenie wymaga nowej daty i godziny; historia poprzedniego dnia pozostaje widoczna. Anulowanie z wpłatą wymaga wcześniejszego rozliczenia jej w szczegółach.
4. Przy wykonaniu potwierdź cenę końcową (również 0), rzeczywiste zużycie chemii, zdjęcia przed/po i notatkę po wykonaniu lub „Bez uwag”. Cena rezerwacji jest podpowiedzią, nie automatycznie zatwierdzonym wykonaniem.
5. Dla każdej usługi zachowaj istniejący pomiar, wpisz faktyczne ilości albo jawnie potwierdź „Nie użyto chemii”. Prognoza nie jest automatycznie przepisywana na pomiar. Puste pola nadal oznaczają brak danych.
6. Dodaj zdjęcia w rozwijanej sekcji. Jeżeli zdjęcia nie ma, wpisz powód w sekcji wyjątków; zapisany wyjątek jest jawnie pokazany. Zdjęcia zapisują się osobno bez kasowania wpisanej notatki i ilości. Ręczny czas i timer mają pierwszeństwo przed wyliczeniem z nazw.
7. „Zapisz i następne” zapisuje formularz i wybiera kolejne niekompletne zlecenie. „Jeszcze nie wiem” pozostaje do sprawdzenia; nie zamknie dokumentacji.
8. „Zamknij dzień” sprawdza kompletność na serwerze. Błąd podaje klienta i brakujące dane; link prowadzi do brakującego zlecenia. Zamknięcie jest trwałe, ale późniejsza zmiana dokumentacji lub dodanie zlecenia powoduje konieczność ponownego sprawdzenia dnia.

Nie trzeba otrzymać całej płatności, aby zamknąć dokumentację. Płatności, należności i kompletność kosztów są niezależne. Niepełny wynik finansowy pozostaje oznaczony nawet po zamknięciu dnia. Licznik, agregat i tankowania są dostępne przez osobny link.

## Dane i spójność

- Flyway V11 dodaje `completion_note`, świadome „Bez uwag”, powody wyjątków zdjęć, powód zmiany statusu, jawne zerowe zużycie, historię przełożeń oraz `day_review`.
- V1–V10 nie zmieniono. `work_note` pozostaje bez zmian: historyczna zawartość mogła już łączyć ustalenia i wykonanie, więc migracja jej nie interpretuje i nie nadpisuje. Nowe notatki po wykonaniu są oddzielne, także w zwykłym panelu zlecenia, historii klienta i eksporcie CSV.
- Edycje używają wersji zlecenia i pomiarów. Zapis jest transakcyjny. Ponowienie nie tworzy drugiego przełożenia ani drugiego wykonania z tego samego stanu. Przy konflikcie formularz zostaje w przeglądarce; użytkownik musi sprawdzić aktualny stan.
- Zamknięcie zapisuje podpis stanu dnia i porównuje go przy odczycie. Obejmuje zlecenia, pozycje, rzeczywiste pomiary, metadane zdjęć i przełożenia. Podpis nie wczytuje ani nie serializuje binarnej zawartości zdjęć.
- Krótka transakcja zamknięcia sprawdza podany podpis i blokuje zapisy do właściwych tabel na czas kontroli. Wspólna kolejność blokad wykonania, pomiarów i zdjęć chroni przed zakleszczeniem. Nowa zmiana po zatwierdzeniu zostanie wykryta przy kolejnym odczycie.
- OWNER, CSRF, walidacja serwerowa i prywatne zdjęcia pozostają obowiązkowe. Nie dodano SMS, GPS, zadań nocnych ani nowych połączeń z bazą.

## Wygląd

Mobile first: jedna karta edycji, zwarta lista, jasne przyciski i opcjonalne rozwinięcia. Delikatne gradienty, światło i miękkie cienie nadają głębię kartom bez WebGL i przechylania elementów. Statusy są opisane tekstem, a ruch wyłączany przez `prefers-reduced-motion`. Stopka zapisu jest dostępna podczas przewijania; istniejąca obsługa VisualViewport zwalnia dolną nawigację podczas edycji z klawiaturą.

## Weryfikacja

**Maven Wrapper `verify`: 74 testy, w tym 24 jednostkowe/MVC i 50 integracyjnych z rzeczywistym PostgreSQL Testcontainers; zero błędów, niepowodzeń i pominiętych.**

Nowe testy obejmują:

- rozdzielenie notatek, kwotę zero i potwierdzone zerowe zużycie;
- brak chemii, notatki i zdjęć, świadome wyjątki i odrzucenie pustego pomiaru;
- anulowanie, przełożenie i zachowanie historii;
- pusty dzień, częściową wpłatę niezależną od kompletności;
- trwałość zamknięcia po restarcie, ponowne otwarcie po dodaniu zlecenia lub zdjęcia;
- nieaktualną wersję, nieaktualny podpis i zakaz zamknięcia przyszłego dnia;
- równoczesne zdjęcie i zamknięcie w dwóch wątkach bez zakleszczenia i nieaktualnego stanu;
- kontrolę OWNER i CSRF oraz renderowanie prawdziwego ekranu.

Próba przeglądarkowa Chromium na osobnym, usuwanym schemacie PostgreSQL:

- początkowo osiem zleceń; wykonanie, anulowanie i przełożenie;
- pomiar 80 ml chemii, zdjęcia przed/po i automatyczne 45 minut;
- rozdzielenie ustaleń od notatki po wykonaniu i zachowanie formularza podczas wysyłania zdjęć;
- symulowany HTTP503 i skuteczne ponowienie;
- konflikt dwóch kart bez utraty roboczego tekstu;
- zamknięcie po uzupełnieniu braków, restart oraz wykrycie późniejszej zmiany;
- szerokości 360, 390, 768 i 1440 px: ekran zamknięcia, dashboard i historia klienta bez poziomego przewijania i błędów JavaScript.

Nie testowano fizycznej klawiatury iOS/Android na urządzeniu. Testy Chromium przy szerokościach mobilnych nie zastępują takiej próby.

## Podgląd HTML

Wygenerowany z aktualnych odpowiedzi Thymeleaf oraz CSS/JS. Wszystkie kontakty, pomiary i zdjęcia są syntetyczne. Pozwala uzupełniać przykładowe karty, obserwować postęp i zamknąć dzień w pamięci podglądu. Można przechodzić do kalendarza, klientów i ustawień. Po odświeżeniu pliku stan demonstracji resetuje się; finanse są przykładowymi wartościami, bez przeliczenia bazy. Inne formularze wykonują wyłącznie zapis demonstracyjny.

Sprawdzono zwykły podgląd oraz zagnieżdżony iframe `sandbox="allow-scripts"`: uzupełnienie pięciu kart, postęp 2/7 → 7/7, błąd niekompletnego dnia, zamknięcie i nawigację bez błędów JavaScript. Środowisko nie udostępnia publicznego podglądu serwera (`capabilities: []`); trwały zapis i zabezpieczenia sprawdzono w rzeczywistej aplikacji, a nie w pliku HTML.
