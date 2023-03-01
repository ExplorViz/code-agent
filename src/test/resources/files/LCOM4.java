package com.easy.life;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

// LCOM4=1
public class LCOM4 {
    int a;

    private void methodA() {
        a = 4;
    }

    public void methodB() {
        int b = a;
    }
}

// LCOM4=1
class LCOM4Class2 {
    public String someField = "";
    public int someOtherField = 1;

    public void SomeMethod() {
        Clazz clazz = new Clazz();
        String someField = "sameName, different variable";
        someOtherField = 6;
        String someOtherField = "empty";
        someOtherField = someField.length();
        int a = someField.split(":").clone().getClass().getName().length();
        clazz.field.value = "ads";
        someField = "";
    }

    public void theOtherMethod(int a) {
        int b = a + someOtherField;
    }
}

// LCOM4 = 1
class LCOM4Class3 {
    private void methodA() {
        String str = "";
    }

    public void methodB() {
        methodA();
    }
}

// LCOM4=0 all methods belong to <List>
class LCOM4Class4 implements List {

    @Override
    public int size() {
        throw new UnsupportedOperationException("Unimplemented method 'size'");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Unimplemented method 'isEmpty'");
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Unimplemented method 'contains'");
    }

    @Override
    public Iterator iterator() {
        throw new UnsupportedOperationException("Unimplemented method 'iterator'");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Unimplemented method 'toArray'");
    }

    @Override
    public Object[] toArray(Object[] a) {
        throw new UnsupportedOperationException("Unimplemented method 'toArray'");
    }

    @Override
    public boolean add(Object e) {
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    @Override
    public boolean containsAll(Collection c) {
        throw new UnsupportedOperationException("Unimplemented method 'containsAll'");
    }

    @Override
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException("Unimplemented method 'addAll'");
    }

    @Override
    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException("Unimplemented method 'addAll'");
    }

    @Override
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException("Unimplemented method 'removeAll'");
    }

    @Override
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException("Unimplemented method 'retainAll'");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Unimplemented method 'clear'");
    }

    @Override
    public Object get(int index) {
        throw new UnsupportedOperationException("Unimplemented method 'get'");
    }

    @Override
    public Object set(int index, Object element) {
        throw new UnsupportedOperationException("Unimplemented method 'set'");
    }

    @Override
    public void add(int index, Object element) {
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public Object remove(int index) {
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Unimplemented method 'indexOf'");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Unimplemented method 'lastIndexOf'");
    }

    @Override
    public ListIterator listIterator() {
        throw new UnsupportedOperationException("Unimplemented method 'listIterator'");
    }

    @Override
    public ListIterator listIterator(int index) {
        throw new UnsupportedOperationException("Unimplemented method 'listIterator'");
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Unimplemented method 'subList'");
    }

}

// LCOM4 = 1
class LCOM4Class5 {

    public void entryMethod() {
        this.methodA();
        this.methodB();
    }

    private void methodA() {
        String str = "";
    }
    public void methodB() {
        methodA();
        methodC();
    }
    private void methodC() {
        methodD(methodE());
    }
    private void methodD(int a) {
        System.out.println(a);
    }
    private int methodE() {
        return 1337;
    }
}

// LCOM4 = 6
class LCOM4Class6 {
    public void entryMethod() {
        String str = "";
    }

    private void methodA() {
        String str = "";
    }
    public void methodB() {
        String str = "";;
    }
    private void methodC() {
        String str = "";
    }
    private void methodD(int a) {
        System.out.println(a);
    }
    private int methodE() {
        return 1337;
    }
}