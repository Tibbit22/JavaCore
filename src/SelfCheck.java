import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelfCheck {

    private static int failed = 0;

    public static void main(String[] args) {
        parserBasic();
        repeatsKeepLast();
        repeatsTakePositionOfLastAttempt();
        brokenLinesDoNotCrash();
        firstFailedOptional();
        recordEqualsByValue();
        builderUndo();

        System.out.println(failed == 0 ? "ВСЕ ПРОВЕРКИ ПРОЙДЕНЫ" : "ПРОВАЛЕНО: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void parserBasic() {
        TestLogParser p = new TestLogParser();
        List<TestResult> r = p.parse(List.of("LoginTest|PASSED|140", "CommentTest|SKIPPED|0"));
        check("T1: две валидные строки", r.size() == 2);
        check("T1: ошибок нет", p.errors().isEmpty());
        check("T1: поля разобраны",
                r.get(0).equals(new TestResult("LoginTest", Status.PASSED, 140)));
    }

    private static void repeatsKeepLast() {
        TestLogParser p = new TestLogParser();
        List<TestResult> r = p.parse(List.of(
                "CreateTaskTest|FAILED|890",
                "CreateTaskTest|PASSED|920"));
        check("T2: осталась одна запись", r.size() == 1);
        check("T2: именно последняя попытка",
                r.get(0).equals(new TestResult("CreateTaskTest", Status.PASSED, 920)));
    }

    private static void repeatsTakePositionOfLastAttempt() {
        TestLogParser p = new TestLogParser();
        List<TestResult> r = p.parse(List.of("A|PASSED|1", "B|PASSED|2", "A|FAILED|3"));
        check("T2: порядок — по месту последней попытки",
                List.of("B", "A").equals(r.stream().map(TestResult::name).toList()));
    }

    private static void brokenLinesDoNotCrash() {
        TestLogParser p = new TestLogParser();
        List<TestResult> r = p.parse(Arrays.asList(
                "LoginTest|PASSED|140",      // валидная
                "CreateTaskTest!FAILED|89o", // не число в длительности
                "NoSeparatorPASSED",         // нет разделителя
                "CommentTest|PASED|10",      // неизвестный статус
                "NegTest|FAILED|-5",         // отрицательная длительность
                "|PASSED|10",                // пустое имя
                "Empty|PASSED|",             // пустая длительность
                "Too|PASSED|1|2",            // лишнее поле
                "   ",                       // пустая — не ошибка
                "",                          // пустая — не ошибка
                null));                      // null — не ошибка

        check("T1: одна валидная строка", r.size() == 1);
        check("T1: семь ошибок, пустые строки не считаются", p.errors().size() == 7);

        List<String> reasons = new ArrayList<>();
        p.errors().forEach(e -> reasons.add(e.reason()));

        check("T1: опечатка в разделителе -> не число в длительности: 89o",
                reasons.get(0).equals("не число в длительности: 89o"));
        check("T1: нет разделителя", reasons.get(1).contains("нет разделителя"));
        check("T1: неизвестный статус: PASED",
                reasons.get(2).equals("неизвестный статус: PASED"));
        check("T1: отрицательная длительность", reasons.get(3).contains("отрицательная длительность"));
        check("T1: пустое имя теста", reasons.get(4).contains("пустое имя"));
        check("T1: пустая длительность", reasons.get(5).contains("пустая длительность"));
        check("T1: лишнее поле", reasons.get(6).contains("найдено 4"));
        check("T1: номер строки сохранён", p.errors().get(0).lineNumber() == 2);
    }

    private static void firstFailedOptional() {
        TestLogParser p = new TestLogParser();
        List<TestResult> results = List.of(
                new TestResult("A", Status.PASSED, 1),
                new TestResult("B", Status.FAILED, 2),
                new TestResult("C", Status.FAILED, 3));

        check("T3: найден первый упавший",
                p.firstFailed(results).map(TestResult::name).orElse("").equals("B"));
        check("T3: без падений — Optional.empty",
                p.firstFailed(List.of(new TestResult("A", Status.PASSED, 1))).isEmpty());
        check("T3: null-список — Optional.empty", p.firstFailed(null).isEmpty());
        check("T3: пустой список — Optional.empty", p.firstFailed(List.of()).isEmpty());
    }

    private static void recordEqualsByValue() {
        check("record: equals по значению",
                new TestResult("A", Status.PASSED, 1).equals(new TestResult("A", Status.PASSED, 1)));
        check("record: hashCode согласован с equals",
                new TestResult("A", Status.PASSED, 1).hashCode()
                        == new TestResult("A", Status.PASSED, 1).hashCode());
        check("record: разные значения не равны",
                !new TestResult("A", Status.PASSED, 1).equals(new TestResult("A", Status.PASSED, 2)));
        check("record: remove ищет по значению",
                new ArrayList<>(List.of(new TestResult("A", Status.PASSED, 1)))
                        .remove(new TestResult("A", Status.PASSED, 1)));
    }

    private static void builderUndo() {
        SimpleStringBuilder b = new SimpleStringBuilder();
        b.append("Hello").append(", ").append("world");
        check("builder: собрано", "Hello, world".equals(b.toString()));
        b.undo();
        check("builder: один undo", "Hello, ".equals(b.toString()));
        b.append("AQA!");
        check("builder: append после undo", "Hello, AQA!".equals(b.toString()));
        b.undo();
        b.undo();
        check("builder: повторный undo откатывает предыдущий", "Hello".equals(b.toString()));
        b.undo();
        b.undo();
        b.undo();
        check("builder: лишние undo не ломают", "".equals(b.toString()));
        check("builder: undo на пустой истории -> false", !b.undo());
    }

    private static void check(String title, boolean ok) {
        System.out.println((ok ? "[ok]   " : "[FAIL] ") + title);
        if (!ok) {
            failed++;
        }
    }
}