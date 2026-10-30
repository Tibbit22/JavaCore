import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestPlanScanner {

    public static List<TestStep> buildPlan(Class<?> clazz) {
        List<TestStep> plan = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(TestId.class)) {
                TestId annotation = method.getAnnotation(TestId.class);
                plan.add(new TestStep(annotation.value(), method.getName(), annotation.order()));
            }
        }

        plan.sort(Comparator.comparingInt(TestStep::order));
        return plan;
    }

    public static void printPlan(Class<?> clazz) {
        List<TestStep> plan = buildPlan(clazz);

        System.out.println("План прогона для " + clazz.getSimpleName() + ":");
        if (plan.isEmpty()) {
            System.out.println("  Кейсов нет");
            return;
        }
        for (int i = 0; i < plan.size(); i++) {
            TestStep step = plan.get(i);
            System.out.println("  " + (i + 1) + ". " + step.id() + " → " + step.methodName());
        }
    }

    public static void main(String[] args) {
        printPlan(Checklist.class);
        System.out.println();
        printPlan(Object.class);   // класс без аннотированных методов: T4
    }
}