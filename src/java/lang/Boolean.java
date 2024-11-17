/*
 * Copyright (c) 1994, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import jdk.internal.vm.annotation.IntrinsicCandidate;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDescs;
import java.util.Optional;

/**
 * {@code Boolean}クラスは、プリミティブ型{@code boolean}の値をオブジェクトにラップします。
 * {@code Boolean}型のオブジェクトには、型が{@code boolean}である単一のフィールドが含まれています。
 * さらに、このクラスは、{@code boolean}を{@code String}に変換したり、{@code String}を{@code boolean}に変換したりするための多くのメソッドや、
 * {@code boolean}を扱う際に便利な他の定数やメソッドを提供します。
 * これは値ベースのクラスであり、プログラマーは等価なインスタンスを交換可能なものとして扱うべきです。
 * インスタンスを同期に使用すると予測不可能な動作が発生する可能性があるため、使用しないようにしてください。たとえば、将来のリリースで同期が失敗する場合があります。
 *
 * @author  Arthur van Hoff
 * @since   1.0
 */
@jdk.internal.ValueBased
public final class Boolean implements java.io.Serializable,
                                      Comparable<Boolean>, Constable
{
    /**
     *
     * プリミティブ値{@code true}に対応する{@code Boolean}オブジェクトです。
     */
    public static final Boolean TRUE = new Boolean(true);

    /**
     * The {@code Boolean} object corresponding to the primitive
     * value {@code false}.
     */
    public static final Boolean FALSE = new Boolean(false);

    /**
     * プリミティブ型 boolean を表す Class オブジェクトです。
     *
     * @since   1.1
     */
    @SuppressWarnings("unchecked")
    public static final Class<Boolean> TYPE = (Class<Boolean>) Class.getPrimitiveClass("boolean");

    /**
     * The value of the Boolean.
     *
     * @serial
     */
    private final boolean value;

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = -3665804199014368530L;

    /**
     *
     * 引数の値を表す {@code Boolean} オブジェクトを割り当てます。
     *
     * @param   value   the value of the {@code Boolean}.
     *
     * @deprecated
     * このコンストラクタを使用するのは稀です。通常、{@code valueOf(boolean)}という静的ファクトリーメソッドを使う方が、
     * スペースと時間のパフォーマンスが大幅に向上する可能性が高いため、そちらが推奨されます。
     * また、可能であれば、{@code TRUE}および{@code FALSE}という定数フィールドを使用することを検討してください。
     */
    @Deprecated(since="9", forRemoval = true)
    public Boolean(boolean value) {
        this.value = value;
    }

    /**
     *
     * 文字列引数が {@code null} でなく、かつ文字列 {@code "true"}（大文字と小文字を区別せず）と等しい場合、
     * {@code true} を表す {@code Boolean} オブジェクトを割り当てます。それ以外の場合は、{@code false} を表す {@code Boolean} オブジェクトを割り当てます。
     *
     * @param   s   the string to be converted to a {@code Boolean}.
     *
     * @deprecated
     * このコンストラクタを使用することはほとんど適切ではありません。
     * 文字列を {@code boolean} 型のプリミティブに変換するには {@code parseBoolean(String)} を使用し、
     * {@code Boolean} オブジェクトに変換するには {@code valueOf(String)} を使用することをお勧めします。
     */
    @Deprecated(since="9", forRemoval = true)
    public Boolean(String s) {
        this(parseBoolean(s));
    }

    /**
     * 文字列引数を {@code boolean} として解析します。返される {@code boolean} 値は、文字列引数が {@code null} でなく、
     * かつ文字列 {@code "true"}（大文字と小文字を区別しない）と等しい場合は {@code true} となります。
     * それ以外の場合、引数が {@code null} の場合も含めて {@code false} が返されます。
     * <p>
     *     例: {@code Boolean.parseBoolean("True")} は {@code true} を返します。<br>
     *     例: {@code Boolean.parseBoolean("yes")} は {@code false} を返します。
     * </p>
     *
     * @param      s   the {@code String} containing the boolean
     *                 representation to be parsed
     * @return     the boolean represented by the string argument
     * @since 1.5
     */
    public static boolean parseBoolean(String s) {
        return "true".equalsIgnoreCase(s);
    }

    /**
     * Returns the value of this {@code Boolean} object as a boolean
     * primitive.
     *
     * @return  the primitive {@code boolean} value of this object.
     */
    @IntrinsicCandidate
    public boolean booleanValue() {
        return value;
    }

    /**
     * 指定された {@code boolean} 値を表す {@code Boolean} インスタンスを返します。
     * 指定された {@code boolean} 値が {@code true} の場合、このメソッドは {@code Boolean.TRUE} を返し、
     * false の場合は {@code Boolean.FALSE} を返します。
     * 新しい {@code Boolean} インスタンスが不要な場合、このメソッドは通常、{@link Boolean}(boolean) コンストラクタの代わりに使用すべきです。
     * このメソッドは、空間と時間のパフォーマンスが大幅に改善される可能性が高いためです。
     *
     * @param  b a boolean value.
     * @return a {@code Boolean} instance representing {@code b}.
     * @since  1.4
     */
    @IntrinsicCandidate
    public static Boolean valueOf(boolean b) {
        return (b ? TRUE : FALSE);
    }

    /**
     * 指定された文字列で表される値を持つ {@code Boolean} を返します。
     * 返される {@code Boolean} は、文字列引数が {@code null} でなく、かつ文字列 {@code "true"} と等しい（大文字小文字を区別しない）場合、
     * {@code true} の値を表します。それ以外の場合、{@code null} 引数を含めて、{@code false} の値が返されます。
     *
     * @param   s   a string.
     * @return  the {@code Boolean} value represented by the string.
     */
    public static Boolean valueOf(String s) {
        return parseBoolean(s) ? TRUE : FALSE;
    }

    /**
     * 指定された文字列で表される値を持つ {@code Boolean} を返します。
     * 返される {@code Boolean} は、文字列引数が {@code null} でなく、かつ文字列 {@code "true"} と等しい（大文字と小文字を区別しない）場合、
     * {@code true} 値を表します。それ以外の場合、{@code false} 値が返されます。{@code null} 引数の場合も {@code false} が返されます。
     *
     * @param b the boolean to be converted
     * @return the string representation of the specified {@code boolean}
     * @since 1.4
     */
    public static String toString(boolean b) {
        return b ? "true" : "false";
    }

    /**
     * Returns a {@code String} object representing this Boolean's
     * value.  If this object represents the value {@code true},
     * a string equal to {@code "true"} is returned. Otherwise, a
     * string equal to {@code "false"} is returned.
     *
     * @return  a string representation of this object.
     */
    public String toString() {
        return value ? "true" : "false";
    }

    /**
     * Returns a hash code for this {@code Boolean} object.
     *
     * @return  the integer {@code 1231} if this object represents
     * {@code true}; returns the integer {@code 1237} if this
     * object represents {@code false}.
     */
    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }

    /**
     * Returns a hash code for a {@code boolean} value; compatible with
     * {@code Boolean.hashCode()}.
     *
     * @param value the value to hash
     * @return a hash code value for a {@code boolean} value.
     * @since 1.8
     */
    public static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

    /**
     * 引数が null でなく、このオブジェクトと同じ boolean 値を表す Boolean オブジェクトである場合に限り、true を返します。
     *
     * @param   obj   the object to compare with.
     * @return  {@code true} if the Boolean objects represent the
     *          same value; {@code false} otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Boolean) {
            return value == ((Boolean)obj).booleanValue();
        }
        return false;
    }

    /**
     * R引数で指定された名前のシステムプロパティが存在し、その値が（大文字と小文字を区別せずに）文字列 "true" と等しい場合にのみ、
     * true を返します。システムプロパティは、System クラスで定義されている getProperty メソッドを使ってアクセスできます。
     * 指定された名前のプロパティが存在しない場合、または指定された名前が空文字列または null の場合は、false が返されます。
     *
     * @param   name   the system property name.
     * @return  the {@code boolean} value of the system property.
     * @throws  SecurityException for the same reasons as
     *          {@link System#getProperty(String) System.getProperty}
     * @see     System#getProperty(String)
     * @see     System#getProperty(String, String)
     */
    public static boolean getBoolean(String name) {
        boolean result = false;
        try {
            result = parseBoolean(System.getProperty(name));
        } catch (IllegalArgumentException | NullPointerException e) {
        }
        return result;
    }

    /**
     * Compares this {@code Boolean} instance with another.
     *
     * @param   b the {@code Boolean} instance to be compared
     * @return
     * このオブジェクトが引数と同じ {@code boolean} 値を表す場合は 0 を返し、このオブジェクトが {@code true} を表し引数が {@code false} を表す場合は正の値を返し、
     * このオブジェクトが {@code false} を表し引数が {@code true} を表す場合は負の値を返します。
     * @throws  NullPointerException if the argument is {@code null}
     * @see     Comparable
     * @since  1.5
     */
    public int compareTo(Boolean b) {
        return compare(this.value, b.value);
    }

    /**
     * Compares two {@code boolean} values.
     * The value returned is identical to what would be returned by:
     * <pre>
     *    Boolean.valueOf(x).compareTo(Boolean.valueOf(y))
     * </pre>
     *
     * @param  x the first {@code boolean} to compare
     * @param  y the second {@code boolean} to compare
     * @return the value {@code 0} if {@code x == y};
     *         a value less than {@code 0} if {@code !x && y}; and
     *         a value greater than {@code 0} if {@code x && !y}
     * @since 1.7
     */
    public static int compare(boolean x, boolean y) {
        return (x == y) ? 0 : (x ? 1 : -1);
    }

    /**
     * 指定された {@code boolean} オペランドに論理 AND 演算子を適用した結果を返します。
     *
     * @param a the first operand
     * @param b the second operand
     * @return the logical AND of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     * @since 1.8
     */
    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    /**
     * Returns the result of applying the logical OR operator to the
     * specified {@code boolean} operands.
     *
     * @param a the first operand
     * @param b the second operand
     * @return the logical OR of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     * @since 1.8
     */
    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    /**
     * Returns the result of applying the logical XOR operator to the
     * specified {@code boolean} operands.
     *
     * @param a the first operand
     * @param b the second operand
     * @return  the logical XOR of {@code a} and {@code b}
     * @see java.util.function.BinaryOperator
     * @since 1.8
     */
    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }

    /**
     * Returns an {@link Optional} containing the nominal descriptor for this
     * instance.
     *
     * @return an {@link Optional} describing the {@linkplain Boolean} instance
     * @since 15
     */
    @Override
    public Optional<DynamicConstantDesc<Boolean>> describeConstable() {
        return Optional.of(value ? ConstantDescs.TRUE : ConstantDescs.FALSE);
    }
}
