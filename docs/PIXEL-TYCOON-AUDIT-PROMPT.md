# Prompt: iteracyjny audyt i poprawa eksperymentu Pixel Tycoon

Pracuj w repozytorium nikodemwierchlewski-web/ncleancrm, na osobnej gałęzi eksperymentalnej zawierającej pełną, ostatnią wersję CRM. Zachowaj wygląd klasyczny jako domyślny. Nie scalaj eksperymentu do main ani nie wdrażaj na produkcję.

Cel: czytelny, wygodny CRM o wyglądzie izometrycznej gry 3D pixel tycoon. Właściciel firmy nadal ma szybko planować dzień i zapisywać zlecenia. Użyj rzeczywistej aplikacji Spring Boot/Thymeleaf, nie tylko demonstracyjnego HTML.

1. Sprawdź aktualny motyw, wszystkie ekrany i działające formularze. Zrób zrzuty i zanotuj konkretnie zaobserwowane błędy. Oceń kontrast, typografię, głębię kafelków, grafikę izometryczną i spójność komponentów. Unikaj fikcyjnych punktów, wyników i nowych modułów.
2. Uruchom aplikację z odizolowaną bazą PostgreSQL i syntetycznymi danymi. Sprawdź 360, 390, 768 i 1440 px, osiem wizyt, bardzo długie nazwy/adresy, duże kwoty, kilka obszarów oraz czarny/biały/jaskrawy HEX. Zmierz położenie pierwszej karty zlecenia i najważniejszych liczb; sama kontrola braku poziomego przewijania nie wystarcza.
3. Sprawdź dodanie i edycję zlecenia, klienta, cennik/chemię, obszary/plan tygodnia, zdjęcia, realizację/zamknięcie dnia, dashboard, paliwo, koszty/sprzęt oraz notatki. Zweryfikuj zapis/ponowienie po błędzie, klawiaturę, fokus/Escape, wysokość formularza, reduced-motion, konsolę JS, HTTP i logi.
4. Usuń wykryte błędy w aplikacji. Powtórz scenariusze, które ujawniły błąd, a następnie sprawdź pozostałe dotknięte komponenty. Jeżeli ponowna próba wykryje kolejny błąd, popraw go i powtórz test. Zakończ po przejściu kryteriów, raportując konkretne granice weryfikacji zamiast deklarować brak wszystkich możliwych błędów.
5. Zweryfikuj przełączenie klasyczny → tycoon → klasyczny z zachowaniem danych. Zachowaj PostgreSQL, OWNER, CSRF, istniejące obliczenia i migracje. Nie łącz się z nclean.pl ani produkcyjnym CMS. Nie przechowuj sekretów w Git.
6. Przygotuj sprawdzony, klikalny podgląd z jednoznaczną etykietą TEST i możliwością porównania wyglądu. Pasek podglądu ma być kompaktowy na telefonie. Sprawdź sam plik, przełącznik bez utraty formularza oraz osadzenie iframe. Oznacz pamięciową symulację i przykładowe podsumowania. Nie twierdź, że plik zastępuje backend.
7. Podaj rzeczywiste wyniki testów przed i po poprawkach, listę wykrytych problemów, commit, sposób otwarcia podglądu i łatwego powrotu do klasycznego wyglądu. Przygotuj draft PR, jeżeli dostęp GitHub na to pozwala. Wyraźnie wskaż odmowę API lub inne ograniczenia.
