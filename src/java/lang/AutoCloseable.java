package java.lang;

/**
 *
 * リソース（ファイルやソケットのハンドルなど）を保持する可能性のあるオブジェクト。
 * AutoCloseableオブジェクトの{@code close()}メソッドは、
 * オブジェクトがリソース指定ヘッダーで宣言された{@code try-with-resources}ブロックを抜ける際に自動的に呼び出されます。
 * この構文により、リソースが迅速に解放され、リソース枯渇に伴う例外やエラーを回避できます。
 *
 * @apiNote
 * ベースクラスが{@code AutoCloseable}を実装している場合、すべてのサブクラスやインスタンスがリリース可能なリソースを保持しているわけではないことがあります。
 * 完全な一般性で操作しなければならないコードや、{@code AutoCloseable}インスタンスがリソースの解放を必要とすることが分かっている場合は、
 * {@code try-with-resources}構文の使用が推奨されます。
 * しかし、{@code java.util.stream.Stream}のようにI/Oベースと非I/Oベースの両方の形式をサポートする施設を使用する場合、
 * 非I/Oベースの形式を使用する際には、一般的に{@code try-with-resources}ブロックは不要です。
 *
 * @author Josh Bloch
 * @since 1.7
 */
public interface AutoCloseable {
    /**
     * このリソースを閉じ、基になるリソースを解放します。このメソッドは、{@code try-with-resources}ステートメントで管理されているオブジェクトに対して自動的に呼び出されます。
     *
     * @apiNote
     * このインターフェースメソッドは{@code Exception}をスローすることを宣言していますが、
     * 実装者は {@code close} メソッドの具象実装でより具体的な例外をスローするか、
     * または {@code close} 操作が失敗しない場合には例外をスローしないようにすることが強く推奨されます。
     *
     * {@code close} 操作が失敗する可能性のある場合、実装者は慎重に対処する必要があります。
     * リソースを解放し、リソースを閉じたと内部的にマークした後に例外をスローすることを強くお勧めします。
     * このようにすることで、{@code close} メソッドが一度しか呼ばれない可能性が高いため、リソースはタイムリーに解放されます。
     * また、リソースが他のリソースによってラップされている場合などに発生する問題を減少させることができます。
     *
     * このインターフェースの実装者には、{@code close} メソッドが {@link InterruptedException} をスローしないよう強く推奨されます。
     * この例外はスレッドの割り込み状態と相互作用するため、{@code InterruptedException} が抑制された場合、
     * ランタイムで不具合が発生する可能性があります。一般的に、例外が抑制されると問題が発生する場合、その例外は {@code AutoCloseable.close} メソッドでスローすべきではありません。
     *
     * {@link java.io.Closeable} の {@code close} メソッドとは異なり、この {@code close} メソッドは冪等性を必要としません。
     * 言い換えれば、この {@code close} メソッドは複数回呼び出されると何らかの副作用が発生する可能性がありますが、
     * {@code Closeable.close} は複数回呼び出されても効果がないことが求められています。
     * しかし、このインターフェースの実装者には、{@code close} メソッドが冪等性を持つようにすることが強く推奨されます。
     *
     * @throws Exception if this resource cannot be closed
     */
    void close() throws Exception;
}
