package com.edgedb.codegen.generator.types;

import com.edgedb.codegen.generator.GeneratorContext;
import com.edgedb.codegen.generator.GeneratorTargetInfo;
import com.edgedb.codegen.utils.GenerationUtils;
import com.edgedb.codegen.utils.TextUtils;
import com.edgedb.driver.annotations.EdgeDBName;
import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.binary.codecs.scalars.ScalarCodec;
import com.edgedb.driver.binary.protocol.common.Cardinality;
import com.edgedb.driver.datatypes.NullableOptional;
import com.edgedb.driver.datatypes.Range;
import com.edgedb.driver.namingstrategies.NamingStrategy;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class V2TypeGenerator implements TypeGenerator {
    private static abstract class ObjectGenerationInfo {
        public final String typeName;
        public final Map<ObjectCodec.ObjectProperty, TypeName> properties;
        public @Nullable Path filePath;

        public @Nullable ObjectGenerationInfo parent;

        public ObjectGenerationInfo(
                String typeName, Map<ObjectCodec.ObjectProperty, TypeName> properties,
                Function<ObjectGenerationInfo, Path> filePathProvider
        ) {
            this(typeName, properties, filePathProvider,null);
        }
        public ObjectGenerationInfo(
                String typeName, Map<ObjectCodec.ObjectProperty, TypeName> properties,
                Function<ObjectGenerationInfo, Path> filePathProvider, @Nullable ObjectGenerationInfo parent
        ) {
            this.typeName = typeName;
            this.properties = properties;
            this.parent = parent;
            this.filePath = filePathProvider.apply(this);
        }

        public abstract String getTargetFilePath();

        public abstract TypeName getTypeName();

        public String getCleanTypeName() {
            return TextUtils.nameWithoutModule(typeName);
        }
    }

    private static final class ClassGenerationInfo extends ObjectGenerationInfo {
        public final Path edgeQLSourceFile;
        public final ObjectCodec codec;

        public final TypeSpec.Builder typeSpec;
        public final Collection<FieldSpec> fields;

        private final TypeName jTypeName;

        public ClassGenerationInfo(
                String typeName, ObjectCodec codec, Map<ObjectCodec.ObjectProperty, TypeName> properties,
                Function<ObjectGenerationInfo, Path> filePathProvider, Path edgeqlSourceFile, TypeName jTypeName,
                @Nullable ObjectGenerationInfo parent
        ) {
            super(typeName, properties, filePathProvider, parent);
            this.jTypeName = jTypeName;
            this.edgeQLSourceFile = edgeqlSourceFile;
            this.codec = codec;

            this.fields = properties.entrySet().stream()
                    .map(v ->
                            FieldSpec.builder(
                                            applyPropertyCardinality(v.getKey(), v.getValue()),
                                            NamingStrategy.camelCase().convert(v.getKey().name),
                                            Modifier.FINAL, Modifier.PUBLIC
                                    )
                                    .addAnnotation(AnnotationSpec.builder(EdgeDBName.class)
                                            .addMember("value", CodeBlock.of("$S", v.getKey().name)).build()
                                    ).addJavadoc(
                                            "The {@code $L} field on the {@code $L} object",
                                            v.getKey().name,
                                            codec.metadata == null ? typeName : codec.metadata.schemaName
                                    ).build()
                    ).collect(Collectors.toList());

            this.typeSpec = GenerationUtils.generateDataClassBuilder(this.fields, typeName, false);
        }

        @Override
        public String getTargetFilePath() {
            return Path.of("results", getCleanTypeName() + ".java").toString();
        }

        @Override
        public TypeName getTypeName() {
            return this.jTypeName;
        }
    }

    private static final class InterfaceGenerationInfo extends ObjectGenerationInfo {
        private final TypeName jTypeName;
        private final Map<ObjectCodec.ObjectProperty, Boolean> optionalProperties;
        public InterfaceGenerationInfo(
                String typeName, Map<ObjectCodec.ObjectProperty, TypeName> properties,
                Map<ObjectCodec.ObjectProperty, Boolean> optionalProperties,
                Function<ObjectGenerationInfo, Path> filePathProvider, TypeName jTypeName,
                @Nullable ObjectGenerationInfo parent
        ) {
            super(typeName, properties, filePathProvider, parent);
            this.jTypeName = jTypeName;
            this.optionalProperties = optionalProperties;
        }

        public boolean isOptionalProperty(ObjectCodec.ObjectProperty property) {
            if(!optionalProperties.containsKey(property)) {
                throw new IllegalArgumentException("property \"" + property.name + "\" is not part of the supplied interface");
            }

            return optionalProperties.get(property);
        }

        @Override
        public String getTargetFilePath() {
            return Path.of("interfaces", getCleanTypeName() + ".java").toString();
        }

        @Override
        public TypeName getTypeName() {
            return this.jTypeName;
        }
    }

    private final ParserMap parsers = new ParserMap() {{
        define(SparseObjectCodec.class, V2TypeGenerator.this::parseSparseObjectCodec);
        define(ObjectCodec.class, V2TypeGenerator.this::parseObjectCodec);
        define(TupleCodec.class, V2TypeGenerator.this::parseTupleCodec);
        define(ArrayCodec.class, V2TypeGenerator.this::parseArrayCodec);
        define(RangeCodec.class, V2TypeGenerator.this::parseRangeCodec);
        define(SetCodec.class, V2TypeGenerator.this::parseSetCodec);
        define(ScalarCodec.class, V2TypeGenerator.this::parseScalarCodec);
        define(NullCodec.class, V2TypeGenerator.this::parseNullCodec);
    }};

    private final Map<UUID, List<ClassGenerationInfo>> resultShapes;
    private final List<ObjectGenerationInfo> generatedTypes;

    public V2TypeGenerator() {
        resultShapes = new HashMap<>();
        generatedTypes = new ArrayList<>();
    }

    private Optional<ObjectGenerationInfo> getGeneratedType(TypeName typeName) {
        for (var generatedType : generatedTypes) {
            if(generatedType.getTypeName().equals(typeName)) {
                return Optional.of(generatedType);
            }
        }

        return Optional.empty();
    }

    public Collection<GeneratedFileInfo> getGeneratedFiles() {
        return this.generatedTypes.stream().map(v -> {
            assert v.filePath != null;

            if(v instanceof ClassGenerationInfo) {
                var cgi = (ClassGenerationInfo)v;

                return new GeneratedFileInfo(cgi.filePath, List.of(cgi.edgeQLSourceFile));
            } else if (v instanceof InterfaceGenerationInfo) {
                var igi = (InterfaceGenerationInfo)v;
                return new GeneratedFileInfo(igi.filePath, Collections.emptyList());
            } else {
                throw new IllegalArgumentException("Unknown generation type " + v);
            }
        }).collect(Collectors.toList());
    }

    public void removeGeneratedReference(Collection<GeneratedFileInfo> references) {
        for (var item : references) {
            generatedTypes.removeIf(v -> v.filePath == item.getGeneratedPath());
        }
    }

    @Override
    public TypeName getType(Codec<?> codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        var result = parsers.parse(codec, target, context);

        if(result != null) {
            return result;
        }

        throw new IllegalArgumentException("Unknown parse path for codec " + codec);
    }

    private TypeName parseNullCodec(NullCodec nullCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return TypeName.OBJECT;
    }

    private TypeName parseScalarCodec(ScalarCodec<?> scalarCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return TypeName.get(scalarCodec.getConvertingClass());
    }

    private TypeName parseSetCodec(SetCodec<?> setCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return ParameterizedTypeName.get(ClassName.get(List.class), getType(setCodec.innerCodec, target, context));
    }

    private TypeName parseRangeCodec(RangeCodec<?> rangeCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return ParameterizedTypeName.get(ClassName.get(Range.class), getType(rangeCodec.innerCodec, target, context));
    }

    private TypeName parseArrayCodec(ArrayCodec<?> arrayCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return ArrayTypeName.of(getType(arrayCodec.innerCodec, target, context));
    }

    private TypeName parseTupleCodec(TupleCodec tupleCodec, GeneratorTargetInfo target, GeneratorContext context) {
        return ClassName.get("com.edgedb.driver.datatypes", "Tuple");
    }

    private TypeName parseSparseObjectCodec(SparseObjectCodec codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        throw new UnsupportedOperationException("Cannot parse sparse object codec");
    }

    private TypeName parseObjectCodec(ObjectCodec codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) throws IOException {
        var fields = Arrays.stream(codec.elements)
                .map(x -> Map.entry(x, getType(x.codec, target, context)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var typeName = getTypeName(codec, null, target, context);
        var info = new ClassGenerationInfo(
                typeName,
                codec, fields, t -> context.outputDirectory.resolve(t.getTargetFilePath()),
                target != null ? target.path : null,
                ClassName.get(context.packageName + ".results", typeName), null
        );

        generatedTypes.add(info);

        resultShapes.putIfAbsent(codec.typeId, new ArrayList<>());
        resultShapes.get(codec.typeId).add(info);

        return info.getTypeName();
    }

    private String getTypeName(ObjectCodec codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        return getTypeName(codec, null, target, context);
    }

    private String getTypeName(ObjectCodec codec, @Nullable String name, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        var prefix = target != null
                ? target.filename
                : "";

        if(name != null) {
            return prefix + name + (codec.metadata != null ? TextUtils.nameWithoutModule(codec.metadata.schemaName) : "Result");
        } else {
            return prefix += codec.metadata != null ? TextUtils.nameWithoutModule(codec.metadata.schemaName) : "Result";
        }
    }

    @Override
    public void postProcess(GeneratorContext context) throws IOException {
        if(!resultShapes.isEmpty()) {
            Files.createDirectories(context.outputDirectory.resolve("results"));
        }

        var groups = resultShapes.entrySet().stream()
                .flatMap(v -> v.getValue().stream())
                .collect(Collectors.groupingBy(v -> v.codec.metadata == null ? v.codec.typeId.toString() : v.codec.metadata.schemaName));

        var interfaces = new HashMap<Map.Entry<String, InterfaceGenerationInfo>, List<ClassGenerationInfo>>();

        for(var group : groups.entrySet()) {
            var interfaceName = TextUtils.nameWithoutModule(group.getKey());
            interfaces.put(Map.entry(group.getKey(), getAndApplyInterfaceInfo(interfaceName, group.getValue(), context)), group.getValue());
        }

        if(!interfaces.isEmpty()) {
            Files.createDirectories(context.outputDirectory.resolve("interfaces"));
        }

        for(var iface : interfaces.entrySet()) {
            var interfaceGenerationResult = generateInterface(
                    iface.getKey().getValue(),
                    iface.getKey().getKey(),
                    iface.getValue(),
                    context,
                    iface.getValue().stream().map(ObjectGenerationInfo::getTypeName).collect(Collectors.toList()),
                    v -> iface.getValue().stream()
                            .filter(t -> t.properties.entrySet().stream().anyMatch(u -> u.getKey().name.equals(v)))
                            .filter(GenerationUtils.distinctByKey(ObjectGenerationInfo::getCleanTypeName))
                            .map(ObjectGenerationInfo::getTypeName)
                            .collect(Collectors.toList())
            );

            for(var type : iface.getValue()) {
                type.typeSpec.addSuperinterface(interfaceGenerationResult.name);

                for (var interfaceElement : interfaceGenerationResult.members) {
                    var field = type.fields.stream().filter(v -> v.name.equals(interfaceElement.cleanName)).findFirst();

                    var methodSpec = MethodSpec.methodBuilder(interfaceElement.getMethod.name)
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .returns(interfaceElement.getMethod.returnType);

                    if(field.isPresent()) {
                        if(!interfaceElement.isOptional) {
                            methodSpec.addJavadoc("Returns the {@code $L} field of this class", field.get().name);
                            methodSpec.addCode("return this.$N;", field.get());
                        } else {
                            var optionalType = interfaceElement.property.cardinality == Cardinality.AT_MOST_ONE
                                    ? NullableOptional.class
                                    : Optional.class;
                            methodSpec.addCode("return $T.of(this.$N);", optionalType, field.get());
                            methodSpec.addJavadoc(
                                    "Returns an optional wrapping the {@code $L} field, which is always present on " +
                                            "this type.",
                                    field.get().name
                            );
                        }
                    } else {
                        methodSpec.addCode("return $T.empty();", Optional.class);
                        methodSpec.addJavadoc("Returns an optional whose value isn't present on the current class");
                    }

                    type.typeSpec.addMethod(methodSpec.build());
                }

                var jFile = JavaFile.builder(context.packageName + ".results", type.typeSpec.build()).build();

                try(var writer = Files.newBufferedWriter(context.outputDirectory.resolve(Path.of("results", type.typeName + ".java")))) {
                    jFile.writeTo(writer);
                    writer.flush();
                }
            }
        }
    }

    private static class InterfaceGenerationResult {
        private static class InterfaceMember {
            public final TypeName typeName;
            public final String cleanName;
            public final MethodSpec getMethod;
            public final ObjectCodec.ObjectProperty property;
            public final boolean isOptional;

            private InterfaceMember(TypeName typeName, String cleanName, MethodSpec getMethod, ObjectCodec.ObjectProperty property, boolean isOptional) {
                this.typeName = typeName;
                this.cleanName = cleanName;
                this.getMethod = getMethod;
                this.property = property;
                this.isOptional = isOptional;
            }
        }
        public final TypeName name;
        public final Collection<InterfaceMember> members;

        private InterfaceGenerationResult(TypeName name, Collection<InterfaceMember> members) {
            this.name = name;
            this.members = members;
        }
    }

    private InterfaceGenerationInfo getAndApplyInterfaceInfo(
            String name, Collection<ClassGenerationInfo> subTypes, GeneratorContext context
    ) {
        var props = subTypes.stream().flatMap(v ->
                        v.properties.entrySet().stream().map(t ->
                                Map.entry(t.getValue(), Arrays.stream(v.codec.elements).filter(u -> u.name.equals(t.getKey().name)).findFirst().get())
                        ))
                .filter(GenerationUtils.distinctByKey(v -> v.getValue().name))
                .collect(Collectors.toMap(
                        v -> v,
                        v -> subTypes.stream()
                                .allMatch(t -> Arrays.stream(t.codec.elements)
                                        .anyMatch(u ->
                                                u.name.equals(v.getValue().name) &&
                                                        u.cardinality == v.getValue().cardinality
                                        )
                                )
                ));


        var interfaceInfo = new InterfaceGenerationInfo(
                name,
                props.entrySet().stream().collect(Collectors.toMap(v -> v.getKey().getValue(), v -> v.getKey().getKey())),
                props.entrySet().stream().collect(Collectors.toMap(v -> v.getKey().getValue(), v -> !v.getValue())),
                v -> context.outputDirectory.resolve(v.getTargetFilePath()),
                ClassName.get(context.packageName + ".interfaces", name),
                null
        );

        subTypes.forEach(v -> v.parent = interfaceInfo);

        return interfaceInfo;
    }

    private InterfaceGenerationResult generateInterface(
            InterfaceGenerationInfo interfaceInfo, String schemaName,
            Collection<ClassGenerationInfo> subTypes, GeneratorContext context,
            Collection<TypeName> descendants, Function<String, Collection<TypeName>> getAvailability
    ) throws IOException {
        generatedTypes.add(interfaceInfo);

        var ifaceBuilder = TypeSpec.interfaceBuilder(interfaceInfo.typeName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(CodeBlock.builder()
                        .add("Represents the schema type {@code $L} with properties that are shared\n", schemaName)
                        .add(" across the following types:\n" +  descendants.stream()
                                .map(v -> "{@linkplain $T}")
                                .collect(Collectors.joining("\n")), descendants.toArray())
                        .build()
                );

        var methodSpecMap = new HashMap<ObjectCodec.ObjectProperty, MethodSpec>();

        for (var prop : interfaceInfo.properties.entrySet()) {
            var availableDescendants = getAvailability.apply(prop.getKey().name);
            var isOptional = interfaceInfo.isOptionalProperty(prop.getKey());
            var availability = !isOptional
                    ? "all descendants of this interface."
                    : availableDescendants.stream()
                            .map(v -> "{@linkplain $T}")
                            .collect(Collectors.joining(", "));

            var propertyType = getInterfacePropertyType(interfaceInfo, prop.getKey(), prop.getValue());

            var method = MethodSpec.methodBuilder("get" + NamingStrategy.pascalCase().convert(prop.getKey().name))
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .returns(propertyType)
                    .addJavadoc(CodeBlock.builder()
                            .add("Gets the {@code $L}", prop.getKey().name)
                            .add(" property, available on " + availability, !isOptional ? new Object[0] : availableDescendants.toArray() )
                            .build())
                    .build();

            ifaceBuilder.addMethod(method);
            methodSpecMap.put(prop.getKey(), method);
        }

        var jFile = JavaFile.builder(context.packageName + ".interfaces", ifaceBuilder.build()).build();

        try(var writer = Files.newBufferedWriter(context.outputDirectory.resolve(Path.of("interfaces", interfaceInfo.typeName + ".java")))) {
            jFile.writeTo(writer);
            writer.flush();
        }

        return new InterfaceGenerationResult(
                interfaceInfo.getTypeName(),
                interfaceInfo.properties.entrySet().stream().map(v -> new InterfaceGenerationResult.InterfaceMember(
                        v.getValue(),
                        NamingStrategy.camelCase().convert(v.getKey().name),
                        methodSpecMap.get(v.getKey()),
                        v.getKey(),
                        interfaceInfo.isOptionalProperty(v.getKey())
                )).collect(Collectors.toList())
        );
    }

    private final TypeName getInterfacePropertyType(InterfaceGenerationInfo info, ObjectCodec.ObjectProperty property, TypeName typeName) {
        var elementType = typeName;

        var generated = getGeneratedType(typeName);

        if(generated.isPresent()) {
            var element = generated.get();

            while(element.parent != null) {
                element = element.parent;
            }

            elementType = element.getTypeName();
        }

        elementType = applyPropertyCardinality(property, elementType);

        if(info.isOptionalProperty(property))  {
            var optionalType = property.cardinality == Cardinality.AT_MOST_ONE //elementType.annotations.stream().anyMatch(v -> v.type.equals(ClassName.get(Nullable.class)))
                    ? NullableOptional.class
                    : Optional.class;

            return ParameterizedTypeName.get(ClassName.get(optionalType), elementType);
        }

        return elementType;
    }

    private static TypeName applyPropertyCardinality(ObjectCodec.ObjectProperty property, TypeName type) {
        if(property.cardinality != null) {
            switch (property.cardinality) {
                case AT_MOST_ONE:
                    return type.annotated(AnnotationSpec.builder(Nullable.class).build());
                case MANY:
                case AT_LEAST_ONE:
                    return ParameterizedTypeName.get(ClassName.get(Collection.class), type);
            }
        }

        return type;
    }
}
