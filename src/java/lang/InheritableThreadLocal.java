/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * このクラスは ThreadLocal を拡張して、親スレッドから子スレッドへの値の継承を提供します。
 * 子スレッドが作成されるとき、親スレッドに値が設定されているすべての継承可能なスレッドローカル変数について、子スレッドは初期値を受け取ります。
 * 通常、子スレッドの値は親スレッドの値と同一になりますが、このクラス内の {@code childValue} メソッドをオーバーライドすることで、
 * 子スレッドの値を親スレッドの値を元にした任意の関数の結果とすることができます。
 * 継承可能なスレッドローカル変数は、変数内で維持されるスレッドごとの属性（例：ユーザーID、トランザクションIDなど）が、
 * 新しく作成された子スレッドに自動的に伝達される必要がある場合に、通常のスレッドローカル変数よりも優先して使用されます。
 * 注意: 新しいスレッドを作成する際、継承可能なスレッドローカル変数の初期値の受け取りをオプトアウト（拒否）することが可能です。
 *
 * @author  Josh Bloch and Doug Lea
 * @see     ThreadLocal
 * @since   1.2
 */

public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    /**
     * 継承可能なスレッドローカル変数を作成します。
     */
    public InheritableThreadLocal() {}

    /**
     * 子スレッドが作成される際、親スレッドの値を基に、この継承可能なスレッドローカル変数の子スレッドにおける初期値を計算します。
     * このメソッドは、子スレッドが開始される前に親スレッド内で呼び出されます。
     * このメソッドは単に入力引数をそのまま返しますが、異なる動作を希望する場合はオーバーライドする必要があります。
     *
     * @param parentValue the parent thread's value
     * @return the child thread's initial value
     */
    protected T childValue(T parentValue) {
        return parentValue;
    }

    /**
     *
     * ThreadLocalに関連付けられたマップを取得する。
     *
     * @param t the current thread
     */
    ThreadLocalMap getMap(Thread t) {
       return t.inheritableThreadLocals;
    }

    /**
     * ThreadLocalに関連付けられたマップを作成する。
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the table.
     */
    void createMap(Thread t, T firstValue) {
        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
    }
}
