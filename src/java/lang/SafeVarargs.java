package java.lang;

import java.lang.annotation.*;

/**
 *
 * {@code @SafeVarargs}アノテーションは、プログラマーによる宣言で、
 * 注釈が付けられたメソッドやコンストラクタがその{@code varargs}パラメータに対して潜在的に危険な操作を行わないことを示します。
 * このアノテーションをメソッドやコンストラクタに適用すると、非信頼的な可変長引数型に関する未チェック警告と、
 * 呼び出し元でのパラメータ化された配列作成に関する未チェック警告が抑制されます。
 *
 * {@code @Target}メタアノテーションで指定された使用制限に加えて、
 * コンパイラはこのアノテーションタイプに対して追加の使用制限を実装する必要があります。
 * 以下のいずれかに該当する場合、メソッドやコンストラクタ宣言に{@code @SafeVarargs}アノテーションを付けることはコンパイルエラーになります：
 * <ul>
 * <li>  宣言が固定長引数を持つメソッドまたはコンストラクタである場合
 *
 * <li> 「宣言が可変長引数を持つメソッドで、{@code static}でも{@code final}でも{@code private}でもない場合
 * </ul>
 *
 * <p> コンパイラは、このアノテーションタイプがメソッドまたはコンストラクタの宣言に適用された場合に警告を出すことが推奨されています。警告を出すべき状況は以下の通りです：
 *
 * <ul>
 *
 * <li> 可変長引数のパラメータが再具象化可能な要素型（プリミティブ型、{@code Object}、{@code String} など）を持っている場合。
 * （このアノテーションタイプが抑制する未チェックの警告は、すでに再具象化可能な要素型では発生しません。）
 *
 * <li> メソッドまたはコンストラクタの本体が潜在的に危険な操作を行っている場合（例えば、可変長引数の配列の要素に対する代入が未チェックの警告を引き起こすような操作）。
 * 一部の危険な操作は未チェックの警告を引き起こさないことがあります。例えば、エイリアシング（別名付け）などです。
 *
 * <blockquote><pre>
 * &#64;SafeVarargs // 実際には安全ではない！
 * static void m(List&lt;String&gt;... stringLists) {
 *   Object[] array = stringLists;
 *   List&lt;Integer&gt; tmpList = Arrays.asList(42);
 *   array[0] = tmpList; // 意味的には無効ですが、警告なしでコンパイルされる
 *   String s = stringLists[0].get(0); // おっと、実行時にClassCastExceptionが発生！
 * }
 * </pre></blockquote>
 *
 * 実行時に{@code ClassCastException}を引き起こす原因となります。
 * 将来のプラットフォームのバージョンでは、そのような危険な操作に対してコンパイラエラーが発生することが義務付けられる可能性があります。
 *
 * </ul>
 *
 * @since 1.7
 * @jls 4.7 Reifiable Types
 * @jls 8.4.1 Formal Parameters
 * @jls 9.6.4.7 @SafeVarargs
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface SafeVarargs {}
