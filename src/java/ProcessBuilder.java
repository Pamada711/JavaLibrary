/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
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

package java;

import jdk.internal.event.ProcessStartEvent;
import sun.security.action.GetPropertyAction;

import java.io.*;
import java.util.*;

/**
 * このクラスはオペレーティングシステムのプロセスを作成するために使用されます。
 * 各 ProcessBuilder インスタンスは、プロセスの属性のコレクションを管理します。
 * start() メソッドは、これらの属性を持つ新しい Process インスタンスを作成します。
 * start() メソッドは同じインスタンスから繰り返し呼び出して、新しいサブプロセスを作成することができ、これらのサブプロセスは同一または関連する属性を持ちます。
 * startPipeline メソッドは、新しいプロセスのパイプラインを作成するために呼び出すことができ、各プロセスの出力を次のプロセスに直接送ります。
 * 各プロセスは、それぞれの ProcessBuilder の属性を持ちます。
 *
 * <p>各プロセスビルダーが管理するプロセスの属性:
 *
 * <ul>
 *
 * <li>コマンド：外部プログラムファイルとその引数を示す文字列のリスト。どの文字列が有効なオペレーティングシステムのコマンドを表すかはシステム依存です。
 * 例えば、通常は各概念的な引数がリストの要素となりますが、コマンドライン文字列を自分でトークン化することが期待されるオペレーティングシステムもあります。
 * そういったシステムでは、Javaの実装がコマンドを正確に2つの要素を含むように要求することがあります。
 *
 * <li>環境：システム依存の変数と値のマッピング。初期値は現在のプロセスの環境のコピーです（System.getenv()を参照）。
 *
 * <li>作業ディレクトリ：デフォルト値は現在のプロセスの作業ディレクトリで、通常はシステムプロパティ user.dir で指定されたディレクトリです。
 *
 * <li>標準入力のソース：デフォルトでは、サブプロセスはパイプから入力を読み取ります。
 * Javaコードは {@code Process.getOutputStream()} によって返される出力ストリームを介してこのパイプにアクセスできます。
 * しかし、標準入力は {@code redirectInput} を使用して他のソースにリダイレクトできます。この場合、Process.getOutputStream() は null の出力ストリームを返します。その場合：
 *
 * <ul>
 * <li>write メソッドは常に IOException をスローします
 * <li>close メソッドは何もしません
 * </ul>
 *
 * <li>標準出力と標準エラーの宛先：デフォルトでは、サブプロセスは標準出力と標準エラーをパイプに書き込みます。
 * Javaコードは {@code Process.getInputStream()} および {@code Process.getErrorStream()} によって返される入力ストリームを介してこれらのパイプにアクセスできます。
 * しかし、標準出力と標準エラーは {@code redirectOutput} と {@code redirectError} を使用して他の宛先にリダイレクトできます。
 * この場合、Process.getInputStream() および/または Process.getErrorStream() は null の入力ストリームを返します。その場合：
 *
 * <ul>
 * <li>read メソッドは常に -1 を返します
 * <li>available メソッドは常に 0 を返します
 * <li>close メソッドは何もしません
 * </ul>
 *
 * <li>redirectErrorStream プロパティ：最初は false で、つまりサブプロセスの標準出力とエラー出力は2つの別々のストリームに送られ、
 * Process.getInputStream() および Process.getErrorStream() メソッドを使用してアクセスできます。
 * この値が true に設定されると、次のようになります：
 *
 * <ul>
 * <li>標準エラーは標準出力と統合され、常に同じ宛先に送られます（これによりエラーメッセージと対応する出力を関連付けやすくなります）
 * <li>標準エラーと標準出力の共通の宛先は redirectOutput を使用してリダイレクトできます
 * <li>redirectError メソッドで設定されたリダイレクトはサブプロセスを作成する際に無視されます
 * <li>Process.getErrorStream() から返されるストリームは常に null の入力ストリームとなります
 * </ul>
 *
 * </ul>
 *
 * プロセスビルダーの属性を変更すると、そのオブジェクトの start() メソッドで後で開始されたプロセスに影響を与えますが、以前に開始されたプロセスやJavaプロセス自体には影響を与えません。
 * ほとんどのエラーチェックは start() メソッドによって実行されます。オブジェクトの状態を変更して start() が失敗するようにすることも可能です。
 * 例えば、command 属性を空のリストに設定しても、start() を呼び出さない限り例外は発生しません。
 *
 * このクラスは同期化されていないことに注意してください。複数のスレッドが ProcessBuilder インスタンスに同時にアクセスし、
 * そのうち少なくとも1つのスレッドが属性を構造的に変更する場合は、外部で同期化する必要があります。
 *
 * デフォルトの作業ディレクトリと環境を使用して新しいプロセスを開始するのは簡単です：
 * <pre> {@code
 * Process p = new ProcessBuilder("myCommand", "myArg").start();
 * }</pre>
 *
 * 次の例では、作業ディレクトリと環境を変更し、標準出力とエラーをログファイルに追加するようリダイレクトしています：
 *
 * <pre> {@code
 * ProcessBuilder pb =
 *   new ProcessBuilder("myCommand", "myArg1", "myArg2");
 * Map<String, String> env = pb.environment();
 * env.put("VAR1", "myValue");
 * env.remove("OTHERVAR");
 * env.put("VAR2", env.get("VAR1") + "suffix");
 * pb.directory(new File("myDir"));
 * File log = new File("log");
 * pb.redirectErrorStream(true);
 * pb.redirectOutput(Redirect.appendTo(log));
 * Process p = pb.start();
 * assert pb.redirectInput() == Redirect.PIPE;
 * assert pb.redirectOutput().file() == log;
 * assert p.getInputStream().read() == -1;
 * }</pre>
 *
 * 明示的に設定された環境変数を使用してプロセスを開始するには、まず Map.clear() を呼び出してから環境変数を追加してください。
 * 特に記載がない限り、このクラスのコンストラクタまたはメソッドに null 引数を渡すと、NullPointerException がスローされます。
 *
 * @author Martin Buchholz
 * @since 1.5
 */

public final class ProcessBuilder
{
    private List<String> command;
    private File directory;
    private Map<String,String> environment;
    private boolean redirectErrorStream;
    private Redirect[] redirects;

    /**
     * 指定されたオペレーティングシステムのプログラムと引数を使用してプロセスビルダーを構築します。このコンストラクタはコマンドリストのコピーを作成しません。
     * リストへの後続の更新はプロセスビルダーの状態に反映されます。コマンドが有効なオペレーティングシステムのコマンドに対応しているかどうかはチェックされません。
     *
     * @param  command プログラムとその引数を含むリスト
     */
    public ProcessBuilder(List<String> command) {
        if (command == null)
            throw new NullPointerException();
        this.command = command;
    }

    /**
     * 指定されたオペレーティングシステムのプログラムと引数を使用してプロセスビルダーを構築します。これは便利なコンストラクタで、
     * プロセスビルダーのコマンドをコマンド配列と同じ順序で同じ文字列を含む文字列リストに設定します。
     * コマンドが有効なオペレーティングシステムのコマンドに対応しているかどうかはチェックされません。
     *
     * @param command プログラムとその引数を含む文字列配列
     */
    public ProcessBuilder(String... command) {
        this.command = new ArrayList<>(command.length);
        for (String arg : command)
            this.command.add(arg);
    }

    /**
     * このプロセスビルダーのオペレーティングシステムのプログラムと引数を設定します。このメソッドはコマンドリストのコピーを作成しません。
     * リストの後続の更新は、このプロセスビルダーの状態に反映されます。コマンドが有効なオペレーティングシステムコマンドかどうかはチェックされません。
     *
     * @param  command プログラムとその引数を含むリスト
     * @return this process builder
     */
    public ProcessBuilder command(List<String> command) {
        if (command == null)
            throw new NullPointerException();
        this.command = command;
        return this;
    }

    /**
     * このプロセスビルダーのオペレーティングシステムのプログラムと引数を設定します。このメソッドはコマンドリストのコピーを作成しません。
     * リストの後続の更新は、プロセスビルダーの状態に反映されます。コマンドが有効なオペレーティングシステムのコマンドに対応しているかどうかはチェックされません。
     *
     * @param  command プログラムとその引数を含むリスト
     * @return this process builder
     */
    public ProcessBuilder command(String... command) {
        this.command = new ArrayList<>(command.length);
        for (String arg : command)
            this.command.add(arg);
        return this;
    }

    /**
     * このプロセスビルダーのオペレーティングシステムのプログラムと引数を返します。
     * 返されるリストはコピーではありません。リストの後続の更新は、このプロセスビルダーの状態に反映されます。
     *
     * @return このプロセスビルダーのプログラムとその引数
     */
    public List<String> command() {
        return command;
    }

    /**
     * このメソッドは、このプロセスビルダーの環境の文字列マップビューを返します。
     * プロセスビルダーが作成されると、環境は現在のプロセス環境のコピーで初期化されます（{@code System.getenv()}を参照）。
     * その後、このオブジェクトの{@code start()}メソッドによって開始されるサブプロセスは、このマップを自分たちの環境として使用します。
     *
     * 返されるオブジェクトは、通常のMap操作を使用して変更できます。これらの変更は、start()メソッドを介して開始されたサブプロセスに対しても表示されます。
     * 2つのProcessBuilderインスタンスは常に独立したプロセス環境を含んでいるため、
     * 返されたマップへの変更は他のProcessBuilderインスタンスや{@code System.getenv()}によって返される値には反映されません。
     *
     * システムが環境変数をサポートしていない場合、空のマップが返されます。
     * 返されるマップはnullキーまたは値を許可しません。nullキーまたは値の挿入や存在の確認を試みると、NullPointerExceptionがスローされます。
     * String型でないキーや値の存在確認を試みると、ClassCastExceptionがスローされます。
     * 返されるマップの動作はシステム依存です。システムによっては、環境変数の変更を許可しない場合や、特定の変数名や値を禁止する場合があります。
     * このため、マップを変更しようとすると、変更がオペレーティングシステムによって許可されていない場合、
     * UnsupportedOperationExceptionやIllegalArgumentExceptionがスローされることがあります。
     * 環境変数の名前と値の外部形式はシステム依存であるため、JavaのUnicode文字列との間に一対一の対応がない場合があります。
     * それでも、このマップは、Javaコードによって変更されない環境変数は、サブプロセス内で変更されないネイティブ表現を持つように実装されています。
     *
     * 返されるマップとそのコレクションビューは、{@code Object.equals}および{@code Object.hashCode}メソッドの一般的な契約に従わない場合があります。
     * 返されるマップは、通常すべてのプラットフォームでケースセンシティブです。
     * セキュリティマネージャが存在する場合、そのcheckPermissionメソッドは、RuntimePermission("getenv.*")の権限で呼び出されます。
     * これにより、SecurityExceptionがスローされる場合があります。
     * Javaサブプロセスに情報を渡す際には、環境変数よりもシステムプロパティが一般的に推奨されます。
     *
     * @return this process builder's environment
     *
     * @throws SecurityException
     *         セキュリティマネージャが存在し、そのcheckPermissionメソッドがプロセス環境へのアクセスを許可しない場合
     *
     * @see    Runtime#exec(String[],String[], File)
     * @see    System#getenv()
     */
    public Map<String,String> environment() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkPermission(new RuntimePermission("getenv.*"));

        if (environment == null)
            environment = ProcessEnvironment.environment();

        assert environment != null;

        return environment;
    }

    // Only for use by Runtime.exec(...envp...)
    ProcessBuilder environment(String[] envp) {
        assert environment == null;
        if (envp != null) {
            environment = ProcessEnvironment.emptyEnvironment(envp.length);
            assert environment != null;

            for (String envstring : envp) {
                // 1.5以前では、無効な環境文字列を子プロセスに盲目的に渡していました。
                // 例外をスローしたいのですが、古い壊れたコードとの互換性のため、スローしません。
                //このコメントは、Javaのバージョン1.5より前の挙動に関する説明です。
                // 以前は、無効な環境変数（envstrings）が子プロセスに渡されていましたが、それが意図しない動作を引き起こす可能性がありました。
                // 現在ではそのような動作を変更したいと考えていますが、古いコードとの互換性を保つために例外をスローせず、依然として無効な環境変数を渡している、ということを述べています。

                // 末尾の不要なデータをエラーや警告を出さずにに破棄する
                if (envstring.indexOf((int) '\u0000') != -1)
                    envstring = envstring.replaceFirst("\u0000.*", "");

                int eqlsign =
                    envstring.indexOf('=', ProcessEnvironment.MIN_NAME_LENGTH);
                // 必要な = が欠けている envstrings をエラーや警告を出さずに無視する。
                if (eqlsign != -1)
                    environment.put(envstring.substring(0,eqlsign),
                                    envstring.substring(eqlsign+1));
            }
        }
        return this;
    }

    /**
     * このプロセスビルダーの作業ディレクトリを返します。
     * このオブジェクトの start() メソッドによって後に開始されるサブプロセスは、これをその作業ディレクトリとして使用します。
     * 返される値は null である可能性があります。これは、現在の Java プロセスの作業ディレクトリ、
     * 通常はシステムプロパティ user.dir によって指定されたディレクトリを、子プロセスの作業ディレクトリとして使用することを意味します
     *
     * @return this process builder's working directory
     */
    public File directory() {
        return directory;
    }

    /**
     * このプロセスビルダーの作業ディレクトリを設定します。
     * このオブジェクトの start() メソッドによって後に開始されるサブプロセスは、
     * これをその作業ディレクトリとして使用します。引数は null である可能性があります。
     * これは、現在の Java プロセスの作業ディレクトリ、通常はシステムプロパティ user.dir によって指定されたディレクトリを、
     * 子プロセスの作業ディレクトリとして使用することを意味します。
     *
     * @param  directory the new working directory
     * @return this process builder
     */
    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }

    // ---------------- I/O Redirection ----------------

    /**
     * Implements a <a href="#redirect-output">null input stream</a>.
     * このクラスは InputStream を拡張し、何も読み取らない 入力ストリームを提供します。通常、入力ストリームが無効化された場合や、データを読み取る必要がない場合に使用されます。
     * {@code read()}: 常に -1 を返します。-1 は、入力ストリームの終わりを示す値です。つまり、データがない、または無効なストリームを意味します。
     * {@code available()}: 常に 0 を返します。データがないため、利用可能なバイト数はゼロです。
     */
    static class NullInputStream extends InputStream {
        static final NullInputStream INSTANCE = new NullInputStream();
        private NullInputStream() {}
        public int read()      { return -1; }
        public int available() { return 0; }
    }

    /**
     * Implements a <a href="#redirect-input">null output stream</a>.
     * このクラスは OutputStream を拡張し、何も書き込まない 出力ストリームを提供します。出力が無効化された場合や、データを書き込む必要がない場合に使用されます。
     * {@code write(int b)}: このメソッドは、書き込み操作を実行しようとすると IOException をスローします。
     * メッセージ「Stream closed」が含まれます。これは、この出力ストリームが無効であるため、書き込むことができないことを示します。
     * {@code Process} クラスで標準出力や標準エラー出力がリダイレクトされていない場合に、
     * {@code getInputStream()} や {@code getErrorStream()} メソッドで無効な出力ストリームを提供するために使用されます。
     * 例えば、標準エラー出力が無効にされている場合に、このクラスが使われます。
     */
    static class NullOutputStream extends OutputStream {
        static final NullOutputStream INSTANCE = new NullOutputStream();
        private NullOutputStream() {}
        public void write(int b) throws IOException {
            throw new IOException("Stream closed");
        }
    }

    /**
     * サブプロセスの入力元またはサブプロセスの出力先を表します。各 Redirect インスタンスは、次のいずれかです：
     *
     * <ul>
     * <li>特殊値 Redirect.PIPE:パイプを使った入出力
     * <li>特殊値 Redirect.INHERIT:現在のプロセスと同じ入出力
     * <li>特殊値 Redirect.DISCARD:出力を破棄するため、/dev/null や NUL に書き込む
     * <li>ファイルから読み取るためのリダイレクト（Redirect.from(File) の呼び出しによって作成される）
     * <li>ファイルに書き込むためのリダイレクト（Redirect.to(File) の呼び出しによって作成される）
     * <li>ファイルに追記するためのリダイレクト（Redirect.appendTo(File) の呼び出しによって作成される）
     * </ul>
     *
     * 上記の各カテゴリには、関連付けられた一意の Type があります。
     * サブプロセスの入出力リダイレクトの処理を管理する役割を持っています。リダイレクトとは、サブプロセスの標準入力、標準出力、または標準エラーをファイルやパイプなどに接続する処理です。
     * ファイルの読み書きや追記、破棄の動作を、オペレーティングシステムに応じて管理する役割を担っています。
     *
     * @since 1.7
     */
    public abstract static class Redirect {
        /**
         * NULL_FILE は、オペレーティングシステムにおける「nullデバイス」を示すファイルオブジェクトです。
         * Windowsでは "NUL"、Unix系システムでは "/dev/null" を指します。これは出力を破棄するためのファイルです
         */
        private static final File NULL_FILE = new File(
                (GetPropertyAction.privilegedGetProperty("os.name")
                        .startsWith("Windows") ? "NUL" : "/dev/null")
        );

        /**
         * The type of a {@link Redirect}.
         */
        public enum Type {
            /**
             * The type of {@link Redirect#PIPE Redirect.PIPE}.
             */
            PIPE,

            /**
             * The type of {@link Redirect#INHERIT Redirect.INHERIT}.
             */
            INHERIT,

            /**
             * The type of redirects returned from
             * {@link Redirect#from Redirect.from(File)}.
             */
            READ,

            /**
             * The type of redirects returned from
             * {@link Redirect#to Redirect.to(File)}.
             */
            WRITE,

            /**
             * The type of redirects returned from
             * {@link Redirect#appendTo Redirect.appendTo(File)}.
             */
            APPEND
        };

        /**
         * Returns the type of this {@code Redirect}.
         * @return the type of this {@code Redirect}
         */
        public abstract Type type();

        /**
         * サブプロセスの入出力が現在のJavaプロセスとパイプを介して接続されることを示します。
         * これは、サブプロセスの標準入出力におけるデフォルトの処理です。
         * 次のことは常に真であるでしょう：
         *  <pre> {@code
         * Redirect.PIPE.file() == null &&
         * Redirect.PIPE.type() == Redirect.Type.PIPE
         * }</pre>
         */
        public static final Redirect PIPE = new Redirect() {
                public ProcessBuilder.Redirect.Type type() { return ProcessBuilder.Redirect.Type.PIPE; }
                public String toString() { return type().toString(); }};

        /**
         * サブプロセスの入出力元または出力先が現在のプロセスと同じであることを示します。
         * これは、ほとんどのオペレーティングシステムのコマンドインタープリタ（シェル）の通常の動作です。
         * 次のことは常に真であるでしょう：
         *  <pre> {@code
         * Redirect.INHERIT.file() == null &&
         * Redirect.INHERIT.type() == Redirect.Type.INHERIT
         * }</pre>
         */
        public static final Redirect INHERIT = new Redirect() {
                public ProcessBuilder.Redirect.Type type() { return ProcessBuilder.Redirect.Type.INHERIT; }
                public String toString() { return type().toString(); }};


        /**
         * サブプロセスの出力が破棄されることを示します。
         * 一般的な実装では、出力をオペレーティングシステム固有の「nullファイル」に書き込むことで破棄します。
         * 次のことは常に真であるでしょう：
         * <pre> {@code
         * Redirect.DISCARD.file() is the filename appropriate for the operating system
         * and may be null &&
         * Redirect.DISCARD.type() == Redirect.Type.WRITE
         * }</pre>
         * @since 9
         */
        public static final Redirect DISCARD = new Redirect() {
                public ProcessBuilder.Redirect.Type type() { return ProcessBuilder.Redirect.Type.WRITE; }
                public String toString() { return type().toString(); }
                public File file() { return NULL_FILE; }
                boolean append() { return false; }
        };

        /**
         * このリダイレクトに関連付けられたファイルの入力元または出力先を返します。関連付けられたファイルがない場合は null を返します。
         *
         * @return このリダイレクトに関連付けられたファイル、またはそのようなファイルがない場合は null
         */
        public File file() { return null; }

        /**
         * 出力先がリダイレクトされるファイルである場合、出力がファイルの末尾に書き込まれるかどうかを示します。
         */
        boolean append() {
            throw new UnsupportedOperationException();
        }

        /**
         * 指定されたファイルから読み取るためのリダイレクトを返します。
         * 次のことは常に真であるでしょう：
         *  <pre> {@code
         * Redirect.from(file).file() == file &&
         * Redirect.from(file).type() == Redirect.Type.READ
         * }</pre>
         *
         * @param file このリダイレクトの対象となるファイル。
         * @return 指定されたファイルから読み取るためのリダイレクト
         */
        public static Redirect from(final File file) {
            if (file == null)
                throw new NullPointerException();
            return new Redirect() {
                    public ProcessBuilder.Redirect.Type type() { return ProcessBuilder.Redirect.Type.READ; }
                    public File file() { return file; }
                    public String toString() {
                        return "redirect to read from file \"" + file + "\"";
                    }
                };
        }

        /**
         * 指定されたファイルに書き込むためのリダイレクトを返します。
         * サブプロセスが開始される際に指定されたファイルが存在する場合、その以前の内容は破棄されます。
         * 次のことは常に真であるでしょう：
         *  <pre> {@code
         * Redirect.to(file).file() == file &&
         * Redirect.to(file).type() == Redirect.Type.WRITE
         * }</pre>
         *
         * @param file  このリダイレクトの対象となるファイル。
         * @return 指定されたファイルに書き込むためのリダイレクト。
         */
        public static Redirect to(final File file) {
            if (file == null)
                throw new NullPointerException();
            return new Redirect() {
                    public ProcessBuilder.Redirect.Type type() { return ProcessBuilder.Redirect.Type.WRITE; }
                    public File file() { return file; }
                    public String toString() {
                        return "redirect to write to file \"" + file + "\"";
                    }
                    boolean append() { return false; }
                };
        }

        /**
         * 指定されたファイルに追記するためのリダイレクトを返します。
         * 各書き込み操作では、まず位置をファイルの末尾に進め、その後、要求されたデータを書き込みます。
         * 位置の進行とデータの書き込みが単一のアトミックな操作として行われるかどうかはシステム依存であり、したがって未定義です。
         * 次のことは常に真であるでしょう：
         *  <pre> {@code
         * Redirect.appendTo(file).file() == file &&
         * Redirect.appendTo(file).type() == Redirect.Type.APPEND
         * }</pre>
         *
         * @param file このリダイレクトの対象となるファイル。
         * @return 指定されたファイルに追記するためのリダイレクト。
         */
        public static Redirect appendTo(final File file) {
            if (file == null)
                throw new NullPointerException();
            return new Redirect() {
                    public ProcessBuilder.Redirect.Type type() { return ProcessBuilder.Redirect.Type.APPEND; }
                    public File file() { return file; }
                    public String toString() {
                        return "redirect to append to file \"" + file + "\"";
                    }
                    boolean append() { return true; }
                };
        }

        /**
         * 指定されたオブジェクトとこの Redirect を等価性で比較します。
         * 二つのオブジェクトが同一であるか、または両方のオブジェクトが同じタイプの Redirect インスタンスであり、
         * かつ関連付けられた File インスタンスが非 null で等しい場合にのみ、true を返します。
         */
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (! (obj instanceof Redirect r))
                return false;
            if (r.type() != this.type())
                return false;
            assert this.file() != null;
            return this.file().equals(r.file());
        }

        /**
         * Returns a hash code value for this {@code Redirect}.
         * @return a hash code value for this {@code Redirect}
         */
        public int hashCode() {
            File file = file();
            if (file == null)
                return super.hashCode();
            else
                return file.hashCode();
        }

        /**
         * 公開されたコンストラクタはありません。クライアントは、事前定義された静的な Redirect インスタンスまたはファクトリーメソッドを使用する必要があります
         */
        private Redirect() {}
    }

    /**
     * Redirect のプライベート実装サブクラスで、以前に開始されたプロセスの出力に対する FileDescriptor を保持します。
     * この FileDescriptor は、次に開始されるプロセスの標準入力として使用されます。
     */
    static class RedirectPipeImpl extends Redirect {
        final FileDescriptor fd;

        RedirectPipeImpl() {
            this.fd = new FileDescriptor();
        }
        @Override
        public ProcessBuilder.Redirect.Type type() { return ProcessBuilder.Redirect.Type.PIPE; }

        @Override
        public String toString() { return type().toString();}

        FileDescriptor getFd() { return fd; }
    }

    /**
     * 必要に応じてデフォルトを作成し、リダイレクトの配列を返します
     */
    private Redirect[] redirects() {
        if (redirects == null) {
            redirects = new Redirect[] {
                    Redirect.PIPE, Redirect.PIPE, Redirect.PIPE
            };
        }
        return redirects;
    }

    /**
     * このプロセスビルダーの標準入力のソースを設定します。このオブジェクトの start() メソッドによって後で開始されるサブプロセスは、
     * このソースから標準入力を取得します。ソースが Redirect.PIPE（初期値）の場合、
     * サブプロセスの標準入力には Process.getOutputStream() によって返される出力ストリームを使用して書き込むことができます。
     * ソースが他の値に設定されると、Process.getOutputStream() は null の出力ストリームを返します。
     *
     * @param  source 新しい標準入力ソース
     * @return this process builder
     * @throws IllegalArgumentException
     *         リダイレクトが有効なデータソースに対応していない場合、つまり、WRITE または APPEND のタイプの場合にスローされます。
     * @since  1.7
     */
    public ProcessBuilder redirectInput(Redirect source) {
        if (source.type() == Redirect.Type.WRITE ||
            source.type() == Redirect.Type.APPEND)
            throw new IllegalArgumentException(
                "Redirect invalid for reading: " + source);
        redirects()[0] = source;
        return this;
    }

    /**
     * このプロセスビルダーの標準出力の送信先を設定します。このオブジェクトの start() メソッドによって後で開始されるサブプロセスは、その標準出力をこの送信先に送ります。
     * 送信先が Redirect.PIPE（初期値）の場合、サブプロセスの標準出力は Process.getInputStream() によって返される入力ストリームを使って読み取ることができます。
     * 送信先が他の値に設定されると、Process.getInputStream() は null の入力ストリームを返します。
     *
     * @param  destination 新しい標準出力の送信先
     * @return this process builder
     * @throws IllegalArgumentException
     *         リダイレクトが有効なデータの送信先に対応していない場合、つまり、READ のタイプの場合にスローされます。
     * @since  1.7
     */
    public ProcessBuilder redirectOutput(Redirect destination) {
        if (destination.type() == Redirect.Type.READ)
            throw new IllegalArgumentException(
                "Redirect invalid for writing: " + destination);
        redirects()[1] = destination;
        return this;
    }

    /**
     * このプロセスビルダーの標準エラーの送信先を設定します。このオブジェクトの start() メソッドによって後で開始されるサブプロセスは、その標準エラーをこの送信先に送ります。
     * 送信先が Redirect.PIPE（初期値）の場合、サブプロセスのエラー出力は Process.getErrorStream() によって返される入力ストリームを使って読み取ることができます。
     * 送信先が他の値に設定されると、Process.getErrorStream() は null の入力ストリームを返します。
     * もし redirectErrorStream 属性が true に設定されている場合、このメソッドで設定されたリダイレクトは無効になります。
     *
     * @param  destination  新しい標準エラーの送信先
     * @return this process builder
     * @throws IllegalArgumentException
     *         リダイレクトが有効なデータの送信先に対応していない場合、つまり、READ のタイプの場合にスローされます。
     * @since  1.7
     */
    public ProcessBuilder redirectError(Redirect destination) {
        if (destination.type() == Redirect.Type.READ)
            throw new IllegalArgumentException(
                "Redirect invalid for writing: " + destination);
        redirects()[2] = destination;
        return this;
    }

    /**
     * このプロセスビルダーの標準入力ソースをファイルに設定します。
     * これは便宜的なメソッドです。redirectInput(file) の形式で呼び出すことは、redirectInput(Redirect.from(file)) を呼び出すのとまったく同じ動作をします。
     *
     * @param  file the new standard input source
     * @return this process builder
     * @since  1.7
     */
    public ProcessBuilder redirectInput(File file) {
        return redirectInput(Redirect.from(file));
    }

    /**
     * Sets this process builder's standard output destination to a file.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code redirectOutput(file)}
     * behaves in exactly the same way as the invocation
     * {@link #redirectOutput(Redirect) redirectOutput}
     * {@code (Redirect.to(file))}.
     *
     * @param  file the new standard output destination
     * @return this process builder
     * @since  1.7
     */
    public ProcessBuilder redirectOutput(File file) {
        return redirectOutput(Redirect.to(file));
    }

    /**
     * Sets this process builder's standard error destination to a file.
     *
     * <p>This is a convenience method.  An invocation of the form
     * {@code redirectError(file)}
     * behaves in exactly the same way as the invocation
     * {@link #redirectError(Redirect) redirectError}
     * {@code (Redirect.to(file))}.
     *
     * @param  file the new standard error destination
     * @return this process builder
     * @since  1.7
     */
    public ProcessBuilder redirectError(File file) {
        return redirectError(Redirect.to(file));
    }

    /**
     * このプロセスビルダーの標準入力ソースを返します。このオブジェクトの start() メソッドによって後で開始されるサブプロセスは、
     * その標準入力をこのソースから取得します。初期値は Redirect.PIPE です。
     *
     * @return this process builder's standard input source
     * @since  1.7
     */
    public Redirect redirectInput() {
        return (redirects == null) ? Redirect.PIPE : redirects[0];
    }

    /**
     * Returns this process builder's standard output destination.
     *
     * Subprocesses subsequently started by this object's {@link #start()}
     * method redirect their standard output to this destination.
     * The initial value is {@link Redirect#PIPE Redirect.PIPE}.
     *
     * @return this process builder's standard output destination
     * @since  1.7
     */
    public Redirect redirectOutput() {
        return (redirects == null) ? Redirect.PIPE : redirects[1];
    }

    /**
     * Returns this process builder's standard error destination.
     *
     * Subprocesses subsequently started by this object's {@link #start()}
     * method redirect their standard error to this destination.
     * The initial value is {@link Redirect#PIPE Redirect.PIPE}.
     *
     * @return this process builder's standard error destination
     * @since  1.7
     */
    public Redirect redirectError() {
        return (redirects == null) ? Redirect.PIPE : redirects[2];
    }

    /**
     * サブプロセスの標準入出力のソースと送信先を現在のJavaプロセスと同じに設定します。
     * これは便宜的なメソッドです。
     * pb.inheritIO() の形式で呼び出すことは、次のように呼び出すのとまったく同じ動作をします:
     *  <pre> {@code
     * pb.inheritIO()
     * }</pre>
     * behaves in exactly the same way as the invocation
     *  <pre> {@code
     * pb.redirectInput(Redirect.INHERIT)
     *   .redirectOutput(Redirect.INHERIT)
     *   .redirectError(Redirect.INHERIT)
     * }</pre>
     *
     * これにより、ほとんどのオペレーティングシステムのコマンドインタープリタや、標準Cライブラリ関数 system() と同じ動作が得られます。
     *
     * @return this process builder
     * @since  1.7
     */
    public ProcessBuilder inheritIO() {
        Arrays.fill(redirects(), Redirect.INHERIT);
        return this;
    }

    /**
     * このプロセスビルダーが標準エラーと標準出力を統合するかどうかを示します。
     * このプロパティが true の場合、このオブジェクトの start() メソッドによって後で開始されるサブプロセスで生成されるエラー出力は標準出力と統合され、
     * 両方とも Process.getInputStream() メソッドを使って読み取ることができます。これにより、エラーメッセージとそれに対応する出力を関連付けて読みやすくなります。
     * 初期値は false です。
     *
     * @return このプロセスビルダーの redirectErrorStream プロパティ
     */
    public boolean redirectErrorStream() {
        return redirectErrorStream;
    }

    /**
     * このプロセスビルダーの redirectErrorStream プロパティを設定します。
     * このプロパティが true の場合、このオブジェクトの start() メソッドによって後で開始されるサブプロセスで生成されるエラー出力は標準出力と統合され、
     * 両方とも Process.getInputStream() メソッドを使って読み取ることができます。これにより、エラーメッセージとそれに対応する出力を関連付けて読みやすくなります。
     * 初期値は false です。
     *
     * @param  redirectErrorStream the new property value
     * @return this process builder
     */
    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }

    /**
     * このメソッドは、このプロセスビルダーの属性を使って新しいプロセスを開始します。
     * 新しいプロセスは、{@code command()} で指定されたコマンドと引数を使用し、{@code directory()} で指定された作業ディレクトリで実行され、
     * {@code environment()} で指定されたプロセス環境を使用します。
     *
     * このメソッドは、コマンドが有効なオペレーティングシステムコマンドであることを確認します。
     * 有効なコマンドが何であるかはシステム依存ですが、少なくともコマンドは非空で非nullの文字列のリストでなければなりません。
     *
     * 一部のオペレーティングシステムでは、プロセスを開始するためにシステム依存の環境変数の最小セットが必要な場合があります。
     * そのため、サブプロセスはプロセスビルダーの {@code environment()} にある環境変数設定に加えて、追加の環境変数設定を引き継ぐ場合があります。
     *
     * もしセキュリティマネージャが存在する場合、その checkExec メソッドがこのオブジェクトのコマンドリストの最初の要素を引数として呼び出します。
     * これにより SecurityException がスローされる場合があります。
     *
     * オペレーティングシステムでプロセスを開始するのはシステム依存です。起こり得る問題の例としては以下が挙げられます：
     * <ul>
     * <li>オペレーティングシステムプログラムファイルが見つからない。
     * <li>プログラムファイルへのアクセスが拒否される。
     * <li>作業ディレクトリが存在しない。
     * <li>コマンド引数に無効な文字（例：NUL）が含まれている。
     * </ul>
     *
     * このような場合には例外がスローされますが、その例外の正確な性質はシステム依存ですが、必ず IOException のサブクラスになります。
     * もしオペレーティングシステムがプロセスの作成をサポートしていない場合、UnsupportedOperationException がスローされます。
     * その後、プロセスビルダーに対して行った変更は、返される Process には影響しません。
     *
     * @return サブプロセスを管理するための新しい Process オブジェクト
     *
     * @throws NullPointerException
     *          コマンドリストの要素が null の場合。
     *
     * @throws IndexOutOfBoundsException
     *         コマンドが空のリスト（サイズが0）である場合。
     *
     * @throws SecurityException
     *         セキュリティマネージャが存在し、その checkExec メソッドがサブプロセスの作成を許可しない場合や、
     *         サブプロセスの標準入力がファイルからリダイレクトされ、そのセキュリティマネージャの checkRead メソッドが読み取りアクセスを拒否した場合、
     *         または標準出力や標準エラーがファイルにリダイレクトされ、そのセキュリティマネージャの checkWrite メソッドが書き込みアクセスを拒否した場合。
     *
     * @throws  UnsupportedOperationException
     *          オペレーティングシステムがプロセスの作成をサポートしていない場合。
     *
     * @throws IOException I/O エラーが発生した場合
     *
     * @see Runtime#exec(String[], String[], File)
     */
    public Process start() throws IOException {
        return start(redirects);
    }

    /**
     * 新しいプロセスを明示的なリダイレクトの配列を使用して開始します。各プロセスの開始に関する詳細は、start を参照してください。
     * この start(Redirect[] redirects) メソッドは、ProcessBuilder クラス内でサブプロセスを開始するための重要な処理を行っています。
     * 具体的には、標準入力、標準出力、および標準エラーのリダイレクト設定を含むサブプロセスの起動を担当しています。
     * @param redirects 標準入力、標準出力、標準エラーのリダイレクトの配列
     * @return the new Process
     * @throws IOException if an I/O error occurs
     */
    private Process start(Redirect[] redirects) throws IOException {
        // 最初に配列に変換する必要があります -- 悪意のあるユーザーが提供したリストは、セキュリティチェックを回避しようとするかもしれません。
        String[] cmdarray = command.toArray(new String[command.size()]);
        cmdarray = cmdarray.clone();

        for (String arg : cmdarray)
            if (arg == null)
                throw new NullPointerException();
        // Throws IndexOutOfBoundsException if command is empty
        String prog = cmdarray[0];

        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            security.checkExec(prog);

        String dir = directory == null ? null : directory.toString();

        for (String s : cmdarray) {
            if (s.indexOf('\u0000') >= 0) {
                throw new IOException("invalid null character in command");
            }
        }

        try {
            Process process = ProcessImpl.start(cmdarray,
                                     environment,
                                     dir,
                                     redirects,
                                     redirectErrorStream);
            ProcessStartEvent event = new ProcessStartEvent();
            if (event.isEnabled()) {
                StringJoiner command = new StringJoiner(" ");
                for (String s: cmdarray) {
                    command.add(s);
                }
                event.directory = dir;
                event.command = command.toString();
                event.pid = process.pid();
                event.commit();
            }
            return process;
        } catch (IOException | IllegalArgumentException e) {
            String exceptionInfo = ": " + e.getMessage();
            Throwable cause = e;
            if ((e instanceof IOException) && security != null) {
                // Can not disclose the fail reason for read-protected files.
                try {
                    security.checkRead(prog);
                } catch (SecurityException se) {
                    exceptionInfo = "";
                    cause = se;
                }
            }
            // It's much easier for us to create a high-quality error
            // message than the low-level C code which found the problem.
            throw new IOException(
                "Cannot run program \"" + prog + "\""
                + (dir == null ? "" : " (in directory \"" + dir + "\")")
                + exceptionInfo,
                cause);
        }
    }

    /**
     * 各 ProcessBuilder に対してプロセスを開始し、標準出力と標準入力ストリームでリンクされたプロセスのパイプラインを作成します。
     * 各 ProcessBuilder の属性は、それぞれのプロセスを開始するために使用されますが、各プロセスが開始されると、その標準出力は次のプロセスの標準入力にリダイレクトされます。
     * 最初のプロセスの標準入力と最後のプロセスの標準出力のリダイレクトは、それぞれの ProcessBuilder のリダイレクト設定を使用して初期化されます。
     * それ以外のすべての ProcessBuilder のリダイレクトは Redirect.PIPE でなければなりません。
     * 中間プロセス間のすべての入力および出力ストリームにはアクセスできません。最初のプロセスを除くすべてのプロセスの標準入力は null 出力ストリームです。
     * 最後のプロセスを除くすべてのプロセスの標準出力は null 入力ストリームです。
     * 各 ProcessBuilder の redirectErrorStream() はそれぞれのプロセスに適用されます。この値が true に設定されている場合、エラーストリームは標準出力と同じストリームに書き込まれます。
     *
     * いずれかのプロセスを開始するときに例外がスローされた場合、すべてのプロセスが強制的に破棄されます。
     * startPipeline メソッドは、start メソッドと同じチェックを各 ProcessBuilder に対して実行します。
     * 各新しいプロセスは、対応する ProcessBuilder の command() によって指定されたコマンドと引数を、directory() によって指定された作業ディレクトリ内で、
     * environment() によって指定されたプロセス環境とともに呼び出します。
     *
     * 各 ProcessBuilder のコマンドが有効なオペレーティングシステムのコマンドであることを確認します。
     * 有効なコマンドはシステム依存ですが、少なくともコマンドは null でない文字列の非空リストでなければなりません。
     * 一部のオペレーティングシステムでは、プロセスを開始するためにシステム依存の環境変数の最小セットが必要な場合があります。
     * その結果、サブプロセスは ProcessBuilder の環境を超えた追加の環境変数設定を継承することがあります。
     *
     * セキュリティマネージャが存在する場合、その checkExec メソッドが、各 ProcessBuilder のコマンド配列の最初の要素を引数として呼び出されます。
     * これにより、SecurityException がスローされる可能性があります。
     *
     * オペレーティングシステムプロセスを開始することは非常にシステム依存です。起こり得る多くの問題の中には以下が含まれます：
     * <ul>
     *      <li>オペレーティングシステムプログラムファイルが見つからない。
     *      <li>プログラムファイルへのアクセスが拒否される。
     *      <li>作業ディレクトリが存在しない。
     *      <li>コマンド引数に無効な文字（例：NUL）が含まれている。
     * </ul>
     * <p>
     * これらのケースでは例外がスローされます。例外の正確な性質はシステム依存ですが、常に IOException のサブクラスとなります。
     * オペレーティングシステムがプロセスの作成をサポートしていない場合、UnsupportedOperationException がスローされます。
     * 指定されたビルダーに対するその後の変更は、返されるプロセスには影響しません。
     * @apiNote
     * Unix 互換プラットフォームでファイル階層内のすべてのファイルのユニークなインポートをカウントする例を次に示します:
     * <pre>{@code
     * String directory = "/home/duke/src";
     * ProcessBuilder[] builders = {
     *              new ProcessBuilder("find", directory, "-type", "f"),
     *              new ProcessBuilder("xargs", "grep", "-h", "^import "),
     *              new ProcessBuilder("awk", "{print $2;}"),
     *              new ProcessBuilder("sort", "-u")};
     * List<Process> processes = ProcessBuilder.startPipeline(
     *         Arrays.asList(builders));
     * Process last = processes.get(processes.size()-1);
     * try (InputStream is = last.getInputStream();
     *         Reader isr = new InputStreamReader(is);
     *         BufferedReader r = new BufferedReader(isr)) {
     *     long count = r.lines().count();
     * }
     * }</pre>
     *
     * @param builders a List of ProcessBuilders
     * @return 対応する ProcessBuilder から開始されたプロセスのリスト (List<Process>)
     * @throws IllegalArgumentException 最初のビルダーの標準入力および最後のビルダーの標準出力以外のリダイレクトが ProcessBuilder.Redirect.PIPE でない場合
     * @throws NullPointerException
     *         コマンドリストの要素が null の場合、ProcessBuilder リストの要素が null の場合、または builders 引数が null の場合
     * @throws IndexOutOfBoundsException
     *         コマンドが空のリスト（サイズ 0）の場合
     * @throws SecurityException
     *         セキュリティマネージャが存在し、
     *         <ul>
     *         <li>checkExec メソッドがサブプロセスの作成を許可しない場合、または
     *         <li>サブプロセスの標準入力がファイルからリダイレクトされ、セキュリティマネージャの checkRead メソッドがそのファイルへの読み取りアクセスを拒否する場合、または
     *         <li>サブプロセスの標準出力または標準エラーがファイルにリダイレクトされ、セキュリティマネージャの checkWrite メソッドがそのファイルへの書き込みアクセスを拒否する場合
     *         </ul>
     *
     * @throws  UnsupportedOperationException
     *          オペレーティングシステムがプロセスの作成をサポートしていない場合
     *
     * @throws IOException if an I/O error occurs
     * @since 9
     */
    public static List<Process> startPipeline(List<ProcessBuilder> builders) throws IOException {
        // ビルダーを蓄積してチェックする
        final int numBuilders = builders.size();
        List<Process> processes = new ArrayList<>(numBuilders);
        try {
            // prevOutput は、前のプロセスの出力リダイレクトを格納
            Redirect prevOutput = null;
            for (int index = 0; index < builders.size(); index++) {
                ProcessBuilder builder = builders.get(index);
                // builder.redirects() で現在のプロセスのリダイレクト情報を取得
                Redirect[] redirects = builder.redirects();
                if (index > 0) {
                    // 最初のプロセス以外では、標準入力が Redirect.PIPE に設定されている必要がある。
                    // 現在のビルダーが前のビルダーから入力を受け取れるかどうかを確認する
                    if (builder.redirectInput() != Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectInput()" +
                                " must be PIPE except for the first builder: "
                                + builder.redirectInput());
                    }
                    // 最初のプロセス以外の入力リダイレクトに、前のプロセスの出力（prevOutput）を設定。
                    redirects[0] = prevOutput;
                }
                if (index < numBuilders - 1) {
                    // 最後以外のプロセスでは、標準出力が Redirect.PIPE に設定されている必要がある。
                    // ただし、最後のプロセスだけは例外で、標準出力を直接ファイルや別の場所にリダイレクトすることが許容される場合があるため、ここでは特に除外されています。
                    if (builder.redirectOutput() != Redirect.PIPE) {
                        throw new IllegalArgumentException("builder redirectOutput()" +
                                " must be PIPE except for the last builder: "
                                + builder.redirectOutput());
                    }
                    // 新しいパイプ（RedirectPipeImpl）を出力リダイレクトに設定
                    redirects[1] = new RedirectPipeImpl();  // placeholder for new output
                }
                // 現在の ProcessBuilder を使用してプロセスを開始し、結果の Process オブジェクトをリスト processes に追加
                processes.add(builder.start(redirects));
                // 前のプロセスの出力リダイレクト（RedirectPipeImpl）が使用済みの場合、対応するファイルディスクリプタを閉じる
                if (prevOutput instanceof RedirectPipeImpl redir) {
                    // ファイルディスクリプタ（fd）を閉じられるようにラップする
                    new Process.PipeInputStream(redir.getFd()).close();
                }
                // prevOutput を更新して、次のプロセスで使用
                prevOutput = redirects[1];
            }
        } catch (Exception ex) {
            // Cleanup processes already started
            processes.forEach(Process::destroyForcibly);
            processes.forEach(p -> {
                try {
                    p.waitFor();        // Wait for it to exit
                } catch (InterruptedException ie) {
                    // If interrupted; continue with next Process
                    Thread.currentThread().interrupt();
                }
            });
            throw ex;
        }
        return processes;
    }
}
