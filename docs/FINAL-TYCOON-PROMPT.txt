# PROMPT WYKONAWCZY — domknięcie ncLEAN CRM 0.1 / Tycoon

Pracuj w repozytorium `nikodemwierchlewski-web/ncleancrm`.

Twoim zadaniem jest domknąć istniejący CRM do stabilnej wersji codziennej pracy właściciela ncLEAN. Połącz perspektywę lean, projektowania UX/UI, developera Spring Boot i właściciela jednoosobowej firmy sprzątającej. Wprowadź działające zmiany, zweryfikuj je i dostarcz przeglądalny rezultat. Nie kończ na planie, raporcie ani osobnym prototypie.

## 0. Decyzje obowiązujące w tym zadaniu

1. **TYCOON JEST DOCELOWYM WYGLĄDEM CAŁEJ APLIKACJI.** Nie jest eksperymentem. Nowa decyzja właściciela zastępuje wcześniejsze instrukcje o domyślnym wyglądzie klasycznym i opcjonalnym Tycoonie.
2. Zachowaj uporządkowany układ lean. Grafika gry ma zachęcać do pracy; dane, czytelność i szybkość pozostają pierwsze.
3. Nawigacja: **Dzisiaj | Kalendarz | Klienci | Więcej**. Kalendarz pozostaje ekranem startowym. Więcej zawiera wyniki, koszty, notatki i konfigurację. Ustawienia służą konfiguracji.
4. Wykorzystaj istniejące funkcje, modele, transakcje i zapis. Nie twórz drugiego CRM, nowego dashboardu ani równoległego systemu celów/osiągnięć.
5. Spring Boot + Java 17 + Thymeleaf + PostgreSQL + Flyway + CSS i niewielki JavaScript. Bez Reacta i ciężkiego silnika 3D.
6. Nie łącz się z produkcyjną bazą nclean.pl i nie modyfikuj CMS. Nie wdrażaj ani nie scalaj do main automatycznie.
7. Nie dodawaj SMS, GPS, publicznego bookingu, fakturowania, magazynu, OCR, multiusera, nowej analityki ani integracji. Pomysły poza stabilizacją zapisz na później.

## 1. Ustal rzeczywistą bazę i przeczytaj audyt

Najpierw sprawdź gałęzie i niezapisane zmiany. Wykorzystaj najnowszą pełną aplikację z `ux/lean-workspace` lub jej nowszego, pełnego następcy. Main może nadal zawierać jedynie wcześniejszy etap. Nie nadpisuj pracy użytkownika i nie cofaj repozytorium do starego szkieletu.

Przeczytaj:
- `docs/FINAL-TYCOON-AUDIT.md`;
- `docs/audit/final-tycoon-readiness.json`;
- `docs/audit/final-route-inventory.json`;
- aktualne instrukcje Windows, backupu, uruchamiania i ostatnie zmiany Lean workspace.

Audyt wykonano dla `ux/lean-workspace@900ea19`, kod aplikacji `ffabed1`. Zweryfikuj, czy opisane przypadki nadal występują. Jeśli coś zostało już naprawione, zachowaj poprawkę i potwierdź ją testem. Nie przywracaj starych błędów tylko po to, żeby wykonać listę.

Utwórz osobną gałąź. Uruchom stan bazowy na odizolowanym PostgreSQL. Zanotuj commit, wersję Javy, migracje i wynik testów. Bazowy audyt miał 107 zaliczonych testów; ta liczba nie jest dowodem kompletności ani docelowym limitem testów.

Pracuj etapami: reprodukcja → poprawka → właściwy test → ponowne sprawdzenie. W pierwszej kolejności popraw integralność i niezawodność zapisu, później pełny wygląd i dopracowanie.

## 2. Napraw potwierdzone P1: przyszła wizyta nie może być wykonana

Audyt R01:
- przyszła wizyta + POST `/orders/{id}` z `calendar=1`, `status=DONE` kończył się HTTP 200 i DONE w bazie;
- stary POST `/work/{id}/complete` również zapisywał przyszłe wykonanie;
- `/day-closeout` poprawnie blokował ten przypadek.

Wprowadź jedną regułę domenową obowiązującą wszystkie wejścia, nie wyłącznie ukrycie opcji w UI. Uwzględnij edycję statusu, stary endpoint, wspólny formularz i zmianę terminu już wykonanej wizyty.

- Nie przyznawaj odznaki ani nie ujmuj przyszłej wizyty jako realnego wykonania.
- Odrzucona operacja ma pozostawić dane, wersję, pomiary, wpłaty i historię bez częściowego zapisu.
- Przyszły termin zlecenia ma pozostawać planem; nie kasuj samoczynnie wcześniejszych rozliczeń przy próbie zmiany.
- Komunikat ma wyjaśniać, co poprawić. Nie wyłączaj walidacji dla starego endpointu.
- Daty pracy interpretuj zgodnie z Europe/Warsaw. Przetestuj dziś, jutro, poprzedni dzień oraz istotne granice dat.

Dodaj testy pokazujące błąd starego kodu i potwierdzające poprawkę dla każdego wejścia.

## 3. Napraw P1: jawny identyfikator zawsze oznacza tę samą wizytę

Audyt R02: `/day-closeout?date=...&order=999999999` zwracał HTTP 200 i formularz innego zlecenia, np. `/day-closeout/1`.

- Jeśli użytkownik podał jawny ID, nie wybieraj w zamian innego klienta.
- Nieistniejący ID: poprawny 404 albo czytelny stan bez edytora innej wizyty.
- ID istniejący, ale należący do innego dnia: czytelna informacja lub kontrolowane przejście do właściwego dnia z zachowaniem tego ID.
- Automatyczne wybranie pierwszego braku jest dozwolone, gdy ID nie podano.
- Sprawdź ten sam warunek na GET i POST oraz po przełożeniu wizyty i otwarciu starego linku.
- Zapis ma wiązać ID, datę i wersję rekordu. Nie podstawiaj nowszej wersji automatycznie pod cudze zmiany.

Uporządkuj także odpowiedzi dla nieistniejących klientów, zleceń, obszarów i plików. Brak zasobu nie powinien wyglądać jak błąd walidacji formularza ani losowa strona błędu serwera.

## 4. Napraw P1: odzyskanie sesji bez utraty szkicu

Audyt R03: szkic w karcie A, wylogowanie/ponowne logowanie w karcie B, ponowienie w A → 403. Pola zostały, ale token CSRF był nieaktualny. Po wymianie tylko tokenu ten sam szkic zapisał się raz.

Zaprojektuj wspólną obsługę sesji dla istniejących formularzy i operacji zdjęć:
- wykrycie wygaśnięcia sesji, przekierowania do logowania i błędu CSRF;
- jasny komunikat i możliwość bezpiecznego zalogowania;
- odnowienie tokenu w pozostawionym formularzu po potwierdzonym zalogowaniu;
- świadome ponowienie z zachowaniem wpisanych danych, wybranych usług, cen, adresu i oczekujących plików;
- brak podwójnego zapisu, także gdy pierwsza odpowiedź zginęła już po commicie;
- zachowanie tokenu idempotencji tej samej operacji;
- jeśli rekord zmienił się w międzyczasie, pokaż konflikt i zachowaj propozycję użytkownika. Nie omijaj blokady wersji.

Nie wyłączaj CSRF ani OWNER. Nie zapisuj haseł, danych klientów lub zdjęć w nieszyfrowanym localStorage, publicznym cache czy pliku demonstracji. Nie buduj nowego systemu offline. Otwarta karta może przechowywać szkic w pamięci; zachowaj uczciwy komunikat o granicach tej ochrony.

Ujednolić należy formularze kalendarza, klienta, wykonania, notatek, wydatków, paliwa, zdjęć i płatności. Nie kopiuj kolejnych niezależnych obsług fetch, jeśli można wykorzystać mały wspólny moduł.

Sprawdź także zamknięcie brudnego formularza i zmianę widoku. Zachowuj szkic lub ostrzegaj przy rzeczywistym ryzyku jego utraty, bez potwierdzania każdego zwykłego kliknięcia.

## 5. Napraw P1 UX: korekta kwoty/czasu w pakiecie

Audyt R04: zlecenie za 200 zł z podziałem 100 + 100, zmiana kwoty na 210 zł przy zwiniętej sekcji podziału. Serwer słusznie odrzucał zapis, ale ukryte pola utrudniały zrozumienie i poprawienie.

- Gdy zmieniona suma lub czas nie pasuje do istniejącego podziału, pokaż to przy głównym polu.
- Otwórz lub wskaż odpowiednią sekcję i przenieś fokus na sensowny cel.
- Daj świadomy wybór: poprawiam podział albo usuwam bieżący podział i zachowuję rozliczenie całego zlecenia.
- Nie wyliczaj sam proporcji, nie udawaj pomiaru i nie zmieniaj historii dawnych wersji.
- Nie usuwaj walidacji sum i atomowości transakcji.
- Nie wyzeruj podziału tylko dlatego, że użytkownik go nie rozwinął.
- Błąd zachowuje nową wpisaną kwotę, czas i pozostałe pola. Zapis następuje dopiero po poprawnej, świadomej decyzji.

Sprawdź pojedynczą usługę, pakiet, brak podziału, istniejący podział, pusty czas i działający timer. Kwota niższa od wpłat nadal wymaga najpierw poprawnego rozliczenia wpłat.

## 6. Tycoon jako jeden docelowy system interfejsu

Wprowadź spójny, profesjonalny Cleaning Tycoon dla wszystkich ekranów. Zachowaj istniejące własne grafiki izometryczne, sprzątającą postać, samochód, ekstraktor, bąbelki i odznaki.

- Tycoon ma działać domyślnie przy zwykłym uruchomieniu i profilu produkcyjnym, bez dodatkowego przełącznika startowego.
- Usuń w normalnym produkcie etykiety TEST, eksperyment i opcjonalny motyw. Nie rozwijaj drugiej publicznej wersji klasycznej.
- Zachowaj kontrolę ruchu i pierwszeństwo ustawień systemowych.
- Wspólny wygląd obejmuje: logowanie, błąd logowania, wygaśnięcie sesji, wylogowanie, 403/404/500, puste stany, offline/PWA, wyszukiwanie, ponowny kontakt, wszystkie moduły, formularze i dialogi.
- Audyt wykazał brak wspólnego zestawu assetów na `/search` i `/callbacks`; usuń przyczynę przez wspólny layout/head, nie pojedyncze doraźne linki.
- Współdziel style, fonty, nawigację i komunikaty. Nie przepisuj aplikacji od zera ani nie duplikuj całych szablonów dla każdego wariantu.
- Publiczna strona logowania może dostać wyłącznie niezbędne publiczne zasoby statyczne. Nie upubliczniaj zdjęć, rachunków, eksportów, API ani danych po to, żeby załadował się CSS.
- Normalny CRM zachowuje zabezpieczenia i własną sesję. Demonstracja pozostaje osobnym, jasno oznaczonym artefaktem z syntetycznymi danymi.

Zaktualizuj działające instrukcje i skrypty Windows: zwykłe uruchomienie ma dawać Tycoon. Archiwalne audyty mogą pozostać historią, ale nie mogą być główną instrukcją z nieaktualną decyzją o wyglądzie.

## 7. Zasady wizualne i dostępność

Tycoon ma być atrakcyjny i uporządkowany: subtelna głębia, własna grafika pixel art i płynne reakcje, przy zachowaniu dobrego interfejsu usługowego.

- Jedna paleta, skala odstępów, typografia, cienie, stany i rodziny kontrolek.
- Pixelowy charakter w nagłówkach i grafice; dłuższe teksty, formularze, kwoty i telefony bardzo czytelne.
- Nie używaj kolejnego ciężkiego pudełka wokół każdego akapitu. Zachowaj spokojniejszy układ lean.
- Polskie znaki i długie nazwy muszą mieścić się bez ucinania istotnych danych.
- Kontrast zwykłego tekstu minimum 4,5:1; odpowiedni kontrast dużego tekstu i elementów interaktywnych. Kolor nie jest jedynym nośnikiem informacji.
- Napraw potwierdzone problemy Obszarów pracy i desktopowych dni tygodnia. Sprawdź wszystkie własne kolory obszarów, także bardzo jasne i ciemne.
- Drobne opisy mają być czytelne na telefonie w terenie. Nie uzyskuj pozornej zwięzłości przez font 8 px.
- Minimum 44 × 44 px dla dotyku, poprawny fokus, etykiety, nazwy przycisków ikonowych i sensowne komunikaty błędów.
- Jeden poprawny skip-link, widoczny dla technologii wspomagających H1 i landmark main. Napraw brak celu na wyszukiwaniu/ponownym kontakcie oraz zdublowany skrót kalendarza.
- Klawiatura, Tab/Shift+Tab, Enter, Escape, fokus po zamknięciu dialogu, powiększenie tekstu i brak zasłonięcia przy klawiaturze ekranowej.

Sprawdź axe po ustabilizowaniu ruchu oraz przy prefers-reduced-motion. Nie zaliczaj przejściowych efektów animacji automatycznie jako trwałej wady kontrastu. Nie uznawaj wyników incomplete za zaliczone. Opisz ręczne sprawdzenia; sam brak błędów axe nie oznacza pełnej zgodności WCAG.

## 8. Zachowaj lean w istniejących ścieżkach

Dzisiaj: następna wizyta, kompletność i pierwszy rzeczywisty brak przed pełnym planem. Braki wylicza istniejący DayReviewService. Płatność nie blokuje dokumentacji.

Kalendarz: komputer — miesiąc w 7 kolumnach i panel dnia; telefon — pasek tygodnia, wybrany dzień i czytelne karty. Osiem wizyt ma pozostać czytelnych; nadmiar wpisów w miesiącu prowadzi do pełnego dnia. Data pozostaje po zapisie i zamknięciu panelu.

Dodawanie: jeden formularz z kategoriami, ilościami, cenami uzgodnionymi, klientem, adresem, terminem i sumą. Zmiana oglądanej kategorii nie usuwa wcześniejszych pozycji. Jeden sposób wyboru klienta po telefonie/nazwie; bez automatycznego scalania po numerze.

Klient: profil i historia, źródło pozyskania, ponowny kontakt. Zmiana adresu wizyty nie nadpisuje bez pytania adresu domyślnego. Dane nowego klienta zapisują się w transakcji rezerwacji; nie dubluj wpisu po ponowieniu.

Wykonanie: ten sam komponent w szczegółach wizyty i zamknięciu dnia. Kwota końcowa, rzeczywista chemia, zdjęcia, notatka lub Bez uwag, opcjonalny czas/podział. Nie wracaj do kilku niezależnych formularzy pomiaru.

Podsumowanie: trzy główne liczby, okres, kompletność i jedna najważniejsza akcja na pierwszym ekranie telefonu. Cele, odznaki, szczegółowe analizy i notatki rozwijane na żądanie. Nie przywracaj nieczytelnego wykresu kołowego jako głównej informacji.

Koszty: jeden katalog wydatków i inwestycji, jedno miejsce odczytu samochodu/agregatu i tankowania. Kontekstowe skróty otwierają te same formularze z poprawną datą/ID. Ręczny starszy model paliwa zachowaj jako czytelnie opisany wariant korekty, bez drugiego naliczania.

Ustawienia: cennik, kategorie, chemia, obszary i parametry. Nie przenoś z powrotem codziennych operacji do konfiguracji.

Każdy ekran ma główną czynność i krótkie puste stany. Nie dodawaj kolejnych komunikatów, liczników i przycisków tylko dla efektu gry.

## 9. Dane biznesowe, których nie wolno uprościć błędnie

- Ceny i nazwy w dawnych rezerwacjach są historycznym zapisem. Edycja cennika nie przepisuje starych wycen.
- Prognoza chemii, rzeczywisty pomiar i zakup chemii to różne informacje. Puste nie oznacza zera. Jawne zero ma pozostać rozpoznawalne.
- Standard jest pomocą, a nie automatycznym rzeczywistym zużyciem. Propozycje z danych wymagają odpowiedniej próby i świadomego zastosowania; zachowaj istniejące reguły mediany.
- Przychód, otrzymane wpłaty i należność są oddzielne. Zaplanowane kwoty nie stają się wykonaniem. Wynik po kosztach nie jest zyskiem netto po podatkach.
- Zakup paliwa/chemii nie jest drugim kosztem ich zużycia. Inwestycje nie są automatycznie kosztem realizacji dnia. Rachunek już ujęty w miesięcznych nie nalicza się drugi raz.
- Zmiana ceny paliwa lub materiału nie przepisuje historycznych cen zapisanych pomiarów i dni.
- Korekta odczytu respektuje sąsiednie dni i tankowania. Luki pomiarowe nie udają dziennego zera.
- Anulowana wizyta nie wymaga chemii/zdjęć wykonania. Przełożenie zachowuje historię. Anulowanie nie może usuwać nierozliczonych wpłat.
- Korekty wpłat zachowują ślad, są idempotentne i nie tworzą podwójnego zwrotu.
- Dezaktywacja obszaru/usługi nie usuwa historii; przypisanie obszaru do dnia jest opcjonalne.

Sprawdź te zasady na przykładach liczbowych, nie tylko na statusie HTTP. Zachowaj BigDecimal i właściwe jednostki. Nie stosuj wyłączenia walidacji jako poprawki UX.

## 10. Zdjęcia i rzeczywista praca mobilna

Zachowaj prywatność, walidację i ponowne kodowanie JPEG/PNG oraz rozpoznawanie czasu z nazwy. Zachowuj oryginalną nazwę do tej analizy także przy ewentualnym zmniejszaniu obrazu.

Sprawdź: dwa etapy Przed/Po, kolejkę kilku plików, ponowienie po błędzie, limit rozmiaru/rozdzielczości, błędny plik, datę niepasującą do wizyty, niejednoznaczny czas, ręczną korektę i pierwszeństwo timera. Dodawanie zdjęć nie kasuje pozostałego formularza.

Audyt nie potwierdził rzeczywistych HEIC/dużych zdjęć telefonu i orientacji EXIF. Przygotuj odpowiednie fixture'y i jasny komunikat o obsługiwanych formatach. Jeśli zastosujesz kompresję, zachowaj czytelność rachunku i przewidywalną orientację. Nie dodawaj OCR ani analizy AI.

Po usunięciu zdjęcia stan dokumentacji ma ponownie odzwierciedlać rzeczywiste braki. Wyjątki braku zdjęcia są świadome i jawne, nie automatycznie dopisywane.

## 11. Ruch i osiągnięcia

Zachowaj krótkie reakcje: wciśnięcie przycisku, zmiana dnia/panelu, podświetlenie zapisu, scena czyszczenia i odznaki. Efekt sukcesu następuje po potwierdzonym zapisie.

- Brak punktów za samo klikanie, fikcyjnych zarobków, GPS i sztucznego postępu.
- Brak kolejek efektów po szybkich kliknięciach; kolejny efekt zastępuje poprzedni.
- Animacje nie blokują operacji, nie przejmują fokusu i nie zasłaniają pól/CTA.
- Zatrzymanie poza ekranem/tłem, ograniczony ruch i statyczna alternatywa z tą samą informacją.
- Istniejące odznaki zapisane transakcyjnie i historia pozostają. Nie dubluj ich po odświeżeniu, ponowieniu, restarcie i w dwóch kartach.
- Zachowaj rozdział historycznie osiągniętej odznaki od bieżącego licznika po korekcie danych.

## 12. Wydajność i porządek kodu

Audyt użył syntetycznych 303 klientów i 503 zleceń. Pomiary lokalne są w raporcie; nie udawaj, że stanowią docelowe SLA.

- Ogranicz pobieranie/renderowanie całej kartoteki i wszystkich formularzy naraz. Dodaj sensowną paginację/listę wyników tam, gdzie jej brakuje.
- Lookup powinien mieć ograniczenie w zapytaniu do bazy, stabilną kolejność i brak kasowania szkicu przy odpowiedzi wyszukiwania.
- Sprawdź pętle zapytań na /orders. Kalendarz ma już część zbiorczych zapytań — nie zastępuj ich gorszym rozwiązaniem.
- Zmierz liczbę zapytań, rozmiar HTML i czas pierwszego użytecznego widoku. Odróżnij zimny/ciepły start, wykonaj powtórzenia i podaj medianę.
- Indeksy dobieraj do rzeczywistych zapytań i planu, nie losowo. Nie dodawaj nowej architektury tylko dla kilkuset rekordów.
- Usuń potwierdzony martwy kod i powielone obsługi. Zachowaj potrzebną kompatybilność istniejących adresów i endpointów.
- Nie dodawaj testów kopiujących implementację ani setek zależności do samej oprawy.

## 13. Zabezpieczenia i przygotowanie wydania

Zachowaj OWNER, CSRF, walidację, blokady wersji, transakcje, idempotencję, prywatne pliki, CSP, escapowanie tekstu i ochronę CSV. Publiczny statyczny wygląd nie może otwierać prywatnych zasobów.

Istnieje już profil prod z secure-cookie i ograniczeniem zaufania do lokalnego reverse proxy. Zweryfikuj jego użycie, zamiast twierdzić, że go brak. Nie wystawiaj PostgreSQL ani niezabezpieczonego portu aplikacji publicznie.

- Uporządkuj login/logout, timeout sesji i błędy; komunikaty bez SQL, stack trace i sekretów.
- PWA: spójne logo, kolor i start_url zgodny z ekranem startowym. Nie deklaruj pracy offline ani buforowania prywatnych danych, jeśli nie ma tej funkcji.
- Uruchom build, migrację świeżej bazy i upgrade kopii. Nigdy nie edytuj wykonanych V1–V14. Nowa migracja tylko przy rzeczywistej potrzebie, bez utraty danych.
- Wygeneruj backup i odtwórz do nowej, odizolowanej bazy. Porównaj kluczowe rekordy, płatności, notatki, zdjęcia/rachunki i historię; uruchom aplikację na restore i zaloguj się.
- Nie nadpisuj zwykłej bazy podczas testu restore. Kopie i sekrety poza Gitem; opisz ochronę i miejsce przechowywania.
- Zweryfikuj Windows/Maven Wrapper/PowerShell tam, gdzie środowisko pozwala. Brak Windows zaznacz jawnie; test równoważnego polecenia Linux nie jest testem PowerShell.
- Przygotuj staging i instrukcję dla własnego VPS/reverse proxy/HTTPS. Nie wdrażaj na produkcję i nie łącz CMS bez osobnego, konkretnego zakresu wdrożenia.
- Sprawdź zależności dostępnymi narzędziami; podaj faktyczny raport i ograniczenia. Nie deklaruj braku CVE bez analizy i nie rób niepotrzebnej migracji całego frameworka.

Wersja wydania, nazwa JAR, skrypty Windows, CI i dokumentacja muszą być spójne. Jeśli zmieniasz numer wersji, zaktualizuj wszystkie zakodowane nazwy artefaktów. Serwer testowy uruchamiaj z kopii JAR poza target, aby następny build go nie uszkodził.

## 14. Macierz obowiązkowej weryfikacji

Przetestuj rzeczywistą aplikację na PostgreSQL. HTML demonstracyjny nie zastępuje tych prób.

A. Nowy klient: usługi z dwóch kategorii, ilość, indywidualna cena, telefon, adres i jeden zapis.
B. Powracający klient: wyszukanie, adres poprawiony przed/po wyborze, bez drugiego klienta i nadpisania adresu domyślnego.
C. Rezerwacja: podwójne kliknięcie, powtórzony token, odpowiedź utracona po zapisie, błąd503 i ponowienie bez duplikatu.
D. Sesja: szkic w starej karcie, relogowanie w drugiej, nowy CSRF i poprawne ponowienie; konflikt danych nadal wykrywany.
E. Osiem zleceń dnia: kolejność godzin, więcej wpisów w miesiącu, panel/lista, odczyt klienta/usługi/ulicy.
F. Data i status: dziś/jutro/wczoraj, każde wejście do DONE, przełożenie, stary link, błędny i niepasujący ID.
G. Wykonanie: jeden formularz, pomiar/zero/brak chemii, notatka/Bez uwag, zdjęcia i jawny wyjątek.
H. Pakiet: zmiana kwoty/czasu przy zapisanym podziale; jawne rozwiązanie konfliktu, bez wymyślonych proporcji.
I. Zdjęcia: nazwy/czas, błędny format, limity, ponowienie, orientacja, usunięcie, timer/ręczna korekta.
J. Płatności: częściowa wpłata, korekta, anulowanie z wpłatą, cena niższa od wpłat, powtórzenie żądania.
K. Paliwo: pierwszy odczyt, przerwa w dniach, korekta licznika, agregat, tankowanie i rachunek; brak podwójnego kosztu.
L. Koszty: zakup/zużycie/inwestycja, rachunek ujęty miesięcznie, marketing, korekta i anulowanie.
M. Cennik/chemia: edycja i dezaktywacja, zmiana cen nie zmienia historii, propozycje standardu nie stają się pomiarem.
N. Obszary: kilka na dzień, tygodniowy plan i wyjątek, HEX jasny/ciemny, dezaktywacja zachowuje historię.
O. Klienci/notatki/cele: historia, źródło, ponowny kontakt, przypięcie/kroki, wyszukiwanie i ochrona tekstu.
P. Osiągnięcia: wycofany zapis niczego nie przyznaje; dwie karty/restart nie dublują odznaki.
Q. Bezpieczeństwo: niezalogowany, brak OWNER, brak CSRF, prywatny plik, eksport CSV i błędne wejście.
R. Restart, upgrade i restore: dane i zachowanie pozostają spójne.
S. Wszystkie ekrany: pełny Tycoon, login/błędy/offline, telefon/komputer, brak pominiętych assetów.

Szerokości 360, 390, 768 i 1440 px. Sprawdź klawiaturę ekranową, focus, dotyk, powiększenie tekstu, ograniczony ruch, brak poziomego przewijania i zasłoniętych przycisków. Jeśli fizyczny telefon lub czytnik ekranu nie jest dostępny, nie zapisuj takiego testu jako zaliczony.

Uruchom istniejące testy i nowe regresje dla potwierdzonych problemów. Popraw wykryte błędy i ponów odpowiednie testy. Zakończ wynikiem liczbowym: wykonane, zaliczone, błędy, pominięte oraz co pozostało niesprawdzone.

## 15. Dostarczenie i definicja gotowości

Dostarcz:
1. Działający kod na osobnej gałęzi i Pull Request z opisem rezultatu oraz wynikami.
2. Zamkniętą listę R01–R12: naprawiono / nie występuje z dowodem / jawny blokujący brak środowiska. Nie znikają z raportu bez wyjaśnienia.
3. Raport finalny z macierzą funkcji i scenariuszy, audytem dostępności, pomiarami i ograniczeniami.
4. Krótką instrukcję właściciela: rozmowa → rezerwacja → wykonanie → zamknięcie dnia; osobno koszty, konfiguracja i backup.
5. Spójną instrukcję uruchomienia Windows i przygotowania stagingu na własnym serwerze.
6. Pobieralny ZIP z klikalnym podglądem pełnego Tycoona opartym na końcowej aplikacji. Dane syntetyczne, symulowany zapis jawny. Zachowaj robocze wartości formularzy między widokami demonstracji.
7. Identyfikację: commit kodu, SHA-256 JAR/ZIP i zgodność podglądu z poprawionym interfejsem. Sprawdź pobranie paczki i otwarcie w przeglądarce/obsługiwanym mechanizmie, także w ramce jeśli to dostępny kanał.

Nie podawaj samego chmurowego localhost ani niedostępnej ścieżki /workspace jako dostępu użytkownika. Nie twierdź, że hosting/podgląd działa, dopóki nie został rzeczywiście sprawdzony. Brak możliwości utworzenia PR (np.403 API) opisz, wyślij gałąź i zapisz gotowy opis oraz porównanie; nie wymyślaj numeru PR.

**Wersję można uznać za domkniętą**, gdy potwierdzone P1 są naprawione, Tycoon jest spójny i domyślny, złote ścieżki przechodzą, historyczne dane są zachowane, zapis/odnowienie sesji/konflikty są przewidywalne, a build/restore i instrukcja odpowiadają rzeczywistości.

Pozostałe ograniczenia muszą być jawne. Gdy materialny blokujący problem pozostaje, napisz NIEGOTOWE i konkretny powód, dostarczając pozostałą wykonaną pracę. Nie kończ ogólnym „wszystko działa”, samą liczbą testów ani obietnicą aplikacji bez przyszłych błędów.

Celem jest stabilny ncLEAN CRM do sumiennego, codziennego używania. Dopiero po tym etapie wracamy do nowych pomysłów.
