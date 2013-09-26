/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.ArrayList;
import java.util.List;

public class JvmClassName {

    @NotNull
    public static JvmClassName byInternalName(@NotNull String internalName) {
        return new JvmClassName(internalName);
    }

    @NotNull
    public static JvmClassName byType(@NotNull Type type) {
        if (type.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException("Type is not convertible to " + JvmClassName.class.getSimpleName() + ": " + type);
        }
        return byInternalName(type.getInternalName());
    }

    /**
     * WARNING: fq name cannot be uniquely mapped to JVM class name.
     */
    @NotNull
    public static JvmClassName byFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        JvmClassName r = new JvmClassName(fqNameToInternalName(fqName));
        r.fqName = fqName;
        return r;
    }

    @NotNull
    public static JvmClassName byFqNameWithoutInnerClasses(@NotNull String fqName) {
        return byFqNameWithoutInnerClasses(new FqName(fqName));
    }

    @NotNull
    public static JvmClassName byClass(@NotNull Class<?> klass) {
        return byFqNameWithoutInnerClasses(new FqName(klass.getCanonicalName()));
    }

    private static String encodeSpecialNames(String str) {
        String encodedObjectNames = StringUtil.replace(str, JvmAbi.CLASS_OBJECT_CLASS_NAME, CLASS_OBJECT_REPLACE_GUARD);
        return StringUtil.replace(encodedObjectNames, JvmAbi.TRAIT_IMPL_CLASS_NAME, TRAIT_IMPL_REPLACE_GUARD);
    }

    private static String decodeSpecialNames(String str) {
        String decodedObjectNames = StringUtil.replace(str, CLASS_OBJECT_REPLACE_GUARD, JvmAbi.CLASS_OBJECT_CLASS_NAME);
        return StringUtil.replace(decodedObjectNames, TRAIT_IMPL_REPLACE_GUARD, JvmAbi.TRAIT_IMPL_CLASS_NAME);
    }

    @NotNull
    private static JvmClassName byFqNameAndInnerClassList(@NotNull FqName fqName, @NotNull List<String> innerClassList) {
        String outerClassName = fqNameToInternalName(fqName);
        StringBuilder sb = new StringBuilder(outerClassName);
        for (String innerClassName : innerClassList) {
            sb.append("$").append(innerClassName);
        }
        return new JvmClassName(sb.toString());
    }

    @NotNull
    public static JvmClassName byClassDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        DeclarationDescriptor descriptor = classDescriptor;

        List<String> innerClassNames = new ArrayList<String>();
        while (descriptor.getContainingDeclaration() instanceof ClassDescriptor) {
            innerClassNames.add(descriptor.getName().asString());
            descriptor = descriptor.getContainingDeclaration();
            assert descriptor != null;
        }

        return byFqNameAndInnerClassList(DescriptorUtils.getFQName(descriptor).toSafe(), innerClassNames);
    }

    @NotNull
    private static String fqNameToInternalName(@NotNull FqName fqName) {
        return fqName.asString().replace('.', '/');
    }

    @NotNull
    private static String internalNameToFqName(@NotNull String name) {
        return decodeSpecialNames(encodeSpecialNames(name).replace('$', '.').replace('/', '.'));
    }

    private final static String CLASS_OBJECT_REPLACE_GUARD = "<class_object>";
    private final static String TRAIT_IMPL_REPLACE_GUARD = "<trait_impl>";

    // Internal name:  jet/Map$Entry
    // FqName:         jet.Map.Entry

    private final String internalName;
    private FqName fqName;
    private String descriptor;
    private Type asmType;

    private JvmClassName(@NotNull String internalName) {
        this.internalName = internalName;
    }

    @NotNull
    public FqName getFqName() {
        if (fqName == null) {
            fqName = new FqName(internalNameToFqName(internalName));
        }
        return fqName;
    }

    @NotNull
    public String getInternalName() {
        return internalName;
    }

    @NotNull
    public String getDescriptor() {
        if (descriptor == null) {
            descriptor = "L" + internalName + ';';
        }
        return descriptor;
    }

    @NotNull
    public Type getAsmType() {
        if (asmType == null) {
            asmType = Type.getType(getDescriptor());
        }
        return asmType;
    }

    @Override
    public String toString() {
        return getInternalName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return internalName.equals(((JvmClassName) o).internalName);
    }

    @Override
    public int hashCode() {
        return internalName.hashCode();
    }
}
