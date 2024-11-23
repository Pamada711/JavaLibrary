/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * アサーションステータス指示（例えば、「パッケージ p でアサーションを有効にする」や「クラス c でアサーションを無効にする」など）の集合。
 * このクラスは、JVM が -enableassertions（-ea）および -disableassertions（-da）という java コマンドラインフラグによって示されるアサーションステータス指示を伝達するために使用されます。
 *
 * @since  1.4
 * @author Josh Bloch
 */
class AssertionStatusDirectives {
    /**
     * アサーションを有効または無効にする対象となるクラス。この配列内の文字列は完全修飾クラス名（例: "com.xyz.foo.Bar"）です。
     */
    String[] classes;

    /**
     *
     * クラスに対応する並列配列で、各クラスに対してアサーションを有効にするか無効にするかを示します。
     * classEnabled[i] が true の場合、classes[i] で指定されたクラスにアサーションを有効にすることを示し、
     * false の場合はアサーションを無効にすることを示します。この配列の要素数は classes 配列と同じでなければなりません。
     * 同じクラスに対して矛盾する指示がある場合は、最後の指示が優先されます。
     * つまり、文字列 s が classes 配列に複数回現れる場合、classes[i].equals(s) を満たす最大のインデックス i における classEnabled[i] の値が、
     * クラス s においてアサーションを有効にするか無効にするかを決定します。
     */
    boolean[] classEnabled;

    /**
     * アサーションを有効または無効にする対象となるパッケージツリーを表します。
     * この配列の文字列は完全なパッケージ名または部分的なパッケージ名を示します（例: "com.xyz" や "com.xyz.foo"）。
     */
    String[] packages;

    /**
     * パッケージに対応する並列配列であり、各パッケージツリーがアサーションを有効にするか無効にするかを示します。
     * packageEnabled[i] が true の場合、packages[i] によって指定されたパッケージツリーでアサーションが有効化されることを意味します。
     * 一方、false の場合はアサーションが無効化されます。この配列は packages 配列と同じ要素数を持たなければなりません。
     * 同じパッケージツリーに対して矛盾する指示がある場合、最後の指示が優先されます。言い換えれば、文字列 s が packages 配列内で複数回出現し、
     * packages[i].equals(s) が真となる最大のインデックス i が存在する場合、packageEnabled[i] がパッケージツリー s のアサーションを有効にするか無効にするかを決定します。
     */
    boolean[] packageEnabled;

    /**
     * システムクラス以外のクラスでアサーションをデフォルトで有効にするかどうかを示します。
     */
    boolean deflt;
}
