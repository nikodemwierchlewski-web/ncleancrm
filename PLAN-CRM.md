# ncLEAN CRM — ustalenia i plan realizacji

Data: 28 października 2026. Zakres: osobna aplikacja webowa dla właściciela ncLEAN, wygodna na telefonie i komputerze. Bez zmian strony produkcyjnej, inteligentnego proponowania terminów i GPS w tle.

## 1. Wynik audytu NcleanCMS — aktualizacja po otrzymaniu ZIP

Użytkownik dostarczył `NcleanCMS-master.zip`. Audyt źródeł jest ukończony w zakresie statycznym; zastępuje wcześniejszy brak dostępu do GitHuba. Pełne dowody, lokalizacje i ograniczenia zawiera `AUDYT-NcleanCMS.md`.

Potwierdzony stos: Java 17, Maven, Spring Boot 3.2.3, Spring MVC, Thymeleaf, Spring Security, JPA/Hibernate i PostgreSQL. Frontend: JavaScript, Bootstrap i SCSS; brak Reacta w deklaracjach projektu. Archiwum nie potwierdza konfiguracji aktualnie wdrożonej produkcji ani SHA commita.

Wykryto literalne hasło SMTP i hasło konta inicjalizowanego przez SeedData. Wartości nie są ujawniane ani kopiowane do CRM. Wykryto publiczny endpoint uruchamiający mailing, końcowe `permitAll`, brak znalezionego włączenia ochrony metod i operacje usuwania przez GET. Naprawy CMS i rotacja ewentualnie używanych poświadczeń to osobne zadania; nie zmieniano produkcji.

Datasource produkcji pozostaje nieustalony: w application.properties nie ma adresu i poświadczeń bazy, a docker-compose jest w całości zakomentowany. Potwierdzono `ddl-auto=update`, brak migracji i osobnych profili w archiwum. Nowy CRM otrzymuje osobną bazę, migracje Flyway i walidację schematu zamiast aktualizacji automatycznej.

## 2. Decyzja architektoniczna

Osobne repo `nclean-crm`, osobna aplikacja Spring Boot + Thymeleaf, zwykły JavaScript tylko dla usprawnień formularzy i kalendarza. React nie jest wymagany. Jeden modularny monolit, bez mikroserwisów. Osobna baza, użytkownik bazy, sekrety, sesje, pipeline i kopie zapasowe. Brak dostępu do tabel CMS i brak współdzielonych cookies z nclean.pl.

Szkielet po audycie: Java 17, Maven, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Security, Validation, JPA/Hibernate, PostgreSQL i Flyway. Wersja Boot figuruje w oficjalnej dokumentacji jako stabilna, ale pobranie zależności i zgodność projektu nie zostały tutaj zweryfikowane. PostgreSQL oraz JPA odpowiadają technologiom potwierdzonym w CMS; osobna wersja Boot nie oznacza zmiany istniejącej strony. Nie kopiować przestarzałych wersji wyłącznie dla identyczności.

Dokumentacja: https://docs.spring.io/spring-boot/system-requirements.html oraz https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html . Spring Security domyślnie chroni operacje zmieniające stan przed CSRF; ochrony nie wyłączamy.

## 3. Pierwsze ekrany i zachowanie

### Kalendarz — priorytet 1

Widok miesiąca oraz dnia/listy na telefonie. Właściciel ręcznie nadaje dniowi obszar i kolor; może zmienić albo usunąć oznaczenie. Nazwa zawsze towarzyszy kolorowi. Początkowo jeden obszar na dzień; jeśli praktyka wymaga kilku, później relacja wiele-do-wielu bez zmiany logiki zleceń. Słownik obszarów edytowalny: nazwa, kolor, aktywność. Można używać nazw osiedli lub własnych stref dojazdu.

Kliknięcie dnia pokazuje zlecenia i przycisk „Dodaj zlecenie”. Kolor to plan pracy, nie blokada rezerwacji. Zlecenie spoza obszaru daje najwyżej informację; nie jest automatycznie przesuwane. Widoczne nakładanie godzin i świadome zapisanie konfliktu z ostrzeżeniem, bez szukania lepszego terminu. Obszar jest przypisany także do adresu zlecenia; zmiana koloru dnia nie zmienia historii adresów ani zleceń.

### Szybkie zlecenie i klient — priorytet 2

Jeden formularz: dzień i godzina z kalendarza, wyszukanie klienta telefonem/nazwą lub dodanie nowego w miejscu, adres realizacji, obszar, usługa, ilość, cena, przewidywany czas i notatka. Minimum nowego klienta: nazwa/imię i telefon; minimum planowanego zlecenia obejmuje adres i czas. Telefon normalizowany z obsługą prefiksu, sugestia duplikatu zamiast automatycznego łączenia osób. Wyszukiwanie numeru nie powinno trafiać do URL i logów.

Statusy: robocze, zaplanowane, wykonane, anulowane; płatność osobno: nieopłacone/częściowo/opłacone. Historia klienta, wiele adresów i źródło pozyskania. Zlecenie przechowuje kopię adresu, pozycji i cen z dnia zamówienia. Zmiana klienta lub katalogu nie przepisuje historii. Po zapisaniu przekierowanie i ochrona przed podwójnym wysłaniem; operacje klient+zlecenie w jednej transakcji.

### Katalog usług i chemia — priorytet 3

Usługa: własna nazwa/kategoria, jednostka (szt., m², mb, godz.), cena i podstawa VAT, czas przygotowania plus czas na jednostkę, aktywność. Bez zakodowanych na sztywno PTM/PTS/PR. Chemia: produkt, jednostka zakupu i pojemność, cena, jednostka normy. Relacja usługa–produkt przechowuje zużycie koncentratu na jednostkę usługi; rozcieńczenie można zapisać pomocniczo, ale nie mylić litrów roztworu z koncentratem.

Przy dodaniu pozycji kopiować normę, cenę jednostkową chemii i czas do pozycji zlecenia. Umożliwić wpisanie rzeczywistego zużycia po pracy, wyraźnie rozróżniając koszt planowany i rzeczywisty. Zmiana normy dotyczy nowych pozycji. Nie usuwać używanych usług i produktów; dezaktywować.

### Paliwo, marketing i marża — priorytet 4

Ręczny wpis kilometrów oraz motogodzin agregatu, konfigurowalne spalanie i cena paliwa. Koszt samochodu = km × l/100 km ÷ 100 × cena/l; agregat = motogodziny × l/h × cena/l. Tankowanie jako ewidencja wydatku nie może ponownie zwiększać kosztu zlecenia już liczonego z norm. Jedno wybrane źródło kosztu dla każdego rodzaju; przy rzeczywistych kosztach oznaczać zastąpione estymaty.

Marketing: data/okres, kanał, kampania, kwota, opcjonalne powiązanie z klientem lub zleceniem. Oddzielić koszt kampanii od jego alokacji; suma alokacji nie może przekraczać wydatku i nie może drugi raz trafić do wyniku. Ręczna kilometrówka wystarcza; bez GPS.

Raporty dla zleceń wykonanych, osobno wpływy pieniężne według płatności. Przychód minus chemia, paliwo i pozostałe koszty bezpośrednie = nadwyżka po kosztach bezpośrednich. Marża procentowa = nadwyżka / przychód × 100%; dla przychodu 0 pokazać „brak podstawy”, a nie błąd. Wynik okresu dodatkowo odejmuje marketing i koszty ogólne. Nie nazywać go zyskiem netto firmy bez pełnych kosztów, podatków i rozliczeń.

Opcjonalnie czas pracy × stawka własna jako odrębny koszt kalkulacyjny oraz nadwyżka na godzinę (praca + dojazd). Kwoty BigDecimal/NUMERIC, waluta PLN, jawne zaokrąglenie; bez float/double. Ustalić jeden tryb netto/brutto i spójne traktowanie VAT przed raportami; nie mieszać podstaw. Źródło pozyskania klienta pozwala liczyć koszt nowego klienta na kanał. Brak nowych klientów oznacza brak wskaźnika, nie zero.

## 4. Model danych do kolejnych migracji

| Moduł | Główne tabele i reguły |
| --- | --- |
| Dostęp | app_user, role; hashe, aktywność, audyt logowania; początkowo OWNER |
| Obszary | district, day_district; unikalny dzień w MVP, kolor HEX, dezaktywacja |
| Klienci | customer, customer_address; telefon znormalizowany, historia, źródło pozyskania |
| Zlecenia | job, job_item, job_status_history, payment; snapshot adresu/cen/norm, version do blokady optymistycznej |
| Katalog | service_definition, chemical_product, service_chemical_norm; jawne jednostki |
| Koszty | job_cost, expense, expense_allocation; plan/wykonanie, źródło kosztu, brak podwójnego liczenia |
| Marketing | marketing_channel, campaign; expense wiąże się z kampanią |
| Kontrola | audit_event; aktor, czas, rodzaj operacji i identyfikator rekordu; bez pełnych telefonów i treści notatek w logu |
| Później | consent_event, message_template, message_outbox, message_delivery, upsell_offer |

PostgreSQL + Flyway, migracje wersjonowane, bez `ddl-auto=update`. Dla klientów i zleceń repozytoria Spring Data JPA, analogicznie do CMS; transakcje w serwisach. Daty planowania: Europe/Warsaw; zdarzenia audytowe jako Instant/UTC. Zlecenia mają planowany i rzeczywisty czas; sprawdzić zmianę czasu letniego.

## 5. Kolejność i gotowe pierwsze zadania w Codexie

Każdy punkt to osobna mała zmiana do przeglądu. Nie utworzono nowych czatów ani zadań w aplikacji. Poniższe opisy można wkleić jako polecenia.

1. **Audyt statyczny CMS — wykonany.** Raport jest w `AUDYT-NcleanCMS.md`; po uzyskaniu historii Git można uzupełnić SHA commita i analizę historii sekretów. Oryginalny zakres zadania: „Przeprowadź odczytowy audyt dostępnego checkoutu NcleanCMS: zapisz SHA, stack i strukturę, typ bazy, profile i zabezpieczenia z odwołaniami do plików. Nie wyświetlaj sekretów, nie uruchamiaj skryptów przed przeglądem i nie zmieniaj produkcji. Oddziel fakty od założeń.” Gotowe, gdy raport ma dowody dla każdej tezy.
2. **Uruchom izolowany fundament.** „W osobnym nclean-crm dopasuj wspierane wersje po audycie, dodaj Maven Wrapper, przygotuj osobny lokalny PostgreSQL i testy migracji Testcontainers. Uruchom build i testy MVC. Nie używaj danych ani połączeń CMS.” Gotowe: czysta baza migruje, restart zachowuje dane, brak wymaganych sekretów przerywa start, raport z wykonanych testów.
3. **Dokończ kalendarz.** „Dodaj słownik obszarów z nazwą i kolorem, ręczny zapis/zmianę/usuwanie obszaru dnia oraz widoki miesiąc/dzień na telefonie. Bez sugestii terminów. Dodaj audyt i ochronę równoczesnych zmian.” Gotowe: zapis po restarcie, kolory z etykietami, granice miesięcy i lat, walidacja, CSRF i autoryzacja sprawdzone.
4. **Klienci i szybkie zlecenie.** „Dodaj klientów, wiele adresów i jeden formularz nowego zlecenia z klientem dodawanym w miejscu. Podpowiadaj duplikaty telefonu; nie łącz automatycznie. Zapisuj snapshot adresu, status i osobną płatność.” Gotowe: z kalendarza zapisujesz zlecenie bez opuszczania formularza; anulowanie nie zwiększa przychodu; test transakcji i duplikacji żądania.
5. **Usługi i normy.** „Dodaj edytowalny katalog usług, jednostki, czasy, ceny, produkty chemiczne i normy koncentratu. Kopiuj stawki i normy do zlecenia. Używaj BigDecimal.” Gotowe: zmiana katalogu nie zmienia historii; testy konwersji ml/l, ilości, zaokrągleń i zużycia rzeczywistego.
6. **Koszty i marże.** „Dodaj ręczne km/motogodziny, paliwo, marketing, alokacje i raport zlecenia/miesiąca. Rozdziel koszty planowane, rzeczywiste, płatności i przychód. Zapobiegaj podwójnemu liczeniu.” Gotowe: znany przykład liczbowy zgadza się ręcznie, przychód zero bez dzielenia, anulowanie i marketing między okresami sprawdzone.
7. **Przygotuj do pracy z danymi.** „Dodaj konta w bazie, ograniczanie prób logowania, audyt i maskowanie logów, przegląd uprawnień, kopie zapasowe i próbę odtworzenia. Przygotuj konfigurację HTTPS dla osobnego środowiska testowego. Nie wdrażaj na produkcję.” Gotowe: testy dostępu, odtworzona kopia, instrukcja konfiguracji i przegląd zależności. Retencję i obsługę danych osobowych ustalić przed użyciem rzeczywistych danych.
8. **SMS i upsell później.** „Po ukończeniu MVP przygotuj adapter dostawcy SMS, kolejkę outbox, szablony, strefę czasu, próby ponowienia i idempotencję. Oddziel wiadomości dotyczące realizacji od marketingu. Upsell wymaga zapisanego uprawnienia do kontaktu marketingowego i obsługi wycofania; najpierw podgląd i zatwierdzenie właściciela.” Gotowe: tryb testowy nie wysyła do klientów, ponowienie nie dubluje wiadomości, wycofanie blokuje marketing. Zasady prawne zweryfikować przy wdrażaniu tego etapu.

## 6. Zakres dostarczonego szkieletu i weryfikacji

W folderze `nclean-crm` znajdują się pliki Maven, konfiguracja, logowanie OWNER, migracja obszarów i dni, serwis JPA, kontroler, responsywny szablon oraz pięć testów MVC. To początek zadania 2 i 3, nie gotowy CRM. Klienci, zlecenia, katalog, finanse i wysyłka nie są jeszcze zaimplementowane.

Szkielet nasłuchuje wyłącznie na 127.0.0.1:8081 i wymaga przekazania wszystkich danych logowania oraz połączenia do bazy. Nie ma wpisanych sekretów ani połączenia z nclean.pl. Nie uruchamiano aplikacji, migracji ani wdrożenia. Sprawdzono lokalnie strukturę plików i poprawność XML pom.xml; Java/Maven nie są dostępne, więc testy i kompilacja pozostają do wykonania. Brak integracyjnej weryfikacji SQL oraz renderowania w przeglądarce.

## 7. Granice obecnego zadania

Nie zmieniono repozytorium CMS, DNS, domen, hostingu ani danych produkcyjnych. Nie utworzono repozytorium na GitHubie. Nie zaprogramowano inteligentnego dobierania terminów, optymalizacji tras, GPS ani automatycznej wysyłki. Wdrożenie i wszelkie przyszłe połączenie ze stroną wymagają osobnego polecenia użytkownika.
