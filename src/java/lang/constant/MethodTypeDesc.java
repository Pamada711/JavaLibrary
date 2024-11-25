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

import java.lang.invoke.MethodType;
import java.lang.invoke.TypeDescriptor;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@linkplain MethodType} constant.
 *
 * @since 12
 */
public sealed interface MethodTypeDesc
        extends ConstantDesc,
                TypeDescriptor.OfMethod<ClassDesc, MethodTypeDesc>
        permits MethodTypeDescImpl {
    /**
     * メソッド記述子文字列を与えると、MethodTypeDesc を作成します
     *
     * @param descriptor メソッド記述子文字列
     * @return 所望のメソッド型を記述する MethodTypeDesc
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException 記述子文字列が有効なメソッド記述子でない場合
     * @jvms 4.3.3 Method Descriptors
     */
    static MethodTypeDesc ofDescriptor(String descriptor) {
        return MethodTypeDescImpl.ofDescriptor(descriptor);
    }

    /**
     * 戻り値とパラメーター型を基に MethodTypeDesc を返します
     *
     * @param returnDesc 戻り値の型を記述する ClassDesc
     * @param paramDescs 引数の型を記述する ClassDesc の配列
     * @return 所望のメソッド型を記述する MethodTypeDesc
     * @throws NullPointerException if any argument or its contents are {@code null}
     * @throws IllegalArgumentException paramDescs の要素のいずれかが void の ClassDesc の場合
     */
    static MethodTypeDesc of(ClassDesc returnDesc, ClassDesc... paramDescs) {
        return new MethodTypeDescImpl(returnDesc, paramDescs);
    }

    /**
     * この MethodTypeDesc によって記述されたメソッド型の戻り値の型を取得します。
     *
     * @return メソッド型の戻り値の型を記述する ClassDesc
     */
    ClassDesc returnType();

    /**
     * この MethodTypeDesc が記述するメソッド型のパラメーターの数を返します
     * @return パラメーターの数
     */
    int parameterCount();

    /**
     * この MethodTypeDesc が記述するメソッド型において、指定されたインデックス index のパラメーターの型を返します。
     *
     * @param index 取得したいパラメーターのインデックス。
     * @return 指定されたパラメーターの型を記述する ClassDesc
     * @throws IndexOutOfBoundsException index が範囲 [0, parameterCount()) の外にある場合にスローされます
     */
    ClassDesc parameterType(int index);

    /**
     * この MethodTypeDesc が記述するメソッド型のパラメーター型を、不変のリストとして返します
     *
     * @return パラメーター型を記述する ClassDesc のリスト
     */
    List<ClassDesc> parameterList();

    /**
     * Returns the parameter types as an array.
     *
     * @return an array of {@link ClassDesc} describing the parameter types
     */
    ClassDesc[] parameterArray();

    /**
     * このメソッドは、現在の MethodTypeDesc と同一で、指定された戻り値の型を持つ新しい MethodTypeDesc を返します
     *
     * @param returnType 新しい戻り値の型を記述する ClassDesc
     * @return 所望のメソッド型を記述する MethodTypeDesc
     * @throws NullPointerException 引数が null の場合にスローされます
     */
    MethodTypeDesc changeReturnType(ClassDesc returnType);

    /**
     * このメソッドは、現在の MethodTypeDesc と同一で、指定された位置のパラメーター型が新しい型に変更された新しい MethodTypeDesc を返します
     *
     * @param index 変更するパラメーターの位置（インデックス）
     * @param paramType 新しいパラメーター型を記述する ClassDesc
     * @return 指定されたパラメーター型を変更したメソッド型を記述する MethodTypeDesc
     * @throws NullPointerException 引数が null の場合にスローされます
     * @throws IndexOutOfBoundsException インデックスが範囲 [0, parameterCount) の外の場合にスローされます
     */
    MethodTypeDesc changeParameterType(int index, ClassDesc paramType);

    /**
     * このメソッドは、現在の MethodTypeDesc と同一で、指定された範囲のパラメーター型が削除された新しい MethodTypeDesc を返します
     *
     * @param start 削除を開始する最初のパラメーターのインデックス
     * @param end 削除を終了する最後のパラメーターのインデックス（削除しないパラメーターの次のインデックス）
     * @return 指定された範囲のパラメーター型が削除された新しい MethodTypeDesc
     * @throws IndexOutOfBoundsException start が範囲 [0, parameterCount) の外、または end が範囲 [0, parameterCount] の外、または start > end の場合にスローされます。
     */
    MethodTypeDesc dropParameterTypes(int start, int end);

    /**
     * このメソッドは、現在の MethodTypeDesc と同一で、指定された位置に新しいパラメーター型が挿入された新しい MethodTypeDesc を返します
     *
     * @param pos 最初の新しいパラメーターを挿入するインデックス
     * @param paramTypes 挿入する新しいパラメーター型を記述する ClassDesc の配列
     * @return 新しいパラメーター型が挿入された MethodTypeDesc
     * @throws NullPointerException 引数またはその内容が null の場合にスローされます
     * @throws IndexOutOfBoundsException pos が範囲 [0, parameterCount] の外にある場合にスローされます。
     * @throws IllegalArgumentException paramTypes のいずれかの要素が void の ClassDesc である場合にスローされます。
     */
    MethodTypeDesc insertParameterTypes(int pos, ClassDesc... paramTypes);

    /**
     * このメソッドは、メソッド型記述子の文字列を返します
     *
     * @return メソッド型記述子を表す文字列
     * @jvms 4.3.3 Method Descriptors
     */
    default String descriptorString() {
        return String.format("(%s)%s",
                             Stream.of(parameterArray())
                                   .map(ClassDesc::descriptorString)
                                   .collect(Collectors.joining()),
                             returnType().descriptorString());
    }

    /**
     * このメソッドは、パラメーターと戻り値の型の正式名称を使用して、このメソッド型の人間が読みやすい記述子を返します
     *
     * @return このメソッド型の人間が読みやすい記述子
     */
    default String displayDescriptor() {
        return String.format("(%s)%s",
                             Stream.of(parameterArray())
                                   .map(ClassDesc::displayName)
                                   .collect(Collectors.joining(",")),
                             returnType().displayName());
    }

    /**
     * 指定されたオブジェクトとこの記述子を比較して等しいかを判断します。
     * 指定されたオブジェクトが MethodTypeDesc であり、両者が同じアリティ（引数の数）を持ち、戻り値の型が等しく、
     * 対応する各パラメーター型が等しい場合にのみ、true を返します。
     *
     * @param o the other object
     * @return この記述子が他のオブジェクトと等しいかどうか
     */
    boolean equals(Object o);
}
