/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * メソッド型のための名目的記述子。
 * MethodTypeDescImpl は、クラスファイルの定数プールにおける Constant_MethodType_info エントリに対応します。
 * このクラスは、Javaメソッドの型（戻り値と引数の型）の詳細な情報を保持し、それを操作するためのメソッド群を提供します。
 * 具体的には、メソッド型の変更や検証、他の型との比較、文字列からのメソッド型の生成などを行うために使用されます。
 */
final class MethodTypeDescImpl implements MethodTypeDesc {
    private final ClassDesc returnType;
    private final ClassDesc[] argTypes;

    /**
     * 指定された戻り値の型およびパラメーターの型を持つ MethodTypeDesc を構築します。
     * このコンストラクタの主な目的は、メソッドの型記述における無効な引数（void 型）を防ぐことです。
     *
     * @param returnType 戻り値の型を記述する ClassDesc
     * @param argTypes パラメーターの型を記述する ClassDesc の配列
     */
    MethodTypeDescImpl(ClassDesc returnType, ClassDesc[] argTypes) {
        this.returnType = requireNonNull(returnType);
        this.argTypes = requireNonNull(argTypes);

        // 各引数 (cr) がプリミティブ型（基本型）であり、かつその型が "V"（void 型）である場合に対してチェックを行います。
        //"V" はJavaにおいて void 型を表す記述子であり、引数に void 型を使うことは許可されていません。
        for (ClassDesc cr : argTypes)
            if (cr.isPrimitive() && cr.descriptorString().equals("V"))
                throw new IllegalArgumentException("Void parameters not permitted");
    }

    /**
     * 指定されたメソッド記述子文字列から MethodTypeDescImpl を作成します。
     * descriptor は通常、Javaのメソッド記述子（例えば、(I)V や (Ljava/lang/String;)I のようなもの）を指し、
     * それに基づいてメソッドの戻り値の型や引数の型を構築します。
     * 文字列で指定されたメソッド記述子（例えば、(I)V や (Ljava/lang/String;)I）を解析し、それに対応する MethodTypeDescImpl インスタンスを生成するファクトリメソッド
     *
     * @param descriptor メソッド記述子文字列
     * @return 指定されたメソッド型を記述する MethodTypeDescImpl
     * @throws IllegalArgumentException 記述子文字列が有効なメソッド記述子でない場合
     * @jvms 4.3.3 Method Descriptors
     */
    static MethodTypeDescImpl ofDescriptor(String descriptor) {
        requireNonNull(descriptor);
        // descriptor（メソッド記述子文字列）を解析し、その結果を List<String> として返します。
        // parseMethodDescriptor メソッドは、メソッド記述子（例えば、(Ljava/lang/String;)I）をパースして、戻り値の型や引数の型をリストとして取り出す処理を行います。
        List<String> types = ConstantUtils.parseMethodDescriptor(descriptor);
        // 最初の要素（戻り値の型）をスキップし、引数の型だけを処理する
        ClassDesc[] paramTypes = types.stream().skip(1).map(ClassDesc::ofDescriptor).toArray(ClassDesc[]::new);
        // 戻り値の型を ClassDesc.ofDescriptor(types.get(0)) で取得し、引数の型（paramTypes）とともに MethodTypeDescImpl のインスタンスを作成します
        return new MethodTypeDescImpl(ClassDesc.ofDescriptor(types.get(0)), paramTypes);
    }

    @Override
    public ClassDesc returnType() {
        return returnType;
    }

    @Override
    public int parameterCount() {
        return argTypes.length;
    }

    @Override
    public ClassDesc parameterType(int index) {
        return argTypes[index];
    }

    @Override
    public List<ClassDesc> parameterList() {
        return List.of(argTypes);
    }

    @Override
    public ClassDesc[] parameterArray() {
        return argTypes.clone();
    }

    @Override
    public MethodTypeDesc changeReturnType(ClassDesc returnType) {
        return MethodTypeDesc.of(returnType, argTypes);
    }

    /**
     * このメソッド changeParameterType は、指定されたインデックスの引数の型を新しい型に変更した新しい MethodTypeDesc を返します。
     * 具体的には、argTypes 配列の指定されたインデックスの引数型を変更し、それを基に新しい MethodTypeDesc を作成します。
     * 元の MethodTypeDesc は変更されません（不変性）
     * @param index the index of the parameter to change
     * @param paramType a field descriptor describing the new parameter type
     * @return
     */
    @Override
    public MethodTypeDesc changeParameterType(int index, ClassDesc paramType) {
        ClassDesc[] newArgs = argTypes.clone();
        newArgs[index] = paramType;
        return MethodTypeDesc.of(returnType, newArgs);
    }

    /**
     * このメソッド dropParameterTypes は、引数の型リストから指定された範囲の引数型を削除し、新しい MethodTypeDesc を返すものです。
     * 具体的には、引数リストの一部（指定された start と end の範囲）の要素を削除し、残りの引数型から新しいメソッド型を構築します。
     * @param start the index of the first parameter to remove
     * @param end the index after the last parameter to remove
     * @return
     */
    @Override
    public MethodTypeDesc dropParameterTypes(int start, int end) {
        // // 引数の範囲が無効な場合（インデックスが範囲外）
        if (start < 0 || start >= argTypes.length || end < 0 || end > argTypes.length || start > end)
            throw new IndexOutOfBoundsException();
        // // 新しい引数型の配列を作成（削除する引数の分だけ小さくする）
        ClassDesc[] newArgs = new ClassDesc[argTypes.length - (end - start)];
        // // start から前半部分を新しい配列にコピー
        System.arraycopy(argTypes, 0, newArgs, 0, start);
        // end から後半部分を新しい配列にコピー
        System.arraycopy(argTypes, end, newArgs, start, argTypes.length - end);
        // 新しい引数型の配列と戻り値型で新しい MethodTypeDesc を作成して返す
        return MethodTypeDesc.of(returnType, newArgs);
    }

    @Override
    public MethodTypeDesc insertParameterTypes(int pos, ClassDesc... paramTypes) {
        // pos は挿入位置を指定するインデックスです。pos が負の値や、現在の引数配列（argTypes）の長さを超えている場合、IndexOutOfBoundsException がスローされます。
        if (pos < 0 || pos > argTypes.length)
            throw new IndexOutOfBoundsException(pos);
        ClassDesc[] newArgs = new ClassDesc[argTypes.length + paramTypes.length];
        System.arraycopy(argTypes, 0, newArgs, 0, pos);
        System.arraycopy(paramTypes, 0, newArgs, pos, paramTypes.length);
        System.arraycopy(argTypes, pos, newArgs, pos+paramTypes.length, argTypes.length - pos);
        return MethodTypeDesc.of(returnType, newArgs);
    }

    /**
     * resolveConstantDesc メソッドは、MethodHandleDesc から MethodType を解決するために使用されます。
     * 特に、このメソッドは、メソッドの型（引数と戻り値の型）を特定し、その型情報を MethodType として返します。
     * @param lookup The {@link MethodHandles.Lookup} to provide name resolution
     *               and access control context
     * @return
     * @throws ReflectiveOperationException
     */
    @Override
    public MethodType resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException {
        @SuppressWarnings("removal")
        MethodType mtype = AccessController.doPrivileged(new PrivilegedAction<>() {
            @Override
            public MethodType run() {
                // メソッドの記述子文字列を受け取り、それに対応する MethodType を作成します。
                // このメソッドは、メソッドの戻り値の型と引数の型を解析して MethodType を構築します。
                return MethodType.fromMethodDescriptorString(descriptorString(),
                                                             lookup.lookupClass().getClassLoader());
            }
        });

        // MethodHandles.Lookup を使ってクラスへのアクセス許可を確認します。
        // このメソッドは、mtype.returnType()（戻り値の型）および mtype.parameterArray()（引数の型の配列）のすべてのクラスについてアクセス確認を行います。
        lookup.accessClass(mtype.returnType());
        for (Class<?> paramType: mtype.parameterArray()) {
            lookup.accessClass(paramType);
        }
        return mtype;
    }

    /**
     * この MethodTypeDescImpl が別の MethodTypeDescImpl と等しい場合に true を返します。
     * 等価性は、両方の記述子が同じ戻り値の型および引数の型を持っているかによって判断されます。
     * 等しいと判断されるためには、returnType と argTypes が完全に一致している必要があります。
     *
     * @param o この MethodTypeDescImpl と比較する MethodTypeDescImpl
     * @return 指定された MethodTypeDescImpl がこの MethodTypeDescImpl と等しい場合、true を返します。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodTypeDescImpl constant = (MethodTypeDescImpl) o;

        return returnType.equals(constant.returnType)
               && Arrays.equals(argTypes, constant.argTypes);
    }

    /**
     * 素数（特に小さな素数）は、ハッシュコードの計算において効果的であると考えられています。
     * 理由は、素数を使うことで衝突（異なるオブジェクトが同じハッシュコードを持つこと）を減らすことができるからです。
     * 例えば、31 は素数であり、31 * result + x という式の計算において、ハッシュコードの異なるビットパターンがより均等に分散される傾向があるため、衝突の確率を減少させるとされています。
     * 31 は、2進数の世界では非常に効率的に計算できます。なぜなら、31 は 2^5 - 1 という形の数であり、ビットシフト演算を使った最適化が可能だからです。
     * 具体的には、31 * result の計算は、コンピュータ上ではビットシフトを用いて高速に実行できます。
     * @return
     */
    @Override
    public int hashCode() {
        int result = returnType.hashCode();
        result = 31 * result + Arrays.hashCode(argTypes);
        return result;
    }

    @Override
    public String toString() {
        return String.format("MethodTypeDesc[%s]", displayDescriptor());
    }
}
