/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * クラスが {@code Cloneable} インターフェースを実装するのは、{@code Object.clone()} メソッドに対して、
 * そのクラスのインスタンスをフィールド単位でコピーすることが合法であることを示すためです。
 * {@code Cloneable} インターフェースを実装していないインスタンスに対して {@code Object.clone()} メソッドを呼び出すと、{@link CloneNotSupportedException} がスローされます。
 * 慣例として、このインターフェースを実装するクラスは、{@code Object.clone}（保護されたメソッド）をオーバーライドし、
 * パブリックメソッドとして提供するべきです。詳細については、{@code Object.clone()} のオーバーライドに関する記述を参照してください。
 * 注意すべき点として、このインターフェースには {@code clone} メソッドが含まれていません。
 * そのため、このインターフェースを実装しているだけでは、オブジェクトをクローンできる保証はありません。
 * たとえリフレクションを用いて {@code clone} メソッドを呼び出した場合でも、それが成功する保証はありません。
 *
 * @see     CloneNotSupportedException
 * @see     Object#clone()
 * @since   1.0
 */
public interface Cloneable {
}
