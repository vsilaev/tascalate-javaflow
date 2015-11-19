/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.javaflow.providers.asm3;

import org.apache.commons.javaflow.core.Continuable;
import org.apache.commons.javaflow.spi.ContinuableClassInfo;
import org.apache.commons.javaflow.spi.ContinuableClassInfoResolver;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * ContinuableClassVisitor
 * 
 * @author Evgueni Koulechov
 */
public class ContinuableClassVisitor extends ClassAdapter {
	
	final private ContinuableClassInfoResolver cciResolver;
	final private byte[] originalBytes;

	private String className;
	private ContinuableClassInfo classInfo;
	private boolean skipEnchancing = false;

	public ContinuableClassVisitor(final ClassVisitor cv, final ContinuableClassInfoResolver cciResolver, final byte[] originalBytes) {
		super(cv);
		this.cciResolver = cciResolver;
		this.originalBytes = originalBytes;
	}

	boolean skipEnchancing() {
		return skipEnchancing;
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		className = name;
		classInfo = cciResolver.resolve(name, originalBytes);

		if (null == classInfo || classInfo.isClassProcessed()) {
			skipEnchancing = true;
			// Must exit by throwing exception, otherwise NPE is possible in nested visitor
			throw AbortTransformationException.INSTANCE;
		}

		if (null != interfaces)
			for (final String interfaceInternalName : interfaces) {
				if (CONTINUABLE_MARKER_INTERFACE_NAME.equals(interfaceInternalName)) {
					skipEnchancing = true;
					break;
				}
			}

		final String[] newInterfaces;
		if (skipEnchancing) {
			classInfo.markClassProcessed();
			throw AbortTransformationException.INSTANCE;
		} else {
			final int size = null == interfaces ? 0 : interfaces.length;
			newInterfaces = new String[size + 1];
			System.arraycopy(interfaces, 0, newInterfaces, 0, size);
			newInterfaces[size] = CONTINUABLE_MARKER_INTERFACE_NAME;
		}
		cv.visit(version, access, name, signature, superName, interfaces);
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		final boolean skip = skipEnchancing || null == classInfo || mv == null
				|| (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0 || "<init>".equals(name)
				|| !classInfo.isContinuableMethod(access, name, desc, signature);
		if (skip) {
			return mv;
		} else {
			return new ContinuableMethodNode(access, name, desc, signature, exceptions, className, cciResolver, mv);
		}
	}

	private final static String CONTINUABLE_MARKER_INTERFACE_NAME = Type.getInternalName(Continuable.class);

}
