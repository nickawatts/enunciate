/*
 * Copyright 2006-2008 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webcohesion.enunciate.modules.jackson.model;

import com.webcohesion.enunciate.EnunciateException;
import com.webcohesion.enunciate.javac.decorations.type.DecoratedTypeMirror;
import com.webcohesion.enunciate.javac.decorations.type.TypeMirrorUtils;
import com.webcohesion.enunciate.metadata.qname.XmlQNameEnum;
import com.webcohesion.enunciate.metadata.qname.XmlQNameEnumValue;
import com.webcohesion.enunciate.metadata.qname.XmlUnknownQNameEnumValue;
import com.webcohesion.enunciate.modules.jackson.EnunciateJacksonContext;
import com.webcohesion.enunciate.modules.jackson.model.types.KnownJsonType;
import com.webcohesion.enunciate.modules.jackson.model.types.JsonType;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A qname enum type definition.
 *
 * @author Ryan Heaton
 */
public class QNameEnumTypeDefinition extends EnumTypeDefinition {

  private final String namespace;
  private final XmlQNameEnum.BaseType baseType;

  public QNameEnumTypeDefinition(TypeElement delegate, EnunciateJacksonContext context) {
    super(delegate, context);

    XmlQNameEnum xmlQNameEnum = getAnnotation(XmlQNameEnum.class);
    if (xmlQNameEnum == null) {
      throw new IllegalArgumentException(delegate.getQualifiedName() + " is not a qname enum (not annotated with @com.webcohesion.enunciate.metadata.qname.XmlQNameEnum)");
    }

    String namespace = getPackage().getNamespace();
    if (!"##default".equals(xmlQNameEnum.namespace())) {
      namespace = xmlQNameEnum.namespace();
    }
    this.namespace = namespace;

    this.baseType = xmlQNameEnum.base();
  }

  @Override
  protected Map<String, Object> loadEnumValues() {
    String namespace = getPackage().getNamespace();
    XmlQNameEnum xmlQNameEnum = getAnnotation(XmlQNameEnum.class);
    if (xmlQNameEnum != null && !"##default".equals(xmlQNameEnum.namespace())) {
      namespace = xmlQNameEnum.namespace();
    }
    if (namespace == null) {
      namespace = "";
    }

    List<VariableElement> enumConstants = getEnumConstants();
    Map<String, Object> enumValueMap = new LinkedHashMap<String, Object>();
    HashSet<QName> enumValues = new HashSet<QName>(enumConstants.size());
    String unknownQNameConstant = null;
    for (VariableElement enumConstant : enumConstants) {
      XmlUnknownQNameEnumValue unknownQNameEnumValue = enumConstant.getAnnotation(XmlUnknownQNameEnumValue.class);
      if (unknownQNameEnumValue != null) {
        if (unknownQNameConstant != null) {
          throw new EnunciateException(getQualifiedName() + ": no more than two constants can be annotated with @XmlUnknownQNameEnumValue.");
        }

        unknownQNameConstant = enumConstant.getSimpleName().toString();
        continue;
      }

      String ns = namespace;
      String localPart = enumConstant.getSimpleName().toString();
      XmlQNameEnumValue enumValueInfo = enumConstant.getAnnotation(XmlQNameEnumValue.class);
      if (enumValueInfo != null) {
        if (enumValueInfo.exclude()) {
          continue;
        }

        if (!"##default".equals(enumValueInfo.namespace())) {
          ns = enumValueInfo.namespace();
        }
        if (!"##default".equals(enumValueInfo.localPart())) {
          localPart = enumValueInfo.localPart();
        }
      }

      QName qname = new QName(ns, localPart);
      if (!enumValues.add(qname)) {
        throw new EnunciateException(getQualifiedName() + ": duplicate qname enum value: " + qname);
      }

      enumValueMap.put(enumConstant.getSimpleName().toString(), qname);
    }

    if (unknownQNameConstant != null) {
      //enter the unknown qname constant as a null qname.
      enumValueMap.put(unknownQNameConstant, null);
    }
    
    return enumValueMap;
  }

  // Inherited.
  @Override
  public JsonType getBaseType() {
    return isUriBaseType() ? KnownJsonType.ANY_URI : KnownJsonType.QNAME;
  }

  public boolean isUriBaseType() {
    return this.baseType == XmlQNameEnum.BaseType.URI;
  }

  @Override
  public DecoratedTypeMirror getEnumBaseClass() {
    return isUriBaseType() ?
      TypeMirrorUtils.mirrorOf(URI.class, this.env) :
      TypeMirrorUtils.mirrorOf(QName.class, this.env);
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return true;
  }

  public boolean isQNameEnum() {
    return true;
  }

}