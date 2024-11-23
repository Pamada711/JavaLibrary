/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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


import jdk.internal.misc.VM;

/**
 * このShutdownクラスは、Java仮想マシン（JVM）のシャットダウンシーケンスを管理するための内部的なユーティリティクラスです。
 * 具体的には、システムシャットダウンの際に実行されるフック（フックメソッド）を登録・実行するロジックを提供します。
 * このクラスは、システムシャットダウン時に実行されるフックを管理することを目的としており、アプリケーションやシステムの終了処理が適切に行われるようにします。
 *
 * @author   Mark Reinhold
 * @since    1.3
 *
 * @see java.io.Console
 * @see ApplicationShutdownHooks
 * @see java.io.DeleteOnExitHook
 */

class Shutdown {
    /**
     * システムのシャットダウンフックは、あらかじめ定義されたスロットに登録されます。シャットダウンフックのリストは以下の通りです：
     * <ul>
     *     <li>コンソール復元フック</li>
     *     <li>ApplicationShutdownHooks（すべての登録されたアプリケーションシャットダウンフックを呼び出し、終了するまで待機）</li>
     *     <li>DeleteOnExit フック</li>
     * </ul>
    */
    private static final int MAX_SYSTEM_HOOKS = 10;
    private static final Runnable[] hooks = new Runnable[MAX_SYSTEM_HOOKS];

    // 現在実行中のシャットダウンフックのインデックスをフックの配列に追加します。
    private static int currentRunningHook = -1;

    /* 前述の静的フィールドは、このロックによって保護されています。 */
    private static class Lock { };
    private static Object lock = new Lock();

    /**
     * 「ネイティブの halt メソッド用のロックオブジェクト」というのは、Javaの仮想マシン（JVM）でのシャットダウンプロセスに関連する内部メカニズムです。
     * halt メソッドは、JVMのシャットダウンを強制的に停止するためのメソッドで、通常、JVMを終了させる際に使用されます。
     * このメソッドの実行中にリソースを正しく管理するため、ロックオブジェクトが用いられます。
     * このロックオブジェクトは、halt メソッドが安全に実行されるように、同時に複数のスレッドがシャットダウン処理に関与してしまうことを防ぐために存在します。
     * これにより、シャットダウン処理が複数のスレッドで競合しないようにし、一貫性を保つことができます。
     */
    private static Object haltLock = new Lock();

    /**
     * 新しいシステムシャットダウンフックを追加します。シャットダウンの状態とフック自体を確認しますが、セキュリティチェックは行いません。
     * registerShutdownInProgress パラメータは、DeleteOnExitHook を登録する場合を除いて false であるべきです。
     * なぜなら、最初のファイルはアプリケーションのシャットダウンフックによって「終了時削除リスト」に追加される可能性があるからです。
     *
     * @params slot  シャットダウン時に順番に呼び出されるシャットダウンフック配列のスロット
     * @params registerShutdownInProgress を true に設定すると、シャットダウンが進行中でもフックを登録できるようになります。
     * @params hook  the hook to be registered
     *
     * @throws IllegalStateException
     *         もし registerShutdownInProgress が false でシャットダウンが進行中の場合、
     *         または registerShutdownInProgress が true でシャットダウンプロセスがすでに指定されたスロットを通過している場合
     */
    static void add(int slot, boolean registerShutdownInProgress, Runnable hook) {
        if (slot < 0 || slot >= MAX_SYSTEM_HOOKS) {
            throw new IllegalArgumentException("Invalid slot: " + slot);
        }
        synchronized (lock) {
            if (hooks[slot] != null)
                throw new InternalError("Shutdown hook at slot " + slot + " already registered");

            if (!registerShutdownInProgress) {
                if (currentRunningHook >= 0)
                    throw new IllegalStateException("Shutdown in progress");
            } else {
                if (VM.isShutdown() || slot <= currentRunningHook)
                    throw new IllegalStateException("Shutdown in progress");
            }

            hooks[slot] = hook;
        }
    }

    /** すべてのシステムシャットダウンフックを実行します。システムシャットダウンフックは、Shutdown.class で同期されたスレッドで実行されます。
     * シャットダウン処理中に他のスレッドがシャットダウンを開始するのを防ぐために、ロックを取得します。
     * 他のスレッドが Runtime::exit、Runtime::halt または JNI の DestroyJavaVM を呼び出すと、それらは無限にブロックされます。
     * ApplicationShutdownHooks は、すべてのアプリケーションシャットダウンフックを開始し、それらが完了するまで待機する単一のフックとして登録されます。
     *
     */
    private static void runHooks() {
        synchronized (lock) {
            /* DestroyJavaVM がシャットダウンシーケンスを開始した後に、
             * デーモンスレッドが exit を呼び出す可能性を防ぐための対策を講じます。
             */
            if (VM.isShutdown()) return;
        }

        for (int i=0; i < MAX_SYSTEM_HOOKS; i++) {
            try {
                Runnable hook;
                synchronized (lock) {
                    // シャットダウン中に登録されたフックがここで見えるようにするために、ロックを取得します。
                    currentRunningHook = i;
                    hook = hooks[i];
                }
                if (hook != null) hook.run();
            } catch (Throwable t) {
                if (t instanceof ThreadDeath td) {
                    throw td;
                }
            }
        }

        // set shutdown state
        VM.shutdown();
    }

    /* VMにシャットダウンのタイミングを通知します。*/
    static native void beforeHalt();

    /* halt メソッドは、シャットダウン時に削除されるファイルリストの破損を避けるために、halt ロックで同期化されています。
     * このメソッドは、実際のネイティブの halt メソッドを呼び出します。
     * haltメソッドは、シャットダウン時にJVMを強制的に終了させます。haltLockで同期を取り、リソース管理を確保します。
     */
    static void halt(int status) {
        synchronized (haltLock) {
            halt0(status);
        }
    }

    static native void halt0(int status);

    /** Runtime.exit によって呼び出され、すべてのセキュリティチェックを行います。
     * また、システム提供の終了イベントのハンドラーによっても呼び出され、これらは非ゼロのステータスコードを渡すべきです。
     */
    static void exit(int status) {
        synchronized (lock) {
            if (status != 0 && VM.isShutdown()) {
                /* 非ゼロのステータスの場合、即座に停止します。 */
                halt(status);
            }
        }
        synchronized (Shutdown.class) {
            /*
             * クラスオブジェクトで同期を取ることにより、シャットダウンを開始しようとする他のスレッドが無期限に停止するようにします。
             */
            beforeHalt();
            runHooks();
            halt(status);
        }
    }


    /**
     * 最後の非デーモンスレッドが終了したときに、JNIのDestroyJavaVM手続きによって呼び出されます。
     * exitメソッドとは異なり、このメソッドは実際にはVMを停止させません。
     */
    static void shutdown() {
        synchronized (Shutdown.class) {
            runHooks();
        }
    }

}
