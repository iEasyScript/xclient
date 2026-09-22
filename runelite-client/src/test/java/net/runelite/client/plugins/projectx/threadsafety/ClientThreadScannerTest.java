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
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manually-invoked scanner that walks compiled bytecode looking for client-thread
 * markers and emits a manifest under {@code docs/client-thread-manifest.md}.
 *
 * <p>Run with:
 * <pre>
 *   ./gradlew :client:runClientThreadScanner
 * </pre>
 *
 * <p>Or via JUnit directly with the system property:
 * <pre>
 *   ./gradlew :client:test --tests ClientThreadScannerTest -Dprojectx.scanner.enabled=true
 * </pre>
 *
 * <h2>What it detects</h2>
 * <ul>
 *   <li><b>REQUIRES_CLIENT_THREAD</b> — methods containing {@code assert client.isClientThread()}.
 *       These will throw {@link AssertionError} if called off-thread when assertions are enabled.</li>
 *   <li><b>CHECKS_THREAD_GUARD</b> — methods that branch on {@code isClientThread()} but do not
 *       throw. Typically early-return guards (e.g. {@code Global.sleep}) or hybrid helpers.</li>
 *   <li><b>SELF_MARSHALLING</b> — methods that wrap their own work in {@code ClientThread.invoke*()}
 *       / {@code runOnClientThreadOptional()}, making them safe to call from any thread.</li>
 *   <li><b>EVENT_HANDLER</b> — methods annotated with {@code @Subscribe}; these are dispatched on
 *       the client thread by RuneLite's event bus.</li>
 *   <li><b>Inferred RuneLite API methods</b> — every {@code net.runelite.api.*} method invoked from
 *       inside a REQUIRES_CLIENT_THREAD method. Strong evidence those API methods need the client
 *       thread.</li>
 * </ul>
 *
 * <h2>What it does NOT detect</h2>
 * Many RuneLite API methods are silently unsafe off-thread (no assertion, no documented contract).
 * The scanner cannot find those — only the explicitly-marked ones.
 */
public class ClientThreadScannerTest
{
	private static final String CLIENT_THREAD_INTERNAL = "net/runelite/client/callback/ClientThread";
	private static final String SUBSCRIBE_DESC = "Lnet/runelite/client/eventbus/Subscribe;";
	private static final String ASSERTION_ERROR_INTERNAL = "java/lang/AssertionError";
	private static final String IS_CLIENT_THREAD = "isClientThread";
	private static final String RUNELITE_API_PREFIX = "net/runelite/api/";

	private enum Category
	{
		REQUIRES_CLIENT_THREAD,
		CHECKS_THREAD_GUARD,
		SELF_MARSHALLING,
		EVENT_HANDLER,
		/** Method body is a lambda that was passed to {@code ClientThread.invoke*()}. */
		CONFIRMED_LAMBDA
	}

	private static final class MethodFinding
	{
		final String className;
		final String methodName;
		final String methodDesc;
		final Set<Category> categories = new TreeSet<>();
		/** Raw API invocations seen in the body, regardless of category. */
		final List<String> rlApiInvocations = new ArrayList<>();
		/** Lambda body methods this method passed to {@code ClientThread.invoke*()}. */
		final List<String> lambdaHandlesPassedToInvoke = new ArrayList<>();

		MethodFinding(String className, String methodName, String methodDesc)
		{
			this.className = className;
			this.methodName = methodName;
			this.methodDesc = methodDesc;
		}

		String simpleSignature()
		{
			return methodName + prettyDescriptor(methodDesc);
		}

		String key()
		{
			return className + "." + methodName + methodDesc;
		}

		String fullyQualified()
		{
			return className.replace('/', '.') + "#" + simpleSignature();
		}
	}

	private static String handleKey(Handle h)
	{
		return h.getOwner() + "." + h.getName() + h.getDesc();
	}

	@Test
	public void scanAndEmitManifest() throws IOException
	{
		Assume.assumeTrue(
			"Scanner is manual-only. Set -Dprojectx.scanner.enabled=true to run.",
			Boolean.getBoolean("projectx.scanner.enabled"));

		Path repoRoot = resolveRepoRoot();
		List<Path> scanRoots = List.of(
			repoRoot.resolve("runelite-api/build/classes/java/main"),
			repoRoot.resolve("runelite-client/build/classes/java/main"));

		// All methods, indexed by "owner.name+desc". We need every method (not just the
		// ones with categories) so the reachability pass can resolve lambda body handles.
		Map<String, MethodFinding> findingsByKey = new LinkedHashMap<>();
		long classCount = 0;
		long methodCount = 0;

		for (Path root : scanRoots)
		{
			if (!Files.isDirectory(root))
			{
				System.err.println("[scanner] Skipping missing classes dir: " + root);
				continue;
			}
			System.out.println("[scanner] Scanning " + root);
			try (Stream<Path> stream = Files.walk(root))
			{
				List<Path> classFiles = stream
					.filter(p -> p.toString().endsWith(".class"))
					.collect(Collectors.toList());
				for (Path classFile : classFiles)
				{
					classCount++;
					try (InputStream in = Files.newInputStream(classFile))
					{
						ClassReader reader = new ClassReader(in);
						ClassNode cn = new ClassNode();
						reader.accept(cn, ClassReader.SKIP_FRAMES);
						for (MethodNode mn : cn.methods)
						{
							methodCount++;
							MethodFinding finding = analyzeMethod(cn, mn);
							findingsByKey.put(finding.key(), finding);
						}
					}
					catch (IOException e)
					{
						System.err.println("[scanner] Failed to read " + classFile + ": " + e);
					}
				}
			}
		}

		int reachedLambdas = traverseLambdaReachability(findingsByKey);

		List<MethodFinding> findings = findingsByKey.values().stream()
			.filter(f -> !f.categories.isEmpty())
			.collect(Collectors.toList());

		Map<Category, List<MethodFinding>> byCategory = groupByCategory(findings);
		Map<String, Map<String, TreeSet<String>>> inferredApi = computeInferredApi(byCategory);

		Path docsDir = repoRoot.resolve("docs");
		Files.createDirectories(docsDir);

		Path manifestPath = docsDir.resolve("client-thread-manifest.md");
		String manifest = renderManifest(findings, byCategory, inferredApi, classCount, methodCount);
		Files.writeString(manifestPath, manifest);

		Path lookupPath = docsDir.resolve("client-thread-lookup.tsv");
		String lookup = renderLookupTsv(inferredApi);
		Files.writeString(lookupPath, lookup);

		System.out.println("[scanner] Scanned " + classCount + " classes / " + methodCount + " methods");
		System.out.println("[scanner] Found " + findings.size() + " methods with client-thread markers");
		System.out.println("[scanner] Reached " + reachedLambdas + " lambda body methods via invoke chains");
		System.out.println("[scanner] Wrote manifest to " + manifestPath);
		System.out.println("[scanner] Wrote lookup TSV to " + lookupPath);
	}

	/**
	 * Walks from every {@code SELF_MARSHALLING} method's lambda handles, marking each reached
	 * lambda body method as {@link Category#CONFIRMED_LAMBDA}. Recurses into nested lambdas
	 * (a lambda that itself calls {@code clientThread.invoke(() -> ...)}).
	 *
	 * @return number of distinct lambda body methods reached
	 */
	private static int traverseLambdaReachability(Map<String, MethodFinding> findingsByKey)
	{
		java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
		Set<String> visited = new java.util.HashSet<>();

		for (MethodFinding f : findingsByKey.values())
		{
			if (f.categories.contains(Category.SELF_MARSHALLING))
			{
				queue.addAll(f.lambdaHandlesPassedToInvoke);
			}
		}

		while (!queue.isEmpty())
		{
			String key = queue.poll();
			if (!visited.add(key))
			{
				continue;
			}
			MethodFinding lambdaBody = findingsByKey.get(key);
			if (lambdaBody == null)
			{
				// Lambda body lives in a class we did not scan (e.g. a third-party dep).
				continue;
			}
			lambdaBody.categories.add(Category.CONFIRMED_LAMBDA);
			// Follow nested lambdas.
			queue.addAll(lambdaBody.lambdaHandlesPassedToInvoke);
		}

		return visited.size();
	}

	private static MethodFinding analyzeMethod(ClassNode cn, MethodNode mn)
	{
		MethodFinding finding = new MethodFinding(cn.name, mn.name, mn.desc);

		if (mn.visibleAnnotations != null)
		{
			for (AnnotationNode an : mn.visibleAnnotations)
			{
				if (SUBSCRIBE_DESC.equals(an.desc))
				{
					finding.categories.add(Category.EVENT_HANDLER);
				}
			}
		}

		if (mn.instructions == null || mn.instructions.size() == 0)
		{
			return finding;
		}

		boolean callsIsClientThread = false;
		boolean newsAssertionError = false;
		boolean callsClientThreadInvoke = false;

		// Track the most recent lambda Handle produced by an INVOKEDYNAMIC. When the very next
		// method invocation is a ClientThread.invoke*() call, we treat that lambda body as
		// "passed to invoke" and queue it for follow-up reachability. Reset on any other
		// method call (the lambda was consumed) or on label boundaries.
		Handle pendingLambdaHandle = null;

		for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext())
		{
			if (insn instanceof MethodInsnNode)
			{
				MethodInsnNode m = (MethodInsnNode) insn;
				if (IS_CLIENT_THREAD.equals(m.name) && m.desc.equals("()Z"))
				{
					callsIsClientThread = true;
				}
				if (CLIENT_THREAD_INTERNAL.equals(m.owner)
					&& (m.name.startsWith("invoke") || m.name.startsWith("runOnClientThread")))
				{
					callsClientThreadInvoke = true;
					if (pendingLambdaHandle != null)
					{
						finding.lambdaHandlesPassedToInvoke.add(handleKey(pendingLambdaHandle));
					}
				}
				if (m.owner.startsWith(RUNELITE_API_PREFIX))
				{
					finding.rlApiInvocations.add(m.owner + "#" + m.name + prettyDescriptor(m.desc));
				}
				pendingLambdaHandle = null;
			}
			else if (insn instanceof InvokeDynamicInsnNode)
			{
				InvokeDynamicInsnNode idn = (InvokeDynamicInsnNode) insn;
				// java.lang.invoke.LambdaMetafactory bsmArgs[1] is the implementation Handle.
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
						pendingLambdaHandle = h;
					}
				}
			}
			else if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW)
			{
				TypeInsnNode t = (TypeInsnNode) insn;
				if (ASSERTION_ERROR_INTERNAL.equals(t.desc))
				{
					newsAssertionError = true;
				}
			}
		}

		if (callsIsClientThread && newsAssertionError)
		{
			finding.categories.add(Category.REQUIRES_CLIENT_THREAD);
		}
		else if (callsIsClientThread)
		{
			finding.categories.add(Category.CHECKS_THREAD_GUARD);
		}

		if (callsClientThreadInvoke)
		{
			finding.categories.add(Category.SELF_MARSHALLING);
		}

		return finding;
	}

	private static Map<Category, List<MethodFinding>> groupByCategory(List<MethodFinding> findings)
	{
		Map<Category, List<MethodFinding>> byCategory = new LinkedHashMap<>();
		for (Category c : Category.values())
		{
			byCategory.put(c, new ArrayList<>());
		}
		for (MethodFinding f : findings)
		{
			for (Category c : f.categories)
			{
				byCategory.get(c).add(f);
			}
		}
		return byCategory;
	}

	private static Map<String, Map<String, TreeSet<String>>> computeInferredApi(
		Map<Category, List<MethodFinding>> byCategory)
	{
		// owner -> member -> set of caller evidence (e.g. "ASSERT:Rs2Inventory#storeInventoryItemsInMemory")
		Map<String, Map<String, TreeSet<String>>> inferredApi = new TreeMap<>();
		recordInferences(inferredApi, byCategory.get(Category.REQUIRES_CLIENT_THREAD), "ASSERT");
		recordInferences(inferredApi, byCategory.get(Category.EVENT_HANDLER), "SUBSCRIBE");
		recordInferences(inferredApi, byCategory.get(Category.CONFIRMED_LAMBDA), "LAMBDA");
		// Method references like `clientThread.invoke(actor::getName)` produce a CONFIRMED_LAMBDA
		// finding whose own class is already in net/runelite/api/. The method reference target
		// IS the API method we want to flag, so add it directly.
		for (MethodFinding f : byCategory.get(Category.CONFIRMED_LAMBDA))
		{
			if (f.className.startsWith(RUNELITE_API_PREFIX))
			{
				inferredApi
					.computeIfAbsent(f.className.replace('/', '.'), k -> new TreeMap<>())
					.computeIfAbsent(f.simpleSignature(), k -> new TreeSet<>())
					.add("LAMBDA:method-reference");
			}
		}
		return inferredApi;
	}

	private static String renderManifest(
		List<MethodFinding> findings,
		Map<Category, List<MethodFinding>> byCategory,
		Map<String, Map<String, TreeSet<String>>> inferredApi,
		long classCount,
		long methodCount)
	{
		int totalInferred = inferredApi.values().stream()
			.mapToInt(Map::size).sum();

		StringBuilder sb = new StringBuilder();
		sb.append("# ProjectX Client-Thread Manifest\n\n");
		sb.append("Generated: ").append(LocalDate.now()).append("  \n");
		sb.append("Source: `runelite-client/src/test/java/net/runelite/client/plugins/projectx/threadsafety/ClientThreadScannerTest.java`\n\n");
		sb.append("> Manually regenerate with `./gradlew :client:runClientThreadScanner`. ")
			.append("Commit the diff to track how RuneLite's client-thread surface evolves between revisions.\n\n");

		sb.append("## Summary\n\n");
		sb.append("| Category | Count |\n");
		sb.append("|---|---:|\n");
		sb.append("| Classes scanned | ").append(classCount).append(" |\n");
		sb.append("| Methods scanned | ").append(methodCount).append(" |\n");
		sb.append("| `REQUIRES_CLIENT_THREAD` (asserts) | ").append(byCategory.get(Category.REQUIRES_CLIENT_THREAD).size()).append(" |\n");
		sb.append("| `CHECKS_THREAD_GUARD` (branches) | ").append(byCategory.get(Category.CHECKS_THREAD_GUARD).size()).append(" |\n");
		sb.append("| `SELF_MARSHALLING` (wraps invoke) | ").append(byCategory.get(Category.SELF_MARSHALLING).size()).append(" |\n");
		sb.append("| `EVENT_HANDLER` (`@Subscribe`) | ").append(byCategory.get(Category.EVENT_HANDLER).size()).append(" |\n");
		sb.append("| `CONFIRMED_LAMBDA` (passed to invoke) | ").append(byCategory.get(Category.CONFIRMED_LAMBDA).size()).append(" |\n");
		sb.append("| RuneLite API methods inferred client-thread-only | ")
			.append(totalInferred).append(" |\n\n");

		sb.append("## Legend\n\n");
		sb.append("- **REQUIRES_CLIENT_THREAD** — `assert client.isClientThread()` in body. ")
			.append("Throws `AssertionError` off-thread when `-ea` is enabled.\n");
		sb.append("- **CHECKS_THREAD_GUARD** — Reads `isClientThread()` to branch. ")
			.append("Often a sleep/wait guard or a hybrid helper.\n");
		sb.append("- **SELF_MARSHALLING** — Calls `ClientThread.invoke*()` / `runOnClientThreadOptional()`. ")
			.append("Safe to call from any thread.\n");
		sb.append("- **EVENT_HANDLER** — Annotated `@Subscribe`. ")
			.append("RuneLite's event bus dispatches these on the client thread.\n");
		sb.append("- **CONFIRMED_LAMBDA** — Synthetic lambda body that was passed to ")
			.append("`ClientThread.invoke*()`. Reached transitively, including nested lambdas.\n\n");

		appendCategorySection(sb, "Methods that ASSERT client thread", byCategory.get(Category.REQUIRES_CLIENT_THREAD));
		appendCategorySection(sb, "Methods that GUARD on client thread", byCategory.get(Category.CHECKS_THREAD_GUARD));
		appendCategorySection(sb, "Self-marshalling helpers", byCategory.get(Category.SELF_MARSHALLING));
		appendCategorySection(sb, "Event handlers (@Subscribe)", byCategory.get(Category.EVENT_HANDLER));
		appendCategorySection(sb, "Confirmed lambda bodies (reached via invoke)", byCategory.get(Category.CONFIRMED_LAMBDA));

		sb.append("## Inferred RuneLite API methods needing client thread\n\n");
		sb.append("These `net.runelite.api.*` methods are invoked from inside methods that are guaranteed to run on the client thread. ")
			.append("Each entry is tagged with the strength of the evidence:\n\n");
		sb.append("- **`ASSERT`** — caller has `assert client.isClientThread()` (highest confidence)\n");
		sb.append("- **`SUBSCRIBE`** — caller is `@Subscribe`-annotated, dispatched on the client thread by the event bus\n");
		sb.append("- **`LAMBDA`** — caller is a lambda body that was passed (transitively) to `ClientThread.invoke*()`\n\n");
		sb.append("> This list is derived, not exhaustive. It catches API methods reached from known-on-thread callers in this repo. ")
			.append("Many other API methods are also unsafe off-thread but call no asserting/subscribing/marshalling wrapper here.\n\n");
		if (inferredApi.isEmpty())
		{
			sb.append("_No inferences found._\n\n");
		}
		else
		{
			for (Map.Entry<String, Map<String, TreeSet<String>>> e : inferredApi.entrySet())
			{
				sb.append("### `").append(e.getKey()).append("`\n\n");
				sb.append("| Method | Evidence | Caller count |\n");
				sb.append("|---|---|---:|\n");
				for (Map.Entry<String, TreeSet<String>> me : e.getValue().entrySet())
				{
					TreeSet<String> callers = me.getValue();
					Set<String> kinds = new TreeSet<>();
					for (String c : callers)
					{
						kinds.add(c.substring(0, c.indexOf(':')));
					}
					sb.append("| `").append(me.getKey()).append("` | ")
						.append(String.join(", ", kinds)).append(" | ")
						.append(callers.size()).append(" |\n");
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	/**
	 * Emits a flat tab-separated lookup file consumed by `projectx-cli client-thread`.
	 * Columns: {@code class\tsignature\tevidence\tcallers}. Sorted for stable diffs.
	 * Includes a single header line beginning with {@code #}.
	 */
	private static String renderLookupTsv(Map<String, Map<String, TreeSet<String>>> inferredApi)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("# class\tsignature\tevidence\tcallers\n");
		for (Map.Entry<String, Map<String, TreeSet<String>>> classEntry : inferredApi.entrySet())
		{
			String owner = classEntry.getKey();
			for (Map.Entry<String, TreeSet<String>> memberEntry : classEntry.getValue().entrySet())
			{
				String signature = memberEntry.getKey();
				TreeSet<String> callers = memberEntry.getValue();
				Set<String> kinds = new TreeSet<>();
				for (String c : callers)
				{
					kinds.add(c.substring(0, c.indexOf(':')));
				}
				sb.append(owner).append('\t')
					.append(signature).append('\t')
					.append(String.join(",", kinds)).append('\t')
					.append(callers.size()).append('\n');
			}
		}
		return sb.toString();
	}

	private static void recordInferences(
		Map<String, Map<String, TreeSet<String>>> sink,
		List<MethodFinding> findings,
		String evidenceTag)
	{
		for (MethodFinding f : findings)
		{
			for (String inv : f.rlApiInvocations)
			{
				int hash = inv.indexOf('#');
				String owner = inv.substring(0, hash).replace('/', '.');
				String member = inv.substring(hash + 1);
				sink
					.computeIfAbsent(owner, k -> new TreeMap<>())
					.computeIfAbsent(member, k -> new TreeSet<>())
					.add(evidenceTag + ":" + f.fullyQualified());
			}
		}
	}

	private static void appendCategorySection(StringBuilder sb, String title, List<MethodFinding> entries)
	{
		sb.append("## ").append(title).append("\n\n");
		if (entries.isEmpty())
		{
			sb.append("_None._\n\n");
			return;
		}
		Map<String, List<MethodFinding>> byClass = new TreeMap<>();
		for (MethodFinding f : entries)
		{
			byClass.computeIfAbsent(f.className.replace('/', '.'), k -> new ArrayList<>()).add(f);
		}
		sb.append("<details><summary>")
			.append(entries.size()).append(" method(s) across ").append(byClass.size()).append(" class(es)")
			.append("</summary>\n\n");
		for (Map.Entry<String, List<MethodFinding>> e : byClass.entrySet())
		{
			sb.append("**`").append(e.getKey()).append("`**\n\n");
			List<MethodFinding> sorted = new ArrayList<>(e.getValue());
			sorted.sort(Comparator.comparing(MethodFinding::simpleSignature));
			for (MethodFinding f : sorted)
			{
				sb.append("- `").append(f.simpleSignature()).append("`\n");
			}
			sb.append("\n");
		}
		sb.append("</details>\n\n");
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
