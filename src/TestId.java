import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 RUNTIME нужен, чтобы аннотация была видна через reflection во время работы программы.
 Если поставить CLASS, аннотация попадёт в .class-файл, но JVM не будет её
 загружать. method.isAnnotationPresent(TestId.class) вернёт false, сканер
 найдёт 0 кейсов и напечатает «Кейсов нет». Ошибки не будет, поэтому такой
 сбой сложно заметить.

 При SOURCE аннотация вообще удаляется компилятором и в .class не попадает,
 то есть сканер не найдёт её даже теоретически.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestId {
    String value();   // номер тест-кейса, например "TC-101"
    int order();      // порядок выполнения в прогоне
}