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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.DirectMethodHandleDesc.Kind.CONSTRUCTOR;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@link MethodHandle} constant.
 *
 * @since 12
 */
public sealed interface MethodHandleDesc
        extends ConstantDesc
        permits AsTypeMethodHandleDesc,
                DirectMethodHandleDesc {

    /**
     * 宣言されたメソッドの呼び出し、コンストラクタの呼び出し、またはフィールドへのアクセスに対応する MethodHandleDesc を作成します。
     * ルックアップ記述子文字列は、CONSTANT_MethodHandle_info のさまざまなバリエーションや、MethodHandles.Lookup のルックアップメソッドと同じ形式を持っています。
     * メソッドやコンストラクタの呼び出しの場合、それはメソッド型記述子として解釈されます。
     * フィールドアクセスの場合、それはフィールド記述子として解釈されます。もし kind が CONSTRUCTOR の場合、name パラメーターは無視され、
     * ルックアップ記述子の戻り値の型は void でなければなりません。もし kind が仮想メソッド呼び出しに対応している場合、ルックアップ型はメソッドのパラメーターを含みますが、受信者型は含みません。
     *
     * @param kind 記述するメソッドハンドルの種類
     * @param owner メソッド、コンストラクタ、またはフィールドを含むクラスを記述する ClassDesc
     * @param name メソッドやフィールドの非修飾名（kind が CONSTRUCTOR の場合は無視される）
     * @param lookupDescriptor メソッド呼び出しの場合はメソッド記述子文字列、フィールドまたはコンストラクタ呼び出しの場合は呼び出しタイプを記述する記述子文字列
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException 無視されない引数が null の場合
     * @throws IllegalArgumentException 記述子文字列が有効なメソッドまたはフィールド記述子でない場合
     * @jvms 4.4.8 The CONSTANT_MethodHandle_info Structure
     * @jvms 4.2.2 Unqualified Names
     * @jvms 4.3.2 Field Descriptors
     * @jvms 4.3.3 Method Descriptors
     */
    static DirectMethodHandleDesc of(DirectMethodHandleDesc.Kind kind,
                                     ClassDesc owner,
                                     String name,
                                     String lookupDescriptor) {
        switch (kind) {
            case GETTER:
            case SETTER:
            case STATIC_GETTER:
            case STATIC_SETTER:
                return ofField(kind, owner, name, ClassDesc.ofDescriptor(lookupDescriptor));
            default:
                return new DirectMethodHandleDescImpl(kind, owner, name, MethodTypeDesc.ofDescriptor(lookupDescriptor));
        }
    }

    /**
     * 宣言されたメソッドまたはコンストラクタの呼び出しに対応する MethodHandleDesc を作成します。
     * ルックアップ記述子文字列は、MethodHandles.Lookup のルックアップメソッドと同じ形式を持ちます。
     * もし kind が CONSTRUCTOR の場合、name は無視され、ルックアップ型の戻り値の型は void でなければなりません。
     * もし kind が仮想メソッド呼び出しに対応する場合、ルックアップ型にはメソッドのパラメーターが含まれますが、受信者型は含まれません。
     *
     * @param kind 記述するメソッドハンドルの種類
     *             {@code SPECIAL, VIRTUAL, STATIC, INTERFACE_SPECIAL,
     *             INTERFACE_VIRTUAL, INTERFACE_STATIC, CONSTRUCTOR}
     * @param owner メソッドまたはコンストラクタを含むクラスを記述する ClassDesc
     * @param name メソッドやフィールドの非修飾名（kind が CONSTRUCTOR の場合は無視される）
     * @param lookupMethodType ルックアップ型を記述する MethodTypeDesc
     * @throws NullPointerException 無視されない引数が null の場合
     * @throws IllegalArgumentException name の形式が正しくない場合、または kind が無効な場合
     * @jvms 4.2.2 Unqualified Names
     */
    static DirectMethodHandleDesc ofMethod(DirectMethodHandleDesc.Kind kind,
                                           ClassDesc owner,
                                           String name,
                                           MethodTypeDesc lookupMethodType) {
        switch (kind) {
            case GETTER:
            case SETTER:
            case STATIC_GETTER:
            case STATIC_SETTER:
                throw new IllegalArgumentException(kind.toString());
            case VIRTUAL:
            case SPECIAL:
            case INTERFACE_VIRTUAL:
            case INTERFACE_SPECIAL:
            case INTERFACE_STATIC:
            case STATIC:
            case CONSTRUCTOR:
                return new DirectMethodHandleDescImpl(kind, owner, name, lookupMethodType);
            default:
                throw new IllegalArgumentException(kind.toString());
        }
    }

    /**
     * フィールドにアクセスするメソッドハンドルに対応する MethodHandleDesc を作成します
     *
     * @param kind 記述するメソッドハンドルの種類。以下のいずれかでなければなりません：GETTER、SETTER、STATIC_GETTER、または STATIC_SETTER
     * @param owner フィールドを含むクラスを記述する ClassDesc
     * @param fieldName フィールドの非修飾名
     * @param fieldType フィールドの型を記述する ClassDesc
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException 引数がいずれか1つでも null の場合
     * @throws IllegalArgumentException kind が有効な値のいずれかでない場合、またはフィールド名が無効な場合
     * @jvms 4.2.2 Unqualified Names
     */
    static DirectMethodHandleDesc ofField(DirectMethodHandleDesc.Kind kind,
                                          ClassDesc owner,
                                          String fieldName,
                                          ClassDesc fieldType) {
        MethodTypeDesc mtr = switch (kind) {
            case GETTER        -> MethodTypeDesc.of(fieldType, owner);
            case SETTER        -> MethodTypeDesc.of(CD_void, owner, fieldType);
            case STATIC_GETTER -> MethodTypeDesc.of(fieldType);
            case STATIC_SETTER -> MethodTypeDesc.of(CD_void, fieldType);
            default -> throw new IllegalArgumentException(kind.toString());
        };
        return new DirectMethodHandleDescImpl(kind, owner, fieldName, mtr);
    }

    /**
     * コンストラクタの呼び出しに対応する MethodHandleDesc を返します。
     *
     * @param owner コンストラクタを含むクラスを記述する ClassDesc
     * @param paramTypes コンストラクタのパラメーター型を記述する ClassDesc の配列
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException 引数またはその内容が null の場合
     */
    static DirectMethodHandleDesc ofConstructor(ClassDesc owner,
                                                ClassDesc... paramTypes) {
        return MethodHandleDesc.ofMethod(CONSTRUCTOR, owner, ConstantDescs.DEFAULT_NAME,
                                         MethodTypeDesc.of(CD_void, paramTypes));
    }

    /**
     * このメソッドハンドルを異なる型に適合させたものを記述する MethodHandleDesc を返します。これは、MethodHandle.asType(MethodType) によるものと同様です。
     *
     * @param type 新しいメソッド型を記述する MethodHandleDesc
     * @return 適合されたメソッドハンドルの MethodHandleDesc
     * @throws NullPointerException 引数が null の場合
     */
    default MethodHandleDesc asType(MethodTypeDesc type) {
        return (invocationType().equals(type)) ? this : new AsTypeMethodHandleDesc(this, type);
    }

    /**
     * この名目的記述子によって記述されるメソッドハンドルの呼び出し型を記述する MethodTypeDesc を返します。
     * 呼び出し型は、呼び出しによって消費される全てのスタック値（レシーバーがある場合はそれも含む）を記述します。
     *
     * @return メソッドハンドル型を記述する MethodTypeDesc
     */
    MethodTypeDesc invocationType();

    /**
     * 指定されたオブジェクトをこの記述子と比較して等しいかどうかを判定します。
     * 指定されたオブジェクトが MethodHandleDesc であり、両者が同じ名目的記述をエンコードしている場合に限り、true を返します。
     *
     * @param o 比較対象のオブジェクト
     * @return この記述子が他のオブジェクトと等しいかどうか
     */
    boolean equals(Object o);
}
