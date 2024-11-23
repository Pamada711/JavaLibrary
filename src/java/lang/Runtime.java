/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Azul Systems, Inc. All rights reserved.
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

import jdk.internal.access.SharedSecrets;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * すべてのJavaアプリケーションには、アプリケーションが実行されている環境とインターフェースをするための Runtime クラスの単一のインスタンスがあります。
 * 現在のランタイムは getRuntime メソッドから取得できます。
 * アプリケーションはこのクラスのインスタンスを自分で作成することはできません。
 *
 * @see     Runtime#getRuntime()
 * @since   1.0
 */

public class Runtime {
    private static final Runtime currentRuntime = new Runtime();

    private static Version version;

    /**
     * 現在のJavaアプリケーションに関連付けられたランタイムオブジェクトを返します。
     * Runtime クラスのほとんどのメソッドはインスタンスメソッドであり、現在のランタイムオブジェクトに対して呼び出さなければなりません。
     *
     * @return  the {@code Runtime} object associated with the current
     *          Java application.
     */
    public static Runtime getRuntime() {
        return currentRuntime;
    }

    /** このクラスを他の誰にもインスタンス化させないでください。 */
    private Runtime() {}

    /**
     *
     * 現在実行中のJava仮想マシンを終了させ、そのシャットダウンシーケンスを開始します。このメソッドは通常戻りません。
     * 引数はステータスコードとして使用され、慣例として非ゼロのステータスコードは異常終了を示します。
     * 登録されているすべてのシャットダウンフック（もしあれば）は、順序は不明ですが、並行して実行され、完了するまで実行されます。
     * これが完了すると、仮想マシンは停止します。 もしこのメソッドがすべてのシャットダウンフックが実行された後に呼び出され、
     * ステータスが非ゼロの場合、このメソッドは指定されたステータスコードで仮想マシンを停止させます。
     * そうでない場合、このメソッドは無期限にブロックされます。 System.exit メソッドは、このメソッドを呼び出すための慣習的で便利な手段です。
     *
     * @param  status
     *          終了ステータス。慣例として、非ゼロのステータスコードは異常終了を示します。
     *
     * @throws SecurityException
     *         セキュリティマネージャが存在し、その checkExit メソッドが指定されたステータスでの終了を許可しない場合
     *
     * @see SecurityException
     * @see SecurityManager#checkExit(int)
     * @see #addShutdownHook
     * @see #removeShutdownHook
     * @see #halt(int)
     */
    public void exit(int status) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkExit(status);
        }
        Shutdown.exit(status);
    }

    /**
     * 新しい仮想マシンのシャットダウンフックを登録します。
     * Java仮想マシンは、次の2種類のイベントに応じてシャットダウンします：
     * <ul>
     *     <li>プログラムが正常に終了する場合（最後の非デーモンスレッドが終了するか、exit（または System.exit）メソッドが呼び出されるとき）</li>
     *     <li>ユーザーの割り込みやシステム全体のイベント（例えば、ユーザーのログオフやシステムシャットダウン）による仮想マシンの終了</li>
     * </ul>
     * シャットダウンフックは、初期化されたが開始されていないスレッドです。
     * 仮想マシンがシャットダウンシーケンスを開始すると、すべての登録されたシャットダウンフックを未定義の順序で開始し、並行して実行させます。
     * すべてのフックが終了した後、仮想マシンは停止します。デーモンスレッドはシャットダウンシーケンス中も実行され続け、
     * exit メソッドが呼び出されてシャットダウンが開始された場合は、非デーモンスレッドも実行されます。
     * シャットダウンシーケンスが開始された後は、halt メソッドを呼び出さない限り、そのシーケンスを停止することはできません。このメソッドは仮想マシンを強制終了させます。
     * シャットダウンシーケンスが始まると、新しいシャットダウンフックの登録や既存のフックの登録解除は不可能となります。
     * これらの操作を試みると、IllegalStateException がスローされます。
     *
     * シャットダウンフックは、仮想マシンのライフサイクルにおいて非常に繊細なタイミングで実行されるため、防御的にコーディングする必要があります。
     * 特に、スレッドセーフであり、可能な限りデッドロックを回避するように記述すべきです。
     * また、他のシャットダウンフックが登録されている可能性のあるサービスに盲目的に依存することは避けるべきです。
     * 例えば、AWTイベントディスパッチスレッドを使用しようとするとデッドロックが発生する可能性があります。
     * シャットダウンフックは迅速に作業を終わらせるべきです。exit メソッドが呼び出されると、仮想マシンは速やかにシャットダウンして終了することが期待されています。
     * ユーザーのログオフやシステムシャットダウンによって仮想マシンが終了する場合、
     * オペレーティングシステムはシャットダウンと終了のための制限時間を設けていることがあります。
     * したがって、シャットダウンフック内でユーザーとの対話を試みたり、長時間実行される計算を行うことは避けるべきです。
     *
     * シャットダウンフック内で発生した未処理の例外は、他のスレッドと同様に、スレッドの ThreadGroup オブジェクトの uncaughtException メソッドによって処理されます。
     * このメソッドのデフォルト実装は、例外のスタックトレースを System.err に出力し、スレッドを終了させますが、仮想マシンを終了または停止させることはありません。
     *
     * 稀に、仮想マシンが中断されることがあります。これは、仮想マシンが外部から終了された場合（例：UnixのSIGKILL信号やMicrosoft Windowsの TerminateProcess 呼び出し）や、
     * ネイティブメソッドが内部データ構造を破損させたり、存在しないメモリにアクセスするなどの問題を起こした場合に発生します。
     * 仮想マシンが中断されると、シャットダウンフックが実行されるかどうかに関しては保証されません。
     *
     * @param   hook
     *          An initialized but unstarted {@link Thread} object
     *
     * @throws  IllegalArgumentException
     *          If the specified hook has already been registered,
     *          or if it can be determined that the hook is already running or
     *          has already been run
     *
     * @throws  IllegalStateException
     *          If the virtual machine is already in the process
     *          of shutting down
     *
     * @throws  SecurityException
     *          If a security manager is present and it denies
     *          {@link RuntimePermission}{@code ("shutdownHooks")}
     *
     * @see #removeShutdownHook
     * @see #halt(int)
     * @see #exit(int)
     * @since 1.3
     */
    public void addShutdownHook(Thread hook) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("shutdownHooks"));
        }
        ApplicationShutdownHooks.add(hook);
    }

    /**
     * 以前に登録された仮想マシンのシャットダウンフックを登録解除します。
     *
     * @param hook the hook to remove
     * @return {@code true} 指定されたフックが以前に登録されていて正常に登録解除された場合は true を、それ以外の場合は false を返します。
     *
     * @throws  IllegalStateException
     *          If the virtual machine is already in the process of shutting
     *          down
     *
     * @throws  SecurityException
     *          If a security manager is present and it denies
     *          {@link RuntimePermission}{@code ("shutdownHooks")}
     *
     * @see #addShutdownHook
     * @see #exit(int)
     * @since 1.3
     */
    public boolean removeShutdownHook(Thread hook) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("shutdownHooks"));
        }
        return ApplicationShutdownHooks.remove(hook);
    }


    /**
     * 現在実行中のJava仮想マシンを強制的に終了します。このメソッドは通常の方法では戻りません。
     * このメソッドは非常に慎重に使用する必要があります。exit メソッドとは異なり、このメソッドはシャットダウンフックを開始しません。
     * シャットダウンシーケンスがすでに開始されている場合、このメソッドは実行中のシャットダウンフックが作業を完了するのを待ちません。
     *
     * @param  status
     *      終了ステータス。慣例として、ゼロ以外のステータスコードは異常終了を示します。
     *      exit（または同等の System.exit）メソッドがすでに呼び出されている場合、このステータスコードはそのメソッドに渡されたステータスコードを上書きします。
     *
     * @throws SecurityException
     *         If a security manager is present and its
     *         {@link SecurityManager#checkExit checkExit} method
     *         does not permit an exit with the specified status
     *
     * @see #exit
     * @see #addShutdownHook
     * @see #removeShutdownHook
     * @since 1.3
     */
    public void halt(int status) {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkExit(status);
        }
        Shutdown.beforeHalt();
        Shutdown.halt(status);
    }

    /**
     * 指定された文字列コマンドを別のプロセスで実行します。
     * これは便利メソッドです。exec(command) の形式での呼び出しは、exec(command, null, null) の呼び出しとまったく同じ方法で動作します。
     *
     * @param   command   a specified system command.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code command} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If {@code command} is empty
     *
     * @see     #exec(String[], String[], File)
     * @see     ProcessBuilder
     */
    public Process exec(String command) throws IOException {
        return exec(command, null, null);
    }

    /**
     * 指定された環境で、指定された文字列コマンドを別のプロセスで実行します。
     * これは便利メソッドです。exec(command, envp) の形式での呼び出しは、exec(command, envp, null) の呼び出しとまったく同じ方法で動作します。
     *
     * @param   command   a specified system command.
     *
     * @param   envp
     * 文字列の配列で、各要素は環境変数の設定を name=value という形式で表します。
     * サブプロセスが現在のプロセスの環境を引き継ぐ場合は null を指定します。
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code command} is {@code null},
     *          or one of the elements of {@code envp} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If {@code command} is empty
     *
     * @see     #exec(String[], String[], File)
     * @see     ProcessBuilder
     */
    public Process exec(String command, String[] envp) throws IOException {
        return exec(command, envp, null);
    }

    /**
     * 指定された文字列コマンドを、指定された環境および作業ディレクトリで、別のプロセスとして実行します。
     * これは利便性のためのメソッドです。exec(command, envp, dir) の呼び出しは、exec(cmdarray, envp, dir) と全く同じ方法で動作します。
     * ここで、cmdarray は command のすべてのトークンを含む配列です。
     * より正確には、コマンド文字列は、new StringTokenizer(command) の呼び出しによって作成された StringTokenizer を使用してトークンに分割されます。
     * この際、文字カテゴリに対するさらなる修正は行われません。トークナイザによって生成されたトークンは、順序を保ったまま新しい文字列配列 cmdarray に格納されます。
     *
     * @param   command   a specified system command.
     *
     * @param   envp      array of strings, each element of which
     *                    has environment variable settings in the format
     *                    <i>name</i>=<i>value</i>, or
     *                    {@code null} if the subprocess should inherit
     *                    the environment of the current process.
     *
     * @param   dir       the working directory of the subprocess, or
     *                    {@code null} if the subprocess should inherit
     *                    the working directory of the current process.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code command} is {@code null},
     *          or one of the elements of {@code envp} is {@code null}
     *
     * @throws  IllegalArgumentException
     *          If {@code command} is empty
     *
     * @see     ProcessBuilder
     * @since 1.3
     */
    public Process exec(String command, String[] envp, File dir)
        throws IOException {
        if (command.isEmpty())
            throw new IllegalArgumentException("Empty command");

        StringTokenizer st = new StringTokenizer(command);
        String[] cmdarray = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            cmdarray[i] = st.nextToken();
        return exec(cmdarray, envp, dir);
    }

    /**
     * Executes the specified command and arguments in a separate process.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code exec(cmdarray)}
     * behaves in exactly the same way as the invocation
     * {@link #exec(String[], String[], File) exec}{@code (cmdarray, null, null)}.
     *
     * @param   cmdarray  array containing the command to call and
     *                    its arguments.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code cmdarray} is {@code null},
     *          or one of the elements of {@code cmdarray} is {@code null}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code cmdarray} is an empty array
     *          (has length {@code 0})
     *
     * @see     ProcessBuilder
     */
    public Process exec(String cmdarray[]) throws IOException {
        return exec(cmdarray, null, null);
    }

    /**
     * Executes the specified command and arguments in a separate process
     * with the specified environment.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code exec(cmdarray, envp)}
     * behaves in exactly the same way as the invocation
     * {@link #exec(String[], String[], File) exec}{@code (cmdarray, envp, null)}.
     *
     * @param   cmdarray  array containing the command to call and
     *                    its arguments.
     *
     * @param   envp      array of strings, each element of which
     *                    has environment variable settings in the format
     *                    <i>name</i>=<i>value</i>, or
     *                    {@code null} if the subprocess should inherit
     *                    the environment of the current process.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code cmdarray} is {@code null},
     *          or one of the elements of {@code cmdarray} is {@code null},
     *          or one of the elements of {@code envp} is {@code null}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code cmdarray} is an empty array
     *          (has length {@code 0})
     *
     * @see     ProcessBuilder
     */
    public Process exec(String[] cmdarray, String[] envp) throws IOException {
        return exec(cmdarray, envp, null);
    }


    /**
     * 指定されたコマンドと引数を、指定された環境および作業ディレクトリで別プロセスとして実行します。
     * 文字列配列 cmdarray はコマンドラインのトークンを表し、文字列配列 envp は「環境」変数の設定を表します。このメソッドは、指定されたコマンドを実行する新しいプロセスを作成します。
     * このメソッドは cmdarray が有効なオペレーティングシステムのコマンドであることを確認します。
     * 有効なコマンドはシステムに依存しますが、最低限、コマンドは空でなく、null でない文字列のリストである必要があります。
     * envp が null の場合、サブプロセスは現在のプロセスの環境設定を継承します。
     * 一部のオペレーティングシステムでは、プロセスを開始するためにシステム依存の環境変数の最小セットが必要になる場合があります。
     * その結果、サブプロセスは指定された環境に加えて追加の環境変数設定を継承することがあります。
     *
     * ProcessBuilder.start() は、環境を変更してプロセスを開始するための現在推奨される方法です。
     *
     * 新しいサブプロセスの作業ディレクトリは dir で指定されます。dir が null の場合、サブプロセスは現在のプロセスの作業ディレクトリを継承します。
     *
     * セキュリティマネージャが存在する場合、配列 cmdarray の最初の要素を引数として、checkExec メソッドが呼び出されます。
     * これにより SecurityException がスローされる可能性があります。
     *
     * オペレーティングシステムプロセスの開始は高度にシステム依存であり、次のような問題が発生する可能性があります：
     * <ul>
     *     <li>オペレーティングシステムのプログラムファイルが見つからない。</li>
     *     <li>プログラムファイルへのアクセスが拒否された。</li>
     *     <li>作業ディレクトリが存在しない。</li>
     * </ul>
     * これらの場合、例外がスローされます。例外の具体的な内容はシステムに依存しますが、常に IOException のサブクラスになります。
     *
     * オペレーティングシステムがプロセスの作成をサポートしていない場合、UnsupportedOperationException がスローされます。
     *
     *
     * @param   cmdarray  array containing the command to call and
     *                    its arguments.
     *
     * @param   envp      array of strings, each element of which
     *                    has environment variable settings in the format
     *                    <i>name</i>=<i>value</i>, or
     *                    {@code null} if the subprocess should inherit
     *                    the environment of the current process.
     *
     * @param   dir       the working directory of the subprocess, or
     *                    {@code null} if the subprocess should inherit
     *                    the working directory of the current process.
     *
     * @return  A new {@link Process} object for managing the subprocess
     *
     * @throws  SecurityException
     *          If a security manager exists and its
     *          {@link SecurityManager#checkExec checkExec}
     *          method doesn't allow creation of the subprocess
     *
     * @throws  UnsupportedOperationException
     *          If the operating system does not support the creation of processes.
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  NullPointerException
     *          If {@code cmdarray} is {@code null},
     *          or one of the elements of {@code cmdarray} is {@code null},
     *          or one of the elements of {@code envp} is {@code null}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code cmdarray} is an empty array
     *          (has length {@code 0})
     *
     * @see     ProcessBuilder
     * @since 1.3
     */
    public Process exec(String[] cmdarray, String[] envp, File dir)
        throws IOException {
        return new ProcessBuilder(cmdarray)
            .environment(envp)
            .directory(dir)
            .start();
    }

    /**
     * Java仮想マシンに利用可能なプロセッサの数を返します。
     * この値は、特定の仮想マシンの呼び出し中に変化する可能性があります。
     * したがって、利用可能なプロセッサ数に敏感なアプリケーションは、定期的にこのプロパティをポーリングして、リソースの使用量を適切に調整するべきです。
     *
     * @return  仮想マシンに利用可能な最大プロセッサ数。常に1以上の値です。
     * @since 1.4
     */
    public native int availableProcessors();

    /**
     * Java仮想マシンの空きメモリの量を返します。gcメソッドを呼び出すことで、freeMemoryが返す値が増加する場合があります。
     *
     * @return
     * 現在、将来のオブジェクトの割り当てに利用可能なメモリの総量の概算値で、バイト単位で測定されます。
     */
    public native long freeMemory();

    /**
     * Java仮想マシンの総メモリ量を返します。このメソッドによって返される値は、ホスト環境によって時間とともに変化する場合があります。
     * なお、任意の型のオブジェクトを保持するために必要なメモリ量は、実装によって異なる場合があることに注意してください。
     *
     * @return  現在および将来のオブジェクトのために利用可能な総メモリ量（バイト単位）を返します。
     */
    public native long totalMemory();

    /**
     * Java仮想マシンが使用しようとする最大のメモリ量を返します。もし制限がなければ、その値として Long.MAX_VALUE が返されます。
     *
     * @return  仮想マシンが使用しようとする最大のメモリ量、バイト単位で測定された値。
     * @since 1.4
     */
    public native long maxMemory();

    /**
     * Java仮想マシンでガベージコレクタを実行します。このメソッドを呼び出すことは、
     * Java仮想マシンが未使用のオブジェクトをリサイクルして、そのメモリを再利用できるようにするために努力することを示唆します。
     * メソッド呼び出しが終了すると、Java仮想マシンはすべての未使用オブジェクトからメモリを回収するために最善を尽くします。
     * ただし、この努力によって特定の数の未使用オブジェクトがリサイクルされること、特定の量のメモリが回収されること、またはメソッドが戻る前に、
     * あるいは戻る時点でその努力が完了することは保証されません。
     * また、この努力によって特定のオブジェクトの到達可能性の変更が決定されることや、特定の数のReferenceオブジェクトがクリアされてキューに入れられることも保証されません。
     *
     * gcという名前は「ガベージコレクタ」を意味します。Java仮想マシンは、gcメソッドが明示的に呼び出されなくても、必要に応じて自動的にこのリサイクルプロセスを別のスレッドで実行します。
     * {@code System.gc()}メソッドは、このメソッドを呼び出すための標準的で便利な手段です。
     */
    public native void gc();

    /**
     * 保留中のオブジェクトのfinalizeメソッドを実行します。このメソッドを呼び出すことは、Java仮想マシンが廃棄されたことが確認されたが、
     * まだfinalizeメソッドが実行されていないオブジェクトのfinalizeメソッドを実行するために努力することを示唆します。
     * メソッド呼び出しが終了すると、仮想マシンは未処理のfinalize処理を完了するために最善を尽くします。
     *
     * 仮想マシンは、runFinalizationメソッドが明示的に呼び出されない場合、必要に応じて自動的にこのfinalize処理を別のスレッドで実行します。
     *
     * {@code System.runFinalization()}メソッドは、このメソッドを呼び出すための標準的で便利な手段です。
     *
     * @see     Object#finalize()
     */
    public void runFinalization() {
        SharedSecrets.getJavaLangRefAccess().runFinalization();
    }

    /**
     * 指定されたファイル名引数によってネイティブライブラリを読み込みます。
     * ファイル名引数は絶対パス名でなければなりません。（例えば、Runtime.getRuntime().load("/home/avh/lib/libX11.so");のように）。
     * ファイル名引数が、プラットフォーム固有のライブラリ接頭辞、パス、ファイル拡張子を除去したときに、例えば「L」というライブラリ名を示す場合、
     * 仮想マシンに静的にリンクされているネイティブライブラリ「L」に対応するJNI_OnLoad_L関数が呼び出され、
     * 動的ライブラリを読み込もうとする代わりに、その関数が呼び出されます。引数に一致するファイル名がファイルシステムに存在する必要はありません。
     * 詳細については、JNI仕様を参照してください。それ以外の場合、ファイル名引数は実装依存の方法でネイティブライブラリのイメージにマップされます。
     *
     * まず、セキュリティマネージャーが存在する場合、そのcheckLinkメソッドがファイル名を引数として呼び出されます。これにより、セキュリティ例外が発生することがあります。
     * これは、loadLibrary(String)メソッドに似ていますが、ライブラリ名だけでなく、任意のファイル名を引数として受け取るため、任意のネイティブコードのファイルを読み込むことができます。
     * {@code System.load(String)}メソッドは、このメソッドを呼び出すための標準的で便利な手段です。
     *
     * @param      filename   the file to load.
     * @throws     SecurityException  if a security manager exists and its
     *             {@code checkLink} method doesn't allow
     *             loading of the specified dynamic library
     * @throws     UnsatisfiedLinkError  もし、ファイル名が絶対パス名でない場合、ネイティブライブラリが仮想マシンに静的にリンクされていない場合、
     *             またはホストシステムによってネイティブライブラリイメージにマップできない場合
     * @throws     NullPointerException if {@code filename} is
     *             {@code null}
     * @see        Runtime#getRuntime()
     * @see        SecurityException
     * @see        SecurityManager#checkLink(String)
     */
    @CallerSensitive
    public void load(String filename) {
        load0(Reflection.getCallerClass(), filename);
    }

    void load0(Class<?> fromClass, String filename) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkLink(filename);
        }
        File file = new File(filename);
        if (!file.isAbsolute()) {
            throw new UnsatisfiedLinkError(
                "Expecting an absolute path of the library: " + filename);
        }
        ClassLoader.loadLibrary(fromClass, file);
    }

    /**
     *
     * 指定された libname 引数でネイティブライブラリをロードします。
     * libname 引数はプラットフォーム固有のプレフィックス、ファイル拡張子、またはパスを含んではいけません。
     * もし libname という名前のネイティブライブラリが仮想マシンと静的にリンクされている場合、
     * そのライブラリによってエクスポートされる JNI_OnLoad_libname 関数が呼び出されます。
     * 詳細については、JNI仕様を参照してください。それ以外の場合、libname 引数はシステムのライブラリロケーションからロードされ、実装依存の方法でネイティブライブラリイメージにマップされます。
     *
     * 最初に、セキュリティマネージャが存在する場合、その checkLink メソッドが libname を引数として呼び出されます。これによりセキュリティ例外が発生する場合があります。
     *
     * このメソッドは、{@code System.loadLibrary(String)} という標準的で便利な方法で呼び出されます。
     * ネイティブメソッドをクラスの実装に使用する場合、標準的な戦略として、ネイティブコードをライブラリファイル（これを LibFile と呼ぶ）に配置し、
     * その後クラス宣言内に静的初期化子として次のコードを記述します：
     * <blockquote><pre>
     * static { System.loadLibrary("LibFile"); }
     * </pre></blockquote>
     * これにより、クラスがロードされ初期化される際に、ネイティブメソッドの必要なネイティブコード実装も同時にロードされます。
     * 同じライブラリ名でこのメソッドが複数回呼び出される場合、2回目以降の呼び出しは無視されます。
     *
     * @param      libname   the name of the library.
     * @throws     SecurityException  セキュリティマネージャが存在し、その checkLink メソッドが指定された動的ライブラリの読み込みを許可しない場合
     * @throws     UnsatisfiedLinkError libname 引数がファイルパスを含んでいる場合、ネイティブライブラリが仮想マシンと静的にリンクされていない場合、
     *             またはライブラリがホストシステムによってネイティブライブラリイメージにマップできない場合
     * @throws     NullPointerException if {@code libname} is
     *             {@code null}
     * @see        SecurityException
     * @see        SecurityManager#checkLink(String)
     */
    @CallerSensitive
    public void loadLibrary(String libname) {
        loadLibrary0(Reflection.getCallerClass(), libname);
    }

    void loadLibrary0(Class<?> fromClass, String libname) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkLink(libname);
        }
        if (libname.indexOf((int)File.separatorChar) != -1) {
            throw new UnsatisfiedLinkError(
                "Directory separator should not appear in library name: " + libname);
        }
        ClassLoader.loadLibrary(fromClass, libname);
    }

    /**
     *
     * Java Runtime Environment のバージョンを Runtime.Version として返します。
     *
     * @return  the {@link Version} of the Java Runtime Environment
     *
     * @since  9
     */
    public static Version version() {
        var v = version;
        if (v == null) {
            v = new Version(VersionProps.versionNumbers(),
                    VersionProps.pre(), VersionProps.build(),
                    VersionProps.optional());
            version = v;
        }
        return v;
    }

    /**
     * Java SEプラットフォームの実装におけるバージョン文字列の表現です。バージョン文字列は、バージョン番号の後にオプションで前リリース情報とビルド情報が続きます。
     *
     * <h2><a id="verNum">バージョン番号</a></h2>
     *
     * バージョン番号、$VNUMは、ドット文字（U+002E）で区切られた要素の非空のシーケンスです。要素は、ゼロまたは先頭ゼロのない符号なし整数数字です。
     * バージョン番号の最終要素はゼロであってはなりません。要素がインクリメントされると、それ以降の要素は削除されます。フォーマットは次のようになります：
     *
     * <blockquote><pre>
     * [1-9][0-9]*((\.0)*\.[1-9][0-9]*)*
     * </pre></blockquote>
     *
     * このシーケンスは任意の長さを取ることができますが、最初の4つの要素は特定の意味を持っています。
     *
     * <blockquote><pre>
     * $FEATURE.$INTERIM.$UPDATE.$PATCH
     * </pre></blockquote>
     *
     * <ul>
     *
     * <li><p> <a id="FEATURE">{@code $FEATURE}</a> &#x2014;
     * 機能リリースカウンター。リリースの内容に関係なく、すべての機能リリースでインクリメントされます。
     * 機能リリースでは新機能が追加されることがありますが、あらかじめ1回以上前の機能リリースで通知があれば、機能が削除されることもあります。
     * 不整合な変更が正当化される場合には行われることもあります。 </p></li>
     *
     * <li><p> <a id="INTERIM">{@code $INTERIM}</a> &#x2014;
     * 中間リリースカウンター。互換性のあるバグ修正や改善を含むが、不整合な変更、機能の削除、標準APIへの変更を含まないリリースでインクリメントされます。
     * </p></li>
     *
     * <li><p> <a id="UPDATE">{@code $UPDATE}</a> &#x2014;
     * アップデートリリースカウンター。セキュリティの問題、回帰、不具合の修正、新機能の不具合修正などの互換性のある更新リリースでインクリメントされます。</p></li>
     *
     * <li><p> <a id="PATCH">{@code $PATCH}</a> &#x2014;
     * 緊急パッチリリースカウンター。重大な問題を修正するために緊急リリースを行う必要がある場合にのみインクリメントされます。 </p></li>
     * </ul>

     * <p>バージョン番号の構造と比較方法</p>
     * バージョン番号の5番目以降の要素は、プラットフォームの実装者が特定のパッチリリースを識別するために使用できます。
     *
     * バージョン番号には末尾にゼロの要素は存在しません。もしある要素とその後に続くすべての要素が論理的にゼロであれば、それらはすべて省略されます。
     * バージョン番号の数値シーケンスは、別のシーケンスと数値的に比較されます。たとえば、10.0.4は10.1.2よりも小さいと評価されます。
     * もし一方のシーケンスが他方より短ければ、欠けている要素は長い方のシーケンスの対応する要素よりも小さいと見なされます。たとえば、10.0.2は10.0.2.1よりも小さいと評価されます。
     *
     * <h2><a id="verStr">Version strings</a></h2>
     *
     * <p> A <em>version string</em>, {@code $VSTR}, is a version number {@code
     * $VNUM}, as described above, optionally followed by pre-release and build
     * information, in one of the following formats: </p>
     *
     * <blockquote><pre>
     *     $VNUM(-$PRE)?\+$BUILD(-$OPT)?
     *     $VNUM-$PRE(-$OPT)?
     *     $VNUM(\+-$OPT)?
     * </pre></blockquote>
     *
     * <p> where: </p>
     *
     * <ul>
     *
     * <li><p> <a id="pre">{@code $PRE}</a>, matching {@code ([a-zA-Z0-9]+)}
     * &#x2014; A pre-release identifier.  Typically {@code ea}, for a
     * potentially unstable early-access release under active development, or
     * {@code internal}, for an internal developer build. </p></li>
     *
     * <li><p> <a id="build">{@code $BUILD}</a>, matching {@code
     * (0|[1-9][0-9]*)} &#x2014; The build number, incremented for each promoted
     * build.  {@code $BUILD} is reset to {@code 1} when any portion of {@code
     * $VNUM} is incremented. </p></li>
     *
     * <li><p> <a id="opt">{@code $OPT}</a>, matching {@code ([-a-zA-Z0-9.]+)}
     * &#x2014; Additional build information, if desired.  In the case of an
     * {@code internal} build this will often contain the date and time of the
     * build. </p></li>
     *
     * </ul>
     *
     * <p> A version string {@code 10-ea} matches {@code $VNUM = "10"} and
     * {@code $PRE = "ea"}.  The version string {@code 10+-ea} matches
     * {@code $VNUM = "10"} and {@code $OPT = "ea"}. </p>
     *
     * <p> When comparing two version strings, the value of {@code $OPT}, if
     * present, may or may not be significant depending on the chosen
     * comparison method.  The comparison methods {@link #compareTo(Version)
     * compareTo()} and {@link #compareToIgnoreOptional(Version)
     * compareToIgnoreOptional()} should be used consistently with the
     * corresponding methods {@link #equals(Object) equals()} and {@link
     * #equalsIgnoreOptional(Object) equalsIgnoreOptional()}.  </p>
     *
     * <p> A <em>short version string</em>, {@code $SVSTR}, often useful in
     * less formal contexts, is a version number optionally followed by a
     * pre-release identifier:</p>
     *
     * <blockquote><pre>
     *     $VNUM(-$PRE)?
     * </pre></blockquote>
     *
     * <p>This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
     * class; programmers should treat instances that are
     * {@linkplain #equals(Object) equal} as interchangeable and should not
     * use instances for synchronization, or unpredictable behavior may
     * occur. For example, in a future release, synchronization may fail.</p>
     *
     * @since  9
     */
    @jdk.internal.ValueBased
    public static final class Version
        implements Comparable<Version>
    {
        private final List<Integer>     version;
        private final Optional<String>  pre;
        private final Optional<Integer> build;
        private final Optional<String>  optional;

        /*
         * List of version number components passed to this constructor MUST
         * be at least unmodifiable (ideally immutable). In the case of an
         * unmodifiable list, the caller MUST hand the list over to this
         * constructor and never change the underlying list.
         */
        private Version(List<Integer> unmodifiableListOfVersions,
                        Optional<String> pre,
                        Optional<Integer> build,
                        Optional<String> optional)
        {
            this.version = unmodifiableListOfVersions;
            this.pre = pre;
            this.build = build;
            this.optional = optional;
        }

        /**
         * Parses the given string as a valid
         * <a href="#verStr">version string</a> containing a
         * <a href="#verNum">version number</a> followed by pre-release and
         * build information.
         *
         * @param  s
         *         A string to interpret as a version
         *
         * @throws  IllegalArgumentException
         *          If the given string cannot be interpreted as a valid
         *          version
         *
         * @throws  NullPointerException
         *          If the given string is {@code null}
         *
         * @throws  NumberFormatException
         *          If an element of the version number or the build number
         *          cannot be represented as an {@link Integer}
         *
         * @return  The Version of the given string
         */
        public static Version parse(String s) {
            if (s == null)
                throw new NullPointerException();

            // Shortcut to avoid initializing VersionPattern when creating
            // feature-version constants during startup
            if (isSimpleNumber(s)) {
                return new Version(List.of(Integer.parseInt(s)),
                        Optional.empty(), Optional.empty(), Optional.empty());
            }
            Matcher m = VersionPattern.VSTR_PATTERN.matcher(s);
            if (!m.matches())
                throw new IllegalArgumentException("Invalid version string: '"
                                                   + s + "'");

            // $VNUM is a dot-separated list of integers of arbitrary length
            String[] split = m.group(VersionPattern.VNUM_GROUP).split("\\.");
            Integer[] version = new Integer[split.length];
            for (int i = 0; i < split.length; i++) {
                version[i] = Integer.parseInt(split[i]);
            }

            Optional<String> pre = Optional.ofNullable(
                    m.group(VersionPattern.PRE_GROUP));

            String b = m.group(VersionPattern.BUILD_GROUP);
            // $BUILD is an integer
            Optional<Integer> build = (b == null)
                ? Optional.empty()
                : Optional.of(Integer.parseInt(b));

            Optional<String> optional = Optional.ofNullable(
                    m.group(VersionPattern.OPT_GROUP));

            // empty '+'
            if (!build.isPresent()) {
                if (m.group(VersionPattern.PLUS_GROUP) != null) {
                    if (optional.isPresent()) {
                        if (pre.isPresent())
                            throw new IllegalArgumentException("'+' found with"
                                + " pre-release and optional components:'" + s
                                + "'");
                    } else {
                        throw new IllegalArgumentException("'+' found with neither"
                            + " build or optional components: '" + s + "'");
                    }
                } else {
                    if (optional.isPresent() && !pre.isPresent()) {
                        throw new IllegalArgumentException("optional component"
                            + " must be preceded by a pre-release component"
                            + " or '+': '" + s + "'");
                    }
                }
            }
            return new Version(List.of(version), pre, build, optional);
        }

        private static boolean isSimpleNumber(String s) {
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                char lowerBound = (i > 0) ? '0' : '1';
                if (c < lowerBound || c > '9') {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns the value of the <a href="#FEATURE">feature</a> element of
         * the version number.
         *
         * @return The value of the feature element
         *
         * @since 10
         */
        public int feature() {
            return version.get(0);
        }

        /**
         * Returns the value of the <a href="#INTERIM">interim</a> element of
         * the version number, or zero if it is absent.
         *
         * @return The value of the interim element, or zero
         *
         * @since 10
         */
        public int interim() {
            return (version.size() > 1 ? version.get(1) : 0);
        }

        /**
         * Returns the value of the <a href="#UPDATE">update</a> element of the
         * version number, or zero if it is absent.
         *
         * @return The value of the update element, or zero
         *
         * @since 10
         */
        public int update() {
            return (version.size() > 2 ? version.get(2) : 0);
        }

        /**
         * Returns the value of the <a href="#PATCH">patch</a> element of the
         * version number, or zero if it is absent.
         *
         * @return The value of the patch element, or zero
         *
         * @since 10
         */
        public int patch() {
            return (version.size() > 3 ? version.get(3) : 0);
        }

        /**
         * Returns the value of the major element of the version number.
         *
         * @deprecated As of Java&nbsp;SE 10, the first element of a version
         * number is not the major-release number but the feature-release
         * counter, incremented for every time-based release.  Use the {@link
         * #feature()} method in preference to this method.  For compatibility,
         * this method returns the value of the <a href="#FEATURE">feature</a>
         * element.
         *
         * @return The value of the feature element
         */
        @Deprecated(since = "10")
        public int major() {
            return feature();
        }

        /**
         * Returns the value of the minor element of the version number, or
         * zero if it is absent.
         *
         * @deprecated As of Java&nbsp;SE 10, the second element of a version
         * number is not the minor-release number but the interim-release
         * counter, incremented for every interim release.  Use the {@link
         * #interim()} method in preference to this method.  For compatibility,
         * this method returns the value of the <a href="#INTERIM">interim</a>
         * element, or zero if it is absent.
         *
         * @return The value of the interim element, or zero
         */
        @Deprecated(since = "10")
        public int minor() {
            return interim();
        }

        /**
         * Returns the value of the security element of the version number, or
         * zero if it is absent.
         *
         * @deprecated As of Java&nbsp;SE 10, the third element of a version
         * number is not the security level but the update-release counter,
         * incremented for every update release.  Use the {@link #update()}
         * method in preference to this method.  For compatibility, this method
         * returns the value of the <a href="#UPDATE">update</a> element, or
         * zero if it is absent.
         *
         * @return  The value of the update element, or zero
         */
        @Deprecated(since = "10")
        public int security() {
            return update();
        }

        /**
         * Returns an unmodifiable {@link List List} of the integers
         * represented in the <a href="#verNum">version number</a>.
         * The {@code List} always contains at least one element corresponding to
         * the <a href="#FEATURE">feature version number</a>.
         *
         * @return  An unmodifiable list of the integers
         *          represented in the version number
         */
        public List<Integer> version() {
            return version;
        }

        /**
         * Returns the optional <a href="#pre">pre-release</a> information.
         *
         * @return  The optional pre-release information as a String
         */
        public Optional<String> pre() {
            return pre;
        }

        /**
         * Returns the <a href="#build">build number</a>.
         *
         * @return  The optional build number.
         */
        public Optional<Integer> build() {
            return build;
        }

        /**
         * Returns <a href="#opt">optional</a> additional identifying build
         * information.
         *
         * @return  Additional build information as a String
         */
        public Optional<String> optional() {
            return optional;
        }

        /**
         * Compares this version to another.
         *
         * <p> Each of the components in the <a href="#verStr">version</a> is
         * compared in the following order of precedence: version numbers,
         * pre-release identifiers, build numbers, optional build information.
         * </p>
         *
         * <p> Comparison begins by examining the sequence of version numbers.
         * If one sequence is shorter than another, then the missing elements
         * of the shorter sequence are considered to be less than the
         * corresponding elements of the longer sequence. </p>
         *
         * <p> A version with a pre-release identifier is always considered to
         * be less than a version without one.  Pre-release identifiers are
         * compared numerically when they consist only of digits, and
         * lexicographically otherwise.  Numeric identifiers are considered to
         * be less than non-numeric identifiers.  </p>
         *
         * <p> A version without a build number is always less than one with a
         * build number; otherwise build numbers are compared numerically. </p>
         *
         * <p> The optional build information is compared lexicographically.
         * During this comparison, a version with optional build information is
         * considered to be greater than a version without one. </p>
         *
         * @param  obj
         *         The object to be compared
         *
         * @return  A negative integer, zero, or a positive integer if this
         *          {@code Version} is less than, equal to, or greater than the
         *          given {@code Version}
         *
         * @throws  NullPointerException
         *          If the given object is {@code null}
         */
        @Override
        public int compareTo(Version obj) {
            return compare(obj, false);
        }

        /**
         * Compares this version to another disregarding optional build
         * information.
         *
         * <p> Two versions are compared by examining the version string as
         * described in {@link #compareTo(Version)} with the exception that the
         * optional build information is always ignored. </p>
         *
         * <p> This method provides ordering which is consistent with
         * {@code equalsIgnoreOptional()}. </p>
         *
         * @param  obj
         *         The object to be compared
         *
         * @return  A negative integer, zero, or a positive integer if this
         *          {@code Version} is less than, equal to, or greater than the
         *          given {@code Version}
         *
         * @throws  NullPointerException
         *          If the given object is {@code null}
         */
        public int compareToIgnoreOptional(Version obj) {
            return compare(obj, true);
        }

        private int compare(Version obj, boolean ignoreOpt) {
            if (obj == null)
                throw new NullPointerException();

            int ret = compareVersion(obj);
            if (ret != 0)
                return ret;

            ret = comparePre(obj);
            if (ret != 0)
                return ret;

            ret = compareBuild(obj);
            if (ret != 0)
                return ret;

            if (!ignoreOpt)
                return compareOptional(obj);

            return 0;
        }

        private int compareVersion(Version obj) {
            int size = version.size();
            int oSize = obj.version().size();
            int min = Math.min(size, oSize);
            for (int i = 0; i < min; i++) {
                int val = version.get(i);
                int oVal = obj.version().get(i);
                if (val != oVal)
                    return val - oVal;
            }
            return size - oSize;
        }

        private int comparePre(Version obj) {
            Optional<String> oPre = obj.pre();
            if (!pre.isPresent()) {
                if (oPre.isPresent())
                    return 1;
            } else {
                if (!oPre.isPresent())
                    return -1;
                String val = pre.get();
                String oVal = oPre.get();
                if (val.matches("\\d+")) {
                    return (oVal.matches("\\d+")
                        ? (new BigInteger(val)).compareTo(new BigInteger(oVal))
                        : -1);
                } else {
                    return (oVal.matches("\\d+")
                        ? 1
                        : val.compareTo(oVal));
                }
            }
            return 0;
        }

        private int compareBuild(Version obj) {
            Optional<Integer> oBuild = obj.build();
            if (oBuild.isPresent()) {
                return (build.isPresent()
                        ? build.get().compareTo(oBuild.get())
                        : -1);
            } else if (build.isPresent()) {
                return 1;
            }
            return 0;
        }

        private int compareOptional(Version obj) {
            Optional<String> oOpt = obj.optional();
            if (!optional.isPresent()) {
                if (oOpt.isPresent())
                    return -1;
            } else {
                if (!oOpt.isPresent())
                    return 1;
                return optional.get().compareTo(oOpt.get());
            }
            return 0;
        }

        /**
         * Returns a string representation of this version.
         *
         * @return  The version string
         */
        @Override
        public String toString() {
            StringBuilder sb
                = new StringBuilder(version.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(".")));

            pre.ifPresent(v -> sb.append("-").append(v));

            if (build.isPresent()) {
                sb.append("+").append(build.get());
                if (optional.isPresent())
                    sb.append("-").append(optional.get());
            } else {
                if (optional.isPresent()) {
                    sb.append(pre.isPresent() ? "-" : "+-");
                    sb.append(optional.get());
                }
            }

            return sb.toString();
        }

        /**
         * Determines whether this {@code Version} is equal to another object.
         *
         * <p> Two {@code Version}s are equal if and only if they represent the
         * same version string.
         *
         * @param  obj
         *         The object to which this {@code Version} is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a {@code
         *          Version} that is identical to this {@code Version}
         *
         */
        @Override
        public boolean equals(Object obj) {
            boolean ret = equalsIgnoreOptional(obj);
            if (!ret)
                return false;

            Version that = (Version)obj;
            return (this.optional().equals(that.optional()));
        }

        /**
         * Determines whether this {@code Version} is equal to another
         * disregarding optional build information.
         *
         * <p> Two {@code Version}s are equal if and only if they represent the
         * same version string disregarding the optional build information.
         *
         * @param  obj
         *         The object to which this {@code Version} is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a {@code
         *          Version} that is identical to this {@code Version}
         *          ignoring the optional build information
         *
         */
        public boolean equalsIgnoreOptional(Object obj) {
            if (this == obj)
                return true;
            return (obj instanceof Version that)
                && (this.version().equals(that.version())
                && this.pre().equals(that.pre())
                && this.build().equals(that.build()));
        }

        /**
         * Returns the hash code of this version.
         *
         * @return  The hashcode of this version
         */
        @Override
        public int hashCode() {
            int h = 1;
            int p = 17;

            h = p * h + version.hashCode();
            h = p * h + pre.hashCode();
            h = p * h + build.hashCode();
            h = p * h + optional.hashCode();

            return h;
        }
    }

    private static class VersionPattern {
        // $VNUM(-$PRE)?(\+($BUILD)?(\-$OPT)?)?
        // RE limits the format of version strings
        // ([1-9][0-9]*(?:(?:\.0)*\.[1-9][0-9]*)*)(?:-([a-zA-Z0-9]+))?(?:(\+)(0|[1-9][0-9]*)?)?(?:-([-a-zA-Z0-9.]+))?

        private static final String VNUM
            = "(?<VNUM>[1-9][0-9]*(?:(?:\\.0)*\\.[1-9][0-9]*)*)";
        private static final String PRE      = "(?:-(?<PRE>[a-zA-Z0-9]+))?";
        private static final String BUILD
            = "(?:(?<PLUS>\\+)(?<BUILD>0|[1-9][0-9]*)?)?";
        private static final String OPT      = "(?:-(?<OPT>[-a-zA-Z0-9.]+))?";
        private static final String VSTR_FORMAT = VNUM + PRE + BUILD + OPT;

        static final Pattern VSTR_PATTERN = Pattern.compile(VSTR_FORMAT);

        static final String VNUM_GROUP  = "VNUM";
        static final String PRE_GROUP   = "PRE";
        static final String PLUS_GROUP  = "PLUS";
        static final String BUILD_GROUP = "BUILD";
        static final String OPT_GROUP   = "OPT";
    }
}
