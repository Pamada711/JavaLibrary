package java.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * {@code @Deprecated}アノテーションが付けられたプログラム要素は、プログラマーに使用を避けるよう推奨されるものです。
 * 要素が非推奨となる理由はいくつかあります。例えば、使用するとエラーを引き起こす可能性がある、将来のバージョンで互換性がなく変更されるか削除される可能性がある、
 * より新しく通常は好ましい代替手段が登場した、または時代遅れであるなどです。
 *
 * コンパイラは、非推奨のプログラム要素が使用されたり、非推奨ではないコードでオーバーライドされたりすると警告を出します。
 * ただし、ローカル変数宣言やパラメータ宣言、パッケージ宣言に{@code @Deprecated}アノテーションを付けても、コンパイラが警告を発することには影響しません。
 *
 * モジュールが非推奨とされている場合、そのモジュールが{@code requires}句で使用されると警告が発されますが、
 * {@code exports}や{@code opens}句で使用される場合には警告は発されません。モジュールが非推奨になったからといって、そのモジュール内の型が使用されても警告は発されません。
 *
 * このアノテーションタイプには{@code since}という文字列型の要素があります。
 * この要素の値は、注釈が付けられたプログラム要素が初めて非推奨とされたバージョンを示します。
 *
 * また、このアノテーションタイプには{@code forRemoval}というブール型の要素があります。
 * {@code true}の値は、将来のバージョンでそのプログラム要素を削除する意図があることを示します。
 * {@code false}の値は、そのプログラム要素の使用が推奨されないことを示し、アノテーションが付けられた時点では削除する特定の意図はなかったことを意味します。
 *
 * @apiNote
 * プログラム要素を非推奨にする理由は、{@code @deprecated Javadoc}タグを使用してドキュメントで説明することが強く推奨されます。
 * ドキュメントには、可能であれば推奨される代替APIを提案し、それにリンクを張ることも推奨されます。
 * 代替APIはしばしば微妙に異なる意味論を持つことがあるため、そのような問題についても説明するべきです。
 * 新たにアノテーションを付けたプログラム要素には、{@code since}の値を提供することが推奨されます。
 * ただし、{@code since}は必須ではないことに注意してください。なぜなら、既存の多くのアノテーションにはこの要素の値が欠けているからです。
 * アノテーション要素には定義された順序はありませんが、スタイルの一貫性として、{@code since}要素は最初に配置するべきです。
 *
 * {@code @Deprecated}アノテーションは、{@code @deprecated Javadoc}タグが存在する場合には常に付けられているべきであり、その逆も同様です。
 *
 * @author  Neal Gafter
 * @since 1.5
 * @jls 9.6.4.6 @Deprecated
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, MODULE, PARAMETER, TYPE})
public @interface Deprecated {
    /**
     * この要素は、注釈が付けられたプログラム要素が非推奨になったバージョンを返します。
     * バージョン文字列は、{@code @since Javadoc}タグの値と同じ形式および名前空間である必要があります。デフォルト値は空の文字列です。
     *
     * @return the version string
     * @since 9
     */
    String since() default "";

    /**
     * この要素は、注釈が付けられたプログラム要素が将来のバージョンで削除される予定かどうかを示します。デフォルト値は{@code false}です。
     *
     * @return whether the element is subject to removal
     * @since 9
     */
    boolean forRemoval() default false;
}
