# Klikalny podgląd Pixel Tycoon — TEST

Pobierz **[nclean-pixel-tycoon-test.zip](https://github.com/nikodemwierchlewski-web/ncleancrm/raw/refs/heads/experiment/cleaning-tycoon-progress/preview/nclean-pixel-tycoon-test.zip)**. Jeżeli GitHub wymaga dostępu do repozytorium, zaloguj się na konto z dostępem. Możesz też otworzyć plik ZIP w tym katalogu i wybrać „Download raw file”.

1. Rozpakuj ZIP do zwykłego folderu na komputerze.
2. Otwórz `index.html` w Chrome, Edge lub Firefox.
3. Użyj „Klasyczny”, żeby porównać wygląd; listą u góry zmienisz ekran.

To samodzielny HTML z zasobami w środku, bez Javy i PostgreSQL. Dane są syntetyczne, zmiany pozostają w pamięci otwartej karty. Odświeżenie resetuje próbkę. Nie wpisuj prawdziwych danych klientów. Backend sprawdzono osobno.

Paczka zawiera animacje, odznaki i postęp z eksperymentu `experiment/cleaning-tycoon-progress`. W podsumowaniu rozwiń „Odznaki i historia”; ruch ograniczysz w podsumowaniu lub ustawieniach. Domknięcie przykładowego dnia przyznaje odznakę w pamięci demonstracji. Dołączono licencje fontów. Normalny CRM zachowuje klasyczny wygląd jako domyślny.

## Dlaczego wcześniejszy link nie działał

Odnośnik `/workspace/...` wskazywał plik środowiska chmurowego, którego panel tej sesji nie potrafił wczytać. Sam plik istnieje, a lokalny serwer zwraca HTTP200; to nie dowodzi dostępu użytkownika do serwera. Środowisko nie deklaruje pulpitu/przekierowania portów (`capabilities: []`); `open_in_codex` zwraca „This app tool is unavailable through dynamic tools”. Dlatego paczka jest dostarczona przez repozytorium, a nie ponownie przez nieskuteczną ścieżkę chmurową.
