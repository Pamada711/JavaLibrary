package java.lang;

import java.lang.annotation.*;

/**
 *
 * このアノテーションタイプは、インターフェース型の宣言がJava言語仕様で定義された関数型インターフェースであることを示すために使用されます。
 * 概念的には、関数型インターフェースは正確に1つの抽象メソッドを持っています。
 * デフォルトメソッドは実装を持っているため、抽象ではありません。
 * インターフェースが{@code java.lang.Object}の公開メソッドの1つをオーバーライドする抽象メソッドを宣言している場合、
 * そのメソッドはインターフェースの抽象メソッドのカウントには含まれません。
 * なぜなら、インターフェースの実装は{@code java.lang.Object}または他の場所から実装を持つからです。
 *
 * 関数型インターフェースのインスタンスは、ラムダ式、メソッド参照、またはコンストラクタ参照を使って作成できます。
 * このアノテーションタイプで型が注釈されている場合、コンパイラは次の条件を満たさない限りエラーメッセージを生成する必要があります：
 *
 * <ul>
 * <li> 型がインターフェース型であり、アノテーション型、列挙型、またはクラスではないこと。
 * <li> 注釈された型が関数型インターフェースの要件を満たしていること。
 * </ul>
 *
 * ただし、コンパイラはインターフェースが関数型インターフェースの定義を満たしていれば、
 * {@code FunctionalInterface}アノテーションがインターフェース宣言に存在するかどうかに関わらず、それを関数型インターフェースとして扱います。
 *
 * @jls 4.3.2 The Class Object
 * @jls 9.8 Functional Interfaces
 * @jls 9.4.3 Interface Method Body
 * @jls 9.6.4.9 @FunctionalInterface
 * @since 1.8
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FunctionalInterface {}
