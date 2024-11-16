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
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.Optional;

/**
 *
 * 定数可能（Constable）な型を表します。<em>Constable</em>型とは、その値が定数であり、Javaクラスファイルの定数プール（JVMS 4.4に記載）で表現可能であり、
 * インスタンスが自分自身を定数記述子（ConstantDesc）として名目的に表現できる型のことです。
 *
 * いくつかの{@link Constable}型は定数プール内にネイティブな表現を持ちます。
 * これには、{@link String}、{@link Integer}、{@link Long}、{@link Float}、{@link Double}、{@link Class}、{@link MethodType}、
 * および{@link MethodHandle}が含まれます。これらの型のうち、{@link String}、{@link Integer}、{@link Long}、{@link Float}、
 * および{@link Double}は自分自身をそのまま名目的な記述子として使用できます。
 * 一方、{@link Class}、{@link MethodType}、および{@link MethodHandle}には、それぞれ対応する記述子である{@link ClassDesc}、
 * {@link MethodTypeDesc}、および{@link MethodHandleDesc}があります。
 *
 * その他の参照型も、自身のインスタンスを{@link ConstantDesc}として名目的に記述できる場合、{@link Constable}と見なされます。
 * Java SEプラットフォームAPIの例として、{@link Enum}のようなJava言語機能をサポートする型や、
 * {@link VarHandle}のようなランタイムサポートクラスがあります。
 * これらは通常、動的に生成される定数を記述する{@link DynamicConstantDesc}を使用して記述されます（JVMS 4.4.10）。
 *
 * {@link Constable}型のインスタンスの名目的な形式は{@code describeConstable()}メソッドで取得できます。
 * ただし、{@link Constable}型がすべてのインスタンスを{@link ConstantDesc}形式で記述できるとは限りません。
 * また、そのような記述を選択しない場合もあります。このメソッドは{@link Optional}を返し、
 * 記述子を作成できない場合には空の値を返します（たとえば、{@link MethodHandle}は直接のメソッドハンドルに対しては名目的な記述子を生成しますが、
 * メソッドハンドルのコンビネータによって生成されたものについては必ずしも生成しない場合があります）。
 * @jvms 4.4 The Constant Pool
 * @jvms 4.4.10 The {@code CONSTANT_Dynamic_info} and {@code CONSTANT_InvokeDynamic_info} Structures
 *
 * @since 12
 */
public interface Constable {
    /**
     *
     * このインスタンスに対する名目的な記述子が構築可能であれば、それを含むOptionalを返します。構築できない場合は、空のOptionalを返します。
     *
     * @return An {@link Optional} containing the resulting nominal descriptor,
     * or an empty {@link Optional} if one cannot be constructed.
     */
    Optional<? extends ConstantDesc> describeConstable();
}
