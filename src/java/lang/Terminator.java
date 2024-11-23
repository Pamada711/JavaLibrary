/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Signal;


/**
 * 終了トリガーによるシャットダウンのためのプラットフォーム固有のサポートを設定および解除するためのパッケージプライベートユーティリティクラス。
 * Terminator クラスは、プラットフォーム固有のサポートを設定および解除するためのユーティリティクラスで、
 * 終了トリガー（例えば、HUP、INT、TERM シグナル）によるシャットダウンを処理する目的で使用されます。
 * 要するに、Terminator クラスは、OSの終了シグナルを受け取って、適切なシャットダウン手続きを実行するためのインフラを提供します。
 * @author   Mark Reinhold
 * @since    1.3
 */

class Terminator {

    private static Signal.Handler handler = null;

    /* セットアップとティアダウンの呼び出しはすでにシャットダウンロックで同期されているため、ここでさらに同期を取る必要はありません。
     */

    /**
     * 終了シグナル（HUP、INT、TERM）に対してシャットダウン処理を設定します。
     * これらのシグナルが発生すると、Shutdown.exit() メソッドを呼び出し、JVMをシャットダウンさせる処理を実行します。
     * 特に、-Xrs オプションが指定された場合、ユーザーが自ら System.exit() を呼び出さなければならないことを示唆しています。
     */
    static void setup() {
        if (handler != null) return;
        Signal.Handler sh = new Signal.Handler() {
            public void handle(Signal sig) {
                Shutdown.exit(sig.getNumber() + 0200);
            }
        };
        handler = sh;

        //-Xrs が指定されている場合、ユーザーは System.exit() を呼び出すことによってシャットダウンフックが実行されるようにする責任を負います。
        try {
            Signal.handle(new Signal("HUP"), sh);
        } catch (IllegalArgumentException e) {
        }
        try {
            Signal.handle(new Signal("INT"), sh);
        } catch (IllegalArgumentException e) {
        }
        try {
            Signal.handle(new Signal("TERM"), sh);
        } catch (IllegalArgumentException e) {
        }
    }

    static void teardown() {
        /* 現在、sun.misc.Signal クラスがハンドラのキャンセルをサポートしていないため、teardown メソッド内で特に何も行われません。
         */
    }

}
