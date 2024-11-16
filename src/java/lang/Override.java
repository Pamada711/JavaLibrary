package java.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * このアノテーションは、メソッド宣言がスーパークラスのメソッド宣言をオーバーライドすることを意図していることを示します。
 * このアノテーションタイプでメソッドが注釈されている場合、コンパイラは少なくとも次のいずれかの条件を満たさない限り、エラーメッセージを生成する必要があります：
 *
 * <ul><li>
 * メソッドがスーパークラスで宣言されたメソッドをオーバーライドまたは実装していること。
 * </li><li>
 * メソッドのシグネチャが、Objectクラスで宣言された任意の公開メソッドとオーバーライド相当であること。
 * </li></ul>
 *
 * @author  Peter von der Ah&eacute;
 * @author  Joshua Bloch
 * @jls 8.4.8 Inheritance, Overriding, and Hiding
 * @jls 9.4.1 Inheritance and Overriding
 * @jls 9.6.4.4 @Override
 * @since 1.5
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
