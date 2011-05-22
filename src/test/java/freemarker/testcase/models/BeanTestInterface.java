package freemarker.testcase.models;

public interface BeanTestInterface<T>
{
    T getSomething();
    void setSomething(T s);
}
