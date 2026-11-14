# Prompt audytu i naprawy kandydata do pilotażu ncLEAN CRM

Sprawdź pełny CRM z aktualnej gałęzi, nie stary main ani sam HTML demonstracyjny.
Pracuj na osobnej gałęzi. Zachowaj dane, historyczne ceny, wygląd ncLEAN i ncLEANKA,
OWNER/CSRF, PostgreSQL, Thymeleaf oraz testy granic architektury. Nie twórz PR,
nie wdrażaj produkcji, nie dodawaj SMS/GPS i nie łącz się z CMS ani jego bazą.

1. Zrób inwentaryzację funkcji, endpointów, zapisów, zdjęć, migracji i konfiguracji.
   Zapisz ustalenia z priorytetami, dowodami i statusem; odróżnij aktualny kod od
   archiwum NcleanCMS, a środowisko testowe od nieznanego serwera właściciela.
2. Napraw pełną obsługę ścieżek / i /crm: HTML, JS, CSS, JSON, formularze,
   eksporty, logowanie, cookies, obrazy, manifest i PWA. Użyj wspólnego kontraktu
   adresowania, bez kopiowania /crm do każdego modułu. Nie wyłączaj zabezpieczeń.
   Worker może usuwać tylko własny cache i nigdy cache CMS; nie cache'uj danych
   prywatnych. Sprawdź współistnienie publicznego stubu strony i CRM przez HTTPS.
3. Sprawdź błędy walidacji, sesji, konfliktów wersji, duplikatów i sieci. Napraw
   konkretne wykryte defekty. Błąd zapisu zachowuje dane formularza. Sprawdź
   klientów, cennik/chemię, kalendarz/obszary, realizacje/zdjęcia, koszty/paliwo,
   notatki, przypomnienia i zamknięcie dnia. Nie zastępuj niewiedzy zerem.
4. Umożliw uczciwe wprowadzanie dawnych realizacji bez wymyślonego imienia lub
   telefonu, z zachowaniem historii, cen i zdjęć. Wydziel tę regułę od zwykłego
   umawiania, oznacz wpis historyczny i nie uruchamiaj nim dawnych przypomnień.
   Nie zmieniaj V1–V15; ewentualny schemat rozszerz kolejną migracją i testem upgrade.
5. Przygotuj bezpieczny pakiet operacyjny: trwała baza niezależna od wydania,
   jawna tożsamość danych, backup bazy ze zdjęciami, weryfikacja i odtworzenie do
   nowej bazy, aktualizacja z obowiązkowym backupem oraz instrukcja powrotu.
   Nie kasuj wolumenów, nie nadpisuj sekretów ani kopii. Przetestuj restart,
   migrację istniejących danych i zgodność binarnych zdjęć po restore.
6. Wykonaj Maven verify z PostgreSQL, testy jednostkowe/MVC/architektury i nowe
   regresje. Sprawdź prawdziwy UI w Chromium 360/390/768/1440, osiem wpisów,
   formularze, błędy i ponowienie, wiele kategorii/obszarów i nawigację.
7. Przygotuj aktualny klikalny podgląd na podstawie poprawionej aplikacji,
   zweryfikuj go i spróbuj otworzyć obsługiwanym mechanizmem platformy. Jeśli
   viewer nie działa, dostarcz sprawdzony samodzielny HTML z eksportem/importem
   stanu demonstracji, wyraźnym opisem jego ograniczeń i bez sekretów. Nie
   przedstawiaj demonstracji jako produkcyjnego zapisu w PostgreSQL.
8. Podaj rzeczywiste wyniki i pozostałe ograniczenia, zapisz prompt i raport,
   instrukcję administratora oraz sprawdzoną paczkę bez haseł/danych klientów.
   Commit i push na osobną gałąź, bez PR i bez publikowania produkcji.
