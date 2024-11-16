package java.lang;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 指定されたコンパイラ警告が、アノテーションが付けられた要素（およびその要素内のすべてのプログラム要素）で抑制されることを示します。
 * なお、特定の要素で抑制される警告のセットは、その要素を含むすべての要素で抑制される警告のスーパーセットです。
 * 例えば、クラスに1つの警告を抑制するアノテーションを付け、メソッドに別の警告を抑制するアノテーションを付けた場合、両方の警告はメソッド内で抑制されます。
 * しかし、{@code module-info}ファイル内で警告が抑制されている場合、その抑制はファイル内の要素に適用され、モジュール内の型には適用されないことに注意してください。
 *
 * スタイルとしては、プログラマーはこのアノテーションを効果的な最も深くネストされた要素に使用するべきです。
 * 特定のメソッドで警告を抑制したい場合は、そのメソッドにアノテーションを付けるべきであり、そのクラスには付けないべきです。
 *
 * @author Josh Bloch
 * @since 1.5
 * @jls 4.8 Raw Types
 * @jls 4.12.2 Variables of Reference Type
 * @jls 5.1.9 Unchecked Conversion
 * @jls 5.5 Casting Contexts
 * @jls 9.6.4.5 @SuppressWarnings
 */
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, MODULE})
@Retention(RetentionPolicy.SOURCE)
public @interface SuppressWarnings {
    /**
     * アノテーションが付けられた要素でコンパイラが抑制する警告のセットです。
     * 重複した名前は許可されています。名前が2回目以降に現れると、それらは無視されます。
     * 認識されない警告名が存在することはエラーではありません。コンパイラは認識できない警告名を無視しなければなりません。
     * ただし、アノテーションに認識できない警告名が含まれている場合は、警告を出力しても構いません。
     *
     * 文字列「{@code unchecked}」は、未チェックの警告を抑制するために使用されます。
     * コンパイラベンダーは、このアノテーションタイプと関連してサポートする追加の警告名を文書化するべきです。
     * 複数のコンパイラで同じ名前が動作するように協力することが推奨されます。
     * @return the set of warnings to be suppressed
     */
    String[] value();
}
