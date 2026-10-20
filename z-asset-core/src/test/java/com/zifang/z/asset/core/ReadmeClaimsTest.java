package com.zifang.z.asset.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * README 里每一条定量主张都必须能从源码重算出来 —— 广告出去的话就是承诺。
 *
 * <p>为什么要有这一层：本仓 README 把「AssetProvider 接口 5 个方法」「内嵌 AssetDTO 10 字段」
 * 「AssetEntity 15 个字段」「3 个 module」写成了硬数字，而每加一个 SPI 方法或一个字段都会漂一次。
 * 没有任何尺子去问源码「现在还是吗」，那这些数字的保质期就是上一次编辑的时刻。</p>
 *
 * <p>口径：缺一条主张 = 红（不许「正则没匹配上就跳过」，那正是空跑）；
 * 数值不等 = 红且把<b>重算值</b>打进消息（红了要知道真数是多少，否则只是又一处猜）。
 * 尺自己的牙口由 {@link #rulerActuallyCondemnsWrongNumbers()} 用合成 README 证明，
 * 而「不许冤枉健康页」由 {@link #readmeClaimsMatchTheCode()} 本身证明。</p>
 */
class ReadmeClaimsTest {

    private static final String API_POM_REL = "z-asset-api/src/main/java/com/zifang/z/asset/api/AssetProvider.java";
    private static final String ENTITY_REL =
            "z-asset-core/src/main/java/com/zifang/z/asset/core/domain/entity/AssetEntity.java";

    /** 仓根：向上找同时有 README.md 与 pom.xml 的目录（测试跑在模块目录，user.dir 不是仓根）。 */
    private static Path repoRoot() {
        Path p = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 5 && p != null; i++, p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("README.md")) && Files.isRegularFile(p.resolve("pom.xml"))) {
                return p;
            }
        }
        throw new IllegalStateException("定位仓根失败：user.dir=" + System.getProperty("user.dir"));
    }

    private static String read(Path f) {
        try {
            return new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 解析根 pom 的<b>项目自身</b> version / module 数。
     *
     * <p>用 XML 解析而不是正则，有两个实在的好处：① 根 pom 里同时有 parent 的
     * {@code <version>1.1.0</version>} 和自身的 {@code <version>1.0.1</version>}，
     * 正则会抓错；② 被 {@code <!-- -->} 注释掉的 module 不会被算进去
     * （组织里多仓都有「注释排除某个模块」的做法）。</p>
     */
    private static Element projectElement(Path pom) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(false);
            Document doc = f.newDocumentBuilder().parse(pom.toFile());
            return doc.getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("解析 pom 失败：" + pom, e);
        }
    }

    private static String projectVersion(Path root) {
        Element project = projectElement(root.resolve("pom.xml"));
        NodeList children = project.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && "version".equals(n.getNodeName())) {
                return n.getTextContent().trim();
            }
        }
        throw new IllegalStateException("根 pom 里没有项目自身的 <version>");
    }

    private static int moduleCount(Path root) {
        Element project = projectElement(root.resolve("pom.xml"));
        NodeList children = project.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && "modules".equals(n.getNodeName())) {
                NodeList mods = ((Element) n).getElementsByTagName("module");
                int c = 0;
                for (int j = 0; j < mods.getLength(); j++) {
                    c++;
                }
                return c;
            }
        }
        throw new IllegalStateException("根 pom 里没有 <modules>");
    }

    /** 源文件里「声明式」的方法/字段行（排除注释行）。 */
    private static List<String> declLines(String src, Pattern p) {
        List<String> out = new ArrayList<>();
        for (String line : src.split("\n")) {
            String s = line.trim();
            if (s.isEmpty() || s.startsWith("//") || s.startsWith("*") || s.startsWith("/*")) {
                continue;
            }
            if (p.matcher(s).matches()) {
                out.add(s);
            }
        }
        return out;
    }

    /** 接口里声明的方法数（形如 `Type name(args);`，无方法体）。 */
    private static int interfaceMethodCount(Path f) {
        return declLines(read(f), Pattern.compile("[\\w<>,.\\[\\]\\s?]+ \\w+\\([^)]*\\);")).size();
    }

    /** 类里的 private 字段数。 */
    private static int privateFieldCount(Path f) {
        return declLines(read(f), Pattern.compile("private [\\w<>,.\\[\\]\\s?]+ \\w+;")).size();
    }

    /** 内部类 AssetDTO 的 private 字段数（从 `class AssetDTO` 往后数）。 */
    private static int nestedDtoFieldCount(Path f) {
        String src = read(f);
        int at = src.indexOf("class AssetDTO");
        if (at < 0) {
            throw new IllegalStateException("AssetProvider 里找不到内部类 AssetDTO");
        }
        return declLines(src.substring(at), Pattern.compile("private [\\w<>,.\\[\\]\\s?]+ \\w+;")).size();
    }

    /** 一次重算出的全部数字。 */
    private static final class Counts {
        final int providerMethods;
        final int dtoFields;
        final int entityFields;
        final int modules;
        final String version;

        Counts(int providerMethods, int dtoFields, int entityFields, int modules, String version) {
            this.providerMethods = providerMethods;
            this.dtoFields = dtoFields;
            this.entityFields = entityFields;
            this.modules = modules;
            this.version = version;
        }
    }

    private static Counts recomputed() {
        Path root = repoRoot();
        return new Counts(
                interfaceMethodCount(root.resolve(API_POM_REL)),
                nestedDtoFieldCount(root.resolve(API_POM_REL)),
                privateFieldCount(root.resolve(ENTITY_REL)),
                moduleCount(root),
                projectVersion(root));
    }

    /** README 里逐条抠出数字并与重算值对账；不一致就抛 AssertionError 并把真数打进去。 */
    private static void verifyClaims(String readme, Counts c) {
        Matcher m;

        // 主张 1：「`AssetProvider` 接口（5 个方法）」
        m = Pattern.compile("`AssetProvider` 接口（(\\d+) 个方法）").matcher(readme);
        if (!m.find()) {
            throw new AssertionError("README 里找不到「`AssetProvider` 接口（N 个方法）」这条主张");
        }
        assertEquals(c.providerMethods, Integer.parseInt(m.group(1)),
                "AssetProvider 方法数对不上（接口里声明式方法行数）");

        // 主张 2：「内嵌 `AssetDTO`（10 字段）」
        m = Pattern.compile("内嵌 `AssetDTO`（(\\d+) 字段）").matcher(readme);
        if (!m.find()) {
            throw new AssertionError("README 里找不到「内嵌 `AssetDTO`（N 字段）」这条主张");
        }
        assertEquals(c.dtoFields, Integer.parseInt(m.group(1)),
                "AssetDTO 字段数对不上（AssetProvider 内部类的 private 字段数）");

        // 主张 3：「`AssetEntity` 的 15 个字段」
        m = Pattern.compile("`AssetEntity` 的 (\\d+) 个字段").matcher(readme);
        if (!m.find()) {
            throw new AssertionError("README 里找不到「`AssetEntity` 的 N 个字段」这条主张");
        }
        assertEquals(c.entityFields, Integer.parseInt(m.group(1)),
                "AssetEntity 字段数对不上（实体的 private 字段数）");

        // 主张 4：「3 个 `<module>` 全是库件」
        m = Pattern.compile("(\\d+) 个 `<module>`").matcher(readme);
        if (!m.find()) {
            throw new AssertionError("README 里找不到「N 个 `<module>`」这条主张");
        }
        assertEquals(c.modules, Integer.parseInt(m.group(1)), "Maven 模块数对不上（根 pom <modules> 条数）");

        // 主张 5：「根 POM：`packaging=pom`，`<version>1.0.1</version>`」
        m = Pattern.compile("`<version>([^<]+)</version>`").matcher(readme);
        if (!m.find()) {
            throw new AssertionError("README 里找不到「`<version>X.Y.Z</version>`」这条主张");
        }
        assertEquals(c.version, m.group(1), "版本号对不上（根 pom 项目自身的 <version>）");
    }

    @Test
    @DisplayName("README 的定量主张与源码重算值逐条对账")
    void readmeClaimsMatchTheCode() {
        Path root = repoRoot();
        Counts c = recomputed();
        verifyClaims(read(root.resolve("README.md")), c);
    }

    @Test
    @DisplayName("尺子的牙口：把 README 的数改错，判定必须判红")
    void rulerActuallyCondemnsWrongNumbers() {
        String readme = read(repoRoot().resolve("README.md"));
        String tampered = readme.replaceFirst("`AssetProvider` 接口（\\d+ 个方法）",
                "`AssetProvider` 接口（4 个方法）");
        assertNotEquals(readme, tampered, "篡改失败：README 里找不到「`AssetProvider` 接口（N 个方法）」字样（写法变了？）");
        AssertionError err = assertThrows(AssertionError.class,
                () -> verifyClaims(tampered, recomputed()));
        assertTrue(err.getMessage().contains("方法数"), "报错要指向真正对不上的那条主张，实际是：" + err.getMessage());
    }

    @Test
    @DisplayName("不许冤枉健康页：真实 README 不得被判红")
    void healthyReadmeIsNotCondemned() {
        String readme = read(repoRoot().resolve("README.md"));
        verifyClaims(readme, recomputed());
    }
}
