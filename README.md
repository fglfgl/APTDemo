## APT
- APT（Annotation Processing Tool）[`Github`](https://github.com/square/javapoet) 实际上就是通过注解，在编译时自动生成代码
- 如果你用过 [ButterKnife](https://github.com/JakeWharton/butterknife) ，下面就是运用APT写的入入入门版的而已
- DEMO最后功能：通过注解绑定View，即实现findViewById
- DEMO使用了Android Studio 是3.0的导包方式，会兼容不了低版本的
- 只讲使用，你需要先了解 `Annotation`、`Annotation Processor`、`auto-service`

## 创建Demo工程 `APTDemo`
- 配置APTDemo `build.gradle`

```
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.0.0'
        // 引入APT
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```
- Demo分离的处理器和注解，主要结构如下：

```
APTDemo
|--app :最终Demo
|--apt-annotation :注解Java Library
|--apt-compiler :将来生成代码的Java Library
```

#### 一、Module `apt-annotation`
创建一个Java Library 工程`apt-annotation`（注意是Java Library，不要Android Library，因为不需要用到Android特有的东西）。工程主要存放提供给其他项目或者Module用的Annotation。

- apt-annotation `build.gradle` 配置

```
apply plugin: 'java'

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])
}

compileJava {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```
- 创建`BindActivity` 、`BindView` 两个注解类：

```
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface BindActivity {
}
```

```
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BindView {
    /**
     * @return 返回View的Id
     */
    int value();
}
```

#### 二、Module `apt-compiler`
1. [auto-service](https://github.com/google/auto/tree/master/service) : 谷歌提供的插件，方便注解 processor 类，自定义processor类时要用到`@AutoService(Processor.class)`
2. [javapoet](https://github.com/square/javapoet)  : 帮助我们通过类调用的形式来生成代码。
- apt-compiler `build.gradle` 配置

```
apply plugin: 'java'

compileJava {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation project(':apt-annotation')
    implementation 'com.google.auto.service:auto-service:1.0-rc3'
    implementation 'com.squareup:javapoet:1.9.0'
}
```
- 定义BindProcessor.java 继承`AbstractProcessor`，并使用`AutoService`注解。

```
@AutoService(Processor.class)
public class BindProcessor extends AbstractProcessor {
   /**
     * 可以使用注解@SupportedAnnotationTypes，添加在类上，但建议用重写的方式
     * @return 返回这个注解器支持的注解类型完整路径
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return super.getSupportedAnnotationTypes();
    }

    /**
     * 可以使用注解@SupportedSourceVersion，添加在类上，但建议用重写的方式
     * @return 指定使用的Java版本,通常返回SourceVersion.latestSupported()
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 初始化方法会被注解处理工具调用，并输入processingEnvironment参数。
     * processingEnvironment提供很多有用的工具类如Elements, Types和Filer等。
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
    }

    /**
     * 这相当于每个处理器的主函数，主要的逻辑处理都在这里，扫描、评估和处理注解的代码，
     * 通过Javapoet生成Java文件都在这个方法下处理
     * @param set 输入参数annotations 请求处理的注解类型集合
     * @param roundEnvironment 可以让你查询出包含特定注解的被注解元素。
     * @return 如果返回 true，则这些注解已声明并且不要求后续 Processor 处理它们；如果返回 false，则这些注解未声明并且可能要求后续 Processor 处理它们
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        progressBindView(roundEnvironment);
        return false;
    }
}
```
- `progressBindView(...)`方法用到了[`javapoet`](https://github.com/square/javapoet)的东西，后面会简单介绍。

##### Element
Element是处理器中`process(...)`方法的核心对象，所以我们通过Element来了解注解处理器。Element代表语言元素，如：包、类、方法等。

- `Element.java` 源码：

```
public interface Element extends AnnotatedConstruct {
      /**
     * @return 返回此元素定义实际的Java类型
     */
    TypeMirror asType();

    /**
     * @return 返回此元素的种类：包、类、接口、方法、字段,返回值是枚举
     */
    ElementKind getKind();

    /**
     * @return 返回此元素的修饰符
     */
    Set<Modifier> getModifiers();

    /**
     * @return 返回此元素的类名
     */
    Name getSimpleName();

    /**
     * @return 返回封装此元素的最近的外层元素，
     * 如果此元素的声明在词法上直接封装在另一个元素的声明中，则返回那个封装元素，
     * 如果此元素是顶层类型，则返回它的包，
     * 如果此元素是一个包或者泛型参数，则返回 null。
     */
    Element getEnclosingElement();

    /**
     * @return 返回此元素的内层元素
     */
    List<? extends Element> getEnclosedElements();

    /**
     * @return 返回直接存在于此元素上的注释
     */
    List<? extends AnnotationMirror> getAnnotationMirrors();

    /**
     * @return 返回此元素针对指定类型的注解, 没有则返回 null。注解可以是继承的，也可以是直接存在于此元素上的
     */
    <A extends Annotation> A getAnnotation(Class<A> var1);

    /**
     * 将一个 visitor 应用到此元素。
     * @return
     */
    <R, P> R accept(ElementVisitor<R, P> var1, P var2);
}
```
- Element 转化
Element代表元素的种类：包、类、接口、方法、字段,返回值是枚举，但是Element并没有包含自身的信息，需要通过`asType()`来获取：

```
TypeMirror mirror = element.asType();

// mirror类型是DeclaredType或者TypeVariable时候可以转化成Element
Types types = processingEnvironment.getTypeUtils()
Element element = types.asElement(mirror)
```
- 判断和获取类型`getKind()`，不要使用`instanceof`，因为`getKind()`可以取到具体的类类型，如：枚举、接口、注解等。

- Element关键子类

类 | 描述
--------- | -------------
PackageElement | 一个包程序元素
TypeElement | 类或接口程序元素
ExecutableElement | 类或接口的方法、构造方法或初始化程序（静态或实例），包括注解类型元素
TypeParameterElement | 一般类、接口、方法或构造方法元素的泛型参数
VariableElement | 一个字段、enum 常量、方法或构造方法参数、局部变量或异常参数

还记得`Annotation`的`@target`吗，实际上上面的类就是`ElementType`下对应的类型：

类 | 对应ElementType类型（作用域）
--------- | -------------
PackageElement | ElementType.PACKAGE
TypeElement | ElementType.TYPE
ExecutableElement | ElementType.METHOD
TypeParameterElement、VariableElement | ElementType.PARAMETER,ElementType.FIELD,ElementType.LOCAL_VARIABLE

- `processBindview`方法

```
 private void processBindView(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(BindActivity.class);
        for (Element element : elements) {
            // 判断类型
            if (element.getKind() != ElementKind.CLASS) {
                return;
            }
            // 作用在类上所以转成 TypeElement
            TypeElement typeElement = (TypeElement) element;
            // 静态方法，传入BindActivity注解的Activity作为参数，名称为：activity
            String paramActivityName = "activity";
            MethodSpec.Builder builder = MethodSpec.methodBuilder("bindView")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addParameter(ClassName.get(typeElement.asType()), paramActivityName);
            // 获取该元素的内层注解
            List<? extends Element> members = elementUtils.getAllMembers(typeElement);
            for (Element item : members) {
                // 属性名称
                String name = item.getSimpleName().toString();
                // 属性的类名
                String type = ClassName.get(item.asType()).toString();
                BindView bindView = item.getAnnotation(BindView.class);
                if (bindView != null) {
                    String statement = String.format("activity.%s = (%s) %s.findViewById(%s)", name, type, paramActivityName, bindView.value());
                    builder.addStatement(statement);
                }
            }
            // 生成一个类，并添加上面生成的方法
            TypeSpec typeSpec = TypeSpec.classBuilder("APT" + element.getSimpleName())
                    .superclass(TypeName.get(typeElement.asType()))
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(builder.build())
                    .build();
            String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
```
- 在`app`Module 下`MainActivity.java`使用：

```
@BindActivity
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_hello_world)
    TextView tvHelloWorld;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        APTMainActivity.bindView(this);
    }
}
```
- `Build`-->`Rebuild Project` 编译生成代码，可以在`app`Module下看到,`app/build/generated/source/apt/debug/...`

```
package com.fdz.aptdemo;

public final class APTMainActivity extends MainActivity {
  public static void bindView(MainActivity activity) {
    activity.tvHelloWorld = (android.widget.TextView) activity.findViewById(2131165303);
  }
}
```
#### 最后
到这里，应该知道生成代码是怎样一个流程，希望可以对你有用，当然`Hello World`级别的代码还是自己敲一敲，附上源码：[Github](https://github.com/fglfgl/APTDemo)


