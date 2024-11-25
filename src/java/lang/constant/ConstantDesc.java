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

import java.lang.Enum.EnumDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle.VarHandleDesc;

/**
 * 読み込むことが可能な定数値の記述子（JVMS 4.4で定義されている）
 * このような記述子は、resolveConstantDesc(MethodHandles.Lookup) を通じて解決され、定数値そのものを取得することができます。
 * 記述子内のクラス名は、クラスファイルの定数プール内のクラス名と同様に、特定のクラスローダーに関連付けて解釈される必要があります。ただし、このクラスローダーは記述子自体には含まれません。
 * 定数プール内でネイティブに表現できる静的定数（String、Integer、Long、Float、および Double）は ConstantDesc を実装しており、自身の記述子として機能します。
 * ネイティブでリンク可能な定数（Class、MethodType、および MethodHandle）には、それに対応する ConstantDesc 型（ClassDesc、MethodTypeDesc、および MethodHandleDesc）があります。
 * その他の定数は DynamicConstantDesc のサブタイプによって表されます。
 * バイトコードの生成や解析を行うAPIは、ConstantDesc を使用して次の項目を記述することが推奨されます：
 * <ul>
 *     <li>ldc 命令（動的定数を含む）のオペランド
 *     <li>動的定数や invokedynamic 命令の静的ブートストラップ引数
 *     <li>定数プールを利用する他のバイトコードやクラスファイル構造
 * </ul>
 * さまざまな一般的な定数（プラットフォーム型の ClassDesc インスタンスなど）を記述する定数は、ConstantDescs に含まれています。
 * ConstantDesc の実装は不変であるべきであり、その動作はオブジェクトの同一性に依存してはいけません。
 * 非プラットフォームクラスは ConstantDesc を直接実装するべきではありません。
 * 代わりに、DynamicConstantDesc を拡張する必要があります（例：Enum.EnumDesc や java.lang.invoke.VarHandle.VarHandleDesc）。
 * 記述子は、Object.equals(Object) メソッドを使用して比較されるべきです。特定のエンティティが常に同じ記述子インスタンスで表される保証はありません。
 *
 * @see Constable
 * @see ConstantDescs
 *
 * @jvms 4.4 The Constant Pool
 *
 * @since 12
 */
public sealed interface ConstantDesc
        permits ClassDesc,
                MethodHandleDesc,
                MethodTypeDesc,
                Double,
                DynamicConstantDesc,
                Float,
                Integer,
                Long,
                String {
    /**
     * この記述子をリフレクションを用いて解決します
     * JVMS 5.4.3 の解決動作と JVMS 5.4.4 のアクセス制御動作を模倣します。
     * 解決とアクセス制御のコンテキストは MethodHandles.Lookup パラメーターによって提供されます。結果として得られる値はキャッシュされません。
     *
     * @param lookup 名前解決とアクセス制御のコンテキストを提供する MethodHandles.Lookup
     * @return 解決された定数値
     * @throws ReflectiveOperationException 解決の過程でクラス、メソッド、またはフィールドをリフレクションで解決できなかった場合にスローされます。
     * @throws LinkageError リンケージエラーが発生した場合にスローされます。
     *
     * @apiNote MethodTypeDesc は、255 を超えるパラメータスロットを持つメソッドのように、MethodType では表現できないメソッド型記述子を表現できます。
     * そのため、これらを解決しようとするとエラーが発生する可能性があります。
     *
     * @jvms 5.4.3 Resolution
     * @jvms 5.4.4 Access Control
     */
    Object resolveConstantDesc(MethodHandles.Lookup lookup) throws ReflectiveOperationException;
}
