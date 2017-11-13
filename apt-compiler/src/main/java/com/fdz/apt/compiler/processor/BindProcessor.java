package com.fdz.apt.compiler.processor;

import com.fdz.apt.annotation.annotation.BindActivity;
import com.fdz.apt.annotation.annotation.BindView;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * @author : fangguiliang
 * @version : 1.0.0
 * @since : 2017/11/6
 */

@AutoService(Processor.class)
public class BindProcessor extends AbstractProcessor {
    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(BindActivity.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
    }


    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        processBindView(roundEnv);
        return false;
    }

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
}
