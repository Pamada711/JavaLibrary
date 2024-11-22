/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.IdentityHashMap;

/**
 * Runtime.addShutdownHookを通じて登録されたユーザーレベルのシャットダウンフックを追跡し、実行するクラス。
 * このクラスは、シャットダウンフックの管理を安全に行い、複数のフックが同時に実行される際の順序を適切に制御しています。
 * @see java.lang.Runtime#addShutdownHook
 * @see java.lang.Runtime#removeShutdownHook
 */

class ApplicationShutdownHooks {
    /* 登録されたフックのセット */
    private static IdentityHashMap<Thread, Thread> hooks;
    static {
        try {
            Shutdown.add(1 /* シャットダウンフックの呼び出し順序 */,
                false /* シャットダウンが進行中の場合、登録されていません */,
                new Runnable() {
                    public void run() {
                        runHooks();
                    }
                }
            );
            hooks = new IdentityHashMap<>();
        } catch (IllegalStateException e) {
            // シャットダウンが進行中の場合、アプリケーションのシャットダウンフックは追加できません。
            hooks = null;
        }
    }


    private ApplicationShutdownHooks() {}

    /** 新しいシャットダウンフックを追加します。シャットダウン状態とフック自体を確認しますが、セキュリティチェックは行いません。
     * hooks が null であれば、シャットダウンが進行中であることを示し、IllegalStateException をスローします。
     * hook が既に実行中（isAlive()）であれば、IllegalArgumentException をスローします。
     * hooks に既に登録されているフックがあれば、IllegalArgumentException をスローします。
     * 上記の条件をクリアした場合に、フックを hooks に登録します。
     */
    static synchronized void add(Thread hook) {
        if(hooks == null)
            throw new IllegalStateException("Shutdown in progress");

        if (hook.isAlive())
            throw new IllegalArgumentException("Hook already running");

        if (hooks.containsKey(hook))
            throw new IllegalArgumentException("Hook previously registered");

        hooks.put(hook, hook);
    }

    /** 登録されたシャットダウンフックを削除するメソッドです。
     * hooks が null の場合、シャットダウンが進行中であるため、IllegalStateException をスローします。
     * 引数が null の場合、NullPointerException をスローします。
     * フックが hooks に存在すれば削除し、削除が成功すれば true を返します。削除されなければ false を返します。
     */
    static synchronized boolean remove(Thread hook) {
        if(hooks == null)
            throw new IllegalStateException("Shutdown in progress");

        if (hook == null)
            throw new NullPointerException();

        return hooks.remove(hook) != null;
    }

    /** すべてのアプリケーションフックを繰り返し処理し、それぞれを実行するために新しいスレッドを作成します。
     * フックは並行して実行され、このメソッドはそれらが完了するのを待機します。
     * hooks からフックのスレッドを取得し、synchronized ブロック内でスレッドリストを取得します。この際、hooks を null に設定して、フックの追加を防止します。
     * 各フックを新しいスレッドで実行し（hook.start()）、フックが終了するまで join() メソッドを使って待機します。
     * join() が InterruptedException をスローした場合は、無視して再試行します。
     */
    static void runHooks() {
        Collection<Thread> threads;
        synchronized(ApplicationShutdownHooks.class) {
            threads = hooks.keySet();
            hooks = null;
        }

        for (Thread hook : threads) {
            hook.start();
        }
        for (Thread hook : threads) {
            while (true) {
                try {
                    hook.join();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
