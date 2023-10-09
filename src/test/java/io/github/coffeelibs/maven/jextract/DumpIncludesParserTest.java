package io.github.coffeelibs.maven.jextract;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DumpIncludesParserTest {

    @Test
    void parse() throws IOException {
        File file=new File("jextract.includes.txt");
        List<DumpIncludesParser.Include> includes=DumpIncludesParser.parse(file);
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(includes));

        Path dst= Files.createTempFile("test","txt");
        DumpIncludesParser.toFile(includes,dst.toFile());
    }

    @Test
    void parseWithFilter() throws IOException {
        File file=new File("jextract.includes.txt");
        Map<String, String[]> filters=new HashMap<>();
        filters.put(DumpIncludesParser.TYPE_TYPEDEF,new String[]{"AppIndicator"});
        List<DumpIncludesParser.Include> includes=DumpIncludesParser.parse(file,filters);
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(includes));

        Path dst= Files.createTempFile("test","txt");
        DumpIncludesParser.toFile(includes,dst.toFile());
        System.out.println(dst.toFile().getAbsolutePath());
    }
}