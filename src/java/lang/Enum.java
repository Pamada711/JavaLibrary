/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * これは、すべてのJava言語の列挙型クラスの共通の基底クラスです。
 * 列挙型に関する詳細情報（コンパイラによって自動的に生成される暗黙的に宣言されたメソッドの説明を含む）は、Java言語仕様のセクション @jls 8.9 に記載されています。
 *
 * 列挙型クラスはすべてシリアライズ可能であり、シリアライズ機構によって特別に扱われます。
 * ただし、列挙型定数に使用されるシリアライズ表現はカスタマイズできません。
 * シリアライズと相互作用する可能性のあるメソッドやフィールドの宣言（例えば {@code serialVersionUID} を含む）は無視されます。
 * 詳細は、Javaオブジェクトシリアライズ仕様を参照してください。
 * また、列挙型をセットの型やマップのキーの型として使用する場合、特化され効率的なセットやマップの実装が利用可能であることに注意してください。
 *
 * @param <E> The type of the enum subclass
 * @serial exclude
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Class#getEnumConstants()
 * @see     java.util.EnumSet
 * @see     java.util.EnumMap
 * @jls 8.9 Enum Classes
 * @jls 8.9.3 Enum Members
 * @since   1.5
 */
@SuppressWarnings("serial") // No serialVersionUID needed due to
                            // special-casing of enum classes.
public abstract class Enum<E extends Enum<E>>
        implements Constable, Comparable<E>, Serializable {
    /**
     * この列挙型定数の名前は、列挙型宣言で宣言された名前です。
     * ほとんどのプログラマーは、このフィールドに直接アクセスするのではなく、{@code toString} メソッドを使用するべきです。
     */
    private final String name;

    /**
     *
     * この列挙型定数の名前を、列挙型宣言で宣言された通りに正確に返します。
     * ほとんどのプログラマーは、よりユーザーフレンドリーな名前を返す可能性のある {@code toString} メソッドを優先して使用するべきです。
     * このメソッドは、リリースごとに変わらない正確な名前を取得することが重要な特殊な状況での使用を主に目的としています。
     *
     * @return the name of this enum constant
     */
    public final String name() {
        return name;
    }

    /**
     * この列挙型定数の序数（列挙型宣言内での位置。最初の定数には序数 0 が割り当てられます）。ほとんどのプログラマーにとって、このフィールドを使用する機会はありません。
     * このフィールドは、{@link java.util.EnumSet} や {@link java.util.EnumMap} のような高度な列挙型ベースのデータ構造での使用を目的としています。
     */
    private final int ordinal;

    /**
     * この列挙型定数の序数（列挙型宣言内での位置。最初の定数には序数 0 が割り当てられます）を返します。ほとんどのプログラマーにとって、このメソッドを使用する必要はありません。
     * このメソッドは、{@link java.util.EnumSet} や {@link java.util.EnumMap} のような高度な列挙型ベースのデータ構造での使用を目的としています。
     *
     * @return the ordinal of this enumeration constant
     */
    public final int ordinal() {
        return ordinal;
    }

    /**
     * 唯一のコンストラクタです。プログラマーはこのコンストラクタを呼び出すことはできません。
     * これは、列挙型クラスの宣言に応じてコンパイラによって生成されたコードで使用されることを目的としています。
     *
     * @param name - The name of this enum constant, which is the identifier
     *               used to declare it.
     * @param ordinal - この列挙定数の序数（列挙宣言における位置を表し、最初の定数には0が割り当てられます）。
     */
    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    /**
     * この列挙定数の名前を、宣言内で定義されているとおりに返します。
     * このメソッドはオーバーライド可能ですが、通常は必要もしくは望ましくありません。
     * より「プログラマーフレンドリー」な文字列表現が必要な場合には、列挙型クラスでこのメソッドをオーバーライドするべきです。
     *
     * @return the name of this enum constant
     */
    public String toString() {
        return name;
    }

    /**
     * Returns true if the specified object is equal to this
     * enum constant.
     *
     * @param other the object to be compared for equality with this object.
     * @return  true if the specified object is equal to this
     *          enum constant.
     */
    public final boolean equals(Object other) {
        return this==other;
    }

    /**
     * Returns a hash code for this enum constant.
     *
     * @return a hash code for this enum constant.
     */
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * {@link CloneNotSupportedException} をスローします。
     * これにより、列挙型がクローンされることは決してなくなり、列挙型の「シングルトン」ステータスを保持することが保証されます。
     *
     * @return (never returns)
     */
    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Compares this enum with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * Enum constants are only comparable to other enum constants of the
     * same enum type.  The natural order implemented by this
     * method is the order in which the constants are declared.
     */
    public final int compareTo(E o) {
        Enum<?> other = (Enum<?>)o;
        Enum<E> self = this;
        if (self.getClass() != other.getClass() && // optimization
            self.getDeclaringClass() != other.getDeclaringClass())
            throw new ClassCastException();
        return self.ordinal - other.ordinal;
    }

    /**
     * Returns the Class object corresponding to this enum constant's
     * enum type.  Two enum constants e1 and  e2 are of the
     * same enum type if and only if
     *   e1.getDeclaringClass() == e2.getDeclaringClass().
     * (The value returned by this method may differ from the one returned
     * by the {@link Object#getClass} method for enum constants with
     * constant-specific class bodies.)
     *
     * @return the Class object corresponding to this enum constant's
     *     enum type
     */
    @SuppressWarnings("unchecked")
    public final Class<E> getDeclaringClass() {
        Class<?> clazz = getClass();
        Class<?> zuper = clazz.getSuperclass();
        return (zuper == Enum.class) ? (Class<E>)clazz : (Class<E>)zuper;
    }

    /**
     * Returns an enum descriptor {@code EnumDesc} for this instance, if one can be
     * constructed, or an empty {@link Optional} if one cannot be.
     *
     * @return An {@link Optional} containing the resulting nominal descriptor,
     * or an empty {@link Optional} if one cannot be constructed.
     * @since 12
     */
    @Override
    public final Optional<EnumDesc<E>> describeConstable() {
        return getDeclaringClass()
                .describeConstable()
                .map(c -> EnumDesc.of(c, name));
    }

    /**
     * Returns the enum constant of the specified enum class with the
     * specified name.  The name must match exactly an identifier used
     * to declare an enum constant in this class.  (Extraneous whitespace
     * characters are not permitted.)
     *
     * <p>Note that for a particular enum class {@code T}, the
     * implicitly declared {@code public static T valueOf(String)}
     * method on that enum may be used instead of this method to map
     * from a name to the corresponding enum constant.  All the
     * constants of an enum class can be obtained by calling the
     * implicit {@code public static T[] values()} method of that
     * class.
     *
     * @param <T> The enum class whose constant is to be returned
     * @param enumClass the {@code Class} object of the enum class from which
     *      to return a constant
     * @param name the name of the constant to return
     * @return the enum constant of the specified enum class with the
     *      specified name
     * @throws IllegalArgumentException if the specified enum class has
     *         no constant with the specified name, or the specified
     *         class object does not represent an enum class
     * @throws NullPointerException if {@code enumClass} or {@code name}
     *         is null
     * @since 1.5
     */
    public static <T extends Enum<T>> T valueOf(Class<T> enumClass,
                                                String name) {
        T result = enumClass.enumConstantDirectory().get(name);
        if (result != null)
            return result;
        if (name == null)
            throw new NullPointerException("Name is null");
        throw new IllegalArgumentException(
            "No enum constant " + enumClass.getCanonicalName() + "." + name);
    }

    /**
     * enum classes cannot have finalize methods.
     */
    @SuppressWarnings("deprecation")
    protected final void finalize() { }

    /**
     * prevent default deserialization
     */
    @java.io.Serial
    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException {
        throw new InvalidObjectException("can't deserialize enum");
    }

    @java.io.Serial
    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("can't deserialize enum");
    }

    /**
     * A <a href="{@docRoot}/java.base/java/lang/constant/package-summary.html#nominal">nominal descriptor</a> for an
     * {@code enum} constant.
     *
     * @param <E> the type of the enum constant
     *
     * @since 12
     */
    public static final class EnumDesc<E extends Enum<E>>
            extends DynamicConstantDesc<E> {

        /**
         * Constructs a nominal descriptor for the specified {@code enum} class and name.
         *
         * @param constantClass a {@link ClassDesc} describing the {@code enum} class
         * @param constantName the unqualified name of the enum constant
         * @throws NullPointerException if any argument is null
         * @jvms 4.2.2 Unqualified Names
         */
        private EnumDesc(ClassDesc constantClass, String constantName) {
            super(ConstantDescs.BSM_ENUM_CONSTANT, requireNonNull(constantName), requireNonNull(constantClass));
        }

        /**
         * Returns a nominal descriptor for the specified {@code enum} class and name
         *
         * @param <E> the type of the enum constant
         * @param enumClass a {@link ClassDesc} describing the {@code enum} class
         * @param constantName the unqualified name of the enum constant
         * @return the nominal descriptor
         * @throws NullPointerException if any argument is null
         * @jvms 4.2.2 Unqualified Names
         * @since 12
         */
        public static<E extends Enum<E>> EnumDesc<E> of(ClassDesc enumClass,
                                                        String constantName) {
            return new EnumDesc<>(enumClass, constantName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public E resolveConstantDesc(MethodHandles.Lookup lookup)
                throws ReflectiveOperationException {
            return Enum.valueOf((Class<E>) constantType().resolveConstantDesc(lookup), constantName());
        }

        @Override
        public String toString() {
            return String.format("EnumDesc[%s.%s]", constantType().displayName(), constantName());
        }
    }
}
