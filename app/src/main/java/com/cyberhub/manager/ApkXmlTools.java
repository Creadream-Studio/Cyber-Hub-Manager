package com.cyberhub.manager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * APK 内二进制 XML 的批量替换与翻译。只改“人类可读”的文本节点和文本属性，
 * 不触碰 {@code @resource} / {@code ?attr} / 布尔 / 数字 / 颜色值，避免破坏资源引用。
 */
public final class ApkXmlTools {
    private ApkXmlTools() {}

    private static final long MAX_XML = 64L * 1024 * 1024;
    private static final String[] TEXT_ATTRS = {
            "text", "hint", "title", "summary", "label", "description",
            "contentDescription", "tooltipText", "subtitle", "message",
            "positiveButtonText", "negativeButtonText", "neutralButtonText",
            "dialogTitle", "dialogMessage", "shortcutShortLabel", "shortcutLongLabel"
    };

    public static JSONObject batchReplace(File source, File destination, JSONObject opts) throws Exception {
        String find = opts.optString("find", "");
        if (find.isEmpty()) throw new Exception("查找内容不能为空");
        String replacement = opts.optString("replace", "");
        boolean regex = opts.optBoolean("regex", false);
        boolean matchCase = opts.optBoolean("matchCase", true);
        boolean includeManifest = opts.optBoolean("manifest", false);
        JSONArray only = opts.optJSONArray("entries");

        Pattern pattern = regex
                ? Pattern.compile(find, matchCase ? 0 : Pattern.CASE_INSENSITIVE)
                : Pattern.compile(Pattern.quote(find), matchCase ? 0 : Pattern.CASE_INSENSITIVE);

        int[] stats = new int[2];
        Map<String, byte[]> edits = new LinkedHashMap<>();
        Map<String, String> sample = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        try (ZipFile zip = new ZipFile(source)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".xml")) continue;
                boolean manifest = name.equals("AndroidManifest.xml");
                if (manifest && !includeManifest) continue;
                if (!manifest && (name.startsWith("assets/") || name.startsWith("META-INF/"))) continue;
                if (only != null && !contains(only, name)) continue;
                byte[] data = read(zip, entry);
                if (!manifest && !Axml.isBinary(data)) continue;
                try {
                    Axml axml = new Axml();
                    String xml = axml.decode(data);
                    Document document = parse(xml);
                    int changed = replaceInDocument(document, pattern, replacement, stats);
                    if (changed == 0) continue;
                    String serialized = serialize(document);
                    byte[] encoded = axml.encode(serialized);
                    edits.put(name, encoded);
                    stats[0] += changed;
                    stats[1]++;
                    if (sample.size() < 5) sample.put(name, firstLine(serialized));
                } catch (Exception error) {
                    if (errors.size() < 5) errors.add(name + ": " + error.getMessage());
                }
            }
        }
        if (edits.isEmpty()) throw new Exception("没有匹配的 XML 文本" + (errors.isEmpty() ? "" : "；" + String.join(" | ", errors)));
        FileOps.rewriteZip(source, destination, edits);
        JSONObject out = new JSONObject();
        out.put("hits", stats[0]);
        out.put("files", stats[1]);
        out.put("samples", new JSONObject(sample));
        JSONArray errorArray = new JSONArray();
        for (String error : errors) errorArray.put(error);
        out.put("errors", errorArray);
        return out;
    }

    public interface Translator { String translate(String text, String lang) throws Exception; }

    public static JSONObject translate(File source, File destination, Translate translator, JSONObject opts) throws Exception {
        return translate(source, destination, (text, lang) -> translator.translate(
                new JSONObject().put("text", text).put("to", lang)).optString("text", ""), opts);
    }

    public static JSONObject translate(File source, File destination, Translator translator, JSONObject opts) throws Exception {
        String lang = opts.optString("lang", "");
        if (lang.isEmpty()) throw new Exception("请先选择目标语言");
        int limit = opts.optInt("limit", 500);
        boolean includeManifest = opts.optBoolean("manifest", false);
        Map<String, String> cache = new HashMap<>();
        int translated = 0, skipped = 0, files = 0;
        Map<String, byte[]> edits = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        try (ZipFile zip = new ZipFile(source)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".xml")) continue;
                boolean manifest = name.equals("AndroidManifest.xml");
                if (manifest && !includeManifest) continue;
                if (!manifest && (name.startsWith("assets/") || name.startsWith("META-INF/"))) continue;
                if (translated >= limit) break;
                byte[] data = read(zip, entry);
                if (!manifest && !Axml.isBinary(data)) continue;
                try {
                Axml axml = new Axml();
                String xml = axml.decode(data);
                Document document = parse(xml);
                NodeList all = document.getElementsByTagName("*");
                boolean any = false;
                for (int i = 0; i < all.getLength() && translated < limit; i++) {
                    Element element = (Element) all.item(i);
                    NamedNodeMap attrs = element.getAttributes();
                    for (int a = 0; a < attrs.getLength() && translated < limit; a++) {
                        Attr attr = (Attr) attrs.item(a);
                        if (!isTextAttribute(attr)) continue;
                        String value = attr.getValue();
                        if (!translatable(value)) continue;
                        String result = translateValue(translator, cache, value, lang, errors);
                        if (result == null) { skipped++; continue; }
                        if (!result.equals(value)) { attr.setValue(result); translated++; any = true; }
                    }
                    String own = directText(element);
                    if (own != null && translatable(own) && translated < limit) {
                        String result = translateValue(translator, cache, own, lang, errors);
                        if (result != null && !result.equals(own)) {
                            setDirectText(document, element, result);
                            translated++;
                            any = true;
                        } else if (result == null) skipped++;
                    }
                }
                if (!any) continue;
                edits.put(name, axml.encode(serialize(document)));
                files++;
                } catch (Exception error) {
                    if (errors.size() < 5) errors.add(name + ": " + error.getMessage());
                }
            }
        }
        if (edits.isEmpty()) throw new Exception(errors.isEmpty() ? "没有可翻译的 XML 文本" : errors.get(0));
        FileOps.rewriteZip(source, destination, edits);
        JSONObject out = new JSONObject();
        out.put("translated", translated);
        out.put("skipped", skipped);
        out.put("files", files);
        JSONArray errorArray = new JSONArray();
        for (int i = 0; i < Math.min(5, errors.size()); i++) errorArray.put(errors.get(i));
        out.put("errors", errorArray);
        return out;
    }

    private static String translateValue(Translator translator, Map<String, String> cache, String value, String lang, List<String> errors) {
        String cached = cache.get(value);
        if (cached != null) return cached;
        if (errors.size() >= 3) return null;
        try {
            String result = translator.translate(value, lang);
            if (result == null || result.trim().isEmpty()) return null;
            result = result.trim();
            cache.put(value, result);
            return result;
        } catch (Exception error) {
            if (errors.size() < 5) errors.add(error.getMessage() == null ? error.toString() : error.getMessage());
            return null;
        }
    }

    private static boolean isTextAttribute(Attr attr) {
        String local = attr.getLocalName() == null ? attr.getName() : attr.getLocalName();
        for (String name : TEXT_ATTRS) if (name.equalsIgnoreCase(local)) return true;
        return false;
    }

    private static boolean translatable(String value) {
        if (value == null) return false;
        String v = value.trim();
        if (v.isEmpty()) return false;
        if (v.startsWith("@") || v.startsWith("?")) return false;
        if (v.matches("-?\\d+(\\.\\d+)?") || v.matches("0x[0-9a-fA-F]+") || v.matches("#[0-9a-fA-F]{3,8}")) return false;
        if (v.equals("true") || v.equals("false") || v.equals("@null")) return false;
        if (v.matches("[\\p{Punct}\\s]+")) return false;
        return v.codePoints().anyMatch(Character::isLetter);
    }

    private static int replaceInDocument(Document document, Pattern pattern, String replacement, int[] stats) {
        int changed = 0;
        NodeList all = document.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            NamedNodeMap attrs = element.getAttributes();
            for (int a = 0; a < attrs.getLength(); a++) {
                Attr attr = (Attr) attrs.item(a);
                String value = attr.getValue();
                if (!replaceable(value)) continue;
                Matcher matcher = pattern.matcher(value);
                String next = matcher.replaceAll(Matcher.quoteReplacement(replacement));
                if (!next.equals(value)) { attr.setValue(next); changed++; }
            }
            String own = directText(element);
            if (own != null && replaceable(own)) {
                Matcher matcher = pattern.matcher(own);
                String next = matcher.replaceAll(Matcher.quoteReplacement(replacement));
                if (!next.equals(own)) { setDirectText(document, element, next); changed++; }
            }
        }
        return changed;
    }

    private static boolean replaceable(String value) {
        if (value == null) return false;
        String v = value.trim();
        if (v.isEmpty()) return false;
        return !v.startsWith("@") && !v.startsWith("?");
    }

    private static String directText(Element element) {
        StringBuilder text = new StringBuilder();
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(n.getNodeValue());
            }
        }
        return text.length() == 0 ? null : text.toString();
    }

    private static void setDirectText(Document document, Element element, String value) {
        for (Node n = element.getFirstChild(); n != null; ) {
            Node next = n.getNextSibling();
            if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) element.removeChild(n);
            n = next;
        }
        element.appendChild(document.createTextNode(value));
    }

    private static boolean contains(JSONArray array, String name) {
        for (int i = 0; i < array.length(); i++) if (name.equals(array.optString(i))) return true;
        return false;
    }

    private static byte[] read(ZipFile zip, ZipEntry entry) throws Exception {
        try (InputStream in = zip.getInputStream(entry)) { return FileOps.bytes(in, MAX_XML); }
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String firstLine(String xml) {
        int p = xml.indexOf('\n');
        return p < 0 ? xml : xml.substring(0, p);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String serialize(Document document) {
        StringBuilder out = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        serializeNode(document.getDocumentElement(), out);
        return out.toString();
    }

    private static void serializeNode(Node node, StringBuilder out) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String name = element.getTagName();
            out.append('<').append(name);
            NamedNodeMap attrs = element.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr attr = (Attr) attrs.item(i);
                out.append(' ').append(attr.getName()).append("=\"").append(escape(attr.getValue())).append('"');
            }
            if (!element.hasChildNodes()) { out.append("/>"); return; }
            out.append('>');
            for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) serializeNode(child, out);
            out.append("</").append(name).append('>');
        } else if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
            out.append(escape(node.getNodeValue()));
        }
    }
}
