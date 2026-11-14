# Paczka ncLEAN CRM — dla administratora

Cel: pilotaż na Androidzie przez `https://nclean.pl/crm/`, bez subdomeny i bez
modyfikowania bazy CMS. To osobna aplikacja Java17/Spring Boot/PostgreSQL.

1. Przeczytaj [raport](REPORT.md) i [RUNBOOK](../../deploy/ops/RUNBOOK.md).
2. Potwierdź aktualne repozytorium CMS i faktyczną infrastrukturę serwera.
   Nie potwierdzono ich z Codexa; dawny audyt archiwum jest historyczny.
3. Wybierz pełną wersję kodu wskazaną w manifeście paczki. Main może być starszy.
4. Skonfiguruj osobną bazę, trwałe dane, role i sekrety poza checkoutem.
   Zdjęcia usług i rachunki są w PostgreSQL. Nie wystarczy eksport CSV.
5. Dopasuj systemd i fragmenty nginx do istniejącego serwera HTTPS; nie podmieniaj
   routingu publicznej strony. Przykłady są w `deploy/`, konfiguracje ops w `deploy/ops/`.
6. Najpierw uruchom izolowaną próbę, potem backup/restore i aktualizację z danymi.
7. Sprawdź telefon: logowanie, galerię/aparat, klawiaturę i zapis po restarcie.
8. Ustaw kopie poza serwerem i alerty. Nie kasuj wolumenu podczas aktualizacji.
9. Dopiero po odbiorze zaplanuj podłączenie właściwego adresu z właścicielem.

Pełna paczka zawiera zbudowany `nclean-crm.jar`. Mniejsza paczka źródłowa nie
zawiera JAR-a: administrator buduje go poleceniem `./mvnw verify` z JDK17
i Dockerem do testów. Manifest wskazuje wariant paczki. `release-manifest.json`
zawiera commit źródeł, SHA256 i wyniki sprawdzeń; `SHA256SUMS` sumy plików.
Przesłany JAR porównaj z sumą, umieść w niezmiennym katalogu wydania i uruchom
zgodnie z RUNBOOK. Klienci PostgreSQL17 są potrzebni narzędziom kopii/restore.

`preview/index.html` jest samodzielną demonstracją z syntetycznymi danymi.
Nie jest aplikacją serwerową, backupem ani miejscem do zbierania danych klientów.
Eksport ćwiczenia JSON nie obejmuje zdjęć i szkiców formularzy. Docelowy zapis
zleceń i zdjęć działa w PostgreSQL prawdziwej aplikacji.

Nie ma w paczce haseł, `.local`, dumpów bazy, certyfikatów, konfiguracji serwera
ani danych klientów. Po wdrożeniu nie nadpisuj własnych sekretów plikami z ZIP-a.
Nie wykonuj restore nad aktywną bazą ani powrotu starego kodu bez oceny migracji.
