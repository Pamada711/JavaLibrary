/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Void クラスは、Javaの void キーワードを表す Class オブジェクトへの参照を保持するためのクラスです。このクラス自体はインスタンス化できません。
 * 主に、void 型を Class 型として操作する必要がある場合に使用されます。
 * 具体的には、次のような用途で使用されます：
 * <ul>
 *     <li>リフレクションで void 型を表現するため。例えば、メソッドの戻り値が void の場合、その型をリフレクションを使用して取得したり、メソッドの引数として指定したりする場合です。</li>
 *     <li>Void.TYPE は、void 型に対応する Class オブジェクトを提供します。この Class オブジェクトは、リフレクション API で void 型を扱うときに便利です。</li>
 *     <li>Void クラス自体は、void 型を表現するための補助的な役割を果たしますが、インスタンス化されることはありません。</li>
 * </ul>
 * @since   1.1
 */
public final
class Void {

    /**
     * void キーワードに対応する擬似型を表す Class オブジェクトです。
     */
    @SuppressWarnings("unchecked")
    public static final Class<Void> TYPE = (Class<Void>) Class.getPrimitiveClass("void");

    /*
     * The Void class cannot be instantiated.
     */
    private Void() {}
}
