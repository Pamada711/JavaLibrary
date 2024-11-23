/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.ModuleFinder;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;

/**
 *
 * RuntimePermission クラスは、ランタイムでの権限を管理するためのものです。このクラスは名前（ターゲット名とも呼ばれます）を保持しますが、アクションリストは含みません。
 * 指定された名前の権限を持つか、持たないかのいずれかです。
 * ターゲット名は、ランタイム権限の名前であり、命名規則は階層的なプロパティ名規則に従います。
 * また、名前の末尾にアスタリスク（*）が付いている場合、それはワイルドカードマッチを意味します。
 * たとえば、「loadLibrary.*」や「*」はワイルドカードマッチを示しますが、「*loadLibrary」や「a*b」はワイルドカードとはみなされません。
 *
 * <table class="striped">
 * <caption style="display:none">標準の RuntimePermission ターゲット名とその説明
 * 以下の表は、標準の RuntimePermission ターゲット名を示し、それぞれの権限が許可する内容と、その権限をコードに付与するリスクについて説明します。</caption>
 * <thead>
 * <tr>
 * <th scope="col">Permission Target Name</th>
 * <th scope="col">What the Permission Allows</th>
 * <th scope="col">Risks of Allowing this Permission</th>
 * </tr>
 * </thead>
 * <tbody>
 *
 * <tr>
 *   <th scope="row">createClassLoader</th>
 *   <td>クラスローダーの作成</td>
 *   <td> 悪意のあるコードが独自のクラスローダーを作成し、システムに不正なクラスをロードする可能性があり、
 *   これにより、そのクラスが所属する保護ドメインの権限を自動的に取得します。</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getClassLoader</th>
 *   <td>呼び出し元クラスのクラスローダーの取得</td>
 *   <td>攻撃者が特定のクラスのクラスローダーにアクセスすることで、本来アクセスできないクラスをロードできるようになります。</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setContextClassLoader</th>
 *   <td>Sスレッドのコンテキストクラスローダーの設定</td>
 *   <td>攻撃者がスレッドのクラスローダーを変更できるようになり、システムスレッドを含むスレッドのリソースを危険にさらす可能性があります。</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">enableContextClassLoaderOverride</th>
 *   <td>スレッドコンテキストクラスローダーのメソッドをオーバーライドするためのサブクラス化</td>
 *   <td>攻撃者がコンテキストクラスローダーの設定や取得を変更する方法を悪用し、システムコードを危険にさらす可能性があります</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">closeClassLoader</th>
 *   <td>URLClassLoader のクローズ</td>
 *   <td>攻撃者がクラスローダーを閉じてリソースのアクセスを妨害する可能性があります。</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setSecurityManager</th>
 *   <td>セキュリティマネージャの設定（既存のものを置き換える）
 * </td>
 *   <td>現在のセキュリティマネージャを置き換えて、制限の少ないセキュリティマネージャに変更し、セキュリティチェックをバイパスできるようになります</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">createSecurityManager</th>
 *   <td>新しいセキュリティマネージャの作成</td>
 *   <td>悪意のあるコードが保護されたメソッドにアクセスできるようになり、システムのセキュリティを侵害する可能性があります</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getenv.{variable name}</th>
 *   <td>Reading of the value of the specified environment variable</td>
 *   <td>This would allow code to read the value, or determine the
 *       existence, of a particular environment variable.  This is
 *       dangerous if the variable contains confidential data.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">exitVM.{exit status}</th>
 *   <td>Halting of the Java Virtual Machine with the specified exit status</td>
 *   <td>This allows an attacker to mount a denial-of-service attack
 * by automatically forcing the virtual machine to halt.
 * Note: The "exitVM.*" permission is automatically granted to all code
 * loaded from the application class path, thus enabling applications
 * to terminate themselves. Also, the "exitVM" permission is equivalent to
 * "exitVM.*".</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">shutdownHooks</th>
 *   <td>Registration and cancellation of virtual-machine shutdown hooks</td>
 *   <td>This allows an attacker to register a malicious shutdown
 * hook that interferes with the clean shutdown of the virtual machine.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setFactory</th>
 *   <td>Setting of the socket factory used by ServerSocket or Socket,
 * or of the stream handler factory used by URL</td>
 *   <td>This allows code to set the actual implementation
 * for the socket, server socket, stream handler, or RMI socket factory.
 * An attacker may set a faulty implementation which mangles the data
 * stream.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setIO</th>
 *   <td>Setting of System.out, System.in, and System.err</td>
 *   <td>This allows changing the value of the standard system streams.
 * An attacker may change System.in to monitor and
 * steal user input, or may set System.err to a "null" OutputStream,
 * which would hide any error messages sent to System.err. </td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">modifyThread</th>
 *   <td>Modification of threads, e.g., via calls to Thread
 * {@code interrupt, stop, suspend, resume, setDaemon, setPriority,
 * setName} and {@code setUncaughtExceptionHandler}
 * methods</td>
 * <td>This allows an attacker to modify the behaviour of
 * any thread in the system.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">stopThread</th>
 *   <td>Stopping of threads via calls to the Thread {@code stop}
 * method</td>
 *   <td>This allows code to stop any thread in the system provided that it is
 * already granted permission to access that thread.
 * This poses as a threat, because that code may corrupt the system by
 * killing existing threads.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">modifyThreadGroup</th>
 *   <td>modification of thread groups, e.g., via calls to ThreadGroup
 * {@code destroy}, {@code getParent}, {@code resume},
 * {@code setDaemon}, {@code setMaxPriority}, {@code stop},
 * and {@code suspend} methods</td>
 *   <td>This allows an attacker to create thread groups and
 * set their run priority.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getProtectionDomain</th>
 *   <td>Retrieval of the ProtectionDomain for a class</td>
 *   <td>This allows code to obtain policy information
 * for a particular code source. While obtaining policy information
 * does not compromise the security of the system, it does give
 * attackers additional information, such as local file names for
 * example, to better aim an attack.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getFileSystemAttributes</th>
 *   <td>Retrieval of file system attributes</td>
 *   <td>This allows code to obtain file system information such as disk usage
 *       or disk space available to the caller.  This is potentially dangerous
 *       because it discloses information about the system hardware
 *       configuration and some information about the caller's privilege to
 *       write files.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">readFileDescriptor</th>
 *   <td>Reading of file descriptors</td>
 *   <td>This would allow code to read the particular file associated
 *       with the file descriptor read. This is dangerous if the file
 *       contains confidential data.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">writeFileDescriptor</th>
 *   <td>Writing to file descriptors</td>
 *   <td>This allows code to write to a particular file associated
 *       with the descriptor. This is dangerous because it may allow
 *       malicious code to plant viruses or at the very least, fill up
 *       your entire disk.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">loadLibrary.{library name}</th>
 *   <td>Dynamic linking of the specified library</td>
 *   <td>It is dangerous to allow an applet permission to load native code
 * libraries, because the Java security architecture is not designed to and
 * does not prevent malicious behavior at the level of native code.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">accessClassInPackage.{package name}</th>
 *   <td>Access to the specified package via a class loader's
 * {@code loadClass} method when that class loader calls
 * the SecurityManager {@code checkPackageAccess} method</td>
 *   <td>This gives code access to classes in packages
 * to which it normally does not have access. Malicious code
 * may use these classes to help in its attempt to compromise
 * security in the system.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">defineClassInPackage.{package name}</th>
 *   <td>Definition of classes in the specified package, via a class
 * loader's {@code defineClass} method when that class loader calls
 * the SecurityManager {@code checkPackageDefinition} method.</td>
 *   <td>This grants code permission to define a class
 * in a particular package. This is dangerous because malicious
 * code with this permission may define rogue classes in
 * trusted packages like {@code java.security} or {@code java.lang},
 * for example.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">defineClass</th>
 *   <td>Define a class with
 * {@link java.lang.invoke.MethodHandles.Lookup#defineClass(byte[])
 * Lookup.defineClass}.</td>
 *   <td>This grants code with a suitably privileged {@code Lookup} object
 * permission to define classes in the same package as the {@code Lookup}'s
 * lookup class. </td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">accessDeclaredMembers</th>
 *   <td>Access to the declared members of a class</td>
 *   <td>This grants code permission to query a class for its public,
 * protected, default (package) access, and private fields and/or
 * methods. Although the code would have
 * access to the private and protected field and method names, it would not
 * have access to the private/protected field data and would not be able
 * to invoke any private methods. Nevertheless, malicious code
 * may use this information to better aim an attack.
 * Additionally, it may invoke any public methods and/or access public fields
 * in the class.  This could be dangerous if
 * the code would normally not be able to invoke those methods and/or
 * access the fields  because
 * it can't cast the object to the class/interface with those methods
 * and fields.</td>
 * </tr>
 * <tr>
 *   <th scope="row">queuePrintJob</th>
 *   <td>Initiation of a print job request</td>
 *   <td>This could print sensitive information to a printer,
 * or simply waste paper.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getStackTrace</th>
 *   <td>Retrieval of the stack trace information of another thread.</td>
 *   <td>This allows retrieval of the stack trace information of
 * another thread.  This might allow malicious code to monitor the
 * execution of threads and discover vulnerabilities in applications.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getStackWalkerWithClassReference</th>
 *   <td>Get a stack walker that can retrieve stack frames with class reference.</td>
 *   <td>This allows retrieval of Class objects from stack walking.
 *   This might allow malicious code to access Class objects on the stack
 *   outside its own context.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setDefaultUncaughtExceptionHandler</th>
 *   <td>Setting the default handler to be used when a thread
 *   terminates abruptly due to an uncaught exception</td>
 *   <td>This allows an attacker to register a malicious
 *   uncaught exception handler that could interfere with termination
 *   of a thread</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">preferences</th>
 *   <td>Represents the permission required to get access to the
 *   java.util.prefs.Preferences implementations user or system root
 *   which in turn allows retrieval or update operations within the
 *   Preferences persistent backing store.) </td>
 *   <td>This permission allows the user to read from or write to the
 *   preferences backing store if the user running the code has
 *   sufficient OS privileges to read/write to that backing store.
 *   The actual backing store may reside within a traditional filesystem
 *   directory or within a registry depending on the platform OS</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">manageProcess</th>
 *   <td>Native process termination and information about processes
 *       {@link ProcessHandle}.</td>
 *   <td>Allows code to identify and terminate processes that it did not create.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">localeServiceProvider</th>
 *   <td>This {@code RuntimePermission} is required to be granted to
 *   classes which subclass and implement
 *   {@code java.util.spi.LocaleServiceProvider}. The permission is
 *   checked during invocation of the abstract base class constructor.
 *   This permission ensures trust in classes which implement this
 *   security-sensitive provider mechanism. </td>
 *   <td>See <a href= "../util/spi/LocaleServiceProvider.html">
 *   {@code java.util.spi.LocaleServiceProvider}</a> for more
 *   information.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">loggerFinder</th>
 *   <td>This {@code RuntimePermission} is required to be granted to
 *   classes which subclass or call methods on
 *   {@code java.lang.System.LoggerFinder}. The permission is
 *   checked during invocation of the abstract base class constructor, as
 *   well as on the invocation of its public methods.
 *   This permission ensures trust in classes which provide loggers
 *   to system classes.</td>
 *   <td>See {@link System.LoggerFinder java.lang.System.LoggerFinder}
 *   for more information.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">accessSystemModules</th>
 *   <td>Access system modules in the runtime image.</td>
 *   <td>This grants the permission to access resources in the
 *   {@linkplain ModuleFinder#ofSystem system modules} in the runtime image.</td>
 * </tr>
 *
 * </tbody>
 * </table>
 *
 * @implNote
 * Implementations may define additional target names, but should use naming
 * conventions such as reverse domain name notation to avoid name clashes.
 *
 * @see BasicPermission
 * @see Permission
 * @see Permissions
 * @see PermissionCollection
 * @see SecurityManager
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @since 1.2
 */

public final class RuntimePermission extends BasicPermission {

    @java.io.Serial
    private static final long serialVersionUID = 7399184964622342223L;

    /**
     * 指定された名前で新しい RuntimePermission を作成します。名前は RuntimePermission の記号名で、
     * 例えば「exit」や「setFactory」などです。アスタリスク（*）は、名前の末尾にピリオド（.）の後に付くか、単独で表示されることがあり、ワイルドカードマッチを意味します。
     *
     * @param name the name of the RuntimePermission.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */

    public RuntimePermission(String name)
    {
        super(name);
    }

    /**
     * 指定された名前で新しい RuntimePermission オブジェクトを作成します。
     * 名前は RuntimePermission の記号名であり、actions 文字列は現在使用されておらず、null にすべきです。
     *
     * @param name the name of the RuntimePermission.
     * @param actions should be null.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */

    public RuntimePermission(String name, String actions)
    {
        super(name, actions);
    }
}
