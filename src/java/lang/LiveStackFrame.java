/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.StackWalker.StackFrame;
import java.util.EnumSet;
import java.util.Set;

import static java.lang.StackWalker.ExtendedOption.LOCALS_AND_OPERANDS;

/**
 *
 * サポートされていません
 * このインターフェースは、パッケージプライベートとして設計されているか、内部パッケージに移動することを意図しています。
 *
 * LiveStackFrame はデータや部分的な結果を格納するフレームを表します。
 * 各フレームは、ローカル変数用の独自の配列（JVMS セクション 2.6.1）と、メソッド呼び出し用の独自のオペランドスタック（JVMS セクション 2.6.2）を持っています。
 *
 * @jvms 2.6 Frames
 */
/* package-private */
interface LiveStackFrame extends StackFrame {
    /**
     * このスタックフレームが保持しているモニターを返します。
     * このメソッドは、このスタックフレームでモニターが保持されていない場合、空の配列を返します。
     *
     * @return the monitors held by this stack frames
     */
    public Object[] getMonitors();

    /**
     * このスタックフレームのローカル変数配列を取得します。
     * 1つのローカル変数は、boolean、byte、char、short、int、float、reference、またはreturnAddress型の値を保持できます。
     * また、2つのローカル変数でlongまたはdouble型の値を保持することができます（JVMSセクション2.6.1）。
     * プリミティブ型のローカル変数は、返される配列内でPrimitiveSlotsとして表され、longとdoubleは連続する2つのPrimitiveSlotsを占有します。
     * 現在の仮想マシン（VM）の実装では、プリミティブ型のローカル変数に対して特定の型情報は提供されません。
     * このメソッドは、VMのプリミティブ型ローカル変数の生データをベストエフォートで返すだけで、特定の型を示すものではありません。
     * 返される配列には、使用されていないローカル変数に対応するnullエントリが含まれる場合があります。
     *
     * @implNote
     * PrimitiveSlotの具体的なサブクラスは、基盤となるアーキテクチャに応じて異なり、PrimitiveSlot32またはPrimitiveSlot64のいずれかになります。
     * longやdoubleの値が一対のPrimitiveSlotにどのように格納されるかは、基盤となるアーキテクチャやVM（仮想マシン）の実装によって異なります。
     * <ul>
     *     <li>32ビットアーキテクチャの場合、longやdoubleの値は2つのPrimitiveSlot32の間で分割されます。</li>
     *     <li>64ビットアーキテクチャの場合、値全体が1つのPrimitiveSlot64に格納され、もう1つのPrimitiveSlot64は使用されません。</li>
     * </ul>
     * PrimitiveSlot64の未使用の高次ビット部分（longやdouble以外のプリミティブ型が格納される場合）の内容は未定義です。特に、未使用のビットがゼロクリアされているとは限りません。
     *
     * @return  the local variable array of this stack frame.
     */
    public Object[] getLocals();

    /**
     * このスタックフレームのオペランドスタックを取得します。
     *
     * 戻り値の配列の0番目の要素は、オペランドスタックのトップを表します。
     * オペランドスタックが空の場合、このメソッドは空の配列を返します。
     * オペランドスタックの各エントリは、任意の**Java仮想マシン型（JVM Type）**の値を保持できます。
     *
     * プリミティブ型の値の場合、配列内の要素はLiveStackFrame.PrimitiveSlotオブジェクトとなります。
     * 非プリミティブ型の値の場合、配列内の要素はオペランドスタック上のObjectとなります。
     *
     * @return the operand stack of this stack frame.
     */
    public Object[] getStack();

    /**
     * サポート対象外
     * このインターフェースはパッケージプライベートにするか、内部パッケージへ移動することを意図しています。
     *
     * これは、プリミティブ型の値を持つローカル変数またはオペランドスタック上のエントリを表します。
     */
    public abstract class PrimitiveSlot {
        /**
         * Constructor.
         */
        PrimitiveSlot() {}

        /**
         * Returns the size, in bytes, of the slot.
         */
        public abstract int size();

        /**
         * Returns the int value if this primitive value is of size 4
         * @return the int value if this primitive value is of size 4
         *
         * @throws UnsupportedOperationException if this primitive value is not
         * of size 4.
         */
        public int intValue() {
            throw new UnsupportedOperationException("this " + size() + "-byte primitive");
        }

        /**
         * Returns the long value if this primitive value is of size 8
         * @return the long value if this primitive value is of size 8
         *
         * @throws UnsupportedOperationException if this primitive value is not
         * of size 8.
         */
        public long longValue() {
            throw new UnsupportedOperationException("this " + size() + "-byte primitive");
        }
    }


    /**
     * Gets {@code StackWalker} that can get locals and operands.
     *
     * @throws SecurityException if the security manager is present and
     * denies access to {@code RuntimePermission("liveStackFrames")}
     */
    public static StackWalker getStackWalker() {
        return getStackWalker(EnumSet.noneOf(StackWalker.Option.class));
    }

    /**
     * 指定されたオプションを使用して、アクセス可能なスタックフレーム情報を指定する StackWalker インスタンスを取得します。
     * このインスタンスは、最大で maxDepth に指定された数のスタックフレームを走査します。
     * オプションが指定されていない場合、この StackWalker はメソッド名とクラス名を取得し、すべての隠しフレームをスキップします。
     * 返される StackWalker は、ローカル変数やオペランドを取得することができます。
     *
     * @param options stack walk {@link StackWalker.Option options}
     *
     * @throws SecurityException if the security manager is present and
     * it denies access to {@code RuntimePermission("liveStackFrames")};
     * or if the given {@code options} contains
     * {@link StackWalker.Option#RETAIN_CLASS_REFERENCE Option.RETAIN_CLASS_REFERENCE}
     * and it denies access to {@code RuntimePermission("getStackWalkerWithClassReference")}.
     */
    public static StackWalker getStackWalker(Set<StackWalker.Option> options) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("liveStackFrames"));
        }
        return StackWalker.newInstance(options, LOCALS_AND_OPERANDS);
    }
}
