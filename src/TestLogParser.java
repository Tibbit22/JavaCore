import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TestLogParser {

    private static final int FIELD_COUNT = 3;
    private static final char SEPARATOR = '|';

    private final List<ParseError> errors = new ArrayList<>();

    private final Set<TestResult> results = new LinkedHashSet<>();

    private final Map<String, TestResult> lastAttemptByName = new LinkedHashMap<>();

    public List<TestResult> parse(List<String> lines) {
        errors.clear();
        results.clear();
        lastAttemptByName.clear();

        if (lines == null) {
            return List.of();
        }

        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            int lineNumber = i + 1;

            if (raw == null || raw.isBlank()) {
                continue;                       // пустая строка — не ошибка
            }

            try {
                TestResult next = parseLine(raw);

                // T2: последняя попытка побеждает.
                TestResult previous = lastAttemptByName.put(next.name(), next);
                if (previous != null) {
                    results.remove(previous);   // equals/hashCode record -> находит по значению
                }
                results.add(next);

            } catch (InvalidLogLineException e) {
                errors.add(new ParseError(lineNumber, raw, e.getMessage()));
            }
        }
        return new ArrayList<>(results);
    }

    private TestResult parseLine(String raw) throws InvalidLogLineException {
        int firstSep = raw.indexOf(SEPARATOR);
        if (firstSep < 0) {
            throw new InvalidLogLineException("нет разделителя '" + SEPARATOR + "'");
        }

        String[] parts = raw.split("\\|", -1);

        if (parts.length > FIELD_COUNT) {
            throw new InvalidLogLineException(
                    "ожидалось " + FIELD_COUNT + " поля через '" + SEPARATOR
                            + "', найдено " + parts.length);
        }

        String durationText = parts[parts.length - 1].trim();
        if (durationText.isEmpty()) {
            throw new InvalidLogLineException("пустая длительность");
        }
        long durationMs;
        try {
            durationMs = Long.parseLong(durationText);
        } catch (NumberFormatException e) {
            throw new InvalidLogLineException("не число в длительности: " + durationText);
        }
        if (durationMs < 0) {
            throw new InvalidLogLineException("отрицательная длительность: " + durationMs);
        }

        if (parts.length < FIELD_COUNT) {
            throw new InvalidLogLineException(
                    "ожидалось " + FIELD_COUNT + " поля через '" + SEPARATOR
                            + "', найдено " + parts.length);
        }

        String name = parts[0].trim();
        String statusText = parts[1].trim();

        if (name.isEmpty()) {
            throw new InvalidLogLineException("пустое имя теста");
        }

        Status status = parseStatus(statusText);

        return new TestResult(name, status, durationMs);
    }

    private Status parseStatus(String statusText) throws InvalidLogLineException {
        if (statusText.isEmpty()) {
            throw new InvalidLogLineException("пустой статус");
        }
        try {
            return Status.valueOf(statusText);
        } catch (IllegalArgumentException ignored) {
            String hint = "";
            try {
                Status byCase = Status.valueOf(statusText.toUpperCase(Locale.ROOT));
                hint = " (ожидался статус в верхнем регистре: " + byCase + ")";
            } catch (IllegalArgumentException notJustCase) {
                hint = "";
            }
            throw new InvalidLogLineException("неизвестный статус: " + statusText + hint);
        }
    }

    public Optional<TestResult> firstFailed(List<TestResult> results) {
        if (results == null) {
            return Optional.empty();
        }
        return results.stream()
                .filter(r -> r.status() == Status.FAILED)
                .findFirst();
    }

    public List<ParseError> errors() {
        return Collections.unmodifiableList(errors);
    }

    public void printReport(List<TestResult> parsed) {
        System.out.println("Результаты:");
        if (parsed.isEmpty()) {
            System.out.println("  (нет)");
        }
        for (TestResult r : parsed) {
            System.out.printf("  %s — %s (%d ms)%n", r.name(), r.status(), r.durationMs());
        }

        printErrors();

        firstFailed(parsed).ifPresentOrElse(
                f -> System.out.printf("Первый упавший: %s — %s (%d ms)%n",
                        f.name(), f.status(), f.durationMs()),
                () -> System.out.println("Первый упавший: Упадений нет"));
    }

    private void printErrors() {
        if (errors.isEmpty()) {
            System.out.println("Битые строки: нет");
        } else if (errors.size() == 1) {
            System.out.println("Битые строки: " + errors.get(0));
        } else {
            System.out.println("Битые строки:");
            for (ParseError e : errors) {
                System.out.println("  " + e);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Path path = Path.of(args.length > 0 ? args[0] : "run.log");

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (NoSuchFileException e) {
            System.out.println("Файл лога не найден: " + path.toAbsolutePath());
            return;
        }

        TestLogParser parser = new TestLogParser();
        List<TestResult> parsed = parser.parse(lines);
        parser.printReport(parsed);
    }

    private static final class InvalidLogLineException extends Exception {

        private static final long serialVersionUID = 1L;

        InvalidLogLineException(String message) {
            super(message, null, false, false);   // без стека: это ожидаемая ситуация
        }
    }
}