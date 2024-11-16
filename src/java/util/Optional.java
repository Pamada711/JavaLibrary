/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 非nullの値を含む場合と含まない場合があるコンテナオブジェクトです。
 * 値が存在する場合、isPresent()はtrueを返します。値が存在しない場合、そのオブジェクトは空と見なされ、isPresent()はfalseを返します。
 * 含まれる値の有無に依存する追加メソッドも提供されます。
 * たとえば、orElse()は値が存在しない場合にデフォルト値を返し、ifPresent()は値が存在する場合にアクションを実行します。
 * このクラスは値ベースのクラスであり、プログラマーは等価と見なされるインスタンスを交換可能なものとして扱うべきです。
 * また、インスタンスを同期のために使用してはいけません。
 * そうしないと予測不能な動作が発生する可能性があります。たとえば、将来のリリースでは同期が失敗することがあり得ます。
 *
 * @apiNote
 * Optionalは主に、結果が「存在しない」ことを明確に表現する必要がある場合や、
 * nullを使用するとエラーが発生する可能性が高い場合に、メソッドの戻り値の型として使用されることを意図しています。
 * 型がOptionalの変数は、自身がnullになるべきではありません。常にOptionalインスタンスを指す必要があります。
 *
 * @param <T> the type of value
 * @since 1.8
 */
@jdk.internal.ValueBased
public final class Optional<T> {
    /**
     * Common instance for {@code empty()}.
     */
    private static final Optional<?> EMPTY = new Optional<>(null);

    /**
     * 非nullの場合は値を指し、nullの場合は値が存在しないことを示します。
     */
    private final T value;

    /**
     * 空のOptionalインスタンスを返します。このOptionalには値が存在しません。
     *
     * @apiNote
     *
     * 魅力的に思えるかもしれませんが、Optional.empty()が返すインスタンスと==または!=で比較して
     * オブジェクトが空であるかどうかをテストすることは避けてください。
     * そのインスタンスがシングルトンである保証はありません。
     * 代わりに、isEmpty()またはisPresent()メソッドを使用してください。
     *
     * @param <T> The type of the non-existent value
     * @return an empty {@code Optional}
     */
    public static<T> Optional<T> empty() {
        @SuppressWarnings("unchecked")
        Optional<T> t = (Optional<T>) EMPTY;
        return t;
    }

    /**
     * 指定された値を持つインスタンスを構築します。
     *
     * @param value 記述する値。この値が非`null`であることを保証するのは呼び出し元の責任です。
     *              ただし、`empty()`によって返されるシングルトンインスタンスを作成する場合は例外です。
     */
    private Optional(T value) {
        this.value = value;
    }

    /**
     * 指定された非nullの値を記述するOptionalを返します。
     *
     * @param value the value to describe, which must be non-{@code null}
     * @param <T> the type of the value
     * @return an {@code Optional} with the value present
     * @throws NullPointerException if value is {@code null}
     */
    public static <T> Optional<T> of(T value) {
        return new Optional<>(Objects.requireNonNull(value));
    }

    /**
     * 指定された値が非`null`であればそれを記述する`Optional`を返し、そうでない場合は空の`Optional`を返します。
     *
     * @param value the possibly-{@code null} value to describe
     * @param <T> the type of the value
     * @return an {@code Optional} with a present value if the specified value
     *         is non-{@code null}, otherwise an empty {@code Optional}
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> ofNullable(T value) {
        return value == null ? (Optional<T>) EMPTY
                             : new Optional<>(value);
    }

    /**
     * 値が存在する場合はその値を返し、そうでない場合は {@code NoSuchElementException}をスローします。
     *
     * @apiNote
     * このメソッドの推奨される代替方法は、{@code orElseThrow()}です。
     *
     * @return the non-{@code null} value described by this {@code Optional}
     * @throws NoSuchElementException if no value is present
     */
    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    /**
     * If a value is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a value is present, otherwise {@code false}
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * If a value is  not present, returns {@code true}, otherwise
     * {@code false}.
     *
     * @return  {@code true} if a value is not present, otherwise {@code false}
     * @since   11
     */
    public boolean isEmpty() {
        return value == null;
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     * @throws NullPointerException if value is present and the given action is
     *         {@code null}
     */
    public void ifPresent(Consumer<? super T> action) {
        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * 値が存在する場合は、その値を使って指定されたアクションを実行し、そうでない場合は指定された空のアクションを実行します。
     *
     * @param action the action to be performed, if a value is present
     * @param emptyAction the empty-based action to be performed, if no value is
     *        present
     * @throws NullPointerException if a value is present and the given action
     *         is {@code null}, or no value is present and the given empty-based
     *         action is {@code null}.
     * @since 9
     */
    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (value != null) {
            action.accept(value);
        } else {
            emptyAction.run();
        }
    }

    /**
     * 値が存在し、その値が指定された述語と一致する場合は、その値を記述するOptionalを返し、一致しない場合は空のOptionalを返します。
     *
     * @param predicate the predicate to apply to a value, if present
     * @return an {@code Optional} describing the value of this
     *         {@code Optional}, if a value is present and the value matches the
     *         given predicate, otherwise an empty {@code Optional}
     * @throws NullPointerException if the predicate is {@code null}
     */
    public Optional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (!isPresent()) {
            return this;
        } else {
            return predicate.test(value) ? this : empty();
        }
    }

    /**
     * 値が存在する場合は、指定されたマッピング関数をその値に適用した結果を記述するOptionalを返し、
     * 値が存在しない場合は空のOptionalを返します。
     * もしマッピング関数がnullを返す場合、このメソッドは空のOptionalを返します。
     *
     * @apiNote
     * このメソッドは、Optionalの値に対して後処理を行うことをサポートしており、戻り値の状態を明示的にチェックする必要はありません。
     * 例えば、以下のコードはURIのストリームをトラバースし、まだ処理されていないURIを選択してそのURIからパスを作成し、Optional<Path>を返します
     *
     * <pre>{@code
     *     Optional<Path> p =
     *         uris.stream().filter(uri -> !isProcessedYet(uri))
     *                       .findFirst()
     *                       .map(Paths::get);
     * }</pre>
     *
     * このコードでは、urisストリームをフィルタリングして処理されていないURIを選び、それをPathに変換し、Optional<Path>を返します。
     *
     * @param mapper the mapping function to apply to a value, if present
     * @param <U> The type of the value returned from the mapping function
     * @return 値が存在する場合は、このOptionalの値にマッピング関数を適用した結果を記述するOptionalを返し、値が存在しない場合は空のOptionalを返します。
     * @throws NullPointerException if the mapping function is {@code null}
     */
    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            return Optional.ofNullable(mapper.apply(value));
        }
    }

    /**
     * 値が存在する場合は、指定されたOptionalを返すマッピング関数をその値に適用し、値が存在しない場合は空のOptionalを返します。
     * このメソッドはmap(Function)に似ていますが、マッピング関数がすでにOptionalを返すものであり、
     * 呼び出された場合にflatMapはそのOptionalをさらに別のOptionalでラップしません。
     *
     * @param <U> The type of value of the {@code Optional} returned by the
     *            mapping function
     * @param mapper the mapping function to apply to a value, if present
     * @return 値が存在する場合は、このOptionalの値にOptionalを返すマッピング関数を適用した結果を返し、値が存在しない場合は空のOptionalを返します。
     * @throws NullPointerException if the mapping function is {@code null} or
     *         returns a {@code null} result
     */
    public <U> Optional<U> flatMap(Function<? super T, ? extends Optional<? extends U>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            @SuppressWarnings("unchecked")
            Optional<U> r = (Optional<U>) mapper.apply(value);
            return Objects.requireNonNull(r);
        }
    }

    /**
     * 値が存在する場合は、その値を記述するOptionalを返し、値が存在しない場合は、提供された関数によって生成されたOptionalを返します。
     *
     * @param supplier the supplying function that produces an {@code Optional}
     *        to be returned
     * @return returns an {@code Optional} describing the value of this
     *         {@code Optional}, if a value is present, otherwise an
     *         {@code Optional} produced by the supplying function.
     * @throws NullPointerException if the supplying function is {@code null} or
     *         produces a {@code null} result
     * @since 9
     */
    public Optional<T> or(Supplier<? extends Optional<? extends T>> supplier) {
        Objects.requireNonNull(supplier);
        if (isPresent()) {
            return this;
        } else {
            @SuppressWarnings("unchecked")
            Optional<T> r = (Optional<T>) supplier.get();
            return Objects.requireNonNull(r);
        }
    }

    /**
     *
     * 値が存在する場合は、その値のみを含む順次的なStreamを返し、値が存在しない場合は空のStreamを返します。
     *
     * @apiNote
     * このメソッドは、Optionalの要素からなるStreamを、値が存在する要素のみを含むStreamに変換するために使用できます。
     * 例えば、以下のように使用します：
     * <pre>{@code
     *     Stream<Optional<T>> os = ..
     *     Stream<T> s = os.flatMap(Optional::stream)
     * }</pre>
     *
     * @return the optional value as a {@code Stream}
     * @since 9
     */
    public Stream<T> stream() {
        if (!isPresent()) {
            return Stream.empty();
        } else {
            return Stream.of(value);
        }
    }

    /**
     * If a value is present, returns the value, otherwise returns
     * {@code other}.
     *
     * @param other the value to be returned, if no value is present.
     *        May be {@code null}.
     * @return the value, if present, otherwise {@code other}
     */
    public T orElse(T other) {
        return value != null ? value : other;
    }

    /**
     * If a value is present, returns the value, otherwise returns the result
     * produced by the supplying function.
     *
     * @param supplier the supplying function that produces a value to be returned
     * @return the value, if present, otherwise the result produced by the
     *         supplying function
     * @throws NullPointerException if no value is present and the supplying
     *         function is {@code null}
     */
    public T orElseGet(Supplier<? extends T> supplier) {
        return value != null ? value : supplier.get();
    }

    /**
     * If a value is present, returns the value, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the non-{@code null} value described by this {@code Optional}
     * @throws NoSuchElementException if no value is present
     * @since 10
     */
    public T orElseThrow() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    /**
     * If a value is present, returns the value, otherwise throws an exception
     * produced by the exception supplying function.
     *
     * @apiNote
     * 引数リストが空の例外コンストラクタへのメソッド参照を、サプライヤとして使用することができます。
     * 例えば、{@code IllegalStateException::new}のように使用します。
     * <pre>{@code
     *     Optional<String> value = Optional.empty();
     *     String result = value.orElseThrow(IllegalStateException::new);
     * }</pre>
     * この例では、IllegalStateException::newがサプライヤとして渡され、valueが空の場合に例外がスローされます。
     *
     * @param <X> Type of the exception to be thrown
     * @param exceptionSupplier the supplying function that produces an
     *        exception to be thrown
     * @return the value, if present
     * @throws X if no value is present
     * @throws NullPointerException if no value is present and the exception
     *          supplying function is {@code null}
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (value != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     *
     * 他のオブジェクトがこのOptionalと「等しい」かどうかを示します。他のオブジェクトは次の条件を満たす場合に等しいと見なされます：
     * <ul>
     * <li>それも{@code Optional}であること。
     * <li>両方のインスタンスに値が存在しないか、または
     * <li>存在する値がequals()メソッドで比較して等しいこと。
     * </ul>
     * つまり、両方が空のOptionalであれば等しく、両方に値が存在し、その値が等しい場合にも等しいと見なされます。
     *
     * @param obj an object to be tested for equality
     * @return {@code true} if the other object is "equal to" this object
     *         otherwise {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof Optional<?> other
                && Objects.equals(value, other.value);
    }

    /**
     * 値が存在する場合はその値のハッシュコードを返し、値が存在しない場合は0（ゼロ）を返します。
     *
     * @return hash code value of the present value or {@code 0} if no value is
     *         present
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * このOptionalのデバッグに適した非空の文字列表現を返します。
     * 具体的な表示形式は未指定であり、実装やバージョンによって異なる場合があります。
     *
     * @implSpec
     * 値が存在する場合、その結果には値の文字列表現が含まれている必要があります。
     * 空のOptionalと値が存在するOptionalは、明確に区別できる必要があります。
     *
     * @return the string representation of this instance
     */
    @Override
    public String toString() {
        return value != null
            ? ("Optional[" + value + "]")
            : "Optional.empty";
    }
}
