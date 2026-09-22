/*
 * Copyright (c) 2026, ProjectX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 */
package net.runelite.client.plugins.projectx.threadsafety;

import org.junit.Assume;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

/**
 * CI guardrail that fails when plugin code (classes under
 * {@code net.runelite.client.plugins.projectx} excluding {@code util/} and
 * {@code api/}) calls an unsafe queryable terminal method such as
 * {@code nearest()}, {@code first()}, or {@code toList()} on an
 * {@link net.runelite.client.plugins.projectx.api.IEntityQueryable}
 * in a method that also uses a <strong>name-based filter</strong>
 * ({@code withName}, {@code withNameContains}, {@code withNames}).
 *
 * <h2>Why only name-based filters?</h2>
 * Name-based filters call {@code getName()} on every entity in the stream.
 * {@code getName()} internally marshals to the client thread per entity,
 * so evaluating the stream off the client thread causes hundreds of
 * individual thread switches.  ID-based filters ({@code withId},
 * {@code withIds}) read a cached int and are safe from any thread.
 * Using the {@code *OnClientThread()} terminal wraps the entire stream
 * evaluation in a single {@code clientThread.invoke()}, collapsing
 * those per-entity hops into one batch call.
 *
 * <h2>Baseline</h2>
 * Pre-existing violations live in
 * {@code src/test/resources/threadsafety/queryable-terminal-guardrail-baseline.txt}.
 * Regenerate with
 * {@code ./gradlew :client:regenerateQueryableTerminalBaseline}
 * or {@code ./gradlew :client:runUnitTests -Dprojectx.queryable-guardrail.regenerate-baseline=true}.
 */
public class QueryableTerminalGuardrailTest
{
	private static final String CLIENT_THREAD_INTERNAL = "net/runelite/client/callback/ClientThread";

	private static final String PROJECTX_PREFIX = "net/runelite/client/plugins/projectx/";
	private static final String UTIL_PREFIX = "net/runelite/client/plugins/projectx/util/";
	private static final String API_PREFIX = "net/runelite/client/plugins/projectx/api/";

	private static final Set<String> QUERYABLE_TYPES = Set.of(
		"net/runelite/client/plugins/projectx/api/IEntityQueryable",
		"net/runelite/client/plugins/projectx/api/AbstractEntityQueryable",
		"net/runelite/client/plugins/projectx/api/npc/Rs2NpcQueryable",
		"net/runelite/client/plugins/projectx/api/player/Rs2PlayerQueryable",
		"net/runelite/client/plugins/projectx/api/tileitem/Rs2TileItemQueryable",
		"net/runelite/client/plugins/projectx/api/tileobject/Rs2TileObjectQueryable"
	);

	private static final Set<String> NAME_BASED_FILTERS = Set.of(
		"withName", "withNameContains", "withNames"
	);

	private static final Set<String> UNSAFE_TERMINAL_NAMES = Set.of(
		"first", "firstReachable",
		"nearest", "nearestReachable",
		"toList", "count"
	);

	private static final String BASELINE_RESOURCE = "/threadsafety/queryable-terminal-guardrail-baseline.txt";
	private static final Path BASELINE_RELATIVE_PATH = Paths.get(
		"runelite-client", "src", "test", "resources", "threadsafety", "queryable-terminal-guardrail-baseline.txt");

	private static final String REGENERATE_BASELINE_PROPERTY = "projectx.queryable-guardrail.regenerate-baseline";

	private static final class MethodInfo
	{
		final String className;
		final String methodName;
		final String methodDesc;
		boolean confirmedLambda;
		boolean callsClientThreadInvoke;
		boolean hasNameBasedFilter;
		final List<String> lambdaHandles = new ArrayList<>();
		final List<String> unsafeTerminalCalls = new ArrayList<>();

		MethodInfo(String className, String methodName, String methodDesc)
		{
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}

		String key()
		{
			return className + "." + methodName + methodDesc;
		}

		String fullyQualified()
		{
			return className.replace('/', '.') + "#" + methodName + prettyDescriptor(methodDesc);
		}
	}

	@Test
	public void pluginsMustUseOnClientThreadQueryableTerminals() throws IOException
	{
		Path repoRoot = resolveRepoRoot();
		Path scanRoot = repoRoot.resolve("runelite-client/build/classes/java/main");

		Assume.assumeTrue(
			"Compiled main classes not found under runelite-client/build/classes/java/main; "
				+ "run `./gradlew :client:compileJava` first.",
			Files.isDirectory(scanRoot));

		Map<String, MethodInfo> methods = new LinkedHashMap<>();
		try (Stream<Path> stream = Files.walk(scanRoot))
		{
			List<Path> classFiles = stream
				.filter(p -> p.toString().endsWith(".class"))
				.collect(Collectors.toList());
			for (Path classFile : classFiles)
			{
				try (InputStream in = Files.newInputStream(classFile))
				{
					ClassReader reader = new ClassReader(in);
					ClassNode cn = new ClassNode();
					reader.accept(cn, ClassReader.SKIP_FRAMES);
					for (MethodNode mn : cn.methods)
					{
						MethodInfo info = analyzeMethod(cn, mn);
						methods.put(info.key(), info);
					}
				}
				catch (IOException e)
				{
					System.err.println("[queryable-guardrail] Failed to read " + classFile + ": " + e);
				}
			}
		}

		propagateConfirmedLambdas(methods);

		Set<String> violations = collectViolations(methods);

		if (Boolean.getBoolean(REGENERATE_BASELINE_PROPERTY))
		{
			Path baselinePath = repoRoot.resolve(BASELINE_RELATIVE_PATH);
			writeBaseline(baselinePath, violations);
			System.out.println("[queryable-guardrail] Wrote " + violations.size() + " entries to " + baselinePath);
			return;
		}

		Set<String> baseline = loadBaseline();
		Set<String> newViolations = new TreeSet<>(violations);
		newViolations.removeAll(baseline);
		Set<String> obsolete = new TreeSet<>(baseline);
		obsolete.removeAll(violations);

		if (newViolations.isEmpty() && obsolete.isEmpty())
		{
			System.out.println("[queryable-guardrail] " + violations.size() + " known violation(s); no regressions.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		if (!newViolations.isEmpty())
		{
			sb.append(newViolations.size())
				.append(" new queryable-terminal guardrail violation(s) detected.\n")
				.append("Plugin code is calling an unsafe queryable terminal method instead of the ")
				.append("thread-safe *OnClientThread() variant:\n\n");
			for (String v : newViolations)
			{
				sb.append("  + ").append(v).append("\n");
			}
			sb.append("\nFix: use the OnClientThread variant (e.g. nearestOnClientThread()) or ")
				.append("wrap in ProjectX.getClientThread().invoke(() -> ...).\n")
				.append("If intentional, regenerate the baseline with `-D")
				.append(REGENERATE_BASELINE_PROPERTY)
				.append("=true` and commit the diff.\n\n");
		}
		if (!obsolete.isEmpty())
		{
			sb.append(obsolete.size())
				.append(" baseline entry/entries no longer match a real violation. ")
				.append("Regenerate the baseline with `-D")
				.append(REGENERATE_BASELINE_PROPERTY)
				.append("=true` and commit the diff:\n\n");
			for (String v : obsolete)
			{
				sb.append("  - ").append(v).append("\n");
			}
		}

		fail(sb.toString());
	}

	private static Set<String> collectViolations(Map<String, MethodInfo> methods)
	{
		Set<String> violations = new TreeSet<>();
		for (MethodInfo m : methods.values())
		{
			if (!isPluginClass(m.className))
			{
				continue;
			}
			if (m.confirmedLambda)
			{
				continue;
			}
			if (!m.hasNameBasedFilter)
			{
				continue;
			}
			for (String call : m.unsafeTerminalCalls)
			{
				violations.add(m.fullyQualified() + "  ->  " + call);
			}
		}
		return violations;
	}

	private static boolean isPluginClass(String internalName)
	{
		return internalName.startsWith(PROJECTX_PREFIX)
			&& !internalName.startsWith(UTIL_PREFIX)
			&& !internalName.startsWith(API_PREFIX);
	}

	private static MethodInfo analyzeMethod(ClassNode cn, MethodNode mn)
	{
		MethodInfo info = new MethodInfo(cn.name, mn.name, mn.desc);

		if (mn.instructions == null || mn.instructions.size() == 0)
		{
			return info;
		}

		Handle pendingLambda = null;

		for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext())
		{
			if (insn instanceof MethodInsnNode)
			{
				MethodInsnNode m = (MethodInsnNode) insn;

				if (CLIENT_THREAD_INTERNAL.equals(m.owner)
					&& (m.name.startsWith("invoke") || m.name.startsWith("runOnClientThread")))
				{
					info.callsClientThreadInvoke = true;
					if (pendingLambda != null)
					{
						info.lambdaHandles.add(handleKey(pendingLambda));
					}
				}

				if (QUERYABLE_TYPES.contains(m.owner) && UNSAFE_TERMINAL_NAMES.contains(m.name))
				{
					info.unsafeTerminalCalls.add(
						m.owner.replace('/', '.') + "#" + m.name + prettyDescriptor(m.desc));
				}

				if (QUERYABLE_TYPES.contains(m.owner) && NAME_BASED_FILTERS.contains(m.name))
				{
					info.hasNameBasedFilter = true;
				}

				pendingLambda = null;
			}
			else if (insn instanceof InvokeDynamicInsnNode)
			{
				InvokeDynamicInsnNode idn = (InvokeDynamicInsnNode) insn;
				if (idn.bsmArgs != null && idn.bsmArgs.length >= 2 && idn.bsmArgs[1] instanceof Handle)
				{
					Handle h = (Handle) idn.bsmArgs[1];
					int tag = h.getTag();
					if (tag == Opcodes.H_INVOKESTATIC
						|| tag == Opcodes.H_INVOKESPECIAL
						|| tag == Opcodes.H_INVOKEVIRTUAL
						|| tag == Opcodes.H_INVOKEINTERFACE
						|| tag == Opcodes.H_NEWINVOKESPECIAL)
					{
						pendingLambda = h;
					}
				}
			}
		}

		return info;
	}

	private static void propagateConfirmedLambdas(Map<String, MethodInfo> methods)
	{
		ArrayDeque<String> queue = new ArrayDeque<>();
		Set<String> visited = new HashSet<>();

		for (MethodInfo m : methods.values())
		{
			if (m.callsClientThreadInvoke)
			{
				queue.addAll(m.lambdaHandles);
			}
		}

		while (!queue.isEmpty())
		{
			String key = queue.poll();
			if (!visited.add(key))
			{
				continue;
			}
			MethodInfo lambda = methods.get(key);
			if (lambda == null)
			{
				continue;
			}
			lambda.confirmedLambda = true;
			queue.addAll(lambda.lambdaHandles);
		}
	}

	private static Set<String> loadBaseline() throws IOException
	{
		Set<String> baseline = new TreeSet<>();
		try (InputStream in = QueryableTerminalGuardrailTest.class.getResourceAsStream(BASELINE_RESOURCE))
		{
			if (in == null)
			{
				return baseline;
			}
			String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			for (String line : content.split("\n"))
			{
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#"))
				{
					continue;
				}
				baseline.add(trimmed);
			}
		}
		return baseline;
	}

	private static void writeBaseline(Path baselinePath, Set<String> violations) throws IOException
	{
		Files.createDirectories(baselinePath.getParent());
		StringBuilder sb = new StringBuilder();
		sb.append("# Baseline of pre-existing queryable-terminal guardrail violations.\n");
		sb.append("# Plugin code calling unsafe queryable terminals (nearest, first, toList, etc.)\n");
		sb.append("# instead of the thread-safe *OnClientThread() variants.\n");
		sb.append("# Maintained by ").append(QueryableTerminalGuardrailTest.class.getName()).append(".\n");
		sb.append("# Format: <caller>  ->  <queryable type>#<terminal method>\n");
		sb.append("# Regenerate with: ./gradlew :client:regenerateQueryableTerminalBaseline\n");
		sb.append("# Lines starting with '#' are ignored.\n");
		sb.append("\n");
		for (String v : violations)
		{
			sb.append(v).append("\n");
		}
		Files.writeString(baselinePath, sb.toString());
	}

	private static String handleKey(Handle h)
	{
		return h.getOwner() + "." + h.getName() + h.getDesc();
	}

	private static String prettyDescriptor(String desc)
	{
		Type[] args = Type.getArgumentTypes(desc);
		Type ret = Type.getReturnType(desc);
		StringBuilder sb = new StringBuilder("(");
		for (int i = 0; i < args.length; i++)
		{
			if (i > 0)
			{
				sb.append(", ");
			}
			sb.append(simpleTypeName(args[i]));
		}
		sb.append("): ").append(simpleTypeName(ret));
		return sb.toString();
	}

	private static String simpleTypeName(Type t)
	{
		String name = t.getClassName();
		int dot = name.lastIndexOf('.');
		return dot < 0 ? name : name.substring(dot + 1);
	}

	private static Path resolveRepoRoot()
	{
		Path cwd = Paths.get("").toAbsolutePath();
		Path candidate = cwd;
		for (int i = 0; i < 6; i++)
		{
			if (Files.isDirectory(candidate.resolve("runelite-api"))
				&& Files.isDirectory(candidate.resolve("runelite-client")))
			{
				return candidate;
			}
			Path parent = candidate.getParent();
			if (parent == null)
			{
				break;
			}
			candidate = parent;
		}
		throw new IllegalStateException("Could not locate repo root from " + cwd);
	}
}
