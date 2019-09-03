package com.eventbus.complier;

import com.eventbus.annotation.EventBeans;
import com.eventbus.annotation.Subscribe;
import com.eventbus.annotation.SubscriberInfo;
import com.eventbus.annotation.SubscriberMethod;
import com.eventbus.annotation.ThreadMode;
import com.eventbus.complier.utils.Constants;
import com.eventbus.complier.utils.EmptyUtils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedAnnotationTypes(Constants.SUBSCRIBE_ANNOTATION_TYPES)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions({Constants.PACKAGE_NAME, Constants.CLASS_NAME})
public class SubscribeProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;
    private Filer filer;
    private String packageName;
    private String className;
    private final Map<TypeElement, List<ExecutableElement>> methodByClass = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();

        Map<String, String> options = processingEnvironment.getOptions();
        if (!EmptyUtils.isEmpty(options)) {
            packageName = options.get(Constants.PACKAGE_NAME);
            className = options.get(Constants.CLASS_NAME);
            messager.printMessage(Diagnostic.Kind.NOTE, "packageName>>>" + packageName
                    + "className>>>" + className);
        }

        if (EmptyUtils.isEmpty(packageName) || EmptyUtils.isEmpty(className)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "注解处理器需要的参数为空，请在对应build.gradle配置参数");
        }

    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (!EmptyUtils.isEmpty(set)) {
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Subscribe.class);

            if (!EmptyUtils.isEmpty(elements)) {
                //解析元素
                try {
                    parseElements(elements);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    //解析所有被@Subscribe注解的类元素集合
    private void parseElements(Set<? extends Element> elements) throws Exception {
        for (Element element : elements) {
            if (element.getKind() != ElementKind.METHOD) {
                messager.printMessage(Diagnostic.Kind.ERROR, "仅解析@Subscribe作用再方法上的元素", element);
                return;
            }
            //强转方法元素
            ExecutableElement method = (ExecutableElement) element;
            //检查方法，条件，订阅方法必须是非静态的，公开的参数只能用一个
            if (checkHasNoErrors(method)) {
                //获取封装订阅方法的类
                TypeElement classElement = (TypeElement) method.getEnclosingElement();
                List<ExecutableElement> methods = methodByClass.get(classElement);
                if (methods == null) {
                    methods = new ArrayList<>();
                    methodByClass.put(classElement, methods);
                }
                methods.add(method);
                messager.printMessage(Diagnostic.Kind.NOTE, "遍历注解方法：" + classElement.getSimpleName() + method.getSimpleName());

            }
            messager.printMessage(Diagnostic.Kind.NOTE, "遍历注解方法：" + method.getSimpleName());
        }

        //通过Element工具类获取subscriberInfoIndex类型
        TypeElement subscriberIndexType = elementUtils.getTypeElement(Constants.SUBSCRIBEINFO_INDEX);
        //生成类文件
        createFile(subscriberIndexType);
    }

    private void createFile(TypeElement subscriberIndexType) throws IOException {
        //添加静态块代码SUBSCRIBER_INDEX = new HashMap<Class<?>,SubscriberInfo>();
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        codeBlock.addStatement("$N = new $T<$T,$T>()",
                Constants.FIELD_NAME,
                HashMap.class,
                Class.class,
                SubscriberInfo.class);

        //双层循环，第一层遍历被@Subscribe注解的方法所属类。第二层遍历每个类中所有的订阅方法
        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : methodByClass.entrySet()) {
            //此处不能使用codeBlock，会造成错误嵌套
            CodeBlock.Builder contentBlock = CodeBlock.builder();
            CodeBlock contentCode = null;
            String format;

            for (int i = 0; i < entry.getValue().size(); i++) {
                ExecutableElement element = entry.getValue().get(i);
                //获取每个方法上的@Subscribe注解中的注解值
                Subscribe subscribe = element.getAnnotation(Subscribe.class);
                //获取订阅事件方法的所有参数
                List<? extends VariableElement> parameters = element.getParameters();
                //订阅事件方法名
                String methodName = element.getSimpleName().toString();

                TypeElement parameterElement = (TypeElement) typeUtils.asElement(parameters.get(0).asType());
                //如果是最后一个添加，则无需逗号结尾
                if (i == entry.getValue().size() - 1) {
                    format = "new $T($T.class, $S, $T.class, $T.$L, $L, $L)";
                } else {
                    format = "new $T($T.class, $S, $T.class, $T.$L, $L, $L),\n";
                }
                // new SubscriberMethod(MainActivity.class, "abc", UserInfo.class, ThreadMode.POSTING, 0, false)
                contentCode = contentBlock.add(format,
                        SubscriberMethod.class,
                        ClassName.get(entry.getKey()),
                        methodName,
                        ClassName.get(parameterElement),
                        ThreadMode.class,
                        subscribe.threadMode(),
                        subscribe.priority(),
                        subscribe.sticky())
                        .build();
            }

            if (contentCode != null) {
                // putIndex(new EventBeans(MainActivity.class, new SubscriberMethod[] {)
                codeBlock.beginControlFlow("putIndex(new $T($T.class,new $T[]",
                        EventBeans.class,
                        ClassName.get(entry.getKey()),
                        SubscriberMethod.class)
                        .add(contentCode)
                        .endControlFlow("))");
            } else {
                messager.printMessage(Diagnostic.Kind.ERROR, "注解处理器双层循环发生错误！");
            }
        }

        //全局属性：Map<Class<?>,SubscriberMethod>
        TypeName fieldType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(Class.class),
                ClassName.get(SubscriberInfo.class)
        );

        //putIndex方法参数，putIndex(SubscriberInfo info)
        ParameterSpec putIndexParameter = ParameterSpec.builder(
                ClassName.get(SubscriberInfo.class),
                Constants.PUTINDEX_PARAMETER_NAME).build();

        // putIndex方法配置：private static void putIndex(SubscriberMethod info) {
        MethodSpec.Builder putIndexBuilder = MethodSpec
                .methodBuilder(Constants.PUTINDEX_METHOD_NAME)
                .addModifiers(Modifier.PRIVATE,Modifier.STATIC)
                .addParameter(putIndexParameter);

        // 不填returns默认void返回值

        // putIndex方法内容：SUBSCRIBER_INDEX.put(info.getSubscriberClass(), info);
        putIndexBuilder.addStatement("$N.put($N.getSubscriberClass(), $N)",
                Constants.FIELD_NAME,
                Constants.PUTINDEX_PARAMETER_NAME,
                Constants.PUTINDEX_PARAMETER_NAME);

        // getSubscriberInfo方法参数：Class subscriberClass
        ParameterSpec getSubscriberInfoParameter = ParameterSpec.builder(
                ClassName.get(Class.class),
                Constants.GETSUBSCRIBERINFO_PARAMETER_NAME)
                .build();

        // getSubscriberInfo方法配置：public SubscriberMethod getSubscriberInfo(Class<?> subscriberClass) {
        MethodSpec.Builder getSubscriberInfoBuidler = MethodSpec
                .methodBuilder(Constants.GETSUBSCRIBERINFO_METHOD_NAME) // 方法名
                .addAnnotation(Override.class) // 重写方法注解
                .addModifiers(Modifier.PUBLIC) // public修饰符
                .addParameter(getSubscriberInfoParameter) // 方法参数
                .returns(SubscriberInfo.class); // 方法返回值

        // getSubscriberInfo方法内容：return SUBSCRIBER_INDEX.get(subscriberClass);
        getSubscriberInfoBuidler.addStatement("return $N.get($N)",
                Constants.FIELD_NAME,
                Constants.GETSUBSCRIBERINFO_PARAMETER_NAME);

        // 构建类
        TypeSpec typeSpec = TypeSpec.classBuilder(className)
                // 实现SubscriberInfoIndex接口
                .addSuperinterface(ClassName.get(subscriberIndexType))
                // 该类的修饰符
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                // 添加静态块（很少用的api）
                .addStaticBlock(codeBlock.build())
                // 全局属性：private static final Map<Class<?>, SubscriberMethod> SUBSCRIBER_INDEX
                .addField(fieldType, Constants.FIELD_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                // 第一个方法：加入全局Map集合
                .addMethod(putIndexBuilder.build())
                // 第二个方法：通过订阅者对象（MainActivity.class）获取所有订阅方法
                .addMethod(getSubscriberInfoBuidler.build())
                .build();

        // 生成类文件：EventBusIndex
        JavaFile.builder(packageName, // 包名
                typeSpec) // 类构建完成
                .build() // JavaFile构建完成
                .writeTo(filer); // 文件生成器开始生成类文件


    }

    private boolean checkHasNoErrors(ExecutableElement element) {
        if (element.getModifiers().contains(Modifier.STATIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "订阅事件方法不能是static静态方法", element);
            return false;
        }

        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR, "订阅事件方法必须是public修饰的方法", element);
            return false;
        }

        List<? extends VariableElement> parameters = element.getParameters();
        if (parameters.size() != 1) {
            messager.printMessage(Diagnostic.Kind.ERROR, "订阅事件方法有且仅有一个参数", element);
            return false;
        }
        return true;
    }
}
