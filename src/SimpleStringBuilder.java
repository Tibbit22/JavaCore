import java.util.ArrayDeque;
import java.util.Deque;

public class SimpleStringBuilder {

    private final Deque<String> snapshots = new ArrayDeque<>();
    private String text = "";

    public SimpleStringBuilder append(String s) {
        snapshots.push(text);          // snapshot: куда возвращаться при undo()
        text = text + s;               // null -> "null", как в обычном StringBuilder
        return this;
    }

    public boolean undo() {
        if (snapshots.isEmpty()) {
            return false;
        }
        text = snapshots.pop();        // восстанавливаем последний снимок
        return true;
    }

    public int undoDepth() {
        return snapshots.size();
    }

    @Override
    public String toString() {
        return text;
    }

    public static void main(String[] args) {
        SimpleStringBuilder b = new SimpleStringBuilder();
        b.append("Hello");
        System.out.println(b);
        b.append(", ");
        System.out.println(b);
        b.append("world");
        System.out.println(b);
        b.undo();
        System.out.println(b);
        b.append("AQA!");
        System.out.println(b);
        b.undo();
        b.undo();
        System.out.println(b);
        System.out.println("Осталось откатов: " + b.undoDepth() + ", undo() -> " + b.undo());

    }
}
