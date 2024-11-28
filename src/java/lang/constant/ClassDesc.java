/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.constant;

import sun.invoke.util.Wrapper;

import java.lang.invoke.TypeDescriptor;
import java.util.stream.Stream;

import static java.lang.constant.ConstantUtils.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * クラス定数のための名目的記述子。 すべてのプリミティブ型を含む一般的なシステム型については、ConstantDescsに事前定義されたClassDesc定数があります。
 * （java.lang.constant APIでは、voidはプリミティブ型と見なされます。）クラスやインターフェース型のClassDescを作成するには、of または ofDescriptor(String) を使用します。
 * 配列型のClassDescを作成するには、ofDescriptor(String) を使用するか、まずコンポーネント型のClassDescを取得してから、arrayType() または arrayType(int) メソッドを呼び出します。
 * ClassDesc は、Javaの型に関する情報を動的に扱いたい場面で非常に役立つクラスです。特に、リフレクションや動的クラス生成のような高度なシナリオでその威力を発揮します。
 * また、java.lang.constant パッケージ全体の一部として、Javaバイトコードレベルでの型情報操作の標準化にも寄与します。
 *
 * @see ConstantDescs
 *
 * @since 12
 */
public sealed interface ClassDesc
        extends ConstantDesc,
                TypeDescriptor.OfField<ClassDesc>
        permits PrimitiveClassDescImpl,
                ReferenceClassDescImpl {

    /**
     * クラスまたはインターフェイスタイプの名前（例えば、"java.lang.String"）を指定して、そのクラスまたはインターフェイス型を表す ClassDesc を返します。
     * （配列型の記述子を作成するには、ofDescriptor(String) または arrayType() を使用してください。
     * プリミティブ型の記述子を作成するには、ofDescriptor(String) を使用するか、ConstantDescs に定義されている定数を使用してください）。
     *
     * @param name 完全修飾された（ドットで区切られた）バイナリクラス名
     * @return 指定されたクラスを記述する ClassDesc
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException 名前文字列が正しい形式でない場合
     */
    static ClassDesc of(String name) {
        ConstantUtils.validateBinaryClassName(requireNonNull(name));
        return ClassDesc.ofDescriptor("L" + binaryToInternal(name) + ";");
    }

    /**
     * パッケージ名とクラスまたはインターフェイスの限定されていない（単純な）名前を指定して、そのクラスまたはインターフェイスタイプを表す ClassDesc を返します
     *
     * @param packageName パッケージ名（ドットで区切られた形式）。パッケージ名が空文字列の場合、そのクラスは無名パッケージに属すると見なされます。
     * @param className 限定されていない（単純な）クラス名
     * @return 指定されたクラスを記述する ClassDesc
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the package name or class name are
     * not in the correct format
     */
    static ClassDesc of(String packageName, String className) {
        ConstantUtils.validateBinaryClassName(requireNonNull(packageName));
        if (packageName.isEmpty()) {
            return of(className);
        }
        validateMemberName(requireNonNull(className), false);
        return ofDescriptor("L" + binaryToInternal(packageName) +
                (packageName.length() > 0 ? "/" : "") + className + ";");
    }

    /**
     * 指定されたクラス、インターフェイス、配列、またはプリミティブ型の記述子文字列から ClassDesc を返します
     *
     * @apiNote
     *
     * 非配列型のフィールド型記述子文字列は、プリミティブ型を表す1文字のコード（"J"、"I"、"C"、"S"、"B"、"D"、"F"、"Z"、"V"）のいずれか、
     * または文字 "L"、クラスの完全修飾バイナリ名、そして ";" で構成されます。配列型のフィールド型記述子は、コンポーネント型のフィールド記述子の前に文字 "[" を付加します。
     * 有効な型記述子文字列の例としては、"Ljava/lang/String;"、"I"、"[I"、"V"、"[Ljava/lang/String;" などがあります。
     * 詳細は JVMS 4.3.2 ("Field Descriptors") を参照してください。
     *
     * @param descriptor フィールド記述子文字列
     * @return 指定されたクラスを記述する ClassDesc
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException if the name string is not in the
     * correct format
     * @jvms 4.3.2 Field Descriptors
     * @jvms 4.4.1 The CONSTANT_Class_info Structure
     */
    static ClassDesc ofDescriptor(String descriptor) {
        requireNonNull(descriptor);
        if (descriptor.isEmpty()) {
            throw new IllegalArgumentException(
                    "not a valid reference type descriptor: " + descriptor);
        }
        int depth = ConstantUtils.arrayDepth(descriptor);
        if (depth > ConstantUtils.MAX_ARRAY_TYPE_DESC_DIMENSIONS) {
            throw new IllegalArgumentException(
                    "Cannot create an array type descriptor with more than " +
                    ConstantUtils.MAX_ARRAY_TYPE_DESC_DIMENSIONS + " dimensions");
        }
        return (descriptor.length() == 1)
               ? new PrimitiveClassDescImpl(descriptor)
               : new ReferenceClassDescImpl(descriptor);
    }

    /**
     * この ClassDesc によって記述されたコンポーネント型を持つ配列型の ClassDesc を返します。
     *
     * @return 配列型を記述する ClassDesc
     * @throws IllegalStateException  結果として得られる ClassDesc の配列ランクが 255 を超える場合
     * @jvms 4.4.1 The CONSTANT_Class_info Structure
     */
    default ClassDesc arrayType() {
        int depth = ConstantUtils.arrayDepth(descriptorString());
        if (depth >= ConstantUtils.MAX_ARRAY_TYPE_DESC_DIMENSIONS) {
            throw new IllegalStateException(
                    "Cannot create an array type descriptor with more than " +
                    ConstantUtils.MAX_ARRAY_TYPE_DESC_DIMENSIONS + " dimensions");
        }
        return arrayType(1);
    }

    /**
     * この ClassDesc によって記述されたコンポーネント型を持つ、指定されたランクの配列型の ClassDesc を返します。
     *
     * @param rank 配列のランク（次元数）
     * @return 配列型を記述する ClassDesc
     * @throws IllegalArgumentException ランクが 0 以下である場合、または結果として得られる配列型のランクが 255 を超える場合
     * @jvms 4.4.1 The CONSTANT_Class_info Structure
     */
    default ClassDesc arrayType(int rank) {
        int currentDepth = ConstantUtils.arrayDepth(descriptorString());
        if (rank <= 0 || currentDepth + rank > ConstantUtils.MAX_ARRAY_TYPE_DESC_DIMENSIONS)
            throw new IllegalArgumentException("rank: " + currentDepth + rank);
        return ClassDesc.ofDescriptor("[".repeat(rank) + descriptorString());
    }

    /**
     * この ClassDesc によって記述されるクラスまたはインターフェース型のネストされたクラスを記述する ClassDesc を返します。
     *
     * @apiNote
     *
     * 例: 記述子 d がクラス java.util.Map を記述する場合、クラス java.util.Map.Entry の記述子は d.nested("Entry") によって取得できます。
     *
     * @param nestedName ネストされたクラスの単純名（修飾されていない名前）
     * @return ネストされたクラスを記述する ClassDesc
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalStateException この ClassDesc がクラスまたはインターフェース型を記述していない場合
     * @throws IllegalArgumentException ネストされたクラス名が無効な場合
     */
    default ClassDesc nested(String nestedName) {
        validateMemberName(nestedName, false);
        if (!isClassOrInterface())
            throw new IllegalStateException("Outer class is not a class or interface type");
        return ClassDesc.ofDescriptor(dropLastChar(descriptorString()) + "$" + nestedName + ";");
    }

    /**
     * この ClassDesc によって記述されるクラスまたはインターフェース型のネストされたクラスを記述する ClassDesc を返します。
     *
     * @param firstNestedName 最初のレベルのネストされたクラスの単純名（修飾されていない名前）
     * @param moreNestedNames 残りのレベルのネストされたクラスの単純名（修飾されていない名前）
     * @return ネストされたクラスを記述する ClassDesc
     * @throws NullPointerException if any argument or its contents is {@code null}
     * @throws IllegalStateException この ClassDesc がクラスまたはインターフェース型を記述していない場合
     * @throws IllegalArgumentException ネストされたクラス名が無効な場合
     */
    default ClassDesc nested(String firstNestedName, String... moreNestedNames) {
        if (!isClassOrInterface())
            throw new IllegalStateException("Outer class is not a class or interface type");
        validateMemberName(firstNestedName, false);
        requireNonNull(moreNestedNames);
        for (String addNestedNames : moreNestedNames) {
            validateMemberName(addNestedNames, false);
        }
        return moreNestedNames.length == 0
               ? nested(firstNestedName)
               : nested(firstNestedName + Stream.of(moreNestedNames).collect(joining("$", "$", "")));
    }

    /**
     * この ClassDesc が配列型を記述しているかどうかを返します。
     *
     * @return この ClassDesc が配列型を記述している場合は true、そうでない場合は false
     */
    default boolean isArray() {
        return descriptorString().startsWith("[");
    }

    /**
     * この ClassDesc がプリミティブ型を記述しているかどうかを返します。
     *
     * @return この ClassDesc がプリミティブ型を記述している場合は true、そうでない場合は false
     */
    default boolean isPrimitive() {
        return descriptorString().length() == 1;
    }

    /**
     * この ClassDesc がクラスまたはインターフェイスタイプを記述しているかどうかを返します。
     *
     * @return この ClassDesc がクラスまたはインターフェイスタイプを記述している場合は true、そうでない場合は false
     */
    default boolean isClassOrInterface() {
        return descriptorString().startsWith("L");
    }

    /**
     * この ClassDesc が配列型を記述している場合、その要素型を返します。それ以外の場合は null を返します
     *
     * @return 要素型を記述する ClassDesc、またはこの記述子が配列型を記述していない場合は null
     */
    default ClassDesc componentType() {
        return isArray() ? ClassDesc.ofDescriptor(descriptorString().substring(1)) : null;
    }

    /**
     * この ClassDesc がクラスまたはインターフェース型を記述している場合、そのパッケージ名を返します。
     *
     * @return パッケージ名。クラスがデフォルトパッケージにある場合は空文字列、またはこの ClassDesc がクラスやインターフェース型を記述していない場合も空文字列。
     */
    default String packageName() {
        if (!isClassOrInterface())
            return "";
        String className = internalToBinary(ConstantUtils.dropFirstAndLastChar(descriptorString()));
        int index = className.lastIndexOf('.');
        return (index == -1) ? "" : className.substring(0, index);
    }

    /**
     * この記述子によって記述される型の人間が読める名前を返します。
     *
     * @implSpec
     * デフォルトの実装は、以下のように動作します：
     * <ul>
     *     <li>プリミティブ型の場合、単純な名前（例: int）を返します。
     *     <li>クラスまたはインターフェース型の場合、修飾されていないクラス名を返します。
     *     <li>配列型の場合、要素型の表示名に適切な数の [] を付加した名前を返します。
     * </ul>
     * @return 人間が読める名前。
     */
    default String displayName() {
        if (isPrimitive())
            return Wrapper.forBasicType(descriptorString().charAt(0)).primitiveSimpleName();
        else if (isClassOrInterface()) {
            return descriptorString().substring(Math.max(1, descriptorString().lastIndexOf('/') + 1),
                                                descriptorString().length() - 1);
        }
        else if (isArray()) {
            int depth = ConstantUtils.arrayDepth(descriptorString());
            ClassDesc c = this;
            for (int i=0; i<depth; i++)
                c = c.componentType();
            return c.displayName() + "[]".repeat(depth);
        }
        else
            throw new IllegalStateException(descriptorString());
    }

    /**
     * この型に対するフィールド型記述子文字列を返します。
     *
     * @return the descriptor string
     * @jvms 4.3.2 Field Descriptors
     */
    String descriptorString();

    /**
     * 指定されたオブジェクトをこの記述子と比較して等価性を確認します。同じ型を記述する ClassDesc である場合にのみ、true を返します。
     *
     * @param o the other object
     * @return この記述子が他のオブジェクトと等しいかどうか
     */
    boolean equals(Object o);
}
