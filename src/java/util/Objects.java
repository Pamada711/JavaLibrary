package java.util;

import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.ForceInline;

import java.util.function.Supplier;

/**
 * このクラスは、オブジェクトの操作や操作前に特定の条件を確認するための静的ユーティリティメソッドで構成されています。
 * これらのユーティリティには、オブジェクトのハッシュコードを計算するためのnull安全またはnull許容なメソッド、
 * オブジェクトの文字列を返すメソッド、2つのオブジェクトを比較するメソッド、
 * およびインデックスやサブレンジの値が範囲外であるかを確認するメソッドが含まれています。
 * @since 1.7
 */
public final class Objects {

    private Objects() {
        throw new AssertionError("No java.util.Objects instances for you!");
    }


    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
    public static boolean deepEquals(Object a, Object b) {
        if (a == b)
            return true;
        else if (a == null || b == null)
            return false;
        else
            return Arrays.deepEquals0(a, b);
    }


    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }
    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }
    public static String toString(Object o) {
        return String.valueOf(o);
    }
    public static String toString(Object o, String nullDefault) {
        return (o != null) ? o.toString() : nullDefault;
    }


    public static <T> int compare(T a, T b, Comparator<? super T> c) {
        return (a == b) ? 0 :  c.compare(a, b);
    }


    @ForceInline
    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

    @ForceInline
    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null)
            throw new NullPointerException(message);
        return obj;
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }
    public static boolean nonNull(Object obj) {
        return obj != null;
    }
    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        return (obj != null) ? obj : requireNonNull(defaultObj, "defaultObj");
    }
    public static <T> T requireNonNullElseGet(T obj, Supplier<? extends T> supplier) {
        return (obj != null) ? obj
                : requireNonNull(requireNonNull(supplier, "supplier").get(), "supplier.get()");
    }
    public static <T> T requireNonNull(T obj, Supplier<String> messageSupplier) {
        if (obj == null)
            throw new NullPointerException(messageSupplier == null ?
                                           null : messageSupplier.get());
        return obj;
    }

    @ForceInline
    public static
    int checkIndex(int index, int length) {
        return Preconditions.checkIndex(index, length, null);
    }

    public static
    int checkFromToIndex(int fromIndex, int toIndex, int length) {
        return Preconditions.checkFromToIndex(fromIndex, toIndex, length, null);
    }

    public static
    int checkFromIndexSize(int fromIndex, int size, int length) {
        return Preconditions.checkFromIndexSize(fromIndex, size, length, null);
    }


    @ForceInline
    public static
    long checkIndex(long index, long length) {
        return Preconditions.checkIndex(index, length, null);
    }

    public static
    long checkFromToIndex(long fromIndex, long toIndex, long length) {
        return Preconditions.checkFromToIndex(fromIndex, toIndex, length, null);
    }

    public static
    long checkFromIndexSize(long fromIndex, long size, long length) {
        return Preconditions.checkFromIndexSize(fromIndex, size, length, null);
    }
}
