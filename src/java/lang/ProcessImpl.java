/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.access.JavaIOFileDescriptorAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.util.StaticProperty;
import sun.security.action.GetPropertyAction;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * java.lang.Process subclass in the UNIX environment.
 *
 * @author Mario Wolczko and Ross Knippel.
 * @author Konstantin Kladko (ported to Linux and Bsd)
 * @author Martin Buchholz
 * @author Volker Simonis (ported to AIX)
 * @since   1.5
 */
final class ProcessImpl extends Process {
    // Java 内部でファイルディスクリプタにアクセスするための特権 API を取得します。
    // ファイルディスクリプタは、標準入力やファイルリダイレクトの低レベル操作に必要です
    private static final JavaIOFileDescriptorAccess fdAccess
        = SharedSecrets.getJavaIOFileDescriptorAccess();

    // Linuxプラットフォームは通常の（強制的でない）終了シグナルをサポートしています
    static final boolean SUPPORTS_NORMAL_TERMINATION = true;

    private final int pid;
    // プロセスを識別するためのハンドル
    private final ProcessHandleImpl processHandle;
    // プロセス終了時のコードを格納
    private int exitcode;
    // プロセスが終了済みかどうかを示すフラグ
    private boolean hasExited;

    private /* final */ OutputStream stdin;
    private /* final */ InputStream  stdout;
    private /* final */ InputStream  stderr;

    // プロセスを起動するメカニズムを定義
    private static enum LaunchMechanism {
        // 親プロセスの複製から開始する従来の方法
        FORK,
        // POSIX 標準のプロセス生成 API
        POSIX_SPAWN,
        // メモリ消費を抑えた軽量なプロセス生成
        VFORK
    }

    /**
     * 異なるプラットフォーム（OS）のプロセス起動メカニズムを表現。
     * <p>定義されている項目:</p>
     * <li>各プラットフォーム（LINUX, BSD, AIX）に対応する起動メカニズム。
     * <li>デフォルト起動メカニズムと許容される起動メカニズムのリストを管理。
     * <li>実行環境の OS に基づいて適切なプラットフォームを取得する機能を提供。
     */
    private static enum Platform {
        // 各定数に、プラットフォーム固有の起動メカニズム（LaunchMechanism）が渡される
        LINUX(LaunchMechanism.POSIX_SPAWN, LaunchMechanism.VFORK, LaunchMechanism.FORK),

        BSD(LaunchMechanism.POSIX_SPAWN, LaunchMechanism.FORK),

        AIX(LaunchMechanism.POSIX_SPAWN, LaunchMechanism.FORK);

        // プラットフォームごとのデフォルト起動方法
        final LaunchMechanism defaultLaunchMechanism;
        // 許容される起動メカニズムのセット
        final Set<LaunchMechanism> validLaunchMechanisms;

        Platform(LaunchMechanism ... launchMechanisms) {
            this.defaultLaunchMechanism = launchMechanisms[0];
            this.validLaunchMechanisms =
                EnumSet.copyOf(Arrays.asList(launchMechanisms));
        }

        // システムプロパティ jdk.lang.Process.launchMechanism で指定された起動方法を取得。
        // 指定がない場合は、プラットフォームのデフォルト起動メカニズムを使用
        @SuppressWarnings("removal")
        LaunchMechanism launchMechanism() {
            // jdk.lang.Process.launchMechanism にアクセスするためには、通常はセキュリティ権限が必要ですが、doPrivileged() により権限の制限を回避しています
            return AccessController.doPrivileged(
                (PrivilegedAction<LaunchMechanism>) () -> {
                    String s = System.getProperty(
                        "jdk.lang.Process.launchMechanism");
                    LaunchMechanism lm;
                    if (s == null) {
                        lm = defaultLaunchMechanism;
                        s = lm.name().toLowerCase(Locale.ENGLISH);
                    } else {
                        try {
                            lm = LaunchMechanism.valueOf(
                                s.toUpperCase(Locale.ENGLISH));
                        } catch (IllegalArgumentException e) {
                            lm = null;
                        }
                    }
                    if (lm == null || !validLaunchMechanisms.contains(lm)) {
                        throw new Error(
                            s + " is not a supported " +
                            "process launch mechanism on this platform."
                        );
                    }
                    return lm;
                }
            );
        }
        // 実行環境の OS を識別し、対応するプラットフォームを返す
        static Platform get() {
            String osName = GetPropertyAction.privilegedGetProperty("os.name");

            if (osName.equals("Linux")) { return LINUX; }
            if (osName.contains("OS X")) { return BSD; }
            if (osName.equals("AIX")) { return AIX; }

            throw new Error(osName + " is not a supported OS platform.");
        }
    }

    private static final Platform platform = Platform.get();
    private static final LaunchMechanism launchMechanism = platform.launchMechanism();
    // helperpath は、javaHome に /lib/jspawnhelper を付けたパスを toCString メソッドでC言語の文字列形式（null終端）に変換しています
    private static final byte[] helperpath = toCString(StaticProperty.javaHome() + "/lib/jspawnhelper");

    private static byte[] toCString(String s) {
        if (s == null)
            return null;
        byte[] bytes = s.getBytes();
        byte[] result = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0,
                         result, 0,
                         bytes.length);
        result[result.length-1] = (byte)0;
        return result;
    }

    // Only for use by ProcessBuilder.start()
    static Process start(String[] cmdarray, // 実行するコマンドとその引数の配列
                         java.util.Map<String,String> environment,  //新しいプロセスの環境変数
                         String dir, // 新しいプロセスが実行されるディレクトリ
                         Redirect[] redirects, // 標準入出力とエラーストリームのリダイレクト設定
                         boolean redirectErrorStream) // 標準エラーと標準出力をリダイレクトするかどうか
            throws IOException
    {
        assert cmdarray != null && cmdarray.length > 0;

        // Convert arguments to a contiguous block; it's easier to do
        // memory management in Java than in C.
        byte[][] args = new byte[cmdarray.length-1][];
        int size = args.length; // For added NUL bytes
        for (int i = 0; i < args.length; i++) {
            args[i] = cmdarray[i+1].getBytes();
            size += args[i].length;
        }
        byte[] argBlock = new byte[size];
        int i = 0;
        for (byte[] arg : args) {
            System.arraycopy(arg, 0, argBlock, i, arg.length);
            i += arg.length + 1;
            // No need to write NUL bytes explicitly
        }

        int[] envc = new int[1];
        byte[] envBlock = ProcessEnvironment.toEnvironmentBlock(environment, envc);

        int[] std_fds;

        FileInputStream  f0 = null;
        FileOutputStream f1 = null;
        FileOutputStream f2 = null;

        try {
            boolean forceNullOutputStream = false;
            if (redirects == null) {
                std_fds = new int[] { -1, -1, -1 };
            } else {
                std_fds = new int[3];

                // プロセスの標準入力（stdin）に対するリダイレクトの設定を処理しています。
                // redirects[0] は ProcessBuilder で指定された標準入力のリダイレクト設定に対応し、その設定に応じて標準入力のファイルディスクリプタ（std_fds[0]）を適切に設定します
                if (redirects[0] == Redirect.PIPE) {
                    // Redirect.PIPE は、標準入力をプロセス間でパイプとして接続することを意味します。この場合、プロセスは標準入力をパイプから受け取ります。
                    // std_fds[0] = -1; は、標準入力がパイプから供給されることを示すためにファイルディスクリプタとして -1 を設定しています
                    std_fds[0] = -1;
                } else if (redirects[0] == Redirect.INHERIT) {
                    // Redirect.INHERIT は、標準入力を親プロセスから継承することを意味します。つまり、起動した新しいプロセスは親プロセスの標準入力をそのまま使用します。
                    // std_fds[0] = 0; は、標準入力が親プロセスから引き継がれることを示しています。UNIX系のシステムでは、0 は標準入力（stdin）のファイルディスクリプタです
                    std_fds[0] = 0;
                } else if (redirects[0] instanceof ProcessBuilder.RedirectPipeImpl) {
                    // ProcessBuilder.RedirectPipeImpl は、標準入力をパイプで接続するための内部クラスであり、具体的にどのファイルディスクリプタを使うかが決められます。
                    // fdAccess.get() は、指定されたファイルディスクリプタを取得するメソッドです。このファイルディスクリプタは、プロセスの標準入力として使用されます
                    std_fds[0] = fdAccess.get(((ProcessBuilder.RedirectPipeImpl) redirects[0]).getFd());
                } else {
                    // もし上記の条件に該当しない場合、標準入力をファイルにリダイレクトするケースです。
                    // redirects[0].file() は、標準入力がリダイレクトされるファイルを取得します。
                    // f0 = new FileInputStream(redirects[0].file()); は、指定されたファイルを FileInputStream として開き、そのファイルディスクリプタを取得します。
                    // fdAccess.get(f0.getFD()); は、FileInputStream のファイルディスクリプタを取得し、それを std_fds[0] に設定します。これにより、標準入力が指定されたファイルにリダイレクトされます
                    f0 = new FileInputStream(redirects[0].file());
                    std_fds[0] = fdAccess.get(f0.getFD());
                }

                // プロセスの標準出力（stdout）に対するリダイレクトの設定を行っています。
                // 標準出力をどこにリダイレクトするかを決定するロジックは、先程の標準入力（stdin）に対するリダイレクト処理とほぼ同じです
                if (redirects[1] == Redirect.PIPE) {
                    std_fds[1] = -1;
                } else if (redirects[1] == Redirect.INHERIT) {
                    std_fds[1] = 1;
                } else if (redirects[1] instanceof ProcessBuilder.RedirectPipeImpl) {
                    std_fds[1] = fdAccess.get(((ProcessBuilder.RedirectPipeImpl) redirects[1]).getFd());
                    // Force getInputStream to return a null stream,
                    // the fd is directly assigned to the next process.
                    forceNullOutputStream = true;
                } else {
                    f1 = new FileOutputStream(redirects[1].file(),
                            redirects[1].append());
                    std_fds[1] = fdAccess.get(f1.getFD());
                }

                // プロセスの標準エラー出力（stderr）に対するリダイレクト処理を行っています
                if (redirects[2] == Redirect.PIPE) {
                    std_fds[2] = -1;
                } else if (redirects[2] == Redirect.INHERIT) {
                    std_fds[2] = 2;
                } else if (redirects[2] instanceof ProcessBuilder.RedirectPipeImpl) {
                    std_fds[2] = fdAccess.get(((ProcessBuilder.RedirectPipeImpl) redirects[2]).getFd());
                } else {
                    f2 = new FileOutputStream(redirects[2].file(),
                            redirects[2].append());
                    std_fds[2] = fdAccess.get(f2.getFD());
                }
            }

            Process p = new ProcessImpl
                    (toCString(cmdarray[0]),
                            argBlock, args.length,
                            envBlock, envc[0],
                            toCString(dir),
                            std_fds,
                            forceNullOutputStream,
                            redirectErrorStream);

            // このコードは、リダイレクト先がパイプである場合に、標準入出力（stdin、stdout、stderr）のファイルディスクリプタ（FD）を適切に設定し、
            // リダイレクトされたストリームが次のプロセスに接続されるようにすることを目的としています。
            // fdAccess.set() は、ファイルディスクリプタを直接操作するために使用されます。これにより、標準入出力のリダイレクトが他のプロセスにパイプで接続されるようになります
            if (redirects != null) {
                // Copy the fd's if they are to be redirected to another process
                if (std_fds[0] >= 0 &&
                        redirects[0] instanceof ProcessBuilder.RedirectPipeImpl) {
                    fdAccess.set(((ProcessBuilder.RedirectPipeImpl) redirects[0]).getFd(), std_fds[0]);
                }
                if (std_fds[1] >= 0 &&
                        redirects[1] instanceof ProcessBuilder.RedirectPipeImpl) {
                    fdAccess.set(((ProcessBuilder.RedirectPipeImpl) redirects[1]).getFd(), std_fds[1]);
                }
                if (std_fds[2] >= 0 &&
                        redirects[2] instanceof ProcessBuilder.RedirectPipeImpl) {
                    fdAccess.set(((ProcessBuilder.RedirectPipeImpl) redirects[2]).getFd(), std_fds[2]);
                }
            }
            return p;
        } finally {
            // In theory, close() can throw IOException
            // (although it is rather unlikely to happen here)
            try { if (f0 != null) f0.close(); }
            finally {
                try { if (f1 != null) f1.close(); }
                finally { if (f2 != null) f2.close(); }
            }
        }
    }


    /**
     * プロセスを作成します。モードフラグに応じて、以下のいずれかのメカニズムで実行されます
     * <pre>
     *   1 - fork(2) and exec(2)
     *   2 - posix_spawn(3P)
     *   3 - vfork(2) and exec(2)
     * </pre>
     * @param fds 3つのファイルディスクリプタの配列。インデックス 0, 1, 2 は、それぞれ標準入力、標準出力、標準エラーに対応します。
     *            入力時、-1 の値は子プロセスと親プロセスを接続するためにパイプを作成することを意味します。
     *            出力時、-1 でない値は作成されたパイプに対応する親プロセスのパイプ fd です。
     *            この配列の要素は、入力時にのみ -1 であり、出力時には -1 でない場合に限り、-1 として設定されます。
     * @return the pid of the subprocess
     */
    private native int forkAndExec(int mode, byte[] helperpath,
                                   byte[] prog,
                                   byte[] argBlock, int argc,
                                   byte[] envBlock, int envc,
                                   byte[] dir,
                                   int[] fds,
                                   boolean redirectErrorStream)
        throws IOException;

    @SuppressWarnings("removal")
    private ProcessImpl(final byte[] prog,
                        final byte[] argBlock, final int argc,
                        final byte[] envBlock, final int envc,
                        final byte[] dir,
                        final int[] fds,
                        final boolean forceNullOutputStream,
                        final boolean redirectErrorStream)
            throws IOException {

        pid = forkAndExec(launchMechanism.ordinal() + 1,
                          helperpath,
                          prog,
                          argBlock, argc,
                          envBlock, envc,
                          dir,
                          fds,
                          redirectErrorStream);
        processHandle = ProcessHandleImpl.getInternal(pid);

        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                // initStreams メソッドは、標準入力、標準出力、標準エラーのストリームの初期化を行います
                initStreams(fds, forceNullOutputStream);
                return null;
            });
        } catch (PrivilegedActionException ex) {
            throw (IOException) ex.getCause();
        }
    }

    static FileDescriptor newFileDescriptor(int fd) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        fdAccess.set(fileDescriptor, fd);
        return fileDescriptor;
    }

    /**
     * ファイルディスクリプタからストリームを初期化します
     * @param fds 標準入力、標準出力、標準エラーのファイルディスクリプタの配列
     * @param forceNullOutputStream stdoutが次のプロセスに向けてリダイレクトされている場合はtrue。stdoutストリームはnull出力ストリームであるべきです。
     * @throws IOException
     */
    void initStreams(int[] fds, boolean forceNullOutputStream) throws IOException {
        switch (platform) {
            case LINUX:
            case BSD:
                // fds[0], fds[1], fds[2]は、それぞれ標準入力、標準出力、標準エラーに対応するファイルディスクリプタです。
                // -1が指定されている場合は、パイプを使用せずに標準の出力を行わない、または読み取らない設定が行われます
                stdin = (fds[0] == -1) ?
                        ProcessBuilder.NullOutputStream.INSTANCE :
                        new ProcessPipeOutputStream(fds[0]);

                stdout = (fds[1] == -1 || forceNullOutputStream) ?
                         ProcessBuilder.NullInputStream.INSTANCE :
                         new ProcessPipeInputStream(fds[1]);

                stderr = (fds[2] == -1) ?
                         ProcessBuilder.NullInputStream.INSTANCE :
                         new ProcessPipeInputStream(fds[2]);

                // ProcessHandleImpl.completionでプロセスの終了を監視し、プロセスが終了した際に適切な処理を行います。
                // これにより、プロセスが終了した後、各ストリームの状態を更新したり、リソースを解放したりします
                ProcessHandleImpl.completion(pid, true).handle((exitcode, throwable) -> {
                    synchronized (this) {
                        this.exitcode = (exitcode == null) ? -1 : exitcode.intValue();
                        this.hasExited = true;
                        this.notifyAll();
                    }

                    if (stdout instanceof ProcessPipeInputStream)
                        ((ProcessPipeInputStream) stdout).processExited();

                    if (stderr instanceof ProcessPipeInputStream)
                        ((ProcessPipeInputStream) stderr).processExited();

                    if (stdin instanceof ProcessPipeOutputStream)
                        ((ProcessPipeOutputStream) stdin).processExited();

                    return null;
                });
                break;

            case AIX:
                stdin = (fds[0] == -1) ?
                        ProcessBuilder.NullOutputStream.INSTANCE :
                        new ProcessPipeOutputStream(fds[0]);

                stdout = (fds[1] == -1 || forceNullOutputStream) ?
                         ProcessBuilder.NullInputStream.INSTANCE :
                         new DeferredCloseProcessPipeInputStream(fds[1]);

                stderr = (fds[2] == -1) ?
                         ProcessBuilder.NullInputStream.INSTANCE :
                         new DeferredCloseProcessPipeInputStream(fds[2]);

                ProcessHandleImpl.completion(pid, true).handle((exitcode, throwable) -> {
                    synchronized (this) {
                        this.exitcode = (exitcode == null) ? -1 : exitcode.intValue();
                        this.hasExited = true;
                        this.notifyAll();
                    }

                    if (stdout instanceof DeferredCloseProcessPipeInputStream)
                        ((DeferredCloseProcessPipeInputStream) stdout).processExited();

                    if (stderr instanceof DeferredCloseProcessPipeInputStream)
                        ((DeferredCloseProcessPipeInputStream) stderr).processExited();

                    if (stdin instanceof ProcessPipeOutputStream)
                        ((ProcessPipeOutputStream) stdin).processExited();

                    return null;
                });
                break;

            default: throw new AssertionError("Unsupported platform: " + platform);
        }
    }

    public OutputStream getOutputStream() {
        return stdin;
    }

    public InputStream getInputStream() {
        return stdout;
    }

    public InputStream getErrorStream() {
        return stderr;
    }

    public synchronized int waitFor() throws InterruptedException {
        while (!hasExited) {
            wait();
        }
        return exitcode;
    }

    @Override
    public synchronized boolean waitFor(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        long remainingNanos = unit.toNanos(timeout);    // throw NPE before other conditions
        if (hasExited) return true;
        if (timeout <= 0) return false;

        long deadline = System.nanoTime() + remainingNanos;
        do {
            TimeUnit.NANOSECONDS.timedWait(this, remainingNanos);
            if (hasExited) {
                return true;
            }
            remainingNanos = deadline - System.nanoTime();
        } while (remainingNanos > 0);
        return hasExited;
    }

    public synchronized int exitValue() {
        if (!hasExited) {
            throw new IllegalThreadStateException("process hasn't exited");
        }
        return exitcode;
    }

    private void destroy(boolean force) {
        switch (platform) {
            case LINUX:
            case BSD:
            case AIX:
                // pidが再利用されるリスクがあり、その結果、間違ったプロセスを終了させてしまう可能性があります！
                // したがって、実行中と思われるプロセスのみを終了させます。
                // このチェックを行っても、ここには避けられない競合状態がありますが、そのウィンドウは非常に小さく、
                // OSはpidをあまり早く再利用しないように努力しているため、これは非常に安全です。
                synchronized (this) {
                    if (!hasExited)
                        processHandle.destroyProcess(force);
                }
                try { stdin.close();  } catch (IOException ignored) {}
                try { stdout.close(); } catch (IOException ignored) {}
                try { stderr.close(); } catch (IOException ignored) {}
                break;

            default: throw new AssertionError("Unsupported platform: " + platform);
        }
    }

    @Override
    public CompletableFuture<Process> onExit() {
        return ProcessHandleImpl.completion(pid, false)
                .handleAsync((unusedExitStatus, unusedThrowable) -> {
                    boolean interrupted = false;
                    while (true) {
                        // 終了ステータスを設定する並行タスクが完了したことを確認する
                        try {
                            waitFor();
                            break;
                        } catch (InterruptedException ie) {
                            interrupted = true;
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return this;
                });
    }

    @Override
    public ProcessHandle toHandle() {
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("manageProcess"));
        }
        return processHandle;
    }

    @Override
    public boolean supportsNormalTermination() {
        return ProcessImpl.SUPPORTS_NORMAL_TERMINATION;
    }

    @Override
    public void destroy() {
        destroy(false);
    }

    @Override
    public Process destroyForcibly() {
        destroy(true);
        return this;
    }

    @Override
    public long pid() {
        return pid;
    }

    @Override
    public synchronized boolean isAlive() {
        return !hasExited;
    }

    /**
     * toStringメソッドは、プロセスのネイティブプロセスIDとプロセスの終了値からなる文字列を返します。
     *
     * @return オブジェクトの文字列表現
     */
    @Override
    public String toString() {
        return new StringBuilder("Process[pid=").append(pid)
                .append(", exitValue=").append(hasExited ? exitcode : "\"not exited\"")
                .append("]").toString();
    }

    private static native void init();

    static {
        init();
    }

    /**
     * この ProcessPipeInputStream クラスは、サブプロセスの出力を読み取る際の安全なバッファリングとストリーム管理を提供します。
     * プロセスが終了した後に入力ストリームのファイルディスクリプタを解放し、ユーザーが {@code close} メソッドを呼び出すまでストリームが閉じられないようにするための仕組みです。
     * これにより、OSのパイプバッファに残っているデータを保持しつつ、安全にストリームを閉じることができる。
     * <ul>
     * <li>プロセスが終了すると {@code processExited} メソッドが呼び出され、ストリームから読み込んだデータをバッファに保持しつつ、ストリームを閉じる
     * <li>close メソッドを呼び出すと、{@code closeLock} により、{@code processExited} との競合を防ぎ、ストリームが正しく閉じられる
     * </ul>
     * この仕組みの重要なポイントは、ストリームの閉じるタイミングと、OSパイプバッファに残っているデータを保持するタイミングの間の同期性です。
     * これにより、ストリームが適切にクリーンアップされ、出力のデータを安全に読み取ることができます。
     */
    private static class ProcessPipeInputStream extends BufferedInputStream {
        private final Object closeLock = new Object();

        ProcessPipeInputStream(int fd) {
            super(new PipeInputStream(newFileDescriptor(fd)));
        }
        // InputStream から読み込んだデータをバイト配列に変換します
        private static byte[] drainInputStream(InputStream in)
                throws IOException {
            int n = 0;
            int j;
            byte[] a = null;
            while ((j = in.available()) > 0) {
                a = (a == null) ? new byte[j] : Arrays.copyOf(a, n + j);
                n += in.read(a, n, j);
            }
            return (a == null || n == a.length) ? a : Arrays.copyOf(a, n);
        }

        /** このメソッドは、プロセスが終了した際に呼び出される
         * このメソッドにおける synchronized の使い方には2つの異なる目的があります。それぞれを説明します。
         * <ul>
         * <li><p>メソッド全体を同期 (synchronized void processExited())</p>
         * 最初の synchronized は、processExited() メソッド全体に対して適用されています。
         * この同期は、複数のスレッドが同時に processExited() メソッドを呼び出しても競合状態（レースコンディション）が発生しないようにするためのものです。
         * 例えば、同じプロセスに対して複数のスレッドが同時に processExited() を呼び出した場合、同時に this.in を変更したり操作したりすることがないように保護しています。
         *
         * <li><p>同期ブロック (synchronized (closeLock))</p>
         * 次に、closeLock オブジェクトを使った同期があります。これは、processExited() メソッド内で this.in というストリームを操作している箇所に対して、
         * さらに細かく同期を取るためのものです。this.in を操作する部分が複数のスレッドからアクセスされる可能性があるため、
         * その部分だけを厳密に同期するために使用されています。具体的には、ストリームを閉じる処理や、stragglers（余分なバイトデータ）の取り扱いに関して同期を取ることで、
         * データの不整合や不完全な状態を防ぎます。
         * </ul>
         */
        synchronized void processExited() {
            // 同期ブロックを用いて、アクセスの競合を防ぐ
            synchronized (closeLock) {
                try {
                    InputStream in = this.in;
                    // InputStream が閉じられているかどうかをチェックする
                    if (in != null) {
                        byte[] stragglers = drainInputStream(in);
                        in.close();
                        this.in = (stragglers == null) ?
                            ProcessBuilder.NullInputStream.INSTANCE :
                                // stragglers にデータが残っていた場合、新しい ByteArrayInputStream を作成し、stragglers をその中に保持する
                            new ByteArrayInputStream(stragglers);
                    }
                } catch (IOException ignored) {}
            }
        }

        @Override
        public void close() throws IOException {
            // BufferedInputStream#close() は他のメソッドとは異なり、同期されていません。
            // 同期をとることで、processExited() との競合を回避するのに役立ちます。
            synchronized (closeLock) {
                super.close();
            }
        }
    }

    /**
     * サブプロセスのパイプファイルディスクリプタ用のバッファ付き出力ストリームであり、
     * processExited フックを介してプロセスが終了した際に基礎となるファイルディスクリプタを回収できるようにします。
     */
    private static class ProcessPipeOutputStream extends BufferedOutputStream {
        ProcessPipeOutputStream(int fd) {
            super(new FileOutputStream(newFileDescriptor(fd)));
        }

        /** プロセスが終了した際にプロセスリーパー（プロセス回収）スレッドによって呼び出されます */
        synchronized void processExited() {
            OutputStream out = this.out;
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                    // IOExceptionが発生する理由は知らないが、もし発生した場合は、続行する以外にすることはない。
                }
                this.out = ProcessBuilder.NullOutputStream.INSTANCE;
            }
        }
    }

    /**
     * 実際のクローズ操作を、ストリーム上の最後の保留中のI/O操作が完了するまで遅延させることをサポートするFileInputStream。
     * これは{@code Solaris}で必要です。なぜなら、stdinとstdoutのストリームをdestroyメソッドで閉じる必要があり、そうすることで基盤となるファイルディスクリプタを回収できるからです。
     * しかし、そうすると現在そのストリームの読み取りでブロックされているスレッドが{@code IOException("Bad file number")}を受け取ることになり、これは従来の動作と互換性がありません。
     * クローズを遅延させることで、保留中の読み取り操作が以前のように{@code -1（EOF）}を返すようにします。
     *
     * closeDeferred メソッドは、ストリームの遅延クローズを実現する中心的な部分です。
     * 具体的には、ストリームがクローズされるべきかどうかを遅延させ、他の操作が終了するまでクローズを延期します。
     *
     * <p>遅延クローズの実現方法：</p>
     * <ul>
     *      <li>{@code useCount} によってストリームの使用状況を追跡し、使用中のストリームはクローズせず、使用が終わるのを待つ。
     *      <li>{@code closeDeferred} メソッドで、使用中のストリームがクローズされるのを延期し、実際のクローズは後で行う。
     *      <li>{@code close} メソッドで最終的にストリームをクローズし、必要なクリーンアップを行う。
     * </ul>
     * このようにして、ストリームが使用されている間はクローズが行われず、全ての I/O 操作が終了したタイミングでクローズされるようになっています。
     */
    private static class DeferredCloseInputStream extends PipeInputStream {
        DeferredCloseInputStream(FileDescriptor fd) {
            super(fd);
        }

        private Object lock = new Object();     // For the following fields
        private boolean closePending = false;
        private int useCount = 0;
        private InputStream streamToClose;

        // raise() はストリームを使用していることを示し、useCount を増加させます。
        private void raise() {
            synchronized (lock) {
                useCount++;
            }
        }
        // lower() はストリームの使用が終了したことを示し、useCount を減少させます。
        private void lower() throws IOException {
            synchronized (lock) {
                useCount--;
                if (useCount == 0 && closePending) {
                    streamToClose.close();
                }
            }
        }

        private void closeDeferred(InputStream stc) throws IOException {
            synchronized (lock) {
                if (useCount == 0) {
                    stc.close();
                } else {
                    // useCount が 0 でない場合：他のスレッドや操作がこのストリームを使っているため、クローズを延期します。
                    // closePending フラグを true に設定し、クローズ対象のストリームを streamToClose に設定します。これにより、実際のクローズ操作が遅延されます
                    closePending = true;
                    streamToClose = stc;
                }
            }
        }

        public void close() throws IOException {
            synchronized (lock) {
                useCount = 0;
                closePending = false;
            }
            super.close();
        }

        public int read() throws IOException {
            raise();
            try {
                return super.read();
            } finally {
                lower();
            }
        }

        public int read(byte[] b) throws IOException {
            raise();
            try {
                return super.read(b);
            } finally {
                lower();
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            raise();
            try {
                return super.read(b, off, len);
            } finally {
                lower();
            }
        }

        public long skip(long n) throws IOException {
            raise();
            try {
                return super.skip(n);
            } finally {
                lower();
            }
        }

        public int available() throws IOException {
            raise();
            try {
                return super.available();
            } finally {
                lower();
            }
        }
    }

    /**
     * プロセスが終了したときに、processExited フックを介して基盤となるファイル記述子を回収できるようにする、
     * サブプロセスのパイプファイル記述子用のバッファ付き入力ストリームです。これは難しいことです、なぜならユーザーレベルの InputStream は、
     * ユーザーが close() を呼び出すまで閉じたくないからです。
     * また、OSのパイプバッファに残っているバッファデータを引き続き読み取れるようにする必要があります。
     * {@code AIX}ではこれが特に難しいです、なぜなら close() システムコールは、別のスレッドが同時に同じファイル記述子でファイル操作（例えば、read()）にブロックされている場合にブロックするからです。
     * したがって、Linux と BSD で使用される '{@code ProcessPipeInputStream}' のアプローチと、Solaris で使用される '{@code DeferredCloseInputStream}' のアプローチを組み合わせます。
     * これは、ファイル記述子での潜在的にブロックする操作のすべてが、それが実行される前にカウンターをインクリメントし、
     * 終了後にそれをデクリメントすることを意味します。close() 操作は、保留中の操作がない場合にのみ実行されます。
     * それ以外の場合、最後の保留中の操作が終了した後に遅延されます。
     *
     */
    private static class DeferredCloseProcessPipeInputStream
        extends BufferedInputStream {

        private final Object closeLock = new Object();
        private int useCount = 0;
        private boolean closePending = false;

        DeferredCloseProcessPipeInputStream(int fd) {
            super(new PipeInputStream(newFileDescriptor(fd)));
        }

        private InputStream drainInputStream(InputStream in)
                throws IOException {
            int n = 0;
            int j;
            byte[] a = null;
            synchronized (closeLock) {
                if (buf == null) // asynchronous close()?
                    return null; // discard
                j = in.available();
            }
            while (j > 0) {
                a = (a == null) ? new byte[j] : Arrays.copyOf(a, n + j);
                synchronized (closeLock) {
                    if (buf == null) // asynchronous close()?
                        return null; // discard
                    n += in.read(a, n, j);
                    j = in.available();
                }
            }
            return (a == null) ?
                    ProcessBuilder.NullInputStream.INSTANCE :
                    new ByteArrayInputStream(n == a.length ? a : Arrays.copyOf(a, n));
        }

        /** Called by the process reaper thread when the process exits. */
        synchronized void processExited() {
            try {
                InputStream in = this.in;
                if (in != null) {
                    InputStream stragglers = drainInputStream(in);
                    in.close();
                    this.in = stragglers;
                }
            } catch (IOException ignored) { }
        }

        private void raise() {
            synchronized (closeLock) {
                useCount++;
            }
        }

        private void lower() throws IOException {
            synchronized (closeLock) {
                useCount--;
                if (useCount == 0 && closePending) {
                    closePending = false;
                    super.close();
                }
            }
        }

        @Override
        public int read() throws IOException {
            raise();
            try {
                return super.read();
            } finally {
                lower();
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            raise();
            try {
                return super.read(b);
            } finally {
                lower();
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            raise();
            try {
                return super.read(b, off, len);
            } finally {
                lower();
            }
        }

        @Override
        public long skip(long n) throws IOException {
            raise();
            try {
                return super.skip(n);
            } finally {
                lower();
            }
        }

        @Override
        public int available() throws IOException {
            raise();
            try {
                return super.available();
            } finally {
                lower();
            }
        }

        @Override
        public void close() throws IOException {
            // BufferedInputStream#close() is not synchronized unlike most other
            // methods. Synchronizing helps avoid racing with drainInputStream().
            synchronized (closeLock) {
                if (useCount == 0) {
                    super.close();
                }
                else {
                    closePending = true;
                }
            }
        }
    }
}
