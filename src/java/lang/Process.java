/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.util.StaticProperty;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Process は、ProcessBuilder.start() と Runtime.exec() によって開始されたネイティブプロセスを制御するためのクラスです。
 * このクラスには、プロセスからの入力を行う、プロセスへの出力を行う、プロセスの終了を待機する、プロセスの終了ステータスを確認する、
 * プロセスを終了（強制終了）するためのメソッドが提供されています。{@code ProcessBuilder.start()} と {@code Runtime.exec()} メソッドは、ネイティブプロセスを作成し、
 * そのプロセスを制御したり情報を取得するために使用できる {@code Process} のサブクラスのインスタンスを返します。
 *
 * プロセスを作成するメソッドは、ネイティブウィンドウプロセス、デーモンプロセス、Microsoft Windows の Win16/DOS プロセス、
 * シェルスクリプトなど、特定のネイティブプラットフォーム上ではうまく動作しない場合があります。
 *
 * デフォルトでは、作成されたプロセスは独自の端末やコンソールを持ちません。
 * すべての標準 I/O（つまり stdin、stdout、stderr）の操作は親プロセスにリダイレクトされ、
 * {@code getOutputStream(), getInputStream(), getErrorStream() }メソッドを使用してこれらのストリームにアクセスできます。
 * 文字や行単位での I/O ストリームは、{@code outputWriter(), outputWriter(Charset), inputReader(), inputReader(Charset),
 * errorReader(), errorReader(Charset)} メソッドを使用して書き込みや読み取りができます。
 * 親プロセスはこれらのストリームを使って、プロセスへの入力を行い、出力を取得します。
 * 一部のネイティブプラットフォームでは、標準入力や標準出力のストリームに対して制限されたバッファサイズしか提供されていないため、
 * 入力ストリームにすぐに書き込まない、または出力ストリームを読み取らないと、プロセスがブロックしたり、デッドロックする原因になることがあります。
 *
 * 必要に応じて、ProcessBuilder クラスのメソッドを使用してプロセスの I/O をリダイレクトすることもできます。
 * プロセスが終了しても、Process オブジェクトに対する参照がなくなるだけではプロセスは終了しません。プロセスは非同期に実行を続けます。
 * Process オブジェクトで表されるプロセスが、プロセスを所有する Java プロセスとは非同期または並行して実行される必要はありません。
 * 1.5 以降、{@code ProcessBuilder.start()} がプロセスを作成するための推奨方法です。
 *
 * Process のサブクラスは、onExit() と toHandle() メソッドをオーバーライドして、プロセス ID、プロセスに関する情報、直接の子プロセス、
 * 直接の子プロセスおよびその子孫を含む完全なプロセス機能を提供する必要があります。基盤となる {@code Process} または {@code ProcessHandle} に委譲する方法が、通常、最も簡単で効率的です。
 *
 * @since   1.0
 */
public abstract class Process {

    // このプロセスのために作成されたリーダーとライターは、繰り返し呼び出しても同じオブジェクトが返されます。 すべての更新は、この Process オブジェクトを同期化して行う必要があります。
    private BufferedWriter outputWriter;
    private Charset outputCharset;
    private BufferedReader inputReader;
    private Charset inputCharset;
    private BufferedReader errorReader;
    private Charset errorCharset;

    /**
     * Default constructor for Process.
     */
    public Process() {}

    /**
     * このメソッドは、プロセスの通常の入力に接続された出力ストリームを返します。
     * ストリームへの出力は、この Process オブジェクトによって表されるプロセスの標準入力にパイプされます。
     * もしプロセスの標準入力が ProcessBuilder.redirectInput を使ってリダイレクトされている場合、このメソッドは null の出力ストリームを返します。
     *
     * @apiNote
     * getOutputStream() と outputWriter() または outputWriter(Charset) の両方に書き込む場合、
     * BufferedWriter.flush() を呼び出してから出力ストリームへの書き込みを行うことを推奨します。
     *
     * @implNote
     * 返される出力ストリームはバッファリングされていると良いです。
     *
     * @return プロセスの通常の入力に接続された出力ストリーム
     */
    public abstract OutputStream getOutputStream();

    /**
     * このメソッドは、プロセスの通常の出力に接続された入力ストリームを返します。
     * ストリームは、この Process オブジェクトによって表されるプロセスの標準出力からパイプされたデータを取得します。
     * もしプロセスの標準出力が ProcessBuilder.redirectOutput を使ってリダイレクトされている場合、このメソッドは null の入力ストリームを返します。
     * それ以外の場合、もしプロセスの標準エラーが ProcessBuilder.redirectErrorStream を使ってリダイレクトされている場合、
     * このメソッドによって返される入力ストリームは、プロセスの標準出力と標準エラーをマージしたデータを受け取ります。
     *
     * @apiNote
     * getInputStream() と inputReader を使用する際は慎重に扱ってください。BufferedReader は入力ストリームからのデータをバッファリングしている可能性があります。
     *
     * @implNote
     * 返される入力ストリームはバッファリングされていると良いです。
     *
     * @return プロセスの通常の出力に接続された入力ストリーム
     */
    public abstract InputStream getInputStream();

    /**
     * このメソッドは、プロセスのエラー出力に接続された入力ストリームを返します。
     * ストリームは、この Process オブジェクトによって表されるプロセスの標準エラー出力からパイプされたデータを取得します。
     * もしプロセスの標準エラーが ProcessBuilder.redirectError または ProcessBuilder.redirectErrorStream を使ってリダイレクトされている場合、
     * このメソッドは null の入力ストリームを返します。
     *
     * @apiNote
     * getInputStream と inputReader を使用する際は慎重に扱ってください。BufferedReader は入力ストリームからのデータをバッファリングしている可能性があります。
     *
     * @implNote
     * 返される入力ストリームはバッファリングされていると良いです。
     *
     * @return プロセスのエラー出力に接続された入力ストリーム
     */
    public abstract InputStream getErrorStream();

    /**
     * このメソッドは、プロセスの標準出力に接続された BufferedReader を返します。ネイティブエンコーディングの文字セットが使用され、標準出力から文字、行、またはストリーム行を読み取ります。
     *
     * このメソッドは、inputReader(Charset) を呼び出し、native.encoding システムプロパティで指定された文字セットを使用します。
     * もし native.encoding が有効な文字セット名でない場合、またはサポートされていない場合は、Charset.defaultCharset() が使用されます。
     *
     * @return a ネイティブエンコーディングがサポートされていればそれを使用した BufferedReader を返し、サポートされていない場合は Charset.defaultCharset() を使用します。
     * @since 17
     */
    public final BufferedReader inputReader() {
        return inputReader(CharsetHolder.nativeCharset());
    }

    /**
     *
     * このメソッドは、指定した Charset を使用してプロセスの標準出力に接続された BufferedReader を返します。
     * この BufferedReader は、標準出力から文字、行、またはストリームの行を読み取るために使用できます。
     * 文字は、getInputStream() からのバイトを読み取りデコードする InputStreamReader によって読み取られます。
     * バイトは指定された文字セットでデコードされ、無効な入力やマッピングできない文字列は、その文字セットのデフォルトの置換文字で置き換えられます。
     * BufferedReader は InputStreamReader から文字を読み取りバッファリングします。
     *
     * このメソッドが最初に呼び出されると、BufferedReader が作成されます。
     * 同じ Charset で再度呼び出すと、同じ BufferedReader が返されます。異なる Charset を使って再度呼び出すことはエラーです。
     *
     * もしプロセスの標準出力が ProcessBuilder.redirectOutput を使用してリダイレクトされている場合、
     * InputStreamReader は null の入力ストリームから読み取ることになります。
     * それ以外の場合、プロセスの標準エラーが ProcessBuilder.redirectErrorStream でリダイレクトされていると、
     * このメソッドによって返される入力リーダーは、標準出力と標準エラーの統合されたデータを受け取ります。
     *
     * @apiNote
     * getInputStream と inputReader(Charset) を両方使用すると予測できない動作が発生することがあります。これは、BufferedReader が入力ストリームから先読みするためです。
     * プロセスが終了し、標準入力がリダイレクトされていない場合、基になるストリームから利用可能なバイトの読み取りは最善を尽くして行われ、予測できない場合があります。
     *
     * @param charset バイトを文字にデコードするために使用する Charset
     * @return a {@code BufferedReader} 指定された Charset を使用してプロセスの標準出力に接続された BufferedReader
     * @throws NullPointerException if the {@code charset} is {@code null}
     * @throws IllegalStateException if called more than once with different charset arguments
     * @since 17
     */
    public final BufferedReader inputReader(Charset charset) {
        Objects.requireNonNull(charset, "charset");
        synchronized (this) {
            if (inputReader == null) {
                inputCharset = charset;
                inputReader = new BufferedReader(new InputStreamReader(getInputStream(), charset));
            } else {
                if (!inputCharset.equals(charset))
                    throw new IllegalStateException("BufferedReader was created with charset: " + inputCharset);
            }
            return inputReader;
        }
    }

    /**
     * このメソッドは、プロセスの標準エラーに接続された BufferedReader を返します。
     * 指定された文字セットを使用して、標準エラーから文字、行、またはストリームの行を読み取ります。
     * このメソッドは、errorReader(Charset) を使用して、native.encoding システムプロパティで指定された文字セットを利用します。
     * もし native.encoding が有効な文字セット名でないか、サポートされていない場合は、Charset.defaultCharset() が使用されます。
     *
     * @return native.encoding がサポートされていればそれを使用した BufferedReader、サポートされていなければ Charset.defaultCharset() を使用した BufferedReader
     * @since 17
     */
    public final BufferedReader errorReader() {
        return errorReader(CharsetHolder.nativeCharset());
    }

    /**
     * このメソッドは、プロセスの標準エラーに接続された BufferedReader を返します。
     * 指定された文字セットを使用して、標準エラーから文字、行、またはストリームの行を読み取ります。
     * 文字は、getErrorStream() メソッドで取得したバイトストリームを読み込み、デコードする InputStreamReader によって処理されます。
     * バイトは指定された文字セットを使用して文字にデコードされ、無効な入力やマッピングできない文字列は、文字セットのデフォルトの置換文字で置き換えられます。
     * BufferedReader は、InputStreamReader から読み込んだ文字をバッファリングします。
     *
     * このメソッドの最初の呼び出し時に BufferedReader が作成され、同じ文字セットで再度呼び出された場合には、同じ BufferedReader が返されます。
     * 異なる文字セットで再度呼び出すことはエラーとなります。
     *
     * もしプロセスの標準エラーが ProcessBuilder.redirectError または ProcessBuilder.redirectErrorStream によってリダイレクトされている場合、
     * InputStreamReader は null 入力ストリームから読み込むことになります。
     *
     * @apiNote
     * getErrorStream と errorReader(Charset) を両方使用すると、バッファリングされたリーダーがエラーストリームから先読みするため、予測できない動作を引き起こす可能性があります。
     * プロセスが終了し、標準エラーがリダイレクトされていない場合、基盤となるストリームからのバイトの読み取りは最善を尽くす形で行われ、予測できない場合があります。
     *
     * @param charset バイトを文字にデコードするために使用する文字セット
     * @return 指定された文字セットを使用したプロセスの標準エラー用の BufferedReader
     * @throws NullPointerException if the {@code charset} is {@code null}
     * @throws IllegalStateException 異なる文字セットで再度呼び出した場合
     * @since 17
     */
    public final BufferedReader errorReader(Charset charset) {
        Objects.requireNonNull(charset, "charset");
        synchronized (this) {
            if (errorReader == null) {
                errorCharset = charset;
                errorReader = new BufferedReader(new InputStreamReader(getErrorStream(), charset));
            } else {
                if (!errorCharset.equals(charset))
                    throw new IllegalStateException("BufferedReader was created with charset: " + errorCharset);
            }
            return errorReader;
        }
    }

    /**
     * このメソッドは、プロセスの標準入力に接続された BufferedWriter を返します。
     * ネイティブエンコーディングを使用して、文字出力ストリームにテキストを書き込みます。
     * この BufferedWriter は、単一の文字、配列、文字列を効率的に書き込むために文字をバッファリングします。
     * このメソッドは、outputWriter(Charset) を呼び出しており、
     * ネイティブエンコーディングを示す native.encoding システムプロパティで指定された文字セットを使用します。
     * もし native.encoding が有効な文字セット名でない場合やサポートされていない場合は、Charset.defaultCharset() が使用されます。
     *
     * @return ネイティブエンコーディングシステムプロパティで指定された文字セットを使用して、プロセスの標準入力に接続された BufferedWriter
     * @since 17
     */
    public final BufferedWriter outputWriter() {
        return outputWriter(CharsetHolder.nativeCharset());
    }

    /**
     * このメソッドは、指定された Charset を使用してプロセスの標準入力に接続された BufferedWriter を返します。
     * 文字出力ストリームにテキストを書き込む際に、文字をバッファリングして効率的に単一の文字、配列、および文字列を書き込みます。
     * 書き込まれた文字は、OutputStreamWriter を使用してバイトにエンコードされ、指定された Charset でエンコードされたバイトは、
     * このプロセスの標準入力に書き込まれます。もし無効な入力やマッピング不可能な文字列が含まれている場合、指定された文字セットのデフォルトの置換文字で置き換えられます。
     *
     * このメソッドが最初に呼び出されると、BufferedWriter が作成され、同じ Charset で再度呼び出すと、同じ BufferedWriter が返されます。
     * 異なる Charset を使用して再度呼び出すことはエラーとなります。
     *
     * もしプロセスの標準入力が ProcessBuilder.redirectInput を使用してリダイレクトされている場合、OutputStreamWriter は null の出力ストリームに書き込みます。
     *
     * @apiNote
     * BufferedWriter は文字、文字配列、文字列を書き込むために使用されます。
     * BufferedWriter を PrintWriter でラップすると、プリミティブやオブジェクトの効率的なバッファリングとフォーマット、および行末での自動フラッシュ機能が提供されます。
     * BufferedWriter.flush() メソッドを呼び出すことで、プロセスへのバッファリングされた出力をフラッシュすることができます。
     * getOutputStream() と outputWriter() または outputWriter(Charset) に書き込む際には、BufferedWriter.flush() を呼び出してから出力ストリームに書き込むことが推奨されます。
     *
     * @param charset 文字をバイトにエンコードするために使用する Charset
     * @return Charset を使用して、プロセスの標準入力に接続された BufferedWriter
     * @throws NullPointerException if the {@code charset} is {@code null}
     * @throws IllegalStateException 異なる charset 引数で再度呼び出した場合
     * @since 17
     */
    public final BufferedWriter outputWriter(Charset charset) {
        Objects.requireNonNull(charset, "charset");
        synchronized (this) {
            if (outputWriter == null) {
                outputCharset = charset;
                outputWriter = new BufferedWriter(new OutputStreamWriter(getOutputStream(), charset));
            } else {
                if (!outputCharset.equals(charset))
                    throw new IllegalStateException("BufferedWriter was created with charset: " + outputCharset);
            }
            return outputWriter;
        }
    }

    /**
     * このメソッドは、必要に応じて現在のスレッドを待機させ、Process オブジェクトで表されるプロセスが終了するのを待ちます。
     * プロセスがすでに終了している場合、このメソッドはすぐに戻ります。プロセスがまだ終了していない場合、呼び出し元のスレッドはプロセスが終了するまでブロックされます。
     *
     * @return プロセスが終了した際の終了コード（終了値）。慣例として、終了コード 0 は正常終了を示します。
     * @throws InterruptedException 現在のスレッドが他のスレッドによって割り込まれると、待機は終了し、InterruptedException がスローされます。
     */
    public abstract int waitFor() throws InterruptedException;

    /**
     * このメソッドは、指定された待機時間が経過するか、または Process オブジェクトで表されるプロセスが終了するまで、現在のスレッドを待機させます。
     * プロセスがすでに終了している場合、このメソッドは即座に戻り、true を返します。
     * プロセスが終了しておらず、タイムアウト値がゼロ以下である場合、このメソッドは即座に戻り、false を返します。
     * このメソッドのデフォルト実装は、exitValue をポーリングしてプロセスが終了したかどうかを確認します。具体的な実装では、より効率的な実装でこのメソッドをオーバーライドすることが推奨されます。
     *
     * @param timeout 最大待機時間
     * @param unit timeout 引数の時間単位
     * @return プロセスが終了した場合は true を返し、プロセスが終了する前に待機時間が経過した場合は false を返します。
     * @throws InterruptedException  現在のスレッドが待機中に割り込まれた場合にスローされます。
     * @throws NullPointerException unit が null の場合にスローされます。
     * @since 1.8
     */
    public boolean waitFor(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        long remainingNanos = unit.toNanos(timeout); // throw NPE before other conditions
        if (hasExited())
            return true;
        if (timeout <= 0)
            return false;

        long deadline = System.nanoTime() + remainingNanos;
        do {
            Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(remainingNanos) + 1, 100));
            if (hasExited())
                return true;
            remainingNanos = deadline - System.nanoTime();
        } while (remainingNanos > 0);

        return false;
    }

    /**
     * このメソッドは、プロセスの終了値を返します。
     *
     * @return プロセスの終了値。慣習として、値 0 は正常終了を示します。
     * @throws IllegalThreadStateException Process オブジェクトで表されるプロセスがまだ終了していない場合にスローされます。
     */
    public abstract int exitValue();

    /**
     * このメソッドは、プロセスを終了させます。プロセスが正常に終了するかどうかは実装に依存します。
     * 強制的なプロセス終了は、プロセスの即時終了として定義されており、正常終了はプロセスがクリーンに終了することを許可します。プロセスが既に終了している場合、何も行われません。
     * onExit からの CompletableFuture は、プロセスが終了した時点で完了します。
     */
    public abstract void destroy();

    /**
     * このメソッドは、プロセスを強制的に終了させます。この Process オブジェクトが表すプロセスは強制終了されます。
     * 強制的なプロセス終了は、プロセスの即時終了を意味し、正常終了はプロセスがクリーンに終了することを許可します。プロセスが既に終了している場合、何も行われません。
     * onExit からの CompletableFuture は、プロセスが終了した時点で完了します。
     * {@code ProcessBuilder.start()} および {@code Runtime.exec()} メソッドで返される Process オブジェクトにこのメソッドを呼び出すと、プロセスが強制終了されます。
     *
     * @implSpec
     * このメソッドのデフォルトの実装は destroy を呼び出すものであり、そのためプロセスを強制的に終了させることはない可能性があります。
     * @implNote
     * このクラスの具象実装は、適切な実装でこのメソッドをオーバーライドすることを強く推奨します。
     * @apiNote
     * プロセスは直ちに終了しない場合があります。つまり、destroyForcibly() を呼び出した後でも、isAlive() がしばらくの間 true を返すことがあります。
     * このメソッドは、必要に応じて waitFor() とチェーンして使用できます。
     *
     * @return 強制終了されたプロセスを表す Process オブジェクト。
     * @since 1.8
     */
    public Process destroyForcibly() {
        destroy();
        return this;
    }

    /**
     * このメソッドは、destroy の実装がプロセスを正常に終了させる場合は true を返し、プロセスを強制的に即時終了させる場合は false を返します。
     * {@code ProcessBuilder.start()} および {@code Runtime.exec()} から返される Process オブジェクトにこのメソッドを呼び出すと、
     * プラットフォームの実装に応じて true または false が返されます。
     *
     * @implSpec
     * この実装は UnsupportedOperationException のインスタンスをスローし、それ以外の動作は行いません。
     *
     * @return
     * <p>true：destroy の実装がプロセスを正常終了させる場合。</p>
     * <p>false：destroy の実装がプロセスを強制的に終了させる場合。</p>
     * @throws UnsupportedOperationException Process の実装がこの操作をサポートしていない場合にスローされます。
     * @since 9
     */
    public boolean supportsNormalTermination() {
        throw new UnsupportedOperationException(this.getClass()
                + ".supportsNormalTermination() not supported" );
    }

    /**
     * このメソッドは、Process オブジェクトで表されるプロセスが生存しているかどうかをテストします。
     *
     * @return true：プロセスが終了していない場合（プロセスは生存している）。
     * @since 1.8
     */
    public boolean isAlive() {
        return !hasExited();
    }

    /**
     * これは、waitFor(long, TimeUnit) のデフォルト実装から呼び出されます。このメソッドは、exitValue() をポーリングしてプロセスの終了状態を確認するように指定されています。
     */
    private boolean hasExited() {
        try {
            exitValue();
            return true;
        } catch (IllegalThreadStateException e) {
            return false;
        }
    }

    /**
     * このメソッドは、プロセスに対してオペレーティングシステムが割り当てたネイティブプロセスID（PID）を返します。
     *
     * @implSpec
     * このメソッドの実装は、次のようにプロセスIDを返すべきです: toHandle().pid()
     *
     * @return プロセスのネイティブプロセスID
     * @throws UnsupportedOperationException プロセス実装がこの操作をサポートしていない場合にスローされます。
     * @since 9
     */
    public long pid() {
        return toHandle().pid();
    }

    /**
     * このメソッドは、プロセスの終了を待機するための CompletableFuture<Process> を返します。
     * この CompletableFuture を使用すると、プロセス終了時に同期的または非同期的に実行される依存関数やアクションをトリガーできます。
     * プロセスが終了すると、CompletableFuture は終了ステータスに関係なく完了します。
     *
     * onExit().get() を呼び出すことでプロセスの終了を待機し、Process オブジェクトを返します。
     * この CompletableFuture は、プロセスが終了したかどうかをチェックするために使ったり、プロセスが終了するまで待機したりすることができます。
     * CompletableFuture をキャンセルしても、プロセスには影響しません。
     *
     * ProcessBuilder.start() から返されたプロセスは、デフォルトの実装をオーバーライドしてプロセス終了を待つ効率的なメカニズムを提供します。
     *
     * @apiNote
     * onExit は waitFor の代替手段で、追加の並行性を提供し、プロセスの結果に便利にアクセスできるようにします。
     * ラムダ式を使ってプロセスの実行結果を評価することもできます。結果を使用する前に他の処理を行う場合、onExit は現在のスレッドを解放し、
     * 値が必要なときにのみブロックする便利なメカニズムです。
     * <pre> {@code   Process p = new ProcessBuilder("cmp", "f1", "f2").start();
     *    Future<Boolean> identical = p.onExit().thenApply(p1 -> p1.exitValue() == 0);
     *    ...
     *    if (identical.get()) { ... }
     * }</pre>
     *
     * @implSpec
     * この実装は、waitFor() を別スレッドで繰り返し実行し、正常に終了するまで待機します。
     * waitFor の実行中にスレッドが中断された場合、スレッドの中断状態は保持されます。
     * waitFor() が正常に終了すると、CompletableFuture はプロセスの終了ステータスに関係なく完了します。
     * 多数のプロセスを並行して待機する場合、この実装はスレッドスタックに多くのメモリを消費する可能性があります。
     * 外部の実装は、このメソッドをオーバーライドしてより効率的な実装を提供するべきです。例えば、基盤となるプロセスに委譲する場合、次のように実行できます。
     * <pre>{@code
     *    public CompletableFuture<Process> onExit() {
     *       return delegate.onExit().thenApply(p -> this);
     *    }
     * }</pre>
     * @apiNote
     * The process may be observed to have terminated with {@link #isAlive}
     * before the ComputableFuture is completed and dependent actions are invoked.
     *
     * @return a new {@code CompletableFuture<Process>} for the Process
     *
     * @since 9
     */
    public CompletableFuture<Process> onExit() {
        return CompletableFuture.supplyAsync(this::waitForInternal);
    }

    /**
     * プロセスが終了するのを待つために waitFor を呼び出します。スレッドが中断された場合、中断状態を戻す前にそれを記録します。
     * スレッドが waitFor() でブロックされる際に、ForkJoinPool が使用されている場合のワーカースレッド数を補償するために、ForkJoinPool.ManagedBlocker を使用します。
     *
     * @return the Process
     */
    private Process waitForInternal() {
        boolean interrupted = false;
        while (true) {
            try {
                ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
                    @Override
                    public boolean block() throws InterruptedException {
                        waitFor();
                        return true;
                    }

                    @Override
                    public boolean isReleasable() {
                        return !isAlive();
                    }
                });
                break;
            } catch (InterruptedException x) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    /**
     * プロセスのための ProcessHandle を返します。{@code ProcessBuilder.start()} および {@code Runtime.exec()} によって返される Process オブジェクトは、
     * toHandle を {@code ProcessHandle.of(pid)} と同等に実装しており、これには SecurityManager と RuntimePermission("manageProcess") の確認が含まれます。
     *
     * @implSpec
     * この実装は UnsupportedOperationException をスローし、他のアクションは行いません。
     * サブクラスは、このメソッドをオーバーライドしてプロセスのための ProcessHandle を提供すべきです。
     * pid、info、children、および descendants メソッドは、オーバーライドされない限り、ProcessHandle に対して動作します。
     *
     * @return プロセスのための ProcessHandle
     * @throws UnsupportedOperationException プロセスの実装がこの操作をサポートしていない場合
     * @throws SecurityException セキュリティマネージャがインストールされており、RuntimePermission("manageProcess") を拒否する場合
     * @since 9
     */
    public ProcessHandle toHandle() {
        throw new UnsupportedOperationException(this.getClass()
                + ".toHandle() not supported");
    }

    /**
     * プロセスに関する情報のスナップショットを返します。
     * ProcessHandle.Info インスタンスは、利用可能な場合にプロセスに関する情報を返すアクセサメソッドを持っています。
     *
     * @implSpec
     * この実装は、プロセスに関する情報を {@code toHandle().info()} として返します。
     *
     * @return プロセスに関する情報のスナップショット（常に非 null）
     * @throws UnsupportedOperationException プロセスの実装がこの操作をサポートしていない場合
     * @since 9
     */
    public ProcessHandle.Info info() {
        return toHandle().info();
    }

    /**
     * プロセスの直接の子プロセスのスナップショットを返します。直接の子プロセスの親はこのプロセスです。通常、終了したプロセスには子プロセスがありません。
     * プロセスは非同期で作成され、終了します。プロセスが生存している保証はありませんので、注意してください。
     *
     * @implSpec
     * この実装は、直接の子プロセスを toHandle().children() として返します。
     *
     * @return プロセスの直接の子プロセスに対応する ProcessHandle の順序付きストリーム
     * @throws UnsupportedOperationException プロセスの実装がこの操作をサポートしていない場合
     * @throws SecurityException セキュリティマネージャがインストールされており、RuntimePermission("manageProcess") が拒否された場合
     * @since 9
     */
    public Stream<ProcessHandle> children() {
        return toHandle().children();
    }

    /**
     * プロセスの子孫のスナップショットを返します。
     * プロセスの子孫とは、そのプロセスの子プロセスと、さらにその子プロセスの子孫を再帰的に含みます。
     * 通常、終了したプロセスには子プロセスがありません。
     * プロセスは非同期で作成され、終了します。プロセスが生存している保証はありませんので、注意してください。
     *
     * @implSpec
     * この実装は、すべての子プロセスを toHandle().descendants() として返します。
     *
     * @return プロセスの子孫に対応する ProcessHandle の順序付きストリーム
     * @throws UnsupportedOperationException プロセスの実装がこの操作をサポートしていない場合
     * @throws SecurityException セキュリティマネージャがインストールされており、RuntimePermission("manageProcess") が拒否された場合
     * @since 9
     */
    public Stream<ProcessHandle> descendants() {
        return toHandle().descendants();
    }

    /**
     * シークの代わりにバイトを読み取ることでスキップするサブプロセスパイプ用の入力ストリームで、基盤となるパイプはシークをサポートしていません。
     */
    static class PipeInputStream extends FileInputStream {

        PipeInputStream(FileDescriptor fd) {
            super(fd);
        }

        @Override
        public long skip(long n) throws IOException {
            long remaining = n;
            int nr;

            if (n <= 0) {
                return 0;
            }

            int size = (int)Math.min(2048, remaining);
            byte[] skipBuffer = new byte[size];
            while (remaining > 0) {
                nr = read(skipBuffer, 0, (int)Math.min(size, remaining));
                if (nr < 0) {
                    break;
                }
                remaining -= nr;
            }

            return n - remaining;
        }
    }

    /**
     * ネイティブエンコーディングのCharsetの検索を遅延させるためのネストされたクラス
     * 具体的に次の2つの重要な点を指しています。
     * <ul>
     *     <li>
     *         <p>ネイティブエンコーディングのCharset検索の遅延</p>
     *         遅延とは、必要なときに実際に値を計算または取得する方法を指します。
     *         ここでは、「ネイティブエンコーディングの Charset を初期化時にすぐに取得するのではなく、遅れてから取得する」という意味です。
     *         遅延させる目的は、最初に CharsetHolder クラスが読み込まれる際にネイティブエンコーディングをすぐに調べてしまうのではなく、
     *         その Charset が実際に必要になったときに調べるという動作を意味します。
     *     </li>
     *     <li>
     *         <p>ネストされたクラス（CharsetHolder）</p>
     *         ネストされたクラスとは、クラスの中に別のクラスを定義することです。
     *         CharsetHolder は静的な内部クラス（nested static class）として定義されており、外部クラスとは独立して使用できるクラスです。
     *         CharsetHolder は他のクラスから独立して使用され、ネイティブエンコーディングを取得する機能を持っていますが、
     *         実際に Charset を取得する処理が実行されるのは、nativeCharset() メソッドが呼ばれたときです。
     *     </li>
     *     <li>
     *         <p>具体的な遅延の流れ</p>
     *         CharsetHolder クラス内で定義された静的イニシャライザ（static { ... }）ブロックは、クラスが最初にロードされる際に実行されます。
     *         このイニシャライザは、ネイティブエンコーディングの Charset を取得して、nativeCharset に保存します。
     *         しかし、クラスがロードされても nativeCharset を取得する動作はすぐには行われません。nativeCharset() メソッドを呼び出すと初めて、
     *         Charset の検索と設定が行われるわけです。これが「遅延」という部分です。
     *         この遅延によって、プログラムの起動時に不要な処理が行われないため、パフォーマンスを向上させることができます。
     *     </li>
     *     <li>
     *         <p>なぜ遅延させるのか？</p>
     *         遅延させる理由は、プログラムのパフォーマンスを最適化するためです。
     *         もし Charset をクラスがロードされた時点ですぐに取得してしまうと、その処理が必要ない場合でも行われてしまいます。
     *         たとえば、プログラムが nativeCharset() を一度も呼び出さない場合、Charset の検索は行われずに済みます。
     *         これにより、リソースを無駄に消費せず、必要なタイミングでのみ計算が行われます。
     *     </li>
     * </ul>
     */
    private static class CharsetHolder {
        private final static Charset nativeCharset;

        /*
         * この部分はクラスがロードされるときに最初に実行されるブロックです。
         * 静的イニシャライザは、クラスのインスタンス化が行われる前に一度だけ実行されます。
         */
        static {
            Charset cs;
            try {
                cs = Charset.forName(StaticProperty.nativeEncoding());
            } catch (UnsupportedCharsetException uce) {
                cs = Charset.defaultCharset();
            }
            nativeCharset = cs;
        }

        /*
         * このメソッドは静的メソッドで、CharsetHolder クラスがロードされた後に、設定された nativeCharset を返すために使用されます。
         * これにより、他のクラスやコードはこのメソッドを呼び出すことで、ネイティブエンコーディングに対応する Charset を取得することができます。
         */
        static Charset nativeCharset() {
            return nativeCharset;
        }
    }
}
